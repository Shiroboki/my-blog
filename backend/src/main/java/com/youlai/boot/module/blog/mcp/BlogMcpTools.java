package com.youlai.boot.module.blog.mcp;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.context.McpExecutionContext;
import com.youlai.boot.framework.security.model.McpTokenPrincipal;
import com.youlai.boot.module.blog.model.dto.MindMapNodeDTO;
import com.youlai.boot.module.blog.model.dto.MindMapResourceDTO;
import com.youlai.boot.module.blog.model.form.BlogArticleForm;
import com.youlai.boot.module.blog.model.form.BlogLearningPathForm;
import com.youlai.boot.module.blog.model.query.BlogArticleQuery;
import com.youlai.boot.module.blog.model.vo.BlogArticleDetailVO;
import com.youlai.boot.module.blog.model.vo.BlogArticlePageVO;
import com.youlai.boot.module.blog.model.vo.BlogLearningPathVO;
import com.youlai.boot.module.blog.service.BlogArticleService;
import com.youlai.boot.module.blog.service.BlogCategoryService;
import com.youlai.boot.module.blog.service.BlogConfigService;
import com.youlai.boot.module.blog.service.BlogDashboardService;
import com.youlai.boot.module.blog.service.BlogLearningPathService;
import com.youlai.boot.module.blog.service.BlogTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MyBlog MCP Tools 与 Resources 暴露组件（由 Spring AI 2.0.2 自动扫描注册）
 */
@Component
@RequiredArgsConstructor
public class BlogMcpTools {

    private final BlogArticleService blogArticleService;
    private final BlogCategoryService blogCategoryService;
    private final BlogTagService blogTagService;
    private final BlogLearningPathService blogLearningPathService;
    private final BlogConfigService blogConfigService;
    private final BlogDashboardService blogDashboardService;
    private final JsonMapper objectMapper;

    @McpTool(
            name = "blog_search_articles",
            description = "按关键字、分类或标签分页检索博客文章元数据（不包含正文），适合先筛选定位文章再调用 blog_get_article 查看全文"
    )
    public Map<String, Object> searchArticles(
            @McpToolParam(required = false, description = "搜索关键字（匹配标题、摘要与正文）") String keywords,
            @McpToolParam(required = false, description = "分类 ID") Long categoryId,
            @McpToolParam(required = false, description = "标签 ID") Long tagId,
            @McpToolParam(required = false, description = "是否包含草稿（需 write 作用域权限，默认 false）") Boolean includeDrafts,
            @McpToolParam(required = false, description = "页码，默认 1") Integer pageNum,
            @McpToolParam(required = false, description = "每页条数，默认 10，最大 30") Integer pageSize
    ) {
        McpExecutionContext.requireScope("read");
        boolean withDrafts = Boolean.TRUE.equals(includeDrafts);
        if (withDrafts) {
            McpExecutionContext.requireScope("write");
        }

        BlogArticleQuery query = new BlogArticleQuery();
        query.setKeywords(StrUtil.trimToNull(keywords));
        query.setCategoryId(categoryId);
        query.setTagId(tagId);
        query.setPageNum(pageNum != null && pageNum > 0 ? pageNum : 1);
        query.setPageSize(pageSize != null && pageSize > 0 ? Math.min(pageSize, 30) : 10);

        Page<BlogArticlePageVO> page = withDrafts
                ? blogArticleService.getArticlePage(query)
                : blogArticleService.getPublicArticlePage(query);

        List<Map<String, Object>> items = page.getRecords().stream().map(art -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(art.getId()));
            item.put("slug", art.getSlug());
            item.put("title", art.getTitle());
            item.put("summary", art.getSummary());
            item.put("categoryName", art.getCategoryName());
            item.put("tags", art.getTags());
            item.put("status", art.getStatus());
            item.put("viewCount", art.getViewCount());
            item.put("publishTime", art.getPublishTime());
            String routeKey = StrUtil.isNotBlank(art.getSlug()) ? art.getSlug() : String.valueOf(art.getId());
            item.put("url", "/article/" + routeKey);
            return item;
        }).toList();

        return Map.of(
                "total", page.getTotal(),
                "pageNum", page.getCurrent(),
                "pageSize", page.getSize(),
                "items", items
        );
    }

    @McpTool(
            name = "blog_get_article",
            description = "读取单篇文章的完整 Markdown 正文与元信息；支持传入数字 ID 或 URL Slug，不会虚增阅读量"
    )
    public Map<String, Object> getArticle(
            @McpToolParam(required = true, description = "文章 ID 或 URL 别名（slug）") String idOrSlug
    ) {
        McpExecutionContext.requireScope("read");
        if (StrUtil.isBlank(idOrSlug)) {
            throw new BusinessException("文章 ID 或 Slug 不能为空");
        }
        String key = idOrSlug.trim();
        McpTokenPrincipal principal = McpExecutionContext.get();
        boolean canReadDraft = principal != null && principal.hasScope("write");

        try {
            BlogArticleDetailVO detail = blogArticleService.getPublicArticleDetail(key, "127.0.0.1", false);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", String.valueOf(detail.getId()));
            res.put("slug", detail.getSlug());
            res.put("title", detail.getTitle());
            res.put("summary", detail.getSummary());
            res.put("categoryName", detail.getCategoryName());
            res.put("tags", detail.getTags());
            res.put("wordCount", detail.getWordCount());
            res.put("readingMinutes", detail.getReadingMinutes());
            res.put("publishTime", detail.getPublishTime());
            res.put("status", 1);
            String routeKey = StrUtil.isNotBlank(detail.getSlug()) ? detail.getSlug() : String.valueOf(detail.getId());
            res.put("url", "/article/" + routeKey);
            res.put("content", detail.getContent());
            return res;
        } catch (BusinessException ex) {
            if (canReadDraft && key.matches("\\d+")) {
                BlogArticleForm draft = blogArticleService.getArticleForm(Long.valueOf(key));
                Map<String, Object> res = new LinkedHashMap<>();
                res.put("id", String.valueOf(draft.getId()));
                res.put("slug", draft.getSlug());
                res.put("title", draft.getTitle());
                res.put("summary", draft.getSummary());
                res.put("categoryId", draft.getCategoryId());
                res.put("tagIds", draft.getTagIds());
                res.put("status", draft.getStatus());
                res.put("content", draft.getContent());
                return res;
            }
            throw ex;
        }
    }

    @McpTool(
            name = "blog_list_taxonomy",
            description = "列出站点全部分类与标签（含各自 ID、Slug 与文章数），方便发文章前复用既有标签与分类"
    )
    public Map<String, Object> listTaxonomy() {
        McpExecutionContext.requireScope("read");
        return Map.of(
                "categories", blogCategoryService.listPublicCategories(),
                "tags", blogTagService.listPublicTags()
        );
    }

    @McpTool(
            name = "blog_get_learning_paths",
            description = "获取全部已发布学习路径（思维导图）目录及节点/资源统计"
    )
    public List<Map<String, Object>> getLearningPaths() {
        McpExecutionContext.requireScope("read");
        return blogLearningPathService.listPublicPaths().stream().map(path -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(path.getId()));
            item.put("slug", path.getSlug());
            item.put("title", path.getTitle());
            item.put("summary", path.getSummary());
            item.put("coverColor", path.getCoverColor());
            item.put("nodeCount", path.getNodeCount());
            item.put("resourceCount", path.getResourceCount());
            String routeKey = StrUtil.isNotBlank(path.getSlug()) ? path.getSlug() : String.valueOf(path.getId());
            item.put("url", "/paths/" + routeKey);
            return item;
        }).toList();
    }

    @McpTool(
            name = "blog_get_learning_path_detail",
            description = "读取单条学习路径的完整思维导图树结构（节点 ID、层级与已挂载文章/视频/网页清单）"
    )
    public Map<String, Object> getLearningPathDetail(
            @McpToolParam(required = true, description = "学习路径 ID 或 URL 别名（slug）") String idOrSlug
    ) {
        McpExecutionContext.requireScope("read");
        BlogLearningPathVO vo = blogLearningPathService.getPublicPath(idOrSlug);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(vo.getId()));
        result.put("slug", vo.getSlug());
        result.put("title", vo.getTitle());
        result.put("summary", vo.getSummary());
        result.put("coverColor", vo.getCoverColor());
        result.put("nodeCount", vo.getNodeCount());
        result.put("resourceCount", vo.getResourceCount());
        result.put("tree", parseTree(vo.getTreeJson()));
        return result;
    }

    @McpTool(
            name = "blog_get_site_overview",
            description = "获取博客站点基础配置与统计概览；持有 write 权限时额外返回草稿与待审核评论数"
    )
    public Map<String, Object> getSiteOverview() {
        McpExecutionContext.requireScope("read");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("siteInfo", blogConfigService.getSiteInfo());
        McpTokenPrincipal principal = McpExecutionContext.get();
        if (principal != null && principal.hasScope("write")) {
            result.put("dashboard", blogDashboardService.getDashboardStats());
        }
        return result;
    }

    @McpTool(
            name = "blog_create_article",
            description = "创建博客文章。出于安全考虑默认存为草稿（status=0）；仅当后台开启 mcp_allow_publish 且传入 publish=true 时才直接公开发布"
    )
    public Map<String, Object> createArticle(
            @McpToolParam(required = true, description = "文章标题（不超过200字）") String title,
            @McpToolParam(required = true, description = "文章 Markdown 正文") String content,
            @McpToolParam(required = false, description = "文章摘要（不超过500字）") String summary,
            @McpToolParam(required = false, description = "URL 别名（不可纯数字且全局唯一）") String slug,
            @McpToolParam(required = false, description = "分类 ID") Long categoryId,
            @McpToolParam(required = false, description = "标签 ID 列表") List<Long> tagIds,
            @McpToolParam(required = false, description = "是否尝试立即发布，默认 false（存草稿）") Boolean publish,
            @McpToolParam(required = false, description = "是否置顶，默认 false") Boolean isTop,
            @McpToolParam(required = false, description = "是否允许评论，默认 true") Boolean allowComment
    ) {
        McpExecutionContext.requireScope("write");
        boolean allowDirectPublish = "1".equals(blogConfigService.getConfigValue("mcp_allow_publish", "0"));
        boolean wantsPublish = Boolean.TRUE.equals(publish);
        int targetStatus = (wantsPublish && allowDirectPublish) ? 1 : 0;

        BlogArticleForm form = new BlogArticleForm();
        form.setTitle(title);
        form.setContent(content);
        form.setSummary(summary);
        form.setSlug(slug);
        form.setCategoryId(categoryId);
        form.setTagIds(tagIds);
        form.setStatus(targetStatus);
        form.setIsTop(Boolean.TRUE.equals(isTop) ? 1 : 0);
        form.setAllowComment(Boolean.FALSE.equals(allowComment) ? 0 : 1);

        blogArticleService.saveArticle(form);
        String routeKey = StrUtil.isNotBlank(form.getSlug()) ? form.getSlug().trim() : String.valueOf(form.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", form.getId() != null ? String.valueOf(form.getId()) : null);
        res.put("slug", form.getSlug());
        res.put("status", targetStatus);
        res.put("statusLabel", targetStatus == 1 ? "已发布" : "草稿");
        res.put("url", targetStatus == 1 ? "/article/" + routeKey : "/admin/#/blog/article");
        if (wantsPublish && !allowDirectPublish) {
            res.put("notice", "后台未开启 MCP 直接发布开关（mcp_allow_publish=0），文章已安全保存为草稿");
        }
        return res;
    }

    @McpTool(
            name = "blog_update_article",
            description = "增量更新指定 ID 的博客文章字段（仅覆盖传入的非空字段），正文与元数据变更会自动失效缓存"
    )
    public Map<String, Object> updateArticle(
            @McpToolParam(required = true, description = "要修改的文章 ID") Long id,
            @McpToolParam(required = false, description = "新标题") String title,
            @McpToolParam(required = false, description = "新 Markdown 正文") String content,
            @McpToolParam(required = false, description = "新摘要") String summary,
            @McpToolParam(required = false, description = "新 URL 别名") String slug,
            @McpToolParam(required = false, description = "新分类 ID") Long categoryId,
            @McpToolParam(required = false, description = "新标签 ID 列表") List<Long> tagIds,
            @McpToolParam(required = false, description = "目标状态：true 尝试发布，false 退回草稿，不传保留原状态") Boolean publish
    ) {
        McpExecutionContext.requireScope("write");
        BlogArticleForm form = blogArticleService.getArticleForm(id);
        if (StrUtil.isNotBlank(title)) {
            form.setTitle(title.trim());
        }
        if (StrUtil.isNotBlank(content)) {
            form.setContent(content);
        }
        if (summary != null) {
            form.setSummary(summary);
        }
        if (slug != null) {
            form.setSlug(slug);
        }
        if (categoryId != null) {
            form.setCategoryId(categoryId);
        }
        if (tagIds != null) {
            form.setTagIds(tagIds);
        }

        String notice = null;
        if (publish != null) {
            boolean allowDirectPublish = "1".equals(blogConfigService.getConfigValue("mcp_allow_publish", "0"));
            if (Boolean.TRUE.equals(publish)) {
                if (allowDirectPublish) {
                    form.setStatus(1);
                } else {
                    form.setStatus(0);
                    notice = "后台未开启 MCP 直接发布开关（mcp_allow_publish=0），文章保留草稿状态";
                }
            } else {
                form.setStatus(0);
            }
        }

        blogArticleService.updateArticle(id, form);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", String.valueOf(id));
        res.put("status", form.getStatus());
        res.put("updated", true);
        if (notice != null) {
            res.put("notice", notice);
        }
        return res;
    }

    @McpTool(
            name = "blog_upsert_learning_path",
            description = "新建或整体更新一条学习路径及其思维导图 treeJson（受服务端树深度上限与安全 URL 校验保护）"
    )
    public Map<String, Object> upsertLearningPath(
            @McpToolParam(required = false, description = "路径 ID；留空为新建，传入为更新") Long id,
            @McpToolParam(required = true, description = "学习路径标题") String title,
            @McpToolParam(required = false, description = "URL 别名（如 go-microservices）") String slug,
            @McpToolParam(required = false, description = "路径简介") String summary,
            @McpToolParam(required = false, description = "主题色十六进制代码，默认 #0d9488") String coverColor,
            @McpToolParam(required = true, description = "完整 MindMapNodeDTO 树 JSON 字符串") String treeJson
    ) {
        McpExecutionContext.requireScope("write");
        BlogLearningPathForm form = new BlogLearningPathForm();
        form.setId(id);
        form.setTitle(title);
        form.setSlug(slug);
        form.setSummary(summary);
        form.setCoverColor(StrUtil.blankToDefault(coverColor, "#0d9488"));
        form.setTreeJson(treeJson);
        form.setSort(0);
        form.setStatus(1);

        Long targetId;
        if (id == null) {
            targetId = blogLearningPathService.savePath(form);
        } else {
            blogLearningPathService.updatePath(id, form);
            targetId = id;
        }
        BlogLearningPathVO saved = blogLearningPathService.getPublicPath(String.valueOf(targetId));
        String routeKey = StrUtil.isNotBlank(saved.getSlug()) ? saved.getSlug() : String.valueOf(saved.getId());
        return Map.of(
                "id", String.valueOf(saved.getId()),
                "slug", saved.getSlug() != null ? saved.getSlug() : "",
                "nodeCount", saved.getNodeCount(),
                "resourceCount", saved.getResourceCount(),
                "url", "/paths/" + routeKey
        );
    }

    @McpTool(
            name = "blog_append_mindmap_node",
            description = "向既有学习路径导图的指定父节点下增量追加子节点，并可选同步挂载站内文章、视频或参考网页，无需重写整棵 JSON 树"
    )
    public Map<String, Object> appendMindmapNode(
            @McpToolParam(required = true, description = "学习路径 ID 或 Slug") String pathIdOrSlug,
            @McpToolParam(required = false, description = "父节点 ID 或精确节点标题；留空则挂到根节点下") String parentNodeIdOrLabel,
            @McpToolParam(required = true, description = "新知识点节点名称") String label,
            @McpToolParam(required = false, description = "阶段标签（如 核心 / 实战 / 选学）") String tag,
            @McpToolParam(required = false, description = "节点学习要点说明") String description,
            @McpToolParam(required = false, description = "可选：关联的站内文章 ID 或 Slug") String articleIdOrSlug,
            @McpToolParam(required = false, description = "可选：挂载的视频链接（http(s)://）") String videoUrl,
            @McpToolParam(required = false, description = "可选：挂载的参考网页链接（http(s)://）") String webpageUrl,
            @McpToolParam(required = false, description = "可选：挂载资源的展示标题") String resourceTitle,
            @McpToolParam(required = false, description = "可选：挂载资源的学习备注") String resourceNote
    ) {
        McpExecutionContext.requireScope("write");
        if (StrUtil.isBlank(label)) {
            throw new BusinessException("新节点名称不能为空");
        }

        BlogLearningPathVO pathVo = blogLearningPathService.getPublicPath(pathIdOrSlug);
        BlogLearningPathForm form = blogLearningPathService.getPathForm(pathVo.getId());
        MindMapNodeDTO root = parseTree(form.getTreeJson());

        MindMapNodeDTO parent = StrUtil.isBlank(parentNodeIdOrLabel)
                ? root
                : findTargetNode(root, parentNodeIdOrLabel.trim());
        if (parent == null) {
            throw new BusinessException("未找到指定的父节点: " + parentNodeIdOrLabel);
        }

        MindMapNodeDTO child = new MindMapNodeDTO();
        child.setId("node-" + IdUtil.fastSimpleUUID().substring(0, 8));
        child.setLabel(label.trim());
        child.setTag(StrUtil.trimToNull(tag));
        child.setDescription(StrUtil.trimToNull(description));
        child.setCollapsed(false);
        child.setChildren(new ArrayList<>());

        List<MindMapResourceDTO> resources = new ArrayList<>();
        if (StrUtil.isNotBlank(articleIdOrSlug)) {
            BlogArticleDetailVO art = blogArticleService.getPublicArticleDetail(articleIdOrSlug.trim(), "127.0.0.1", false);
            MindMapResourceDTO res = new MindMapResourceDTO();
            res.setId("res-" + IdUtil.fastSimpleUUID().substring(0, 8));
            res.setType("article");
            res.setTitle(StrUtil.blankToDefault(StrUtil.trimToNull(resourceTitle), art.getTitle()));
            res.setArticleId(String.valueOf(art.getId()));
            res.setArticleSlug(art.getSlug());
            res.setNote(StrUtil.trimToNull(resourceNote));
            resources.add(res);
        }
        if (StrUtil.isNotBlank(videoUrl)) {
            MindMapResourceDTO res = new MindMapResourceDTO();
            res.setId("res-" + IdUtil.fastSimpleUUID().substring(0, 8));
            res.setType("video");
            res.setTitle(StrUtil.blankToDefault(StrUtil.trimToNull(resourceTitle), label.trim() + " 视频讲解"));
            res.setUrl(videoUrl.trim());
            res.setNote(StrUtil.trimToNull(resourceNote));
            resources.add(res);
        }
        if (StrUtil.isNotBlank(webpageUrl)) {
            MindMapResourceDTO res = new MindMapResourceDTO();
            res.setId("res-" + IdUtil.fastSimpleUUID().substring(0, 8));
            res.setType("webpage");
            res.setTitle(StrUtil.blankToDefault(StrUtil.trimToNull(resourceTitle), label.trim() + " 参考文档"));
            res.setUrl(webpageUrl.trim());
            res.setNote(StrUtil.trimToNull(resourceNote));
            resources.add(res);
        }
        child.setResources(resources);

        if (parent.getChildren() == null) {
            parent.setChildren(new ArrayList<>());
        }
        parent.setCollapsed(false);
        parent.getChildren().add(child);

        try {
            form.setTreeJson(objectMapper.writeValueAsString(root));
        } catch (Exception e) {
            throw new BusinessException("序列化思维导图树失败");
        }
        blogLearningPathService.updatePath(pathVo.getId(), form);
        BlogLearningPathVO updated = blogLearningPathService.getPublicPath(String.valueOf(pathVo.getId()));
        String routeKey = StrUtil.isNotBlank(updated.getSlug()) ? updated.getSlug() : String.valueOf(updated.getId());

        return Map.of(
                "pathId", String.valueOf(updated.getId()),
                "parentNodeId", parent.getId(),
                "createdNodeId", child.getId(),
                "createdLabel", child.getLabel(),
                "mountedResourceCount", resources.size(),
                "totalNodes", updated.getNodeCount(),
                "totalResources", updated.getResourceCount(),
                "url", "/paths/" + routeKey + "?node=" + child.getId()
        );
    }

    @McpResource(
            uri = "blog://overview",
            name = "blog-overview",
            title = "MyBlog 站点信息与文章统计概览",
            description = "返回博客站点配置、博主介绍与核心统计信息 JSON",
            mimeType = "application/json"
    )
    public String readSiteOverviewResource() {
        McpExecutionContext.requireScope("read");
        try {
            return objectMapper.writeValueAsString(blogConfigService.getSiteInfo());
        } catch (Exception e) {
            throw new BusinessException("读取站点概览资源失败");
        }
    }

    @McpResource(
            uri = "blog://taxonomy",
            name = "blog-taxonomy",
            title = "MyBlog 分类与标签目录",
            description = "返回站内全部分类与标签字典 JSON",
            mimeType = "application/json"
    )
    public String readTaxonomyResource() {
        McpExecutionContext.requireScope("read");
        try {
            return objectMapper.writeValueAsString(listTaxonomy());
        } catch (Exception e) {
            throw new BusinessException("读取分类标签资源失败");
        }
    }

    private MindMapNodeDTO parseTree(String treeJson) {
        try {
            return objectMapper.readValue(treeJson, MindMapNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException("解析学习路径导图结构失败");
        }
    }

    private MindMapNodeDTO findTargetNode(MindMapNodeDTO current, String key) {
        if (current == null) {
            return null;
        }
        if (Objects.equals(current.getId(), key) || Objects.equals(current.getLabel(), key)) {
            return current;
        }
        if (current.getChildren() != null) {
            for (MindMapNodeDTO child : current.getChildren()) {
                MindMapNodeDTO found = findTargetNode(child, key);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
