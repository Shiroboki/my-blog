package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建 MCP API 密钥请求表单
 */
@Data
@Schema(description = "创建 MCP API 密钥表单")
public class BlogMcpTokenCreateForm {

    @Schema(description = "密钥名称")
    @NotBlank(message = "密钥名称不能为空")
    @Size(max = 80, message = "密钥名称不能超过80个字符")
    private String name;

    @Schema(description = "权限作用域(read / read,write / admin)")
    @Size(max = 128, message = "作用域长度不能超过128")
    private String scopes = "read,write";

    @Schema(description = "有效天数（空表示永不过期）")
    @Min(value = 1, message = "有效期至少 1 天")
    @Max(value = 3650, message = "有效期不能超过 3650 天")
    private Integer expireDays;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
