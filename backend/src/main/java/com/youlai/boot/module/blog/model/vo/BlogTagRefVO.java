package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章关联标签轻量视图对象
 */
@Data
@Schema(description = "文章关联标签视图对象")
public class BlogTagRefVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签别名")
    private String slug;

    @Schema(description = "标签颜色")
    private String color;
}
