package com.youlai.boot.framework.security.model;

import java.util.Set;

/**
 * MCP API 密钥认证主体
 */
public record McpTokenPrincipal(
        Long tokenId,
        String name,
        Set<String> scopes
) {
    public boolean hasScope(String requiredScope) {
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        if (scopes.contains("admin")) {
            return true;
        }
        if ("read".equals(requiredScope)) {
            return scopes.contains("read") || scopes.contains("write");
        }
        return scopes.contains(requiredScope);
    }
}
