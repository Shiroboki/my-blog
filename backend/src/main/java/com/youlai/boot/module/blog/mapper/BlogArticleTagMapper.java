package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章-标签关联访问层
 */
@Mapper
public interface BlogArticleTagMapper extends BaseMapper<BlogArticleTag> {
}
