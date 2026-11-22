package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogCategoryMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.form.BlogCategoryForm;
import com.youlai.boot.module.blog.model.query.BlogCategoryQuery;
import com.youlai.boot.module.blog.model.vo.BlogCategoryVO;
import com.youlai.boot.module.blog.service.BlogCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 博客分类业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl implements BlogCategoryService {

    private final BlogCategoryMapper blogCategoryMapper;
    private final BlogArticleMapper blogArticleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Page<BlogCategoryVO> getCategoryPage(BlogCategoryQuery queryParams) {
        Page<BlogCategoryVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        return blogCategoryMapper.getCategoryPage(page, queryParams);
    }

    @Override
    public List<BlogCategoryVO> listPublicCategories() {
        return blogCategoryMapper.listPublicCategories();
    }

    @Override
    public List<Option<Long>> listCategoryOptions() {
        List<BlogCategory> categories = blogCategoryMapper.selectList(
                new LambdaQueryWrapper<BlogCategory>().orderByAsc(BlogCategory::getSort).orderByDesc(BlogCategory::getId)
        );
        return categories.stream()
                .map(c -> new Option<>(c.getId(), c.getName()))
                .toList();
    }

    @Override
    public BlogCategoryForm getCategoryForm(Long id) {
        BlogCategory entity = blogCategoryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("分类不存在");
        }
        BlogCategoryForm form = new BlogCategoryForm();
        form.setId(entity.getId());
        form.setName(entity.getName());
        form.setSlug(entity.getSlug());
        form.setDescription(entity.getDescription());
        form.setSort(entity.getSort());
        return form;
    }

    @Override
    public boolean saveCategory(BlogCategoryForm form) {
        validateUnique(form.getName(), form.getSlug(), null);
        BlogCategory entity = new BlogCategory();
        entity.setName(form.getName().trim());
        entity.setSlug(form.getSlug().trim());
        entity.setDescription(form.getDescription());
        entity.setSort(form.getSort() != null ? form.getSort() : 0);
        boolean ok = blogCategoryMapper.insert(entity) > 0;
        if (ok) {
            evictCaches(false);
        }
        return ok;
    }

    @Override
    public boolean updateCategory(Long id, BlogCategoryForm form) {
        BlogCategory existing = blogCategoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }
        validateUnique(form.getName(), form.getSlug(), id);
        existing.setName(form.getName().trim());
        existing.setSlug(form.getSlug().trim());
        existing.setDescription(form.getDescription());
        existing.setSort(form.getSort() != null ? form.getSort() : 0);
        boolean ok = blogCategoryMapper.updateById(existing) > 0;
        if (ok) {
            evictCaches(true);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategories(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }
        Long articleCount = blogArticleMapper.selectCount(new LambdaQueryWrapper<BlogArticle>()
                .in(BlogArticle::getCategoryId, ids));
        if (articleCount > 0) {
            throw new BusinessException("所选分类下仍有文章，请先调整文章分类后再删除");
        }
        boolean ok = blogCategoryMapper.deleteByIds(ids) > 0;
        if (ok) {
            evictCaches(false);
        }
        return ok;
    }

    private void validateUnique(String name, String slug, Long excludeId) {
        Long nameCount = blogCategoryMapper.selectCount(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getName, name.trim())
                .ne(excludeId != null, BlogCategory::getId, excludeId));
        if (nameCount > 0) {
            throw new BusinessException("分类名称已存在");
        }
        Long slugCount = blogCategoryMapper.selectCount(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getSlug, slug.trim())
                .ne(excludeId != null, BlogCategory::getId, excludeId));
        if (slugCount > 0) {
            throw new BusinessException("分类别名已存在");
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
            log.warn("清理分类相关缓存失败: {}", e.getMessage());
        }
    }
}
