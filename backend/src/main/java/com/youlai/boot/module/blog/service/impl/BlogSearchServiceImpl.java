package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.module.blog.enums.ArticleStatusEnum;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogCategoryMapper;
import com.youlai.boot.module.blog.mapper.BlogLearningPathMapper;
import com.youlai.boot.module.blog.model.dto.MindMapNodeDTO;
import com.youlai.boot.module.blog.model.dto.MindMapResourceDTO;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.entity.BlogLearningPath;
import com.youlai.boot.module.blog.model.vo.BlogArchiveArticleVO;
import com.youlai.boot.module.blog.model.vo.BlogSearchResultVO;
import com.youlai.boot.module.blog.service.BlogSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 博客全局搜索服务实现（支持全文片段摘要、思维导图节点匹配与归档速查）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogSearchServiceImpl implements BlogSearchService {

    private static final Pattern MARKDOWN_NOISE = Pattern.compile("(?m)^#{1,6}\\s*|```[a-zA-Z0-9_-]*|`|\\*\\*|__|~~|>\\s*");
    private static final Pattern MARKDOWN_LINKS = Pattern.compile("!?\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern WHITESPACE_COLLAPSE = Pattern.compile("\\s+");

    private final BlogArticleMapper blogArticleMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final BlogLearningPathMapper blogLearningPathMapper;
    private final JsonMapper objectMapper;

    @Override
    public BlogSearchResultVO searchPublic(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        String kw = StrUtil.trimToEmpty(query);
        String kwLower = kw.toLowerCase(Locale.ROOT);

        Map<Long, String> categoryNames = loadCategoryNameMap();

        BlogSearchResultVO result = new BlogSearchResultVO();
        result.setArticles(searchArticles(kw, kwLower, safeLimit, categoryNames));
        result.setPaths(searchPaths(kwLower, safeLimit));
        result.setArchives(searchArchives(kwLower, safeLimit));
        return result;
    }

    private Map<Long, String> loadCategoryNameMap() {
        List<BlogCategory> categories = blogCategoryMapper.selectList(
                new LambdaQueryWrapper<BlogCategory>().select(BlogCategory::getId, BlogCategory::getName));
        Map<Long, String> map = new LinkedHashMap<>();
        for (BlogCategory c : categories) {
            if (c.getId() != null && c.getName() != null) {
                map.put(c.getId(), c.getName());
            }
        }
        return map;
    }

    private List<BlogSearchResultVO.ArticleHit> searchArticles(
            String kw,
            String kwLower,
            int limit,
            Map<Long, String> categoryNames
    ) {
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getSlug, BlogArticle::getTitle,
                        BlogArticle::getSummary, BlogArticle::getContent, BlogArticle::getCategoryId,
                        BlogArticle::getPublishTime, BlogArticle::getCreateTime)
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())
                .and(StrUtil.isNotBlank(kw), q -> q
                        .like(BlogArticle::getTitle, kw)
                        .or()
                        .like(BlogArticle::getSummary, kw)
                        .or()
                        .like(BlogArticle::getContent, kw))
                .orderByDesc(BlogArticle::getIsTop)
                .orderByDesc(BlogArticle::getPublishTime)
                .orderByDesc(BlogArticle::getId)
                .last("LIMIT " + limit);

        List<BlogArticle> rows = blogArticleMapper.selectList(wrapper);
        List<BlogSearchResultVO.ArticleHit> hits = new ArrayList<>();
        for (BlogArticle art : rows) {
            BlogSearchResultVO.ArticleHit hit = new BlogSearchResultVO.ArticleHit();
            hit.setId(art.getId());
            hit.setSlug(art.getSlug());
            hit.setTitle(art.getTitle());
            hit.setCategoryName(art.getCategoryId() != null ? categoryNames.get(art.getCategoryId()) : null);
            hit.setPublishTime(art.getPublishTime() != null ? art.getPublishTime() : art.getCreateTime());
            hit.setSnippet(extractSnippet(art.getSummary(), art.getContent(), kw, kwLower));
            hits.add(hit);
        }
        return hits;
    }

    private List<BlogSearchResultVO.PathHit> searchPaths(String kwLower, int limit) {
        List<BlogLearningPath> allPaths = blogLearningPathMapper.selectList(
                new LambdaQueryWrapper<BlogLearningPath>()
                        .eq(BlogLearningPath::getStatus, 1)
                        .orderByAsc(BlogLearningPath::getSort)
                        .orderByDesc(BlogLearningPath::getId));

        List<BlogSearchResultVO.PathHit> hits = new ArrayList<>();
        for (BlogLearningPath path : allPaths) {
            if (hits.size() >= limit) {
                break;
            }
            MindMapNodeDTO matchedNode = null;
            if (StrUtil.isNotBlank(kwLower)) {
                matchedNode = findBestMatchingNode(parseTreeSafe(path.getTreeJson()), kwLower);
                boolean metaMatched = StrUtil.containsIgnoreCase(path.getTitle(), kwLower)
                        || StrUtil.containsIgnoreCase(path.getSummary(), kwLower);
                if (!metaMatched && matchedNode == null) {
                    continue;
                }
            }
            BlogSearchResultVO.PathHit hit = new BlogSearchResultVO.PathHit();
            hit.setId(path.getId());
            hit.setSlug(path.getSlug());
            hit.setTitle(path.getTitle());
            hit.setSummary(path.getSummary());
            if (matchedNode != null) {
                hit.setMatchedNodeId(matchedNode.getId());
                hit.setMatchedNodeLabel(matchedNode.getLabel());
            }
            hits.add(hit);
        }
        return hits;
    }

    private List<BlogSearchResultVO.ArchiveHit> searchArchives(String kwLower, int limit) {
        List<BlogArchiveArticleVO> rows = blogArticleMapper.listPublishedArchives();
        Map<String, Integer> monthCounts = new LinkedHashMap<>();
        for (BlogArchiveArticleVO row : rows) {
            if (row.getPublishTime() == null) {
                continue;
            }
            String ym = row.getPublishTime().toLocalDate().toString().substring(0, 7);
            monthCounts.merge(ym, 1, Integer::sum);
        }
        List<BlogSearchResultVO.ArchiveHit> hits = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : monthCounts.entrySet()) {
            if (hits.size() >= limit) {
                break;
            }
            String ym = entry.getKey();
            if (StrUtil.isNotBlank(kwLower) && !ym.toLowerCase(Locale.ROOT).contains(kwLower)) {
                continue;
            }
            BlogSearchResultVO.ArchiveHit hit = new BlogSearchResultVO.ArchiveHit();
            hit.setYearMonth(ym);
            hit.setCount(entry.getValue());
            hits.add(hit);
        }
        return hits;
    }

    private MindMapNodeDTO parseTreeSafe(String treeJson) {
        if (StrUtil.isBlank(treeJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(treeJson, MindMapNodeDTO.class);
        } catch (Exception e) {
            log.debug("解析学习路径 JSON 失败，忽略导图节点检索: {}", e.getMessage());
            return null;
        }
    }

    private MindMapNodeDTO findBestMatchingNode(MindMapNodeDTO root, String kwLower) {
        if (root == null) {
            return null;
        }
        MindMapNodeDTO best = null;
        int bestScore = 0;
        List<MindMapNodeDTO> queue = new ArrayList<>();
        queue.add(root);
        for (int i = 0; i < queue.size(); i++) {
            MindMapNodeDTO node = queue.get(i);
            int score = scoreNodeMatch(node, kwLower);
            if (score > bestScore) {
                bestScore = score;
                best = node;
            }
            if (node.getChildren() != null) {
                queue.addAll(node.getChildren());
            }
        }
        return best;
    }

    private int scoreNodeMatch(MindMapNodeDTO node, String kwLower) {
        int score = 0;
        if (containsIgnoreCase(node.getLabel(), kwLower)) {
            score += 100;
        }
        if (containsIgnoreCase(node.getTag(), kwLower)) {
            score += 45;
        }
        if (node.getResources() != null) {
            for (MindMapResourceDTO res : node.getResources()) {
                if (res != null) {
                    if (containsIgnoreCase(res.getTitle(), kwLower)) {
                        score += 30;
                    }
                    if (containsIgnoreCase(res.getNote(), kwLower)) {
                        score += 10;
                    }
                }
            }
        }
        if (containsIgnoreCase(node.getDescription(), kwLower)) {
            score += 10;
        }
        return score;
    }

    private boolean containsIgnoreCase(String source, String kwLower) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(kwLower);
    }

    /**
     * 提取去除 Markdown 噪音后的命中上下文片段
     */
    String extractSnippet(String summary, String content, String kw, String kwLower) {
        String cleanContent = cleanMarkdown(content);
        if (StrUtil.isNotBlank(kw) && StrUtil.isNotBlank(cleanContent)) {
            int idx = cleanContent.toLowerCase(Locale.ROOT).indexOf(kwLower);
            if (idx >= 0) {
                int start = Math.max(0, idx - 34);
                int end = Math.min(cleanContent.length(), idx + kw.length() + 76);
                String prefix = start > 0 ? "..." : "";
                String suffix = end < cleanContent.length() ? "..." : "";
                return prefix + cleanContent.substring(start, end) + suffix;
            }
        }
        String fallback = StrUtil.isNotBlank(summary) ? cleanMarkdown(summary) : cleanContent;
        if (StrUtil.isBlank(fallback)) {
            return "";
        }
        return fallback.length() > 110 ? fallback.substring(0, 110) + "..." : fallback;
    }

    private String cleanMarkdown(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String text = MARKDOWN_LINKS.matcher(raw).replaceAll("$1");
        text = MARKDOWN_NOISE.matcher(text).replaceAll(" ");
        return WHITESPACE_COLLAPSE.matcher(text).replaceAll(" ").trim();
    }
}
