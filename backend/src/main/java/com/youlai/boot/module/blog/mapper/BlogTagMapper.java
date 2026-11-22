package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.entity.BlogTag;
import com.youlai.boot.module.blog.model.query.BlogTagQuery;
import com.youlai.boot.module.blog.model.vo.BlogArticleTagRowVO;
import com.youlai.boot.module.blog.model.vo.BlogTagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 博客标签访问层
 */
@Mapper
public interface BlogTagMapper extends BaseMapper<BlogTag> {

    Page<BlogTagVO> getTagPage(Page<BlogTagVO> page, @Param("queryParams") BlogTagQuery queryParams);

    List<BlogTagVO> listPublicTags();

    List<BlogArticleTagRowVO> listTagsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
