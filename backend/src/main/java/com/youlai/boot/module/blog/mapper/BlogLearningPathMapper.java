package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.module.blog.model.entity.BlogLearningPath;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客学习路径访问层
 */
@Mapper
public interface BlogLearningPathMapper extends BaseMapper<BlogLearningPath> {
}
