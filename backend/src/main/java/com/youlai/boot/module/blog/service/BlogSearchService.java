package com.youlai.boot.module.blog.service;

import com.youlai.boot.module.blog.model.vo.BlogSearchResultVO;

/**
 * 博客全局搜索服务接口
 */
public interface BlogSearchService {

    /**
     * 按关键字搜索已发布文章正文片段、学习路径导图节点与归档月份
     */
    BlogSearchResultVO searchPublic(String query, int limit);
}
