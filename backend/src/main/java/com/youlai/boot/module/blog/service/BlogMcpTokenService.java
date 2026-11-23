package com.youlai.boot.module.blog.service;

import com.youlai.boot.module.blog.model.form.BlogMcpSettingsForm;
import com.youlai.boot.module.blog.model.form.BlogMcpTokenCreateForm;
import com.youlai.boot.module.blog.model.vo.BlogMcpCreatedTokenVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpOverviewVO;

/**
 * 博客 MCP 接入与 API 密钥业务接口
 */
public interface BlogMcpTokenService {

    BlogMcpOverviewVO getOverview();

    boolean updateSettings(BlogMcpSettingsForm form);

    BlogMcpCreatedTokenVO createToken(BlogMcpTokenCreateForm form);

    boolean updateStatus(Long id, Integer status);

    boolean deleteToken(Long id);
}
