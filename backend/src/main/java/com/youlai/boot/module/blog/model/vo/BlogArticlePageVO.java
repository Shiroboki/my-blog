package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客文章分页列表视图对象
 */
@Data
@Schema(description = "博客文章分页视图对象")
public class BlogArticlePageVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章别名")
    private String slug;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "状态(0草稿 1已发布)")
    private Integer status;

    @Schema(description = "是否置顶(0否 1是)")
    private Integer isTop;

    @Schema(description = "是否允许评论(0否 1是)")
    private Integer allowComment;

    @Schema(description = "浏览量")
    private Long viewCount;

    @Schema(description = "点赞数")
    private Long likeCount;

    @Schema(description = "评论数")
    private Long commentCount;

    @Schema(description = "字数统计")
    private Integer wordCount;

    @Schema(description = "预计阅读分钟数")
    private Integer readingMinutes;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "标签列表")
    private List<BlogTagRefVO> tags;
}
