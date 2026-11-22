package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公开评论提交表单
 */
@Data
@Schema(description = "公开评论提交表单")
public class BlogCommentForm {

    @Schema(description = "文章ID")
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    @Schema(description = "父评论ID（0为一级评论）")
    private Long parentId = 0L;

    @Schema(description = "昵称（选填，留空默认匿名读者）")
    @Size(max = 64, message = "昵称不能超过64个字符")
    private String nickname;

    @Schema(description = "邮箱（选填，不公开展示）")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    @Schema(description = "个人主页（选填）")
    @Size(max = 255, message = "网址不能超过255个字符")
    private String website;

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;

    @Schema(description = "划词引用原文")
    @Size(max = 500, message = "引用原文不能超过500个字符")
    private String quoteText;

    @Schema(description = "划词起始偏移")
    @Min(value = 0, message = "划词起始偏移不能为负数")
    private Integer quoteStart;

    @Schema(description = "划词结束偏移")
    @Min(value = 0, message = "划词结束偏移不能为负数")
    private Integer quoteEnd;
}
