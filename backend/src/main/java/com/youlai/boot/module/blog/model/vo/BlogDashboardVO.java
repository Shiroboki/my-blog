package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客管理概览视图对象
 */
@Data
@Schema(description = "博客管理概览数据")
public class BlogDashboardVO {

    @Schema(description = "已发布文章数")
    private Long publishedCount;

    @Schema(description = "草稿文章数")
    private Long draftCount;

    @Schema(description = "分类总数")
    private Long categoryCount;

    @Schema(description = "标签总数")
    private Long tagCount;

    @Schema(description = "总浏览量")
    private Long totalViews;

    @Schema(description = "总点赞数")
    private Long totalLikes;

    @Schema(description = "已通过评论数")
    private Long approvedCommentCount;

    @Schema(description = "待审核评论数")
    private Long pendingCommentCount;

    @Schema(description = "启用友链数")
    private Long linkCount;

    @Schema(description = "热门文章阅读与点赞统计")
    private List<TopArticleStat> topArticles;

    @Schema(description = "分类文章分布")
    private List<CategoryStat> categoryStats;

    @Schema(description = "待审核评论列表")
    private List<BlogCommentVO> pendingComments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopArticleStat {
        private Long id;
        private String title;
        private Long viewCount;
        private Long likeCount;
        private LocalDateTime publishTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String name;
        private Long value;
    }
}
