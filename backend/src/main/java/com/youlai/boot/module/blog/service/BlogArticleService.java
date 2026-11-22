package com.youlai.boot.module.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.form.BlogArticleForm;
import com.youlai.boot.module.blog.model.query.BlogArticleQuery;
import com.youlai.boot.module.blog.model.vo.BlogArchiveVO;
import com.youlai.boot.module.blog.model.vo.BlogArticleDetailVO;
import com.youlai.boot.module.blog.model.vo.BlogArticlePageVO;
import com.youlai.boot.module.blog.model.vo.BlogMarkdownExportVO;

import java.util.List;

/**
 * 博客文章业务接口
 */
public interface BlogArticleService {

    Page<BlogArticlePageVO> getArticlePage(BlogArticleQuery queryParams);

    Page<BlogArticlePageVO> getPublicArticlePage(BlogArticleQuery queryParams);

    BlogArticleForm getArticleForm(Long id);

    boolean saveArticle(BlogArticleForm form);

    boolean updateArticle(Long id, BlogArticleForm form);

    boolean updateArticleStatus(Long id, Integer status);

    boolean updateArticleTop(Long id, Integer isTop);

    boolean deleteArticles(List<Long> ids);

    BlogArticleDetailVO getPublicArticleDetail(Long id, String clientIp);

    Long likeArticle(Long id, String clientIp);

    List<BlogArchiveVO> listArchives();

    BlogMarkdownExportVO exportArticleMarkdown(Long id);

    Long importArticleMarkdown(String originalFilename, String rawContent);
}
