package com.youlai.boot.module.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.module.blog.model.form.BlogCategoryForm;
import com.youlai.boot.module.blog.model.query.BlogCategoryQuery;
import com.youlai.boot.module.blog.model.vo.BlogCategoryVO;

import java.util.List;

/**
 * 博客分类业务接口
 */
public interface BlogCategoryService {

    Page<BlogCategoryVO> getCategoryPage(BlogCategoryQuery queryParams);

    List<BlogCategoryVO> listPublicCategories();

    List<Option<Long>> listCategoryOptions();

    BlogCategoryForm getCategoryForm(Long id);

    boolean saveCategory(BlogCategoryForm form);

    boolean updateCategory(Long id, BlogCategoryForm form);

    boolean deleteCategories(List<Long> ids);
}
