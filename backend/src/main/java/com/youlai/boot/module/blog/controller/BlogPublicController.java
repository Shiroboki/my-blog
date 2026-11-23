package com.youlai.boot.module.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.ratelimit.RateLimit;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.framework.web.util.IPUtils;
import com.youlai.boot.module.blog.model.form.BlogCommentForm;
import com.youlai.boot.module.blog.model.query.BlogArticleQuery;
import com.youlai.boot.module.blog.model.vo.BlogArchiveVO;
import com.youlai.boot.module.blog.model.vo.BlogArticleDetailVO;
import com.youlai.boot.module.blog.model.vo.BlogArticlePageVO;
import com.youlai.boot.module.blog.model.vo.BlogCategoryVO;
import com.youlai.boot.module.blog.model.vo.BlogCommentVO;
import com.youlai.boot.module.blog.model.vo.BlogLearningPathVO;
import com.youlai.boot.module.blog.model.vo.BlogLinkVO;
import com.youlai.boot.module.blog.model.vo.BlogSearchResultVO;
import com.youlai.boot.module.blog.model.vo.BlogSiteInfoVO;
import com.youlai.boot.module.blog.model.vo.BlogTagVO;
import com.youlai.boot.module.blog.service.BlogArticleService;
import com.youlai.boot.module.blog.service.BlogCategoryService;
import com.youlai.boot.module.blog.service.BlogCommentService;
import com.youlai.boot.module.blog.service.BlogConfigService;
import com.youlai.boot.module.blog.service.BlogDashboardService;
import com.youlai.boot.module.blog.service.BlogLearningPathService;
import com.youlai.boot.module.blog.service.BlogLinkService;
import com.youlai.boot.module.blog.service.BlogSearchService;
import com.youlai.boot.module.blog.service.BlogTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客公开访问控制层（免登录，供读者前台使用）
 */
@Tag(name = "19.博客公开接口")
@RestController
@RequestMapping("/api/v1/blog/public")
@RequiredArgsConstructor
public class BlogPublicController {

    private final BlogConfigService blogConfigService;
    private final BlogArticleService blogArticleService;
    private final BlogCategoryService blogCategoryService;
    private final BlogTagService blogTagService;
    private final BlogCommentService blogCommentService;
    private final BlogLinkService blogLinkService;
    private final BlogLearningPathService blogLearningPathService;
    private final BlogSearchService blogSearchService;
    private final BlogDashboardService blogDashboardService;

    @Operation(summary = "获取博客站点信息与统计")
    @GetMapping("/site-info")
    public Result<BlogSiteInfoVO> getSiteInfo() {
        return Result.success(blogConfigService.getSiteInfo());
    }

    @Operation(summary = "获取公开文章分页列表")
    @GetMapping("/articles")
    public PageResult<BlogArticlePageVO> getPublicArticlePage(@Valid BlogArticleQuery queryParams) {
        Page<BlogArticlePageVO> page = blogArticleService.getPublicArticlePage(queryParams);
        return PageResult.success(page);
    }

    @Operation(summary = "获取公开文章详情（支持 ID 或 Slug，trackView=false 时仅预取不计阅读量）")
    @GetMapping("/articles/{idOrSlug}")
    public Result<BlogArticleDetailVO> getPublicArticleDetail(
            @Parameter(description = "文章ID或URL别名") @PathVariable String idOrSlug,
            @Parameter(description = "是否计入阅读量") @RequestParam(defaultValue = "true") boolean trackView,
            HttpServletRequest request
    ) {
        return Result.success(blogArticleService.getPublicArticleDetail(idOrSlug, IPUtils.getIpAddr(request), trackView));
    }

    @Operation(summary = "文章点赞")
    @PostMapping("/articles/{id}/like")
    @RateLimit(limit = 10, window = 60)
    public Result<Long> likeArticle(
            @Parameter(description = "文章ID") @PathVariable Long id,
            HttpServletRequest request
    ) {
        return Result.success(blogArticleService.likeArticle(id, IPUtils.getIpAddr(request)));
    }

    @Operation(summary = "获取文章公开评论列表")
    @GetMapping("/articles/{id}/comments")
    public Result<List<BlogCommentVO>> listPublicComments(@Parameter(description = "文章ID") @PathVariable Long id) {
        return Result.success(blogCommentService.listPublicArticleComments(id));
    }

    @Operation(summary = "匿名提交评论")
    @PostMapping("/comments")
    @RepeatSubmit
    @RateLimit(limit = 5, window = 60)
    public Result<BlogCommentVO> submitPublicComment(
            @RequestBody @Valid BlogCommentForm form,
            HttpServletRequest request
    ) {
        return Result.success(blogCommentService.submitPublicComment(form, IPUtils.getIpAddr(request)));
    }

    @Operation(summary = "获取公开分类列表")
    @GetMapping("/categories")
    public Result<List<BlogCategoryVO>> listPublicCategories() {
        return Result.success(blogCategoryService.listPublicCategories());
    }

    @Operation(summary = "获取公开标签列表")
    @GetMapping("/tags")
    public Result<List<BlogTagVO>> listPublicTags() {
        return Result.success(blogTagService.listPublicTags());
    }

    @Operation(summary = "获取文章时间归档")
    @GetMapping("/archives")
    public Result<List<BlogArchiveVO>> listArchives() {
        return Result.success(blogArticleService.listArchives());
    }

    @Operation(summary = "获取公开友情链接")
    @GetMapping("/links")
    public Result<List<BlogLinkVO>> listPublicLinks() {
        return Result.success(blogLinkService.listPublicLinks());
    }

    @Operation(summary = "获取已发布学习路径与思维导图列表")
    @GetMapping("/learning-paths")
    public Result<List<BlogLearningPathVO>> listPublicLearningPaths() {
        return Result.success(blogLearningPathService.listPublicPaths());
    }

    @Operation(summary = "获取单个学习路径导图（支持ID或Slug）")
    @GetMapping("/learning-paths/{idOrSlug}")
    public Result<BlogLearningPathVO> getPublicLearningPath(
            @Parameter(description = "路径ID或URL别名") @PathVariable String idOrSlug
    ) {
        return Result.success(blogLearningPathService.getPublicPath(idOrSlug));
    }

    @Operation(summary = "全局聚合检索（文章正文片段、学习路径节点、归档年月）")
    @GetMapping("/search")
    public Result<BlogSearchResultVO> searchPublic(
            @Parameter(description = "搜索关键字") @RequestParam(defaultValue = "") String q,
            @Parameter(description = "每类最大返回数") @RequestParam(defaultValue = "8") int limit
    ) {
        return Result.success(blogSearchService.searchPublic(q, limit));
    }

    @Operation(summary = "RSS 2.0 订阅源")
    @GetMapping(value = "/rss.xml", produces = "application/rss+xml;charset=UTF-8")
    public ResponseEntity<String> getRssFeed(HttpServletRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/rss+xml;charset=UTF-8"))
                .body(blogDashboardService.buildRssXml(resolveBaseUrl(request)));
    }

    @Operation(summary = "站点地图 Sitemap")
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap(HttpServletRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body(blogDashboardService.buildSitemapXml(resolveBaseUrl(request)));
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host != null && !host.isBlank()) {
            return scheme + "://" + host;
        }
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }
}
