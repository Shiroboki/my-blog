package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.module.blog.mapper.BlogArticleTagMapper;
import com.youlai.boot.module.blog.mapper.BlogTagMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticleTag;
import com.youlai.boot.module.blog.model.entity.BlogTag;
import com.youlai.boot.module.blog.model.form.BlogTagForm;
import com.youlai.boot.module.blog.model.query.BlogTagQuery;
import com.youlai.boot.module.blog.model.vo.BlogTagVO;
import com.youlai.boot.module.blog.service.BlogTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 博客标签业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogTagServiceImpl implements BlogTagService {

    private final BlogTagMapper blogTagMapper;
    private final BlogArticleTagMapper blogArticleTagMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Page<BlogTagVO> getTagPage(BlogTagQuery queryParams) {
        Page<BlogTagVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        return blogTagMapper.getTagPage(page, queryParams);
    }

    @Override
    public List<BlogTagVO> listPublicTags() {
        return blogTagMapper.listPublicTags();
    }

    @Override
    public List<Option<Long>> listTagOptions() {
        List<BlogTag> tags = blogTagMapper.selectList(
                new LambdaQueryWrapper<BlogTag>().orderByDesc(BlogTag::getId)
        );
        return tags.stream()
                .map(t -> new Option<>(t.getId(), t.getName()))
                .toList();
    }

    @Override
    public BlogTagForm getTagForm(Long id) {
        BlogTag entity = blogTagMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("标签不存在");
        }
        BlogTagForm form = new BlogTagForm();
        form.setId(entity.getId());
        form.setName(entity.getName());
        form.setSlug(entity.getSlug());
        form.setColor(entity.getColor());
        return form;
    }

    @Override
    public boolean saveTag(BlogTagForm form) {
        validateUnique(form.getName(), form.getSlug(), null);
        BlogTag entity = new BlogTag();
        entity.setName(form.getName().trim());
        entity.setSlug(form.getSlug().trim());
        entity.setColor(form.getColor());
        boolean ok = blogTagMapper.insert(entity) > 0;
        if (ok) {
            evictCaches(false);
        }
        return ok;
    }

    @Override
    public boolean updateTag(Long id, BlogTagForm form) {
        BlogTag existing = blogTagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("标签不存在");
        }
        validateUnique(form.getName(), form.getSlug(), id);
        existing.setName(form.getName().trim());
        existing.setSlug(form.getSlug().trim());
        existing.setColor(form.getColor());
        boolean ok = blogTagMapper.updateById(existing) > 0;
        if (ok) {
            evictCaches(true);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTags(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }
        blogArticleTagMapper.delete(new LambdaQueryWrapper<BlogArticleTag>()
                .in(BlogArticleTag::getTagId, ids));
        boolean ok = blogTagMapper.deleteByIds(ids) > 0;
        if (ok) {
            evictCaches(true);
        }
        return ok;
    }

    private void validateUnique(String name, String slug, Long excludeId) {
        Long nameCount = blogTagMapper.selectCount(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getName, name.trim())
                .ne(excludeId != null, BlogTag::getId, excludeId));
        if (nameCount > 0) {
            throw new BusinessException("标签名称已存在");
        }
        Long slugCount = blogTagMapper.selectCount(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getSlug, slug.trim())
                .ne(excludeId != null, BlogTag::getId, excludeId));
        if (slugCount > 0) {
            throw new BusinessException("标签别名已存在");
        }
    }

    private void evictCaches(boolean evictArticles) {
        try {
            stringRedisTemplate.delete(RedisConstants.Blog.SITE_INFO);
            if (evictArticles) {
                Set<String> keys = stringRedisTemplate.keys("blog:article:detail:*");
                if (CollectionUtil.isNotEmpty(keys)) {
                    stringRedisTemplate.delete(keys);
                }
            }
        } catch (Exception e) {
            log.warn("清理标签相关缓存失败: {}", e.getMessage());
        }
    }
}
