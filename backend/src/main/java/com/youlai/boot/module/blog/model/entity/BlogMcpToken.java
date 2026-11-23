package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 博客 MCP API 密钥实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_mcp_token")
public class BlogMcpToken extends BaseEntity {

    /**
     * 密钥名称
     */
    private String name;

    /**
     * 展示前缀掩码
     */
    private String tokenPrefix;

    /**
     * SHA-256 哈希摘要
     */
    private String tokenHash;

    /**
     * 权限作用域（read / write / admin，逗号分隔）
     */
    private String scopes;

    /**
     * 状态（1启用 0禁用）
     */
    private Integer status;

    /**
     * 过期时间（NULL 永不过期）
     */
    private LocalDateTime expireTime;

    /**
     * 最近调用时间
     */
    private LocalDateTime lastUsedTime;

    /**
     * 累计调用次数
     */
    private Long callCount;

    /**
     * 备注说明
     */
    private String remark;
}
