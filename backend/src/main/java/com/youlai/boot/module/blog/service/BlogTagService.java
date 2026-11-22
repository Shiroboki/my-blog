package com.youlai.boot.module.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.module.blog.model.form.BlogTagForm;
import com.youlai.boot.module.blog.model.query.BlogTagQuery;
import com.youlai.boot.module.blog.model.vo.BlogTagVO;

import java.util.List;

/**
 * 博客标签业务接口
 */
public interface BlogTagService {

    Page<BlogTagVO> getTagPage(BlogTagQuery queryParams);

    List<BlogTagVO> listPublicTags();

    List<Option<Long>> listTagOptions();

    BlogTagForm getTagForm(Long id);

    boolean saveTag(BlogTagForm form);

    boolean updateTag(Long id, BlogTagForm form);

    boolean deleteTags(List<Long> ids);
}
