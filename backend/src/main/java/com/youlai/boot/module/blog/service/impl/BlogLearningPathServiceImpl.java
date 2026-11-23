package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.blog.mapper.BlogLearningPathMapper;
import com.youlai.boot.module.blog.model.dto.MindMapNodeDTO;
import com.youlai.boot.module.blog.model.dto.MindMapResourceDTO;
import com.youlai.boot.module.blog.model.entity.BlogLearningPath;
import com.youlai.boot.module.blog.model.form.BlogLearningPathForm;
import com.youlai.boot.module.blog.model.query.BlogLearningPathQuery;
import com.youlai.boot.module.blog.model.vo.BlogLearningPathVO;
import com.youlai.boot.module.blog.service.BlogLearningPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 博客学习路径与思维导图业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogLearningPathServiceImpl implements BlogLearningPathService {

    private static final long PATHS_CACHE_TTL_MINUTES = 10;
    private static final int MAX_TREE_DEPTH = 16;
    private static final Set<String> ALLOWED_RESOURCE_TYPES = Set.of("article", "video", "webpage");

    private final BlogLearningPathMapper blogLearningPathMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper objectMapper;

    private record TreeStats(int nodeCount, int resourceCount) {}

    private record CachedPathList(List<BlogLearningPathVO> items) {}

    @Override
    public Page<BlogLearningPathVO> getPathPage(BlogLearningPathQuery queryParams) {
        LambdaQueryWrapper<BlogLearningPath> wrapper = new LambdaQueryWrapper<BlogLearningPath>()
                .eq(queryParams.getStatus() != null, BlogLearningPath::getStatus, queryParams.getStatus())
                .and(StrUtil.isNotBlank(queryParams.getKeywords()), q -> q
                        .like(BlogLearningPath::getTitle, queryParams.getKeywords().trim())
                        .or()
                        .like(BlogLearningPath::getSlug, queryParams.getKeywords().trim())
                        .or()
                        .like(BlogLearningPath::getSummary, queryParams.getKeywords().trim()))
                .orderByAsc(BlogLearningPath::getSort)
                .orderByDesc(BlogLearningPath::getId);
        Page<BlogLearningPath> page = blogLearningPathMapper.selectPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        Page<BlogLearningPathVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public List<BlogLearningPathVO> listPublicPaths() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.Blog.LEARNING_PATHS);
            if (StrUtil.isNotBlank(cached)) {
                CachedPathList list = objectMapper.readValue(cached, CachedPathList.class);
                if (list != null && list.items() != null) {
                    return list.items();
                }
            }
        } catch (Exception e) {
            log.warn("读取学习路径缓存失败，降级查询数据库: {}", e.getMessage());
        }

        List<BlogLearningPath> entities = blogLearningPathMapper.selectList(new LambdaQueryWrapper<BlogLearningPath>()
                .eq(BlogLearningPath::getStatus, 1)
                .orderByAsc(BlogLearningPath::getSort)
                .orderByDesc(BlogLearningPath::getId));
        List<BlogLearningPathVO> result = entities.stream().map(this::toVO).toList();

        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.Blog.LEARNING_PATHS,
                    objectMapper.writeValueAsString(new CachedPathList(result)),
                    PATHS_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入学习路径缓存失败: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public BlogLearningPathVO getPublicPath(String idOrSlug) {
        if (StrUtil.isBlank(idOrSlug)) {
            throw new BusinessException("学习路径不存在");
        }
        String key = idOrSlug.trim();
        BlogLearningPath entity;
        if (key.matches("\\d+")) {
            long id;
            try {
                id = Long.parseLong(key);
            } catch (NumberFormatException ignored) {
                throw new BusinessException("学习路径不存在");
            }
            entity = blogLearningPathMapper.selectById(id);
        } else {
            entity = blogLearningPathMapper.selectOne(new LambdaQueryWrapper<BlogLearningPath>()
                    .eq(BlogLearningPath::getSlug, key)
                    .eq(BlogLearningPath::getStatus, 1)
                    .last("LIMIT 1"));
        }
        if (entity == null || !Objects.equals(entity.getStatus(), 1)) {
            throw new BusinessException("学习路径不存在或尚未发布");
        }
        return toVO(entity);
    }

    @Override
    public BlogLearningPathForm getPathForm(Long id) {
        BlogLearningPath entity = blogLearningPathMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("学习路径不存在");
        }
        BlogLearningPathForm form = new BlogLearningPathForm();
        form.setId(entity.getId());
        form.setTitle(entity.getTitle());
        form.setSlug(entity.getSlug());
        form.setSummary(entity.getSummary());
        form.setCoverColor(entity.getCoverColor());
        form.setTreeJson(entity.getTreeJson());
        form.setSort(entity.getSort());
        form.setStatus(entity.getStatus());
        return form;
    }

    @Override
    public Long savePath(BlogLearningPathForm form) {
        String normalizedSlug = StrUtil.isBlank(form.getSlug()) ? null : form.getSlug().trim();
        ensureSlugUnique(normalizedSlug, null);

        BlogLearningPath entity = new BlogLearningPath();
        applyForm(entity, form, normalizedSlug);
        blogLearningPathMapper.insert(entity);
        evictPathsCache();
        return entity.getId();
    }

    @Override
    public boolean updatePath(Long id, BlogLearningPathForm form) {
        BlogLearningPath existing = blogLearningPathMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学习路径不存在");
        }
        String normalizedSlug = StrUtil.isBlank(form.getSlug()) ? null : form.getSlug().trim();
        ensureSlugUnique(normalizedSlug, id);

        applyForm(existing, form, normalizedSlug);
        boolean ok = blogLearningPathMapper.updateById(existing) > 0;
        if (ok) {
            evictPathsCache();
        }
        return ok;
    }

    @Override
    public boolean deletePaths(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }
        boolean ok = blogLearningPathMapper.deleteByIds(ids) > 0;
        if (ok) {
            evictPathsCache();
        }
        return ok;
    }

    private void ensureSlugUnique(String slug, Long excludeId) {
        if (StrUtil.isBlank(slug)) {
            return;
        }
        if (slug.matches("\\d+")) {
            throw new BusinessException("路径 URL 别名不能为纯数字");
        }
        LambdaQueryWrapper<BlogLearningPath> wrapper = new LambdaQueryWrapper<BlogLearningPath>()
                .select(BlogLearningPath::getId)
                .eq(BlogLearningPath::getSlug, slug)
                .last("LIMIT 1");
        if (excludeId != null) {
            wrapper.ne(BlogLearningPath::getId, excludeId);
        }
        if (blogLearningPathMapper.selectOne(wrapper) != null) {
            throw new BusinessException("路径 URL 别名已被占用，请更换后再试");
        }
    }

    private void applyForm(BlogLearningPath target, BlogLearningPathForm form, String normalizedSlug) {
        String normalizedTreeJson = normalizeAndValidateTreeJson(form.getTreeJson());
        target.setTitle(form.getTitle().trim());
        target.setSlug(normalizedSlug);
        target.setSummary(StrUtil.isBlank(form.getSummary()) ? null : form.getSummary().trim());
        target.setCoverColor(StrUtil.isBlank(form.getCoverColor()) ? "#0d9488" : form.getCoverColor().trim());
        target.setTreeJson(normalizedTreeJson);
        target.setSort(form.getSort() != null ? form.getSort() : 0);
        target.setStatus(Objects.equals(form.getStatus(), 0) ? 0 : 1);
    }

    private String normalizeAndValidateTreeJson(String rawTreeJson) {
        if (StrUtil.isBlank(rawTreeJson)) {
            throw new BusinessException("思维导图节点数据不能为空");
        }
        MindMapNodeDTO root;
        try {
            root = objectMapper.readValue(rawTreeJson, MindMapNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException("思维导图 JSON 格式无效");
        }
        if (root == null) {
            throw new BusinessException("思维导图根节点不能为空");
        }
        normalizeNode(root, 0);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BusinessException("序列化思维导图数据失败");
        }
    }

    private void normalizeNode(MindMapNodeDTO node, int depth) {
        if (depth > MAX_TREE_DEPTH) {
            throw new BusinessException("思维导图层级不能超过 " + MAX_TREE_DEPTH + " 层");
        }
        if (StrUtil.isBlank(node.getId())) {
            node.setId("node-" + IdUtil.fastSimpleUUID().substring(0, 8));
        } else {
            node.setId(node.getId().trim());
        }
        if (StrUtil.isBlank(node.getLabel())) {
            throw new BusinessException("存在标题为空的思维导图节点");
        }
        node.setLabel(StrUtil.maxLength(node.getLabel().trim(), 100));
        node.setDescription(StrUtil.isBlank(node.getDescription()) ? null : StrUtil.maxLength(node.getDescription().trim(), 500));
        node.setTag(StrUtil.isBlank(node.getTag()) ? null : StrUtil.maxLength(node.getTag().trim(), 32));
        node.setCollapsed(Boolean.TRUE.equals(node.getCollapsed()));

        List<MindMapResourceDTO> safeResources = new ArrayList<>();
        if (node.getResources() != null) {
            for (MindMapResourceDTO res : node.getResources()) {
                if (res == null) {
                    continue;
                }
                safeResources.add(normalizeResource(res));
            }
        }
        node.setResources(safeResources);

        List<MindMapNodeDTO> safeChildren = new ArrayList<>();
        if (node.getChildren() != null) {
            for (MindMapNodeDTO child : node.getChildren()) {
                if (child == null) {
                    continue;
                }
                normalizeNode(child, depth + 1);
                safeChildren.add(child);
            }
        }
        node.setChildren(safeChildren);
    }

    private MindMapResourceDTO normalizeResource(MindMapResourceDTO res) {
        String type = StrUtil.trim(res.getType()).toLowerCase();
        if (!ALLOWED_RESOURCE_TYPES.contains(type)) {
            throw new BusinessException("不支持的挂载资源类型：" + type);
        }
        if (StrUtil.isBlank(res.getTitle())) {
            throw new BusinessException("挂载资源标题不能为空");
        }
        MindMapResourceDTO copy = new MindMapResourceDTO();
        copy.setId(StrUtil.isBlank(res.getId()) ? "res-" + IdUtil.fastSimpleUUID().substring(0, 8) : res.getId().trim());
        copy.setType(type);
        copy.setTitle(StrUtil.maxLength(res.getTitle().trim(), 160));
        copy.setNote(StrUtil.isBlank(res.getNote()) ? null : StrUtil.maxLength(res.getNote().trim(), 300));

        if ("article".equals(type)) {
            String articleId = StrUtil.trimToNull(res.getArticleId());
            String articleSlug = StrUtil.trimToNull(res.getArticleSlug());
            if (articleId == null && articleSlug == null) {
                throw new BusinessException("文章类资源必须选择对应的文章");
            }
            copy.setArticleId(articleId);
            copy.setArticleSlug(articleSlug);
        } else {
            String safeUrl = normalizeSafeResourceUrl(res.getUrl(), "video".equals(type));
            if (safeUrl == null) {
                throw new BusinessException("资源地址无效，请输入合法的 http(s):// 链接");
            }
            copy.setUrl(safeUrl);
        }
        return copy;
    }

    private String normalizeSafeResourceUrl(String rawUrl, boolean allowLocalUpload) {
        if (StrUtil.isBlank(rawUrl)) {
            return null;
        }
        String trimmed = rawUrl.trim();
        String lower = trimmed.toLowerCase();
        if (allowLocalUpload && trimmed.startsWith("/uploads/")) {
            return trimmed;
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.contains(":")) {
            return null;
        }
        return "https://" + trimmed;
    }

    private BlogLearningPathVO toVO(BlogLearningPath entity) {
        BlogLearningPathVO vo = new BlogLearningPathVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setSlug(entity.getSlug());
        vo.setSummary(entity.getSummary());
        vo.setCoverColor(StrUtil.blankToDefault(entity.getCoverColor(), "#0d9488"));
        vo.setTreeJson(entity.getTreeJson());
        TreeStats stats = computeStats(entity.getTreeJson());
        vo.setNodeCount(stats.nodeCount());
        vo.setResourceCount(stats.resourceCount());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private TreeStats computeStats(String treeJson) {
        if (StrUtil.isBlank(treeJson)) {
            return new TreeStats(0, 0);
        }
        try {
            MindMapNodeDTO root = objectMapper.readValue(treeJson, MindMapNodeDTO.class);
            if (root == null) {
                return new TreeStats(0, 0);
            }
            return countRecursive(root, 0);
        } catch (Exception e) {
            return new TreeStats(0, 0);
        }
    }

    private TreeStats countRecursive(MindMapNodeDTO node, int depth) {
        if (node == null || depth > MAX_TREE_DEPTH) {
            return new TreeStats(0, 0);
        }
        int nodes = 1;
        int resources = node.getResources() != null ? node.getResources().size() : 0;
        if (node.getChildren() != null) {
            for (MindMapNodeDTO child : node.getChildren()) {
                TreeStats sub = countRecursive(child, depth + 1);
                nodes += sub.nodeCount();
                resources += sub.resourceCount();
            }
        }
        return new TreeStats(nodes, resources);
    }

    private void evictPathsCache() {
        try {
            stringRedisTemplate.delete(RedisConstants.Blog.LEARNING_PATHS);
        } catch (Exception e) {
            log.warn("清理学习路径缓存失败: {}", e.getMessage());
        }
    }
}
