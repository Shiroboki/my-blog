package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MCP 服务全局开关配置表单
 */
@Data
@Schema(description = "MCP 服务配置表单")
public class BlogMcpSettingsForm {

    @Schema(description = "是否开启 MCP 接入")
    private Boolean mcpEnabled;

    @Schema(description = "是否允许 MCP 工具直接发布文章")
    private Boolean allowPublish;
}
