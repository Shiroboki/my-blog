package com.youlai.boot.module.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.module.blog.model.entity.BlogMcpToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客 MCP API 密钥 Mapper
 */
@Mapper
public interface BlogMcpTokenMapper extends BaseMapper<BlogMcpToken> {
}
