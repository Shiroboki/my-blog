package com.youlai.boot.module.blog.service;

import com.youlai.boot.module.blog.model.vo.BlogDashboardVO;

/**
 * 博客概览与订阅生成服务接口
 */
public interface BlogDashboardService {

    BlogDashboardVO getDashboardStats();

    String buildRssXml(String baseUrl);

    String buildSitemapXml(String baseUrl);
}
