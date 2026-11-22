package com.youlai.boot.module.blog.model.vo;

import lombok.Data;

/**
 * 批量查询文章标签关联行对象（内部拼装用）
 */
@Data
public class BlogArticleTagRowVO {

    private Long articleId;

    private Long id;

    private String name;

    private String slug;

    private String color;
}
