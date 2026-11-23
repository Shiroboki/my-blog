package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 博客学习路径表单对象
 */
@Data
@Schema(description = "学习路径表单")
public class BlogLearningPathForm {

    @Schema(description = "路径ID")
    private Long id;

    @NotBlank(message = "路径标题不能为空")
    @Size(max = 128, message = "路径标题长度不能超过128字符")
    @Schema(description = "路径标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 128, message = "URL别名长度不能超过128字符")
    @Schema(description = "URL别名")
    private String slug;

    @Size(max = 500, message = "路径简介长度不能超过500字符")
    @Schema(description = "路径简介")
    private String summary;

    @Size(max = 64, message = "主题色长度不能超过64字符")
    @Schema(description = "主题强调色")
    private String coverColor;

    @NotBlank(message = "思维导图节点数据不能为空")
    @Schema(description = "思维导图节点树JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private String treeJson;

    @Schema(description = "排序（升序）")
    private Integer sort;

    @Schema(description = "状态(0草稿 1已发布)")
    private Integer status;
}
