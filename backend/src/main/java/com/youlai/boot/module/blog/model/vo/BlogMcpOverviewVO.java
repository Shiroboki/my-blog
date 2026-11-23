package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * MCP 管理台总览响应
 */
@Data
@Schema(description = "MCP 接入总览信息")
public class BlogMcpOverviewVO {

    private boolean mcpEnabled;
    private boolean allowPublish;
    private String endpointPath;
    private boolean bootstrapConfigured;
    private List<BlogMcpToolDescriptorVO> tools;
    private List<BlogMcpTokenVO> tokens;
}
