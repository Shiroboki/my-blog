package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文章时间归档分组视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章时间归档分组")
public class BlogArchiveVO {

    @Schema(description = "年月（如 2026-11）")
    private String yearMonth;

    @Schema(description = "该月已发布文章列表")
    private List<BlogArchiveArticleVO> articles;
}
