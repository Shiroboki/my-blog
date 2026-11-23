package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客学习路径视图对象
 */
@Data
@Schema(description = "学习路径视图对象")
public class BlogLearningPathVO {

    @Schema(description = "路径ID")
    private Long id;

    @Schema(description = "路径标题")
    private String title;

    @Schema(description = "URL别名")
    private String slug;

    @Schema(description = "路径简介")
    private String summary;

    @Schema(description = "主题强调色")
    private String coverColor;

    @Schema(description = "思维导图节点树JSON")
    private String treeJson;

    @Schema(description = "节点总数")
    private Integer nodeCount;

    @Schema(description = "挂载资源总数")
    private Integer resourceCount;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态(0草稿 1已发布)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
