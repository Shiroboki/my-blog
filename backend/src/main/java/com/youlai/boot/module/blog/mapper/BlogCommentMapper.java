package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.entity.BlogComment;
import com.youlai.boot.module.blog.model.query.BlogCommentQuery;
import com.youlai.boot.module.blog.model.vo.BlogCommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 博客评论访问层
 */
@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {

    Page<BlogCommentVO> getCommentPage(Page<BlogCommentVO> page, @Param("queryParams") BlogCommentQuery queryParams);
}
