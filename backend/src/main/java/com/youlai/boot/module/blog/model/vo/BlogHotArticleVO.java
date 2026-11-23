package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 热门文章简要视图对象
 */
@Data
@Schema(description = "热门文章视图对象")
public class BlogHotArticleVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章别名")
    private String slug;

    @Schema(description = "浏览量")
    private Long viewCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
