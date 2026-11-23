package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新建 MCP API 密钥响应（明文密钥仅此次返回）
 */
@Data
@Schema(description = "新建 MCP API 密钥结果")
public class BlogMcpCreatedTokenVO {

    @Schema(description = "脱敏后的密钥信息")
    private BlogMcpTokenVO token;

    @Schema(description = "明文 API 密钥（仅此次展示）")
    private String plainToken;
}
