package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.module.blog.enums.ArticleStatusEnum;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogCategoryMapper;
import com.youlai.boot.module.blog.mapper.BlogConfigMapper;
import com.youlai.boot.module.blog.mapper.BlogTagMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.entity.BlogConfig;
import com.youlai.boot.module.blog.model.entity.BlogTag;
import com.youlai.boot.module.blog.model.form.BlogConfigForm;
import com.youlai.boot.module.blog.model.vo.BlogSiteInfoVO;
import com.youlai.boot.module.blog.service.BlogConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 博客站点配置业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogConfigServiceImpl implements BlogConfigService {

    private static final long SITE_INFO_TTL_MINUTES = 10;

    private final BlogConfigMapper blogConfigMapper;
    private final BlogArticleMapper blogArticleMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final BlogTagMapper blogTagMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper objectMapper;

    @Override
    public BlogConfigForm getConfigForm() {
        Map<String, String> configMap = loadConfigMap();
        BlogConfigForm form = new BlogConfigForm();
        form.setSiteTitle(configMap.getOrDefault("site_title", "MyBlog · 个人技术博客"));
        form.setSiteSubtitle(configMap.getOrDefault("site_subtitle", "基于 Spring Boot + Vue + MySQL + Redis 构建"));
        form.setAuthorName(configMap.getOrDefault("author_name", "博主"));
        form.setAuthorBio(configMap.getOrDefault("author_bio", ""));
        form.setAuthorAvatar(configMap.getOrDefault("author_avatar", ""));
        form.setGithubUrl(configMap.getOrDefault("github_url", ""));
        form.setIcpNumber(configMap.getOrDefault("icp_number", ""));
        form.setCommentAudit(configMap.getOrDefault("comment_audit", "0"));
        form.setGiscusEnabled(configMap.getOrDefault("giscus_enabled", "0"));
        form.setGiscusRepo(configMap.getOrDefault("giscus_repo", ""));
        form.setGiscusRepoId(configMap.getOrDefault("giscus_repo_id", ""));
        form.setGiscusCategory(configMap.getOrDefault("giscus_category", ""));
        form.setGiscusCategoryId(configMap.getOrDefault("giscus_category_id", ""));
        form.setGiscusMapping(configMap.getOrDefault("giscus_mapping", "pathname"));
        form.setAboutContent(configMap.getOrDefault("about_content", ""));
        return form;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConfigForm(BlogConfigForm form) {
        upsertConfig("site_title", StrUtil.blankToDefault(form.getSiteTitle(), "MyBlog"), "站点主标题");
        upsertConfig("site_subtitle", StrUtil.nullToEmpty(form.getSiteSubtitle()), "站点副标题");
        upsertConfig("author_name", StrUtil.blankToDefault(form.getAuthorName(), "博主"), "博主昵称");
        upsertConfig("author_bio", StrUtil.nullToEmpty(form.getAuthorBio()), "博主简介");
        upsertConfig("author_avatar", StrUtil.nullToEmpty(form.getAuthorAvatar()), "博主头像");
        upsertConfig("github_url", StrUtil.nullToEmpty(form.getGithubUrl()), "GitHub 主页");
        upsertConfig("icp_number", StrUtil.nullToEmpty(form.getIcpNumber()), "ICP 备案号");
        upsertConfig("comment_audit", "1".equals(form.getCommentAudit()) ? "1" : "0", "评论是否需要审核");
        upsertConfig("giscus_enabled", "1".equals(form.getGiscusEnabled()) ? "1" : "0", "是否启用 Giscus GitHub 讨论区");
        upsertConfig("giscus_repo", StrUtil.trimToEmpty(form.getGiscusRepo()), "Giscus GitHub 仓库");
        upsertConfig("giscus_repo_id", StrUtil.trimToEmpty(form.getGiscusRepoId()), "Giscus Repository ID");
        upsertConfig("giscus_category", StrUtil.trimToEmpty(form.getGiscusCategory()), "Giscus Discussion 分类名称");
        upsertConfig("giscus_category_id", StrUtil.trimToEmpty(form.getGiscusCategoryId()), "Giscus Category ID");
        upsertConfig("giscus_mapping", StrUtil.blankToDefault(StrUtil.trim(form.getGiscusMapping()), "pathname"), "Giscus 页面映射方式");
        upsertConfig("about_content", StrUtil.nullToEmpty(form.getAboutContent()), "关于页面 Markdown 内容");
        evictSiteInfoCache();
        return true;
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        BlogConfig config = blogConfigMapper.selectOne(new LambdaQueryWrapper<BlogConfig>()
                .eq(BlogConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    @Override
    public BlogSiteInfoVO getSiteInfo() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.Blog.SITE_INFO);
            if (StrUtil.isNotBlank(cached)) {
                return objectMapper.readValue(cached, BlogSiteInfoVO.class);
            }
        } catch (Exception e) {
            log.warn("读取博客站点信息缓存失败，降级查询数据库: {}", e.getMessage());
        }

        BlogConfigForm config = getConfigForm();
        BlogSiteInfoVO siteInfo = new BlogSiteInfoVO();
        siteInfo.setSiteTitle(config.getSiteTitle());
        siteInfo.setSiteSubtitle(config.getSiteSubtitle());
        siteInfo.setAuthorName(config.getAuthorName());
        siteInfo.setAuthorBio(config.getAuthorBio());
        siteInfo.setAuthorAvatar(config.getAuthorAvatar());
        siteInfo.setGithubUrl(config.getGithubUrl());
        siteInfo.setIcpNumber(config.getIcpNumber());
        siteInfo.setCommentAudit(config.getCommentAudit());
        siteInfo.setGiscusEnabled(config.getGiscusEnabled());
        siteInfo.setGiscusRepo(config.getGiscusRepo());
        siteInfo.setGiscusRepoId(config.getGiscusRepoId());
        siteInfo.setGiscusCategory(config.getGiscusCategory());
        siteInfo.setGiscusCategoryId(config.getGiscusCategoryId());
        siteInfo.setGiscusMapping(config.getGiscusMapping());
        siteInfo.setAboutContent(config.getAboutContent());

        siteInfo.setArticleCount(blogArticleMapper.selectCount(new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getStatus, ArticleStatusEnum.PUBLISHED.getValue())));
        siteInfo.setCategoryCount(blogCategoryMapper.selectCount(new LambdaQueryWrapper<BlogCategory>()));
        siteInfo.setTagCount(blogTagMapper.selectCount(new LambdaQueryWrapper<BlogTag>()));
        Long totalViews = blogArticleMapper.sumPublishedViews();
        siteInfo.setTotalViews(totalViews != null ? totalViews : 0L);
        siteInfo.setHotArticles(blogArticleMapper.listHotArticles(5));

        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.Blog.SITE_INFO,
                    objectMapper.writeValueAsString(siteInfo),
                    SITE_INFO_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入博客站点信息缓存失败: {}", e.getMessage());
        }
        return siteInfo;
    }

    @Override
    public void evictSiteInfoCache() {
        try {
            stringRedisTemplate.delete(RedisConstants.Blog.SITE_INFO);
        } catch (Exception e) {
            log.warn("清理博客站点信息缓存失败: {}", e.getMessage());
        }
    }

    private Map<String, String> loadConfigMap() {
        List<BlogConfig> list = blogConfigMapper.selectList(new LambdaQueryWrapper<>());
        return list.stream().collect(Collectors.toMap(
                BlogConfig::getConfigKey,
                item -> StrUtil.nullToEmpty(item.getConfigValue()),
                (a, b) -> b
        ));
    }

    private void upsertConfig(String key, String value, String desc) {
        BlogConfig existing = blogConfigMapper.selectOne(new LambdaQueryWrapper<BlogConfig>()
                .eq(BlogConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            BlogConfig entity = new BlogConfig();
            entity.setConfigKey(key);
            entity.setConfigValue(value);
            entity.setConfigDesc(desc);
            blogConfigMapper.insert(entity);
        } else {
            existing.setConfigValue(value);
            blogConfigMapper.updateById(existing);
        }
    }
}
