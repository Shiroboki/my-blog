package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客标签视图对象
 */
@Data
@Schema(description = "博客标签视图对象")
public class BlogTagVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签别名")
    private String slug;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "已发布文章数")
    private Long articleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
