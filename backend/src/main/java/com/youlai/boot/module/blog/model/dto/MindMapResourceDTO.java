package com.youlai.boot.module.blog.model.dto;

import lombok.Data;

/**
 * 思维导图节点挂载资源（文章 / 视频 / 网页）
 */
@Data
public class MindMapResourceDTO {

    private String id;

    private String type;

    private String title;

    private String articleId;

    private String articleSlug;

    private String url;

    private String note;
}
