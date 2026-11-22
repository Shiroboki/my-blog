package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.module.blog.model.entity.BlogLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客友情链接访问层
 */
@Mapper
public interface BlogLinkMapper extends BaseMapper<BlogLink> {
}
