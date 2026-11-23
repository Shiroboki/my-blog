package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP API 密钥脱敏展示对象
 */
@Data
@Schema(description = "MCP API 密钥列表项")
public class BlogMcpTokenVO {

    private Long id;
    private String name;
    private String tokenPrefix;
    private String scopes;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime lastUsedTime;
    private Long callCount;
    private String remark;
    private LocalDateTime createTime;
}
