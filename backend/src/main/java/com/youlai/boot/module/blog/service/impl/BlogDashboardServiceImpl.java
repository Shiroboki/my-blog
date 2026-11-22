package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.enums.ArticleStatusEnum;
import com.youlai.boot.module.blog.enums.CommentStatusEnum;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogCategoryMapper;
import com.youlai.boot.module.blog.mapper.BlogCommentMapper;
import com.youlai.boot.module.blog.mapper.BlogLinkMapper;
import com.youlai.boot.module.blog.mapper.BlogTagMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.entity.BlogComment;
import com.youlai.boot.module.blog.model.entity.BlogLink;
import com.youlai.boot.module.blog.model.entity.BlogTag;
import com.youlai.boot.module.blog.model.form.BlogConfigForm;
import com.youlai.boot.module.blog.model.query.BlogCommentQuery;
import com.youlai.boot.module.blog.model.vo.BlogCategoryVO;
import com.youlai.boot.module.blog.model.vo.BlogDashboardVO;
import com.youlai.boot.module.blog.service.BlogConfigService;
import com.youlai.boot.module.blog.service.BlogDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 博客概览与 RSS / Sitemap 生成服务实现
 */
@Service
@RequiredArgsConstructor
public class BlogDashboardServiceImpl implements BlogDashboardService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final BlogArticleMapper blogArticleMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final BlogTagMapper blogTagMapper;
    private final BlogCommentMapper blogCommentMapper;
    private final BlogLinkMapper blogLinkMapper;
    private final BlogConfigService blogConfigService;

    @Override
    public BlogDashboardVO getDashboardStats() {
        BlogDashboardVO vo = new BlogDashboardVO();
        vo.setPublishedCount(blogArticleMapper.selectCount(new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())));
        vo.setDraftCount(blogArticleMapper.selectCount(new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getStatus, ArticleStatusEnum.DRAFT.getValue())));
        vo.setCategoryCount(blogCategoryMapper.selectCount(new LambdaQueryWrapper<BlogCategory>()));
        vo.setTagCount(blogTagMapper.selectCount(new LambdaQueryWrapper<BlogTag>()));
        Long views = blogArticleMapper.sumPublishedViews();
        vo.setTotalViews(views != null ? views : 0L);

        List<BlogArticle> publishedMetrics = blogArticleMapper.selectList(new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getTitle, BlogArticle::getViewCount,
                        BlogArticle::getLikeCount, BlogArticle::getPublishTime)
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())
                .orderByDesc(BlogArticle::getViewCount, BlogArticle::getPublishTime));
        long totalLikes = publishedMetrics.stream()
                .mapToLong(a -> a.getLikeCount() != null ? a.getLikeCount() : 0L)
                .sum();
        vo.setTotalLikes(totalLikes);

        vo.setTopArticles(publishedMetrics.stream()
                .limit(7)
                .map(a -> new BlogDashboardVO.TopArticleStat(
                        a.getId(),
                        a.getTitle(),
                        a.getViewCount() != null ? a.getViewCount() : 0L,
                        a.getLikeCount() != null ? a.getLikeCount() : 0L,
                        a.getPublishTime()
                ))
                .toList());

        List<BlogCategoryVO> categories = blogCategoryMapper.listPublicCategories();
        vo.setCategoryStats(categories.stream()
                .map(c -> new BlogDashboardVO.CategoryStat(
                        c.getName(),
                        c.getArticleCount() != null ? c.getArticleCount() : 0L
                ))
                .toList());

        vo.setApprovedCommentCount(blogCommentMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getStatus, CommentStatusEnum.APPROVED.getValue())));
        vo.setPendingCommentCount(blogCommentMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getStatus, CommentStatusEnum.PENDING.getValue())));
        vo.setLinkCount(blogLinkMapper.selectCount(new LambdaQueryWrapper<BlogLink>()
                .eq(BlogLink::getStatus, 1)));

        BlogCommentQuery pendingQuery = new BlogCommentQuery();
        pendingQuery.setPageNum(1);
        pendingQuery.setPageSize(5);
        pendingQuery.setStatus(CommentStatusEnum.PENDING.getValue());
        vo.setPendingComments(blogCommentMapper.getCommentPage(new Page<>(1, 5), pendingQuery).getRecords());
        return vo;
    }

    @Override
    public String buildRssXml(String baseUrl) {
        String normalizedBase = normalizeBaseUrl(baseUrl);
        BlogConfigForm config = blogConfigService.getConfigForm();
        List<BlogArticle> articles = blogArticleMapper.selectList(new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getTitle, BlogArticle::getSummary,
                        BlogArticle::getPublishTime, BlogArticle::getCreateTime)
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())
                .orderByDesc(BlogArticle::getPublishTime, BlogArticle::getId)
                .last("LIMIT 20"));

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\">\n");
        sb.append("  <channel>\n");
        sb.append("    <title>").append(escapeXml(config.getSiteTitle())).append("</title>\n");
        sb.append("    <link>").append(escapeXml(normalizedBase)).append("/</link>\n");
        sb.append("    <description>").append(escapeXml(StrUtil.blankToDefault(config.getSiteSubtitle(), config.getSiteTitle()))).append("</description>\n");
        sb.append("    <language>zh-CN</language>\n");
        for (BlogArticle article : articles) {
            LocalDateTime pub = article.getPublishTime() != null ? article.getPublishTime() : article.getCreateTime();
            String pubDate = pub != null
                    ? pub.atZone(CHINA_ZONE).format(DateTimeFormatter.RFC_1123_DATE_TIME)
                    : "";
            String articleUrl = normalizedBase + "/article/" + article.getId();
            sb.append("    <item>\n");
            sb.append("      <title>").append(escapeXml(article.getTitle())).append("</title>\n");
            sb.append("      <link>").append(escapeXml(articleUrl)).append("</link>\n");
            sb.append("      <guid>").append(escapeXml(articleUrl)).append("</guid>\n");
            if (StrUtil.isNotBlank(pubDate)) {
                sb.append("      <pubDate>").append(escapeXml(pubDate)).append("</pubDate>\n");
            }
            sb.append("      <description>").append(escapeXml(StrUtil.nullToEmpty(article.getSummary()))).append("</description>\n");
            sb.append("    </item>\n");
        }
        sb.append("  </channel>\n");
        sb.append("</rss>\n");
        return sb.toString();
    }

    @Override
    public String buildSitemapXml(String baseUrl) {
        String normalizedBase = normalizeBaseUrl(baseUrl);
        List<BlogArticle> articles = blogArticleMapper.selectList(new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getUpdateTime, BlogArticle::getPublishTime)
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())
                .orderByDesc(BlogArticle::getPublishTime, BlogArticle::getId));

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrlNode(sb, normalizedBase + "/", null, "daily", "1.0");
        appendUrlNode(sb, normalizedBase + "/archives", null, "weekly", "0.7");
        appendUrlNode(sb, normalizedBase + "/links", null, "weekly", "0.6");
        appendUrlNode(sb, normalizedBase + "/about", null, "monthly", "0.6");
        for (BlogArticle article : articles) {
            LocalDateTime mod = article.getUpdateTime() != null ? article.getUpdateTime() : article.getPublishTime();
            String lastmod = mod != null ? mod.toLocalDate().toString() : null;
            appendUrlNode(sb, normalizedBase + "/article/" + article.getId(), lastmod, "weekly", "0.8");
        }
        sb.append("</urlset>\n");
        return sb.toString();
    }

    private void appendUrlNode(StringBuilder sb, String loc, String lastmod, String changefreq, String priority) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (StrUtil.isNotBlank(lastmod)) {
            sb.append("    <lastmod>").append(escapeXml(lastmod)).append("</lastmod>\n");
        }
        sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sb.append("    <priority>").append(priority).append("</priority>\n");
        sb.append("  </url>\n");
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return "http://localhost:9123";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
