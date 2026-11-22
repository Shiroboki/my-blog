package com.youlai.boot.module.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.form.BlogCommentForm;
import com.youlai.boot.module.blog.model.form.BlogCommentReplyForm;
import com.youlai.boot.module.blog.model.query.BlogCommentQuery;
import com.youlai.boot.module.blog.model.vo.BlogCommentVO;

import java.util.List;

/**
 * 博客评论业务接口
 */
public interface BlogCommentService {

    Page<BlogCommentVO> getCommentPage(BlogCommentQuery queryParams);

    List<BlogCommentVO> listPublicArticleComments(Long articleId);

    BlogCommentVO submitPublicComment(BlogCommentForm form, String clientIp);

    boolean auditComment(Long id, Integer status);

    boolean replyComment(BlogCommentReplyForm form, String clientIp);

    boolean deleteComments(List<Long> ids);
}
