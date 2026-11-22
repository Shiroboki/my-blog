package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 相邻文章链接（上一篇 / 下一篇）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "相邻文章导航对象")
public class BlogArticleNeighborVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;
}
