package com.youlai.boot.framework.security.filter;

import com.youlai.boot.framework.security.context.McpExecutionContext;
import com.youlai.boot.framework.security.model.McpTokenPrincipal;
import com.youlai.boot.framework.security.port.McpTokenAuthPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * MCP 接口专属 API 密钥认证过滤器（仅拦截 /api/v1/mcp 路径）
 */
public class McpTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MCP_KEY_HEADER = "X-MCP-Key";

    private final ObjectProvider<McpTokenAuthPort> mcpTokenAuthPortProvider;

    public McpTokenAuthenticationFilter(ObjectProvider<McpTokenAuthPort> mcpTokenAuthPortProvider) {
        this.mcpTokenAuthPortProvider = mcpTokenAuthPortProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        McpTokenAuthPort authPort = mcpTokenAuthPortProvider.getIfAvailable();
        if (authPort == null) {
            writeJsonRpcUnauthorized(response, "MCP service is unavailable");
            return;
        }

        String rawToken = resolveToken(request);
        if (!StringUtils.hasText(rawToken)) {
            writeJsonRpcUnauthorized(response, "Missing MCP API key (use Authorization: Bearer <token> or X-MCP-Key)");
            return;
        }

        Optional<McpTokenPrincipal> authenticated = authPort.authenticate(rawToken);
        if (authenticated.isEmpty()) {
            writeJsonRpcUnauthorized(response, "Invalid, disabled, or expired MCP API key");
            return;
        }

        McpTokenPrincipal principal = authenticated.get();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MCP"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        McpExecutionContext.set(principal);
        if (principal.tokenId() != null) {
            authPort.recordUsage(principal.tokenId());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            McpExecutionContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String customKey = request.getHeader(MCP_KEY_HEADER);
        if (StringUtils.hasText(customKey)) {
            return customKey.trim();
        }
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private void writeJsonRpcUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32001,\"message\":\"" + escaped + "\"}}");
    }
}
