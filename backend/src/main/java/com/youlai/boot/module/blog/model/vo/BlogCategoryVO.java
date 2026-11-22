package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客分类视图对象
 */
@Data
@Schema(description = "博客分类视图对象")
public class BlogCategoryVO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类别名")
    private String slug;

    @Schema(description = "分类描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "已发布文章数")
    private Long articleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
