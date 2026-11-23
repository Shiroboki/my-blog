package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已注册 MCP Tool 元信息描述
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MCP Tool 描述信息")
public class BlogMcpToolDescriptorVO {

    private String name;
    private String scope;
    private String category;
    private String description;
}
