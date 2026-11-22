package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 博客文章表单
 */
@Data
@Schema(description = "博客文章表单")
public class BlogArticleForm {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过200个字符")
    private String title;

    @Schema(description = "文章别名")
    @Size(max = 128, message = "文章别名不能超过128个字符")
    private String slug;

    @Schema(description = "文章摘要")
    @Size(max = 500, message = "文章摘要不能超过500个字符")
    private String summary;

    @Schema(description = "Markdown 正文")
    @NotBlank(message = "文章正文不能为空")
    private String content;

    @Schema(description = "封面图URL")
    @Size(max = 500, message = "封面图URL不能超过500个字符")
    private String coverImage;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态(0草稿 1已发布)")
    private Integer status = 1;

    @Schema(description = "是否置顶(0否 1是)")
    private Integer isTop = 0;

    @Schema(description = "是否允许评论(0否 1是)")
    private Integer allowComment = 1;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;
}
