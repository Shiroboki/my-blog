package com.youlai.boot.module.blog;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.context.McpExecutionContext;
import com.youlai.boot.framework.security.filter.McpTokenAuthenticationFilter;
import com.youlai.boot.framework.security.model.McpTokenPrincipal;
import com.youlai.boot.framework.security.port.McpTokenAuthPort;
import com.youlai.boot.module.blog.controller.BlogMcpController;
import com.youlai.boot.module.blog.controller.BlogPublicController;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogCategoryMapper;
import com.youlai.boot.module.blog.mapper.BlogCommentMapper;
import com.youlai.boot.module.blog.mapper.BlogLearningPathMapper;
import com.youlai.boot.module.blog.mapper.BlogLinkMapper;
import com.youlai.boot.module.blog.mapper.BlogMcpTokenMapper;
import com.youlai.boot.module.blog.mapper.BlogTagMapper;
import com.youlai.boot.module.blog.mcp.BlogMcpTools;
import com.youlai.boot.module.blog.model.dto.MindMapNodeDTO;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.entity.BlogLearningPath;
import com.youlai.boot.module.blog.model.entity.BlogMcpToken;
import com.youlai.boot.module.blog.model.form.BlogArticleForm;
import com.youlai.boot.module.blog.model.form.BlogLearningPathForm;
import com.youlai.boot.module.blog.model.form.BlogMcpTokenCreateForm;
import com.youlai.boot.module.blog.model.vo.BlogArchiveArticleVO;
import com.youlai.boot.module.blog.model.vo.BlogArticleDetailVO;
import com.youlai.boot.module.blog.model.vo.BlogLearningPathVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpCreatedTokenVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpOverviewVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpTokenVO;
import com.youlai.boot.module.blog.model.vo.BlogSearchResultVO;
import com.youlai.boot.module.blog.service.BlogArticleService;
import com.youlai.boot.module.blog.service.BlogCategoryService;
import com.youlai.boot.module.blog.service.BlogCommentService;
import com.youlai.boot.module.blog.service.BlogConfigService;
import com.youlai.boot.module.blog.service.BlogDashboardService;
import com.youlai.boot.module.blog.service.BlogLearningPathService;
import com.youlai.boot.module.blog.service.BlogLinkService;
import com.youlai.boot.module.blog.service.BlogMcpTokenService;
import com.youlai.boot.module.blog.service.BlogSearchService;
import com.youlai.boot.module.blog.service.BlogTagService;
import com.youlai.boot.module.blog.service.impl.BlogDashboardServiceImpl;
import com.youlai.boot.module.blog.service.impl.BlogMcpTokenServiceImpl;
import com.youlai.boot.module.blog.service.impl.BlogSearchServiceImpl;
import jakarta.servlet.FilterChain;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局搜索、Sitemap 扩展、MCP 认证鉴权与工具执行单元测试
 */
@ExtendWith(MockitoExtension.class)
class BlogMcpAndSearchTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Mock
    private BlogArticleMapper blogArticleMapper;
    @Mock
    private BlogCategoryMapper blogCategoryMapper;
    @Mock
    private BlogTagMapper blogTagMapper;
    @Mock
    private BlogCommentMapper blogCommentMapper;
    @Mock
    private BlogLinkMapper blogLinkMapper;
    @Mock
    private BlogLearningPathMapper blogLearningPathMapper;
    @Mock
    private BlogMcpTokenMapper blogMcpTokenMapper;
    @Mock
    private BlogConfigService blogConfigService;
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private BlogCategoryService blogCategoryService;
    @Mock
    private BlogTagService blogTagService;
    @Mock
    private BlogLearningPathService blogLearningPathService;
    @Mock
    private BlogDashboardService blogDashboardService;
    @Mock
    private BlogCommentService blogCommentService;
    @Mock
    private BlogLinkService blogLinkService;
    @Mock
    private BlogSearchService blogSearchService;

    @BeforeAll
    static void initMybatisTables() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlogArticle.class);
        TableInfoHelper.initTableInfo(assistant, BlogCategory.class);
        TableInfoHelper.initTableInfo(assistant, BlogLearningPath.class);
        TableInfoHelper.initTableInfo(assistant, BlogMcpToken.class);
    }

    @AfterEach
    void cleanupContexts() {
        McpExecutionContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchServiceShouldExtractSnippetsAndMatchMindMapNodes() {
        var service = new BlogSearchServiceImpl(
                blogArticleMapper,
                blogCategoryMapper,
                blogLearningPathMapper,
                objectMapper
        );

        BlogCategory cat = new BlogCategory();
        cat.setId(10L);
        cat.setName("架构笔记");
        when(blogCategoryMapper.selectList(any())).thenReturn(List.of(cat));

        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setSlug("redis-rate-limiter");
        article.setTitle("高并发 Redis 限流器实践");
        article.setSummary("");
        article.setContent("## 滑动窗口\n使用 **Redis Lua** 脚本实现高性能[限流算法](https://example.com)并防止热点击穿。");
        article.setCategoryId(10L);
        article.setPublishTime(LocalDateTime.of(2026, 11, 20, 10, 0));
        when(blogArticleMapper.selectList(any())).thenReturn(List.of(article));

        BlogLearningPath path = new BlogLearningPath();
        path.setId(2L);
        path.setSlug("youlai-boot-fullstack");
        path.setTitle("全栈系统路线");
        path.setSummary("后端工程演进");
        path.setTreeJson("{\"id\":\"root\",\"label\":\"全栈\",\"description\":\"涵盖 Redis 缓存架构\",\"children\":[{\"id\":\"node-redis\",\"label\":\"Redis 缓存与防刷\",\"tag\":\"核心\",\"children\":[]}]}");
        when(blogLearningPathMapper.selectList(any())).thenReturn(List.of(path));

        BlogArchiveArticleVO archive = new BlogArchiveArticleVO();
        archive.setId(1L);
        archive.setPublishTime(LocalDateTime.of(2026, 11, 20, 10, 0));
        when(blogArticleMapper.listPublishedArchives()).thenReturn(List.of(archive));

        BlogSearchResultVO res = service.searchPublic("Redis", 8);

        assertThat(res.getArticles()).hasSize(1);
        assertThat(res.getArticles().get(0).getCategoryName()).isEqualTo("架构笔记");
        assertThat(res.getArticles().get(0).getSnippet())
                .contains("Redis")
                .doesNotContain("##", "**", "https://example.com");

        assertThat(res.getPaths()).hasSize(1);
        assertThat(res.getPaths().get(0).getMatchedNodeId()).isEqualTo("node-redis");
        assertThat(res.getPaths().get(0).getMatchedNodeLabel()).isEqualTo("Redis 缓存与防刷");
        assertThat(res.getArchives()).isEmpty();

        BlogSearchResultVO archiveQueryRes = service.searchPublic("2026", 8);
        assertThat(archiveQueryRes.getArchives()).hasSize(1);
        assertThat(archiveQueryRes.getArchives().get(0).getYearMonth()).isEqualTo("2026-11");
        assertThat(archiveQueryRes.getArchives().get(0).getCount()).isEqualTo(1);
    }

    @Test
    void dashboardSitemapShouldIncludePublishedLearningPaths() {
        var dashboardService = new BlogDashboardServiceImpl(
                blogArticleMapper,
                blogCategoryMapper,
                blogTagMapper,
                blogCommentMapper,
                blogLinkMapper,
                blogLearningPathMapper,
                blogConfigService
        );

        when(blogArticleMapper.selectList(any())).thenReturn(List.of());

        BlogLearningPath pathWithSlug = new BlogLearningPath();
        pathWithSlug.setId(5L);
        pathWithSlug.setSlug("spring-cloud-path");
        pathWithSlug.setStatus(1);
        pathWithSlug.setUpdateTime(LocalDateTime.of(2026, 11, 22, 12, 0));

        BlogLearningPath pathById = new BlogLearningPath();
        pathById.setId(6L);
        pathById.setSlug("");
        pathById.setStatus(1);
        when(blogLearningPathMapper.selectList(any())).thenReturn(List.of(pathWithSlug, pathById));

        String sitemap = dashboardService.buildSitemapXml("https://blog.example.com");

        assertThat(sitemap)
                .contains("<loc>https://blog.example.com/paths</loc>")
                .contains("<loc>https://blog.example.com/paths/spring-cloud-path</loc>")
                .contains("<loc>https://blog.example.com/paths/6</loc>");
    }

    @Test
    void mcpTokenServiceShouldCreateHashMaskAndAuthenticateStrictly() {
        var service = new BlogMcpTokenServiceImpl(blogMcpTokenMapper, blogConfigService);
        ReflectionTestUtils.setField(service, "bootstrapToken", "mcp_bootstrap_secret_token");

        doAnswer(inv -> {
            BlogMcpToken entity = inv.getArgument(0);
            entity.setId(99L);
            return 1;
        }).when(blogMcpTokenMapper).insert(any(BlogMcpToken.class));

        BlogMcpTokenCreateForm createForm = new BlogMcpTokenCreateForm();
        createForm.setName("Antigravity Agent");
        createForm.setScopes("write, invalid_scope");
        createForm.setExpireDays(30);

        BlogMcpCreatedTokenVO created = service.createToken(createForm);
        assertThat(created.getPlainToken()).startsWith("mcp_").hasSize(40);
        assertThat(created.getToken().getId()).isEqualTo(99L);
        assertThat(created.getToken().getTokenPrefix()).contains("...");
        assertThat(created.getToken().getScopes()).isEqualTo("read,write");

        ArgumentCaptor<BlogMcpToken> entityCaptor = ArgumentCaptor.forClass(BlogMcpToken.class);
        verify(blogMcpTokenMapper).insert(entityCaptor.capture());
        String storedHash = entityCaptor.getValue().getTokenHash();
        assertThat(storedHash).isEqualTo(DigestUtil.sha256Hex(created.getPlainToken()));

        // Valid DB token authentication
        when(blogConfigService.getConfigValue("mcp_enabled", "1")).thenReturn("1");
        BlogMcpToken dbToken = entityCaptor.getValue();
        when(blogMcpTokenMapper.selectOne(any())).thenReturn(dbToken);
        Optional<McpTokenPrincipal> authOk = service.authenticate(created.getPlainToken());
        assertThat(authOk).isPresent();
        assertThat(authOk.get().hasScope("write")).isTrue();
        assertThat(authOk.get().hasScope("admin")).isFalse();

        // Disabled token rejected
        dbToken.setStatus(0);
        assertThat(service.authenticate(created.getPlainToken())).isEmpty();

        // Expired token rejected
        dbToken.setStatus(1);
        dbToken.setExpireTime(LocalDateTime.now().minusMinutes(5));
        assertThat(service.authenticate(created.getPlainToken())).isEmpty();

        // Bootstrap token works with admin scope
        Optional<McpTokenPrincipal> bootstrapPrincipal = service.authenticate("mcp_bootstrap_secret_token");
        assertThat(bootstrapPrincipal).isPresent();
        assertThat(bootstrapPrincipal.get().tokenId()).isNull();
        assertThat(bootstrapPrincipal.get().hasScope("admin")).isTrue();

        // Master switch off blocks both DB & bootstrap tokens
        when(blogConfigService.getConfigValue("mcp_enabled", "1")).thenReturn("0");
        assertThat(service.authenticate("mcp_bootstrap_secret_token")).isEmpty();
    }

    @Test
    void mcpFilterShouldEnforceAuthAndClearThreadLocal() throws Exception {
        McpTokenAuthPort authPort = mock(McpTokenAuthPort.class);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("mcpTokenAuthPort", authPort);

        var filter = new McpTokenAuthenticationFilter(beanFactory.getBeanProvider(McpTokenAuthPort.class));

        // 1. Missing key -> 401 JSON-RPC
        MockHttpServletRequest missingReq = new MockHttpServletRequest("POST", "/api/v1/mcp");
        MockHttpServletResponse missingResp = new MockHttpServletResponse();
        FilterChain chainNeverCalled = mock(FilterChain.class);
        filter.doFilter(missingReq, missingResp, chainNeverCalled);
        assertThat(missingResp.getStatus()).isEqualTo(401);
        assertThat(missingResp.getContentAsString()).contains("\"code\":-32001");
        verify(chainNeverCalled, never()).doFilter(any(), any());

        // 2. Valid X-MCP-Key sets principal during chain and clears afterward
        McpTokenPrincipal principal = new McpTokenPrincipal(7L, "cursor", Set.of("read", "write"));
        when(authPort.authenticate("mcp_valid_key")).thenReturn(Optional.of(principal));

        MockHttpServletRequest validReq = new MockHttpServletRequest("POST", "/api/v1/mcp");
        validReq.addHeader("X-MCP-Key", "mcp_valid_key");
        MockHttpServletResponse validResp = new MockHttpServletResponse();
        AtomicReference<McpTokenPrincipal> insideChainPrincipal = new AtomicReference<>();

        filter.doFilter(validReq, validResp, (req, resp) -> insideChainPrincipal.set(McpExecutionContext.get()));

        assertThat(insideChainPrincipal.get()).isEqualTo(principal);
        assertThat(McpExecutionContext.get()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authPort).recordUsage(7L);
    }

    @Test
    void mcpToolsShouldEnforceScopesSafeDraftAndAppendMindMapNode() throws Exception {
        var tools = new BlogMcpTools(
                blogArticleService,
                blogCategoryService,
                blogTagService,
                blogLearningPathService,
                blogConfigService,
                blogDashboardService,
                objectMapper
        );

        // Read-only token blocked from creating article
        McpExecutionContext.set(new McpTokenPrincipal(1L, "reader", Set.of("read")));
        assertThatThrownBy(() -> tools.createArticle("t", "c", null, null, null, null, true, false, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("write");

        // Write token with mcp_allow_publish=0 forces draft status=0 even when publish=true
        McpExecutionContext.set(new McpTokenPrincipal(2L, "writer", Set.of("read", "write")));
        when(blogConfigService.getConfigValue("mcp_allow_publish", "0")).thenReturn("0");
        doAnswer(inv -> {
            BlogArticleForm f = inv.getArgument(0);
            f.setId(501L);
            return true;
        }).when(blogArticleService).saveArticle(any(BlogArticleForm.class));

        Map<String, Object> draftRes = tools.createArticle(
                "MCP Safe Draft", "Body text", null, "mcp-safe-draft", null, null, true, false, true);
        assertThat(draftRes.get("status")).isEqualTo(0);
        assertThat(draftRes.get("statusLabel")).isEqualTo("草稿");
        assertThat(String.valueOf(draftRes.get("notice"))).contains("mcp_allow_publish=0");

        // Write token appending mind-map child node + mounted article & video
        BlogLearningPathVO existingVo = new BlogLearningPathVO();
        existingVo.setId(1L);
        existingVo.setSlug("youlai-boot-fullstack");
        existingVo.setTitle("全栈路线");

        BlogLearningPathForm pathForm = new BlogLearningPathForm();
        pathForm.setId(1L);
        pathForm.setTitle("全栈路线");
        pathForm.setSlug("youlai-boot-fullstack");
        pathForm.setTreeJson("{\"id\":\"root\",\"label\":\"核心架构\",\"collapsed\":true,\"children\":[]}");

        BlogArticleDetailVO mountedArticle = new BlogArticleDetailVO();
        mountedArticle.setId(88L);
        mountedArticle.setSlug("mcp-guide");
        mountedArticle.setTitle("MCP 实战指南");

        BlogLearningPathVO updatedVo = new BlogLearningPathVO();
        updatedVo.setId(1L);
        updatedVo.setSlug("youlai-boot-fullstack");
        updatedVo.setNodeCount(2);
        updatedVo.setResourceCount(2);

        when(blogLearningPathService.getPublicPath("youlai-boot-fullstack")).thenReturn(existingVo);
        when(blogLearningPathService.getPathForm(1L)).thenReturn(pathForm);
        when(blogArticleService.getPublicArticleDetail("mcp-guide", "127.0.0.1", false)).thenReturn(mountedArticle);
        when(blogLearningPathService.getPublicPath("1")).thenReturn(updatedVo);

        Map<String, Object> appendRes = tools.appendMindmapNode(
                "youlai-boot-fullstack",
                "核心架构",
                "MCP 协议接入",
                "核心",
                "Spring AI Streamable HTTP",
                "mcp-guide",
                "https://www.bilibili.com/video/BV1test",
                null,
                null,
                "重点实践"
        );

        assertThat(appendRes.get("parentNodeId")).isEqualTo("root");
        assertThat(appendRes.get("mountedResourceCount")).isEqualTo(2);
        assertThat(appendRes.get("totalNodes")).isEqualTo(2);
        assertThat(String.valueOf(appendRes.get("url"))).startsWith("/paths/youlai-boot-fullstack?node=node-");

        ArgumentCaptor<BlogLearningPathForm> updatedCaptor = ArgumentCaptor.forClass(BlogLearningPathForm.class);
        verify(blogLearningPathService).updatePath(eq(1L), updatedCaptor.capture());
        MindMapNodeDTO updatedTree = objectMapper.readValue(updatedCaptor.getValue().getTreeJson(), MindMapNodeDTO.class);
        assertThat(updatedTree.getCollapsed()).isFalse();
        assertThat(updatedTree.getChildren()).hasSize(1);
        assertThat(updatedTree.getChildren().get(0).getLabel()).isEqualTo("MCP 协议接入");
        assertThat(updatedTree.getChildren().get(0).getResources()).hasSize(2);
    }

    @Test
    void controllersShouldExposeSearchAndMcpManagementRoutes() throws Exception {
        BlogMcpTokenService tokenService = mock(BlogMcpTokenService.class);
        BlogMcpController mcpController = new BlogMcpController(tokenService);

        BlogMcpOverviewVO overview = new BlogMcpOverviewVO();
        overview.setMcpEnabled(true);
        overview.setAllowPublish(false);
        overview.setEndpointPath("/api/v1/mcp");
        overview.setTools(List.of());
        overview.setTokens(List.of());
        when(tokenService.getOverview()).thenReturn(overview);

        BlogMcpCreatedTokenVO createdVo = new BlogMcpCreatedTokenVO();
        BlogMcpTokenVO itemVo = new BlogMcpTokenVO();
        itemVo.setId(3L);
        itemVo.setName("IDE Client");
        itemVo.setTokenPrefix("mcp_123456...abcd");
        createdVo.setToken(itemVo);
        createdVo.setPlainToken("mcp_1234567890abcdef");
        when(tokenService.createToken(any(BlogMcpTokenCreateForm.class))).thenReturn(createdVo);
        when(tokenService.updateStatus(3L, 0)).thenReturn(true);

        MockMvc mcpMvc = MockMvcBuilders.standaloneSetup(mcpController).build();
        mcpMvc.perform(get("/api/v1/blog/mcp/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpointPath").value("/api/v1/mcp"))
                .andExpect(jsonPath("$.data.mcpEnabled").value(true));

        mcpMvc.perform(post("/api/v1/blog/mcp/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IDE Client\",\"scopes\":\"read,write\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plainToken").value("mcp_1234567890abcdef"))
                .andExpect(jsonPath("$.data.token.name").value("IDE Client"));

        mcpMvc.perform(patch("/api/v1/blog/mcp/tokens/3/status").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        BlogPublicController publicController = new BlogPublicController(
                blogConfigService,
                blogArticleService,
                blogCategoryService,
                blogTagService,
                blogCommentService,
                blogLinkService,
                blogLearningPathService,
                blogSearchService,
                blogDashboardService
        );
        BlogSearchResultVO searchResult = new BlogSearchResultVO();
        searchResult.setArticles(List.of());
        searchResult.setPaths(List.of());
        searchResult.setArchives(List.of());
        when(blogSearchService.searchPublic("Redis", 8)).thenReturn(searchResult);

        MockMvc publicMvc = MockMvcBuilders.standaloneSetup(publicController).build();
        publicMvc.perform(get("/api/v1/blog/public/search").param("q", "Redis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.articles").isArray());
    }
}
