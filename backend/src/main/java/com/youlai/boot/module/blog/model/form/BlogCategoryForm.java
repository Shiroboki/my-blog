package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 博客分类表单
 */
@Data
@Schema(description = "博客分类表单")
public class BlogCategoryForm {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称不能超过64个字符")
    private String name;

    @Schema(description = "分类别名")
    @NotBlank(message = "分类别名不能为空")
    @Size(max = 64, message = "分类别名不能超过64个字符")
    private String slug;

    @Schema(description = "分类描述")
    @Size(max = 255, message = "分类描述不能超过255个字符")
    private String description;

    @Schema(description = "排序")
    private Integer sort = 0;
}
