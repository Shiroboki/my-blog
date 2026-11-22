package com.youlai.boot.module.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.form.BlogLinkForm;
import com.youlai.boot.module.blog.model.query.BlogLinkQuery;
import com.youlai.boot.module.blog.model.vo.BlogLinkVO;

import java.util.List;

/**
 * 博客友情链接业务接口
 */
public interface BlogLinkService {

    Page<BlogLinkVO> getLinkPage(BlogLinkQuery queryParams);

    List<BlogLinkVO> listPublicLinks();

    BlogLinkForm getLinkForm(Long id);

    boolean saveLink(BlogLinkForm form);

    boolean updateLink(Long id, BlogLinkForm form);

    boolean deleteLinks(List<Long> ids);
}
