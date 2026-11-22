package com.youlai.boot.module.blog;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.blog.enums.ArticleStatusEnum;
import com.youlai.boot.module.blog.enums.CommentStatusEnum;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogArticleTagMapper;
import com.youlai.boot.module.blog.mapper.BlogCategoryMapper;
import com.youlai.boot.module.blog.mapper.BlogCommentMapper;
import com.youlai.boot.module.blog.mapper.BlogLinkMapper;
import com.youlai.boot.module.blog.mapper.BlogTagMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogArticleTag;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.entity.BlogComment;
import com.youlai.boot.module.blog.model.entity.BlogTag;
import com.youlai.boot.module.blog.model.form.BlogArticleForm;
import com.youlai.boot.module.blog.model.form.BlogCommentForm;
import com.youlai.boot.module.blog.model.form.BlogConfigForm;
import com.youlai.boot.module.blog.model.vo.BlogArticleDetailVO;
import com.youlai.boot.module.blog.model.vo.BlogCommentVO;
import com.youlai.boot.module.blog.model.vo.BlogMarkdownExportVO;
import com.youlai.boot.module.blog.service.BlogConfigService;
import com.youlai.boot.module.blog.service.impl.BlogArticleServiceImpl;
import com.youlai.boot.module.blog.service.impl.BlogCategoryServiceImpl;
import com.youlai.boot.module.blog.service.impl.BlogCommentServiceImpl;
import com.youlai.boot.module.blog.service.impl.BlogDashboardServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 博客模块核心业务单元测试
 */
@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogArticleMapper blogArticleMapper;
    @Mock
    private BlogCategoryMapper blogCategoryMapper;
    @Mock
    private BlogTagMapper blogTagMapper;
    @Mock
    private BlogArticleTagMapper blogArticleTagMapper;
    @Mock
    private BlogCommentMapper blogCommentMapper;
    @Mock
    private BlogLinkMapper blogLinkMapper;
    @Mock
    private BlogConfigService blogConfigService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BlogArticle.class);
        TableInfoHelper.initTableInfo(assistant, BlogComment.class);
        TableInfoHelper.initTableInfo(assistant, BlogCategory.class);
        TableInfoHelper.initTableInfo(assistant, BlogTag.class);
    }

    @Test
    void saveArticleShouldExtractSummaryAndSyncTags() {
        var service = new BlogArticleServiceImpl(
                blogArticleMapper,
                blogCategoryMapper,
                blogTagMapper,
                blogArticleTagMapper,
                blogCommentMapper,
                stringRedisTemplate,
                objectMapper
        );
        doAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            article.setId(100L);
            return 1;
        }).when(blogArticleMapper).insert(any(BlogArticle.class));

        BlogArticleForm form = new BlogArticleForm();
        form.setTitle("Spring Boot 4 实战");
        form.setContent("# 标题\n这是一篇介绍 **MySQL** 与 `Redis` 的文章。\n```java\nSystem.out.println();\n```");
        form.setStatus(ArticleStatusEnum.PUBLISHED.getValue());
        form.setTagIds(List.of(1L, 2L, 1L));

        boolean ok = service.saveArticle(form);

        assertThat(ok).isTrue();
        ArgumentCaptor<BlogArticle> captor = ArgumentCaptor.forClass(BlogArticle.class);
        verify(blogArticleMapper).insert(captor.capture());
        BlogArticle saved = captor.getValue();
        assertThat(saved.getSummary()).contains("标题", "MySQL", "Redis").doesNotContain("```");
        assertThat(saved.getPublishTime()).isNotNull();
        verify(blogArticleTagMapper, org.mockito.Mockito.times(2)).insert(any(BlogArticleTag.class));
        verify(stringRedisTemplate).delete(RedisConstants.Blog.SITE_INFO);
    }

    @Test
    void getPublicArticleDetailShouldDeduplicateViewCountInRedis() {
        var service = new BlogArticleServiceImpl(
                blogArticleMapper,
                blogCategoryMapper,
                blogTagMapper,
                blogArticleTagMapper,
                blogCommentMapper,
                stringRedisTemplate,
                objectMapper
        );
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        BlogArticle entity = new BlogArticle();
        entity.setId(1L);
        entity.setTitle("测试文章");
        entity.setContent("正文内容");
        entity.setStatus(ArticleStatusEnum.PUBLISHED.getValue());
        entity.setViewCount(10L);
        entity.setLikeCount(2L);
        entity.setPublishTime(LocalDateTime.now());
        when(blogArticleMapper.selectById(1L)).thenReturn(entity);
        when(blogTagMapper.listTagsByArticleIds(List.of(1L))).thenReturn(List.of());
        String detailKey = RedisConstants.Blog.ARTICLE_DETAIL.replace("{}", "1");
        when(valueOperations.get(detailKey)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(1L), eq(TimeUnit.HOURS)))
                .thenReturn(true, false);

        BlogArticleDetailVO first = service.getPublicArticleDetail(1L, "10.0.0.1");
        assertThat(first.getViewCount()).isEqualTo(11L);
        verify(blogArticleMapper).incrementViewCount(1L);

        when(valueOperations.get(detailKey)).thenReturn(objectMapper.writeValueAsString(first));
        BlogArticleDetailVO second = service.getPublicArticleDetail(1L, "10.0.0.1");
        assertThat(second.getViewCount()).isEqualTo(11L);
        verify(blogArticleMapper, org.mockito.Mockito.times(1)).incrementViewCount(1L);
    }

    @Test
    void submitPublicCommentShouldStripUnsafeWebsiteAndHideEmail() {
        var service = new BlogCommentServiceImpl(
                blogCommentMapper,
                blogArticleMapper,
                blogConfigService,
                stringRedisTemplate
        );
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setStatus(ArticleStatusEnum.PUBLISHED.getValue());
        article.setAllowComment(1);
        when(blogArticleMapper.selectById(1L)).thenReturn(article);
        when(blogConfigService.getConfigValue("comment_audit", "0")).thenReturn("0");
        when(blogCommentMapper.insert(any(BlogComment.class))).thenReturn(1);

        BlogCommentForm form = new BlogCommentForm();
        form.setArticleId(1L);
        form.setNickname("小明");
        form.setEmail("secret@example.com");
        form.setWebsite("javascript:alert(1)");
        form.setContent("写得不错");

        BlogCommentVO result = service.submitPublicComment(form, "127.0.0.1");

        assertThat(result.getEmail()).isNull();
        assertThat(result.getWebsite()).isNull();
        assertThat(result.getStatus()).isEqualTo(CommentStatusEnum.APPROVED.getValue());
        verify(stringRedisTemplate).delete(RedisConstants.Blog.ARTICLE_DETAIL.replace("{}", "1"));
    }

    @Test
    void deleteCategoryShouldFailWhenArticlesExist() {
        var service = new BlogCategoryServiceImpl(blogCategoryMapper, blogArticleMapper, stringRedisTemplate);
        when(blogArticleMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteCategories(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类下仍有文章");
        verify(blogCategoryMapper, never()).deleteByIds(any());
    }

    @Test
    void importAndExportMarkdownShouldHandleFrontMatter() {
        var service = new BlogArticleServiceImpl(
                blogArticleMapper,
                blogCategoryMapper,
                blogTagMapper,
                blogArticleTagMapper,
                blogCommentMapper,
                stringRedisTemplate,
                objectMapper
        );
        BlogCategory techCat = new BlogCategory();
        techCat.setId(1L);
        techCat.setName("技术笔记");
        when(blogCategoryMapper.selectOne(any())).thenReturn(techCat);
        when(blogTagMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            BlogTag tag = invocation.getArgument(0);
            tag.setId(9L);
            return 1;
        }).when(blogTagMapper).insert(any(BlogTag.class));
        doAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            article.setId(88L);
            return 1;
        }).when(blogArticleMapper).insert(any(BlogArticle.class));

        String rawMd = """
                ---
                title: "Markdown 导入测试"
                category: "技术笔记"
                tags: [Spring Boot, Vue 3]
                summary: "测试摘要"
                ---

                # 正文标题
                正文内容
                """;
        Long createdId = service.importArticleMarkdown("demo.md", rawMd);
        assertThat(createdId).isEqualTo(88L);
        ArgumentCaptor<BlogArticle> captor = ArgumentCaptor.forClass(BlogArticle.class);
        verify(blogArticleMapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Markdown 导入测试");
        assertThat(captor.getValue().getCategoryId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(ArticleStatusEnum.DRAFT.getValue());

        BlogArticle exportedEntity = new BlogArticle();
        exportedEntity.setId(88L);
        exportedEntity.setTitle("Markdown 导入测试");
        exportedEntity.setSlug("md-import");
        exportedEntity.setSummary("测试摘要");
        exportedEntity.setContent("正文内容");
        exportedEntity.setCategoryId(1L);
        exportedEntity.setPublishTime(LocalDateTime.of(2026, 11, 22, 10, 0));
        when(blogArticleMapper.selectById(88L)).thenReturn(exportedEntity);
        when(blogCategoryMapper.selectById(1L)).thenReturn(techCat);
        when(blogTagMapper.listTagsByArticleIds(List.of(88L))).thenReturn(List.of());

        BlogMarkdownExportVO exported = service.exportArticleMarkdown(88L);
        assertThat(exported.getFileName()).isEqualTo("md-import.md");
        assertThat(exported.getContent()).contains("title: \"Markdown 导入测试\"", "category: \"技术笔记\"", "正文内容");
    }

    @Test
    void dashboardServiceShouldEscapeRssAndSitemapXml() {
        var dashboardService = new BlogDashboardServiceImpl(
                blogArticleMapper,
                blogCategoryMapper,
                blogTagMapper,
                blogCommentMapper,
                blogLinkMapper,
                blogConfigService
        );
        BlogConfigForm config = new BlogConfigForm();
        config.setSiteTitle("MyBlog & Notes");
        config.setSiteSubtitle("Spring <Boot> & Vue");
        when(blogConfigService.getConfigForm()).thenReturn(config);

        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setTitle("Hello <World> & Blog");
        article.setSummary("Summary & details");
        article.setPublishTime(LocalDateTime.of(2026, 11, 22, 12, 0));
        when(blogArticleMapper.selectList(any())).thenReturn(List.of(article));

        String rss = dashboardService.buildRssXml("http://localhost:9123/");
        assertThat(rss)
                .contains("<rss version=\"2.0\">")
                .contains("<title>MyBlog &amp; Notes</title>")
                .contains("<title>Hello &lt;World&gt; &amp; Blog</title>")
                .contains("<link>http://localhost:9123/article/1</link>");

        String sitemap = dashboardService.buildSitemapXml("http://localhost:9123");
        assertThat(sitemap)
                .contains("<loc>http://localhost:9123/</loc>")
                .contains("<loc>http://localhost:9123/links</loc>")
                .contains("<loc>http://localhost:9123/article/1</loc>");
    }

    @Test
    void submitPublicCommentShouldPersistAndValidateQuoteAnchor() {
        var service = new BlogCommentServiceImpl(
                blogCommentMapper,
                blogArticleMapper,
                blogConfigService,
                stringRedisTemplate
        );
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setStatus(ArticleStatusEnum.PUBLISHED.getValue());
        article.setAllowComment(1);
        when(blogArticleMapper.selectById(1L)).thenReturn(article);
        when(blogConfigService.getConfigValue("comment_audit", "0")).thenReturn("0");
        when(blogCommentMapper.insert(any(BlogComment.class))).thenReturn(1);

        BlogCommentForm validForm = new BlogCommentForm();
        validForm.setArticleId(1L);
        validForm.setContent("这句总结很到位");
        validForm.setQuoteText("  youlai-boot 可以把精力集中在博客内容本身 ");
        validForm.setQuoteStart(15);
        validForm.setQuoteEnd(45);

        BlogCommentVO result = service.submitPublicComment(validForm, "127.0.0.1");
        assertThat(result.getNickname()).isEqualTo("匿名读者");
        assertThat(result.getQuoteText()).isEqualTo("youlai-boot 可以把精力集中在博客内容本身");
        assertThat(result.getQuoteStart()).isEqualTo(15);
        assertThat(result.getQuoteEnd()).isEqualTo(45);

        BlogCommentForm invalidRangeForm = new BlogCommentForm();
        invalidRangeForm.setArticleId(1L);
        invalidRangeForm.setNickname("小李");
        invalidRangeForm.setContent("测试错误区间");
        invalidRangeForm.setQuoteText("abc");
        invalidRangeForm.setQuoteStart(30);
        invalidRangeForm.setQuoteEnd(10);

        assertThatThrownBy(() -> service.submitPublicComment(invalidRangeForm, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("划词位置区间无效");
    }
}
