package com.youlai.boot.module.blog.service;

import com.youlai.boot.module.blog.model.form.BlogConfigForm;
import com.youlai.boot.module.blog.model.vo.BlogSiteInfoVO;

/**
 * 博客站点配置业务接口
 */
public interface BlogConfigService {

    BlogConfigForm getConfigForm();

    boolean saveConfigForm(BlogConfigForm form);

    String getConfigValue(String key, String defaultValue);

    BlogSiteInfoVO getSiteInfo();

    void evictSiteInfoCache();
}
