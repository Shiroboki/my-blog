package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.blog.model.entity.BlogCategory;
import com.youlai.boot.module.blog.model.query.BlogCategoryQuery;
import com.youlai.boot.module.blog.model.vo.BlogCategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 博客分类访问层
 */
@Mapper
public interface BlogCategoryMapper extends BaseMapper<BlogCategory> {

    Page<BlogCategoryVO> getCategoryPage(Page<BlogCategoryVO> page, @Param("queryParams") BlogCategoryQuery queryParams);

    List<BlogCategoryVO> listPublicCategories();
}
