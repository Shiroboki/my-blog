package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 博客公开站点信息与聚合统计
 */
@Data
@Schema(description = "博客公开站点信息")
public class BlogSiteInfoVO {

    @Schema(description = "站点主标题")
    private String siteTitle;

    @Schema(description = "站点副标题")
    private String siteSubtitle;

    @Schema(description = "博主昵称")
    private String authorName;

    @Schema(description = "博主简介")
    private String authorBio;

    @Schema(description = "博主头像")
    private String authorAvatar;

    @Schema(description = "GitHub 主页")
    private String githubUrl;

    @Schema(description = "ICP 备案号")
    private String icpNumber;

    @Schema(description = "评论是否需要审核(0否 1是)")
    private String commentAudit;

    @Schema(description = "是否启用 Giscus(0否 1是)")
    private String giscusEnabled;

    @Schema(description = "Giscus 仓库(owner/repo)")
    private String giscusRepo;

    @Schema(description = "Giscus Repository ID")
    private String giscusRepoId;

    @Schema(description = "Giscus 分类名称")
    private String giscusCategory;

    @Schema(description = "Giscus Category ID")
    private String giscusCategoryId;

    @Schema(description = "Giscus 映射方式")
    private String giscusMapping;

    @Schema(description = "关于页面 Markdown 内容")
    private String aboutContent;

    @Schema(description = "已发布文章总数")
    private Long articleCount;

    @Schema(description = "分类总数")
    private Long categoryCount;

    @Schema(description = "标签总数")
    private Long tagCount;

    @Schema(description = "总浏览量")
    private Long totalViews;

    @Schema(description = "热门文章列表")
    private List<BlogHotArticleVO> hotArticles = new ArrayList<>();
}
