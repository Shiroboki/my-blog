package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 博主回复评论表单
 */
@Data
@Schema(description = "博主回复评论表单")
public class BlogCommentReplyForm {

    @Schema(description = "文章ID")
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    @Schema(description = "父评论ID")
    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    @Schema(description = "回复内容")
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容不能超过1000个字符")
    private String content;
}
