package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.blog.enums.ArticleStatusEnum;
import com.youlai.boot.module.blog.enums.CommentStatusEnum;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogArticleTagMapper;
import com.youlai.boot.module.blog.mapper.BlogCategoryMapper;
import com.youlai.boot.module.blog.mapper.BlogCommentMapper;
import com.youlai.boot.module.blog.mapper.BlogTagMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogArticleTag;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.entity.BlogComment;
import com.youlai.boot.module.blog.model.entity.BlogTag;
import com.youlai.boot.module.blog.model.form.BlogArticleForm;
import com.youlai.boot.module.blog.model.query.BlogArticleQuery;
import com.youlai.boot.module.blog.model.vo.BlogArchiveArticleVO;
import com.youlai.boot.module.blog.model.vo.BlogArchiveVO;
import com.youlai.boot.module.blog.model.vo.BlogArticleDetailVO;
import com.youlai.boot.module.blog.model.vo.BlogArticleNeighborVO;
import com.youlai.boot.module.blog.model.vo.BlogArticlePageVO;
import com.youlai.boot.module.blog.model.vo.BlogArticleTagRowVO;
import com.youlai.boot.module.blog.model.vo.BlogMarkdownExportVO;
import com.youlai.boot.module.blog.model.vo.BlogTagRefVO;
import com.youlai.boot.module.blog.service.BlogArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 博客文章业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl implements BlogArticleService {

    private static final long ARTICLE_DETAIL_TTL_MINUTES = 30;
    private static final long VIEW_UV_TTL_HOURS = 1;
    private static final long LIKE_UV_TTL_HOURS = 24;
    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BlogArticleMapper blogArticleMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final BlogTagMapper blogTagMapper;
    private final BlogArticleTagMapper blogArticleTagMapper;
    private final BlogCommentMapper blogCommentMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper objectMapper;

    @Override
    public Page<BlogArticlePageVO> getArticlePage(BlogArticleQuery queryParams) {
        return queryArticlePage(queryParams, false);
    }

    @Override
    public Page<BlogArticlePageVO> getPublicArticlePage(BlogArticleQuery queryParams) {
        return queryArticlePage(queryParams, true);
    }

    @Override
    public BlogArticleForm getArticleForm(Long id) {
        BlogArticle entity = blogArticleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("文章不存在");
        }
        BlogArticleForm form = new BlogArticleForm();
        form.setId(entity.getId());
        form.setTitle(entity.getTitle());
        form.setSlug(entity.getSlug());
        form.setSummary(entity.getSummary());
        form.setContent(entity.getContent());
        form.setCoverImage(entity.getCoverImage());
        form.setCategoryId(entity.getCategoryId());
        form.setStatus(entity.getStatus());
        form.setIsTop(entity.getIsTop());
        form.setAllowComment(entity.getAllowComment());
        List<Long> tagIds = blogArticleTagMapper.selectList(new LambdaQueryWrapper<BlogArticleTag>()
                        .eq(BlogArticleTag::getArticleId, id))
                .stream()
                .map(BlogArticleTag::getTagId)
                .toList();
        form.setTagIds(tagIds);
        return form;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveArticle(BlogArticleForm form) {
        String normalizedSlug = StrUtil.isBlank(form.getSlug()) ? null : form.getSlug().trim();
        ensureSlugUnique(normalizedSlug, null);
        BlogArticle entity = new BlogArticle();
        applyArticleForm(entity, form);
        entity.setViewCount(0L);
        entity.setLikeCount(0L);
        if (Objects.equals(entity.getStatus(), ArticleStatusEnum.PUBLISHED.getValue())) {
            entity.setPublishTime(LocalDateTime.now());
        }
        boolean ok = blogArticleMapper.insert(entity) > 0;
        if (ok) {
            form.setId(entity.getId());
            syncArticleTags(entity.getId(), form.getTagIds());
            evictSiteInfoCache();
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateArticle(Long id, BlogArticleForm form) {
        BlogArticle existing = blogArticleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }
        String normalizedSlug = StrUtil.isBlank(form.getSlug()) ? null : form.getSlug().trim();
        ensureSlugUnique(normalizedSlug, id);
        applyArticleForm(existing, form);
        if (Objects.equals(existing.getStatus(), ArticleStatusEnum.PUBLISHED.getValue()) && existing.getPublishTime() == null) {
            existing.setPublishTime(LocalDateTime.now());
        }
        boolean ok = blogArticleMapper.updateById(existing) > 0;
        if (ok) {
            syncArticleTags(id, form.getTagIds());
            evictArticleAndSiteCaches(List.of(id));
        }
        return ok;
    }

    @Override
    public boolean updateArticleStatus(Long id, Integer status) {
        BlogArticle existing = blogArticleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }
        Integer targetStatus = Objects.equals(status, ArticleStatusEnum.PUBLISHED.getValue())
                ? ArticleStatusEnum.PUBLISHED.getValue()
                : ArticleStatusEnum.DRAFT.getValue();
        existing.setStatus(targetStatus);
        if (Objects.equals(targetStatus, ArticleStatusEnum.PUBLISHED.getValue()) && existing.getPublishTime() == null) {
            existing.setPublishTime(LocalDateTime.now());
        }
        boolean ok = blogArticleMapper.updateById(existing) > 0;
        if (ok) {
            evictArticleAndSiteCaches(List.of(id));
        }
        return ok;
    }

    @Override
    public boolean updateArticleTop(Long id, Integer isTop) {
        BlogArticle existing = blogArticleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }
        existing.setIsTop(Objects.equals(isTop, 1) ? 1 : 0);
        boolean ok = blogArticleMapper.updateById(existing) > 0;
        if (ok) {
            evictArticleDetailCache(id);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteArticles(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }
        blogArticleTagMapper.delete(new LambdaQueryWrapper<BlogArticleTag>()
                .in(BlogArticleTag::getArticleId, ids));
        blogCommentMapper.delete(new LambdaQueryWrapper<BlogComment>()
                .in(BlogComment::getArticleId, ids));
        boolean ok = blogArticleMapper.deleteByIds(ids) > 0;
        if (ok) {
            evictArticleAndSiteCaches(ids);
        }
        return ok;
    }

    @Override
    public BlogArticleDetailVO getPublicArticleDetail(Long id, String clientIp) {
        return getPublicArticleDetail(String.valueOf(id), clientIp, true);
    }

    @Override
    public BlogArticleDetailVO getPublicArticleDetail(String idOrSlug, String clientIp, boolean trackView) {
        Long targetId = resolvePublishedArticleId(idOrSlug);
        BlogArticleDetailVO detailVO = loadCachedOrDbDetail(targetId);
        if (trackView && shouldRecordView(targetId, clientIp)) {
            blogArticleMapper.incrementViewCount(targetId);
            detailVO.setViewCount((detailVO.getViewCount() == null ? 0L : detailVO.getViewCount()) + 1L);
            cacheArticleDetail(detailVO);
            evictSiteInfoCache();
        }
        return detailVO;
    }

    private Long resolvePublishedArticleId(String idOrSlug) {
        if (StrUtil.isBlank(idOrSlug)) {
            throw new BusinessException("文章不存在或尚未发布");
        }
        String key = idOrSlug.trim();
        if (key.matches("\\d+")) {
            try {
                return Long.parseLong(key);
            } catch (NumberFormatException ignored) {
                throw new BusinessException("文章不存在或尚未发布");
            }
        }
        BlogArticle matched = blogArticleMapper.selectOne(new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId)
                .eq(BlogArticle::getSlug, key)
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())
                .last("LIMIT 1"));
        if (matched == null) {
            throw new BusinessException("文章不存在或尚未发布");
        }
        return matched.getId();
    }

    private void ensureSlugUnique(String slug, Long excludeId) {
        if (StrUtil.isBlank(slug)) {
            return;
        }
        if (slug.matches("\\d+")) {
            throw new BusinessException("文章 URL 别名不能为纯数字");
        }
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId)
                .eq(BlogArticle::getSlug, slug)
                .last("LIMIT 1");
        if (excludeId != null) {
            wrapper.ne(BlogArticle::getId, excludeId);
        }
        if (blogArticleMapper.selectOne(wrapper) != null) {
            throw new BusinessException("文章 URL 别名已被占用，请更换后再试");
        }
    }

    @Override
    public Long likeArticle(Long id, String clientIp) {
        BlogArticle entity = blogArticleMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getStatus(), ArticleStatusEnum.PUBLISHED.getValue())) {
            throw new BusinessException("文章不存在或尚未发布");
        }
        if (!acquireUvKey(RedisConstants.Blog.ARTICLE_LIKE_UV, id, clientIp, LIKE_UV_TTL_HOURS)) {
            throw new BusinessException("24小时内已点过赞啦");
        }
        blogArticleMapper.incrementLikeCount(id);
        BlogArticle updated = blogArticleMapper.selectById(id);
        evictArticleDetailCache(id);
        return updated != null && updated.getLikeCount() != null ? updated.getLikeCount() : 0L;
    }

    @Override
    public List<BlogArchiveVO> listArchives() {
        List<BlogArchiveArticleVO> rows = blogArticleMapper.listPublishedArchives();
        if (CollectionUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }
        Map<String, List<BlogArchiveArticleVO>> grouped = new LinkedHashMap<>();
        for (BlogArchiveArticleVO row : rows) {
            String key = row.getPublishTime() != null ? row.getPublishTime().format(YEAR_MONTH_FMT) : "未归档";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        return grouped.entrySet().stream()
                .map(e -> new BlogArchiveVO(e.getKey(), e.getValue()))
                .toList();
    }

    private Page<BlogArticlePageVO> queryArticlePage(BlogArticleQuery queryParams, boolean onlyPublished) {
        Page<BlogArticlePageVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<BlogArticlePageVO> result = blogArticleMapper.getArticlePage(page, queryParams, onlyPublished);
        for (BlogArticlePageVO record : result.getRecords()) {
            int words = record.getWordCount() != null ? record.getWordCount() : 0;
            record.setWordCount(words);
            record.setReadingMinutes(computeReadingMinutes(words));
        }
        attachTags(result.getRecords());
        return result;
    }

    private void attachTags(List<BlogArticlePageVO> records) {
        if (CollectionUtil.isEmpty(records)) {
            return;
        }
        List<Long> articleIds = records.stream().map(BlogArticlePageVO::getId).toList();
        List<BlogArticleTagRowVO> tagRows = blogTagMapper.listTagsByArticleIds(articleIds);
        Map<Long, List<BlogTagRefVO>> tagMap = tagRows.stream().collect(Collectors.groupingBy(
                BlogArticleTagRowVO::getArticleId,
                Collectors.mapping(row -> {
                    BlogTagRefVO ref = new BlogTagRefVO();
                    ref.setId(row.getId());
                    ref.setName(row.getName());
                    ref.setSlug(row.getSlug());
                    ref.setColor(row.getColor());
                    return ref;
                }, Collectors.toList())
        ));
        for (BlogArticlePageVO record : records) {
            record.setTags(tagMap.getOrDefault(record.getId(), Collections.emptyList()));
        }
    }

    private void applyArticleForm(BlogArticle target, BlogArticleForm form) {
        target.setTitle(form.getTitle().trim());
        target.setSlug(StrUtil.isBlank(form.getSlug()) ? null : form.getSlug().trim());
        target.setContent(form.getContent());
        target.setSummary(StrUtil.isBlank(form.getSummary())
                ? extractSummary(form.getContent())
                : form.getSummary().trim());
        target.setCoverImage(StrUtil.isBlank(form.getCoverImage()) ? null : form.getCoverImage().trim());
        target.setCategoryId(form.getCategoryId());
        target.setStatus(Objects.equals(form.getStatus(), ArticleStatusEnum.PUBLISHED.getValue())
                ? ArticleStatusEnum.PUBLISHED.getValue()
                : ArticleStatusEnum.DRAFT.getValue());
        target.setIsTop(Objects.equals(form.getIsTop(), 1) ? 1 : 0);
        target.setAllowComment(Objects.equals(form.getAllowComment(), 0) ? 0 : 1);
    }

    private String extractSummary(String markdown) {
        if (StrUtil.isBlank(markdown)) {
            return "";
        }
        String plain = markdown
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1")
                .replaceAll("[#>*_~\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return StrUtil.maxLength(plain, 160);
    }

    private void syncArticleTags(Long articleId, List<Long> tagIds) {
        blogArticleTagMapper.delete(new LambdaQueryWrapper<BlogArticleTag>()
                .eq(BlogArticleTag::getArticleId, articleId));
        if (CollectionUtil.isEmpty(tagIds)) {
            return;
        }
        tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(tagId -> blogArticleTagMapper.insert(new BlogArticleTag(articleId, tagId)));
    }

    private BlogArticleDetailVO loadCachedOrDbDetail(Long id) {
        String cacheKey = StrUtil.format(RedisConstants.Blog.ARTICLE_DETAIL, id);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cached)) {
                return objectMapper.readValue(cached, BlogArticleDetailVO.class);
            }
        } catch (Exception e) {
            log.warn("读取文章详情缓存失败, id={}: {}", id, e.getMessage());
        }

        BlogArticle entity = blogArticleMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getStatus(), ArticleStatusEnum.PUBLISHED.getValue())) {
            throw new BusinessException("文章不存在或尚未发布");
        }

        BlogArticleDetailVO detailVO = buildDetailVO(entity);
        cacheArticleDetail(detailVO);
        return detailVO;
    }

    private BlogArticleDetailVO buildDetailVO(BlogArticle entity) {
        BlogArticleDetailVO vo = new BlogArticleDetailVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setSlug(entity.getSlug());
        vo.setSummary(entity.getSummary());
        vo.setContent(entity.getContent());
        vo.setCoverImage(entity.getCoverImage());
        vo.setCategoryId(entity.getCategoryId());
        if (entity.getCategoryId() != null) {
            BlogCategory category = blogCategoryMapper.selectById(entity.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        vo.setStatus(entity.getStatus());
        vo.setIsTop(entity.getIsTop());
        vo.setAllowComment(entity.getAllowComment());
        vo.setViewCount(entity.getViewCount());
        vo.setLikeCount(entity.getLikeCount());
        vo.setCommentCount(blogCommentMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getArticleId, entity.getId())
                .eq(BlogComment::getStatus, CommentStatusEnum.APPROVED.getValue())));
        vo.setPublishTime(entity.getPublishTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        int wordCount = computeWordCount(entity.getContent());
        vo.setWordCount(wordCount);
        vo.setReadingMinutes(computeReadingMinutes(wordCount));

        List<BlogArticleTagRowVO> tagRows = blogTagMapper.listTagsByArticleIds(List.of(entity.getId()));
        vo.setTags(tagRows.stream().map(row -> {
            BlogTagRefVO ref = new BlogTagRefVO();
            ref.setId(row.getId());
            ref.setName(row.getName());
            ref.setSlug(row.getSlug());
            ref.setColor(row.getColor());
            return ref;
        }).toList());

        LocalDateTime cursorTime = entity.getPublishTime() != null ? entity.getPublishTime() : entity.getCreateTime();
        BlogArticle prev = blogArticleMapper.selectOne(new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getTitle, BlogArticle::getSlug)
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())
                .lt(BlogArticle::getPublishTime, cursorTime)
                .orderByDesc(BlogArticle::getPublishTime, BlogArticle::getId)
                .last("LIMIT 1"));
        if (prev != null) {
            vo.setPrevArticle(new BlogArticleNeighborVO(prev.getId(), prev.getTitle(), prev.getSlug()));
        }

        BlogArticle next = blogArticleMapper.selectOne(new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getTitle, BlogArticle::getSlug)
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())
                .gt(BlogArticle::getPublishTime, cursorTime)
                .orderByAsc(BlogArticle::getPublishTime, BlogArticle::getId)
                .last("LIMIT 1"));
        if (next != null) {
            vo.setNextArticle(new BlogArticleNeighborVO(next.getId(), next.getTitle(), next.getSlug()));
        }
        return vo;
    }

    private void cacheArticleDetail(BlogArticleDetailVO detailVO) {
        String cacheKey = StrUtil.format(RedisConstants.Blog.ARTICLE_DETAIL, detailVO.getId());
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(detailVO),
                    ARTICLE_DETAIL_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入文章详情缓存失败, id={}: {}", detailVO.getId(), e.getMessage());
        }
    }

    private boolean shouldRecordView(Long articleId, String clientIp) {
        return acquireUvKey(RedisConstants.Blog.ARTICLE_VIEW_UV, articleId, clientIp, VIEW_UV_TTL_HOURS);
    }

    private boolean acquireUvKey(String template, Long articleId, String clientIp, long hours) {
        String ipHash = DigestUtil.md5Hex(StrUtil.blankToDefault(clientIp, "unknown"));
        String key = StrUtil.format(template, articleId, ipHash);
        try {
            Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", hours, TimeUnit.HOURS);
            return Boolean.TRUE.equals(absent);
        } catch (Exception e) {
            log.warn("Redis UV 防重检测失败，跳过计数: {}", e.getMessage());
            return false;
        }
    }

    private void evictArticleDetailCache(Long id) {
        try {
            stringRedisTemplate.delete(StrUtil.format(RedisConstants.Blog.ARTICLE_DETAIL, id));
        } catch (Exception e) {
            log.warn("删除文章详情缓存失败, id={}: {}", id, e.getMessage());
        }
    }

    private void evictSiteInfoCache() {
        try {
            stringRedisTemplate.delete(RedisConstants.Blog.SITE_INFO);
        } catch (Exception e) {
            log.warn("删除站点信息缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public BlogMarkdownExportVO exportArticleMarkdown(Long id) {
        BlogArticle entity = blogArticleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("文章不存在");
        }
        String categoryName = null;
        if (entity.getCategoryId() != null) {
            BlogCategory category = blogCategoryMapper.selectById(entity.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        List<BlogArticleTagRowVO> tagRows = blogTagMapper.listTagsByArticleIds(List.of(id));
        StringBuilder md = new StringBuilder();
        md.append("---\n");
        md.append("title: ").append(escapeYamlString(entity.getTitle())).append('\n');
        if (StrUtil.isNotBlank(entity.getSlug())) {
            md.append("slug: ").append(escapeYamlString(entity.getSlug())).append('\n');
        }
        if (StrUtil.isNotBlank(categoryName)) {
            md.append("category: ").append(escapeYamlString(categoryName)).append('\n');
        }
        if (CollectionUtil.isNotEmpty(tagRows)) {
            String tagsLine = tagRows.stream().map(BlogArticleTagRowVO::getName).collect(Collectors.joining(", "));
            md.append("tags: [").append(tagsLine).append("]\n");
        }
        if (StrUtil.isNotBlank(entity.getSummary())) {
            md.append("summary: ").append(escapeYamlString(entity.getSummary())).append('\n');
        }
        LocalDateTime date = entity.getPublishTime() != null ? entity.getPublishTime() : entity.getCreateTime();
        if (date != null) {
            md.append("date: ").append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append('\n');
        }
        md.append("---\n\n");
        md.append(StrUtil.nullToEmpty(entity.getContent()));

        String safeBase = StrUtil.blankToDefault(entity.getSlug(), entity.getTitle())
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .trim();
        if (safeBase.isEmpty()) {
            safeBase = "article-" + id;
        }
        return new BlogMarkdownExportVO(safeBase + ".md", md.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importArticleMarkdown(String originalFilename, String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            throw new BusinessException("Markdown 文件内容不能为空");
        }
        String normalized = rawContent.replace("\r\n", "\n");
        Map<String, String> meta = new LinkedHashMap<>();
        List<String> tagNames = new ArrayList<>();
        String body = normalized;

        if (normalized.startsWith("---\n")) {
            int endIdx = normalized.indexOf("\n---\n", 4);
            if (endIdx < 0 && normalized.endsWith("\n---")) {
                endIdx = normalized.length() - 4;
            }
            if (endIdx > 0) {
                String frontMatter = normalized.substring(4, endIdx);
                body = normalized.substring(Math.min(normalized.length(), endIdx + 5)).stripLeading();
                parseFrontMatter(frontMatter, meta, tagNames);
            }
        }

        if (StrUtil.isBlank(body)) {
            throw new BusinessException("文章正文不能为空");
        }

        String title = StrUtil.trim(meta.get("title"));
        if (StrUtil.isBlank(title)) {
            title = extractFirstHeading(body);
        }
        if (StrUtil.isBlank(title) && StrUtil.isNotBlank(originalFilename)) {
            title = originalFilename.replaceAll("\\.(md|markdown|txt)$", "").trim();
        }
        if (StrUtil.isBlank(title)) {
            title = "导入文章 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        }

        BlogArticle article = new BlogArticle();
        article.setTitle(StrUtil.maxLength(title, 200));
        article.setSlug(StrUtil.isBlank(meta.get("slug")) ? null : StrUtil.maxLength(meta.get("slug").trim(), 128));
        article.setContent(body);
        String summary = StrUtil.trim(meta.get("summary"));
        article.setSummary(StrUtil.isBlank(summary) ? extractSummary(body) : StrUtil.maxLength(summary, 500));
        article.setStatus(ArticleStatusEnum.DRAFT.getValue());
        article.setIsTop(0);
        article.setAllowComment(1);
        article.setViewCount(0L);
        article.setLikeCount(0L);

        String categoryName = StrUtil.trim(meta.get("category"));
        if (StrUtil.isNotBlank(categoryName)) {
            BlogCategory category = blogCategoryMapper.selectOne(new LambdaQueryWrapper<BlogCategory>()
                    .eq(BlogCategory::getName, categoryName)
                    .last("LIMIT 1"));
            if (category != null) {
                article.setCategoryId(category.getId());
            }
        }

        blogArticleMapper.insert(article);

        if (!tagNames.isEmpty()) {
            List<Long> tagIds = new ArrayList<>();
            for (String tagName : tagNames) {
                String cleanTag = tagName.trim();
                if (cleanTag.isEmpty()) {
                    continue;
                }
                BlogTag existingTag = blogTagMapper.selectOne(new LambdaQueryWrapper<BlogTag>()
                        .eq(BlogTag::getName, cleanTag)
                        .last("LIMIT 1"));
                if (existingTag == null) {
                    BlogTag newTag = new BlogTag();
                    newTag.setName(StrUtil.maxLength(cleanTag, 64));
                    newTag.setSlug(StrUtil.maxLength(cleanTag.toLowerCase().replaceAll("\\s+", "-"), 64));
                    newTag.setColor("#0d9488");
                    blogTagMapper.insert(newTag);
                    tagIds.add(newTag.getId());
                } else {
                    tagIds.add(existingTag.getId());
                }
            }
            syncArticleTags(article.getId(), tagIds);
        }

        evictSiteInfoCache();
        return article.getId();
    }

    private int computeWordCount(String content) {
        if (StrUtil.isBlank(content)) {
            return 0;
        }
        return content.trim().length();
    }

    private int computeReadingMinutes(int wordCount) {
        return Math.max(1, (int) Math.ceil(wordCount / 300.0));
    }

    private void parseFrontMatter(String frontMatter, Map<String, String> meta, List<String> tagNames) {
        boolean readingTagList = false;
        for (String line : frontMatter.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (readingTagList && trimmed.startsWith("- ")) {
                tagNames.add(unquoteYaml(trimmed.substring(2).trim()));
                continue;
            }
            readingTagList = false;
            int colonIdx = trimmed.indexOf(':');
            if (colonIdx <= 0) {
                continue;
            }
            String key = trimmed.substring(0, colonIdx).trim().toLowerCase();
            String val = trimmed.substring(colonIdx + 1).trim();
            if ("tags".equals(key)) {
                if (val.isEmpty()) {
                    readingTagList = true;
                } else if (val.startsWith("[") && val.endsWith("]")) {
                    String inner = val.substring(1, val.length() - 1);
                    for (String item : inner.split(",")) {
                        String clean = unquoteYaml(item.trim());
                        if (!clean.isEmpty()) {
                            tagNames.add(clean);
                        }
                    }
                } else {
                    for (String item : val.split(",")) {
                        String clean = unquoteYaml(item.trim());
                        if (!clean.isEmpty()) {
                            tagNames.add(clean);
                        }
                    }
                }
            } else {
                meta.put(key, unquoteYaml(val));
            }
        }
    }

    private String unquoteYaml(String val) {
        if (val.length() >= 2 && ((val.startsWith("\"") && val.endsWith("\""))
                || (val.startsWith("'") && val.endsWith("'")))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private String escapeYamlString(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }

    private String extractFirstHeading(String markdown) {
        for (String line : markdown.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        return null;
    }

    private void evictArticleAndSiteCaches(List<Long> ids) {
        for (Long id : ids) {
            evictArticleDetailCache(id);
        }
        evictSiteInfoCache();
    }
}
