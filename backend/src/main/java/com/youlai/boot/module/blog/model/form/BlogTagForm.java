package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 博客标签表单
 */
@Data
@Schema(description = "博客标签表单")
public class BlogTagForm {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 64, message = "标签名称不能超过64个字符")
    private String name;

    @Schema(description = "标签别名")
    @NotBlank(message = "标签别名不能为空")
    @Size(max = 64, message = "标签别名不能超过64个字符")
    private String slug;

    @Schema(description = "标签颜色")
    @Size(max = 32, message = "标签颜色不能超过32个字符")
    private String color;
}
