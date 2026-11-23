package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.model.McpTokenPrincipal;
import com.youlai.boot.framework.security.port.McpTokenAuthPort;
import com.youlai.boot.module.blog.mapper.BlogMcpTokenMapper;
import com.youlai.boot.module.blog.model.entity.BlogMcpToken;
import com.youlai.boot.module.blog.model.form.BlogMcpSettingsForm;
import com.youlai.boot.module.blog.model.form.BlogMcpTokenCreateForm;
import com.youlai.boot.module.blog.model.vo.BlogMcpCreatedTokenVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpOverviewVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpTokenVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpToolDescriptorVO;
import com.youlai.boot.module.blog.service.BlogConfigService;
import com.youlai.boot.module.blog.service.BlogMcpTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 博客 MCP API 密钥管理与认证适配器实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogMcpTokenServiceImpl implements BlogMcpTokenService, McpTokenAuthPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_PREFIX = "mcp_";
    private static final Set<String> VALID_SCOPES = Set.of("read", "write", "admin");

    private final BlogMcpTokenMapper blogMcpTokenMapper;
    private final BlogConfigService blogConfigService;

    @Value("${blog.mcp.bootstrap-token:}")
    private String bootstrapToken;

    @Override
    public BlogMcpOverviewVO getOverview() {
        BlogMcpOverviewVO vo = new BlogMcpOverviewVO();
        vo.setMcpEnabled("1".equals(blogConfigService.getConfigValue("mcp_enabled", "1")));
        vo.setAllowPublish("1".equals(blogConfigService.getConfigValue("mcp_allow_publish", "0")));
        vo.setEndpointPath("/api/v1/mcp");
        vo.setBootstrapConfigured(StrUtil.isNotBlank(bootstrapToken));
        vo.setTools(buildToolCatalog());

        List<BlogMcpToken> entities = blogMcpTokenMapper.selectList(
                new LambdaQueryWrapper<BlogMcpToken>().orderByDesc(BlogMcpToken::getId));
        vo.setTokens(entities.stream().map(this::toVO).toList());
        return vo;
    }

    @Override
    public boolean updateSettings(BlogMcpSettingsForm form) {
        if (form.getMcpEnabled() != null) {
            blogConfigService.setConfigValue(
                    "mcp_enabled",
                    Boolean.TRUE.equals(form.getMcpEnabled()) ? "1" : "0",
                    "是否开启 MCP Server 接入(0否 1是)"
            );
        }
        if (form.getAllowPublish() != null) {
            blogConfigService.setConfigValue(
                    "mcp_allow_publish",
                    Boolean.TRUE.equals(form.getAllowPublish()) ? "1" : "0",
                    "是否允许 MCP 工具直接发布文章(0仅草稿 1允许发布)"
            );
        }
        return true;
    }

    @Override
    public BlogMcpCreatedTokenVO createToken(BlogMcpTokenCreateForm form) {
        String plainToken = generatePlainToken();
        String hash = DigestUtil.sha256Hex(plainToken);
        String prefixMask = plainToken.substring(0, 10) + "..." + plainToken.substring(plainToken.length() - 4);
        String normalizedScopes = normalizeScopes(form.getScopes());

        BlogMcpToken entity = new BlogMcpToken();
        entity.setName(form.getName().trim());
        entity.setTokenPrefix(prefixMask);
        entity.setTokenHash(hash);
        entity.setScopes(normalizedScopes);
        entity.setStatus(1);
        entity.setCallCount(0L);
        entity.setRemark(StrUtil.isBlank(form.getRemark()) ? null : form.getRemark().trim());
        if (form.getExpireDays() != null && form.getExpireDays() > 0) {
            entity.setExpireTime(LocalDateTime.now().plusDays(form.getExpireDays()));
        }

        blogMcpTokenMapper.insert(entity);

        BlogMcpCreatedTokenVO result = new BlogMcpCreatedTokenVO();
        result.setToken(toVO(entity));
        result.setPlainToken(plainToken);
        return result;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        BlogMcpToken existing = blogMcpTokenMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("MCP 密钥不存在");
        }
        existing.setStatus(Integer.valueOf(1).equals(status) ? 1 : 0);
        return blogMcpTokenMapper.updateById(existing) > 0;
    }

    @Override
    public boolean deleteToken(Long id) {
        return blogMcpTokenMapper.deleteById(id) > 0;
    }

    @Override
    public Optional<McpTokenPrincipal> authenticate(String rawToken) {
        if (StrUtil.isBlank(rawToken)) {
            return Optional.empty();
        }
        if (!"1".equals(blogConfigService.getConfigValue("mcp_enabled", "1"))) {
            return Optional.empty();
        }

        String trimmed = rawToken.trim();
        if (StrUtil.isNotBlank(bootstrapToken) && constantTimeMatches(trimmed, bootstrapToken.trim())) {
            return Optional.of(new McpTokenPrincipal(null, "bootstrap-env", Set.of("read", "write", "admin")));
        }

        String hash = DigestUtil.sha256Hex(trimmed);
        BlogMcpToken token = blogMcpTokenMapper.selectOne(new LambdaQueryWrapper<BlogMcpToken>()
                .eq(BlogMcpToken::getTokenHash, hash)
                .last("LIMIT 1"));
        if (token == null || !Integer.valueOf(1).equals(token.getStatus())) {
            return Optional.empty();
        }
        if (token.getExpireTime() != null && token.getExpireTime().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(new McpTokenPrincipal(token.getId(), token.getName(), parseScopeSet(token.getScopes())));
    }

    @Override
    public void recordUsage(Long tokenId) {
        if (tokenId == null) {
            return;
        }
        try {
            blogMcpTokenMapper.update(null, new LambdaUpdateWrapper<BlogMcpToken>()
                    .eq(BlogMcpToken::getId, tokenId)
                    .set(BlogMcpToken::getLastUsedTime, LocalDateTime.now())
                    .setSql("call_count = COALESCE(call_count, 0) + 1"));
        } catch (Exception e) {
            log.warn("更新 MCP 密钥调用统计失败(tokenId={}): {}", tokenId, e.getMessage());
        }
    }

    private String generatePlainToken() {
        byte[] bytes = new byte[18];
        SECURE_RANDOM.nextBytes(bytes);
        return TOKEN_PREFIX + HexFormat.of().formatHex(bytes);
    }

    private String normalizeScopes(String raw) {
        Set<String> set = parseScopeSet(raw);
        if (set.isEmpty()) {
            return "read";
        }
        return String.join(",", set);
    }

    private Set<String> parseScopeSet(String raw) {
        Set<String> set = new LinkedHashSet<>();
        set.add("read");
        if (StrUtil.isBlank(raw)) {
            return set;
        }
        Arrays.stream(raw.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(VALID_SCOPES::contains)
                .forEach(set::add);
        if (set.contains("admin")) {
            set.add("write");
        }
        return set;
    }

    private boolean constantTimeMatches(String candidate, String expected) {
        byte[] a = DigestUtil.sha256(candidate.getBytes(StandardCharsets.UTF_8));
        byte[] b = DigestUtil.sha256(expected.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(a, b);
    }

    private BlogMcpTokenVO toVO(BlogMcpToken entity) {
        BlogMcpTokenVO vo = new BlogMcpTokenVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setTokenPrefix(entity.getTokenPrefix());
        vo.setScopes(entity.getScopes());
        vo.setStatus(entity.getStatus());
        vo.setExpireTime(entity.getExpireTime());
        vo.setLastUsedTime(entity.getLastUsedTime());
        vo.setCallCount(entity.getCallCount() != null ? entity.getCallCount() : 0L);
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private List<BlogMcpToolDescriptorVO> buildToolCatalog() {
        return List.of(
                new BlogMcpToolDescriptorVO("blog_search_articles", "read", "检索", "按关键字、分类、标签检索博客文章摘要列表"),
                new BlogMcpToolDescriptorVO("blog_get_article", "read", "检索", "读取指定 ID 或 Slug 的文章 Markdown 正文"),
                new BlogMcpToolDescriptorVO("blog_list_taxonomy", "read", "检索", "获取站内所有分类与标签目录及文章统计"),
                new BlogMcpToolDescriptorVO("blog_get_learning_paths", "read", "导图", "获取所有已发布学习路径与思维导图清单"),
                new BlogMcpToolDescriptorVO("blog_get_learning_path_detail", "read", "导图", "读取学习路径完整思维导图树与挂载资源"),
                new BlogMcpToolDescriptorVO("blog_get_site_overview", "read", "统计", "获取站点基础信息与运营数据概览"),
                new BlogMcpToolDescriptorVO("blog_create_article", "write", "创作", "创建博客文章（默认草稿，受后台直接发布开关控制）"),
                new BlogMcpToolDescriptorVO("blog_update_article", "write", "创作", "更新已有文章标题、正文、分类标签或发布状态"),
                new BlogMcpToolDescriptorVO("blog_upsert_learning_path", "write", "导图", "创建或整树更新一条学习路径思维导图"),
                new BlogMcpToolDescriptorVO("blog_append_mindmap_node", "write", "导图", "向指定导图父节点增量追加子节点并挂载文章/视频/网页")
        );
    }
}
