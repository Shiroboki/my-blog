package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.query.BlogArticleQuery;
import com.youlai.boot.module.blog.model.vo.BlogArchiveArticleVO;
import com.youlai.boot.module.blog.model.vo.BlogArticlePageVO;
import com.youlai.boot.module.blog.model.vo.BlogHotArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 博客文章访问层
 */
@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    Page<BlogArticlePageVO> getArticlePage(
            Page<BlogArticlePageVO> page,
            @Param("queryParams") BlogArticleQuery queryParams,
            @Param("onlyPublished") boolean onlyPublished
    );

    int incrementViewCount(@Param("id") Long id);

    int incrementLikeCount(@Param("id") Long id);

    Long sumPublishedViews();

    List<BlogHotArticleVO> listHotArticles(@Param("limit") int limit);

    List<BlogArchiveArticleVO> listPublishedArchives();
}
