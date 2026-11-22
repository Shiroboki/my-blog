package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 归档文章简要项
 */
@Data
@Schema(description = "归档文章项")
public class BlogArchiveArticleVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
