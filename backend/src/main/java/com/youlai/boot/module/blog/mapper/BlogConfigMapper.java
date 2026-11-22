package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.module.blog.model.entity.BlogConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客站点配置访问层
 */
@Mapper
public interface BlogConfigMapper extends BaseMapper<BlogConfig> {
}
