package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 友情链接表单
 */
@Data
@Schema(description = "友情链接表单")
public class BlogLinkForm {

    @Schema(description = "友链ID")
    private Long id;

    @Schema(description = "站点名称")
    @NotBlank(message = "站点名称不能为空")
    @Size(max = 64, message = "站点名称不能超过64个字符")
    private String name;

    @Schema(description = "站点地址")
    @NotBlank(message = "站点地址不能为空")
    @Size(max = 255, message = "站点地址不能超过255个字符")
    private String url;

    @Schema(description = "站点图标URL")
    @Size(max = 500, message = "图标URL不能超过500个字符")
    private String avatar;

    @Schema(description = "站点简介")
    @Size(max = 255, message = "站点简介不能超过255个字符")
    private String description;

    @Schema(description = "排序")
    private Integer sort = 0;

    @Schema(description = "状态(1启用 0停用)")
    private Integer status = 1;
}
