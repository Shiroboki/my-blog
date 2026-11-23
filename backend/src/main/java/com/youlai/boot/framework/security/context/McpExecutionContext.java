package com.youlai.boot.framework.security.context;

import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.model.McpTokenPrincipal;

/**
 * MCP 单次工具调用线程上下文（用于校验当前 API 密钥的作用域权限）
 */
public final class McpExecutionContext {

    private static final ThreadLocal<McpTokenPrincipal> HOLDER = new ThreadLocal<>();

    private McpExecutionContext() {
    }

    public static void set(McpTokenPrincipal principal) {
        HOLDER.set(principal);
    }

    public static McpTokenPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static void requireScope(String scope) {
        McpTokenPrincipal principal = HOLDER.get();
        if (principal == null || !principal.hasScope(scope)) {
            throw new BusinessException(ResultCode.ACCESS_UNAUTHORIZED, "当前 MCP 密钥缺少权限作用域: " + scope);
        }
    }
}
