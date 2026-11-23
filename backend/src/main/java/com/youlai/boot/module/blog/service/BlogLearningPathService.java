package com.youlai.boot.module.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.form.BlogLearningPathForm;
import com.youlai.boot.module.blog.model.query.BlogLearningPathQuery;
import com.youlai.boot.module.blog.model.vo.BlogLearningPathVO;

import java.util.List;

/**
 * 博客学习路径与思维导图业务接口
 */
public interface BlogLearningPathService {

    Page<BlogLearningPathVO> getPathPage(BlogLearningPathQuery queryParams);

    List<BlogLearningPathVO> listPublicPaths();

    BlogLearningPathVO getPublicPath(String idOrSlug);

    BlogLearningPathForm getPathForm(Long id);

    Long savePath(BlogLearningPathForm form);

    boolean updatePath(Long id, BlogLearningPathForm form);

    boolean deletePaths(List<Long> ids);
}
