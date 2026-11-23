package com.youlai.boot.framework.security.port;

import com.youlai.boot.framework.security.model.McpTokenPrincipal;

import java.util.Optional;

/**
 * MCP API 密钥认证端口（避免 security 配置反向依赖业务模块）
 */
public interface McpTokenAuthPort {

    /**
     * 校验 MCP 服务总开关及传入密钥的有效性
     */
    Optional<McpTokenPrincipal> authenticate(String rawToken);

    /**
     * 异步或轻量记录密钥调用计数与最后使用时间
     */
    void recordUsage(Long tokenId);
}
