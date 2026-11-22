package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章-标签关联实体
 */
@TableName("blog_article_tag")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogArticleTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long articleId;

    private Long tagId;
}
