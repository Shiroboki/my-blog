package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客评论实体
 */
@EqualsAndHashCode(callSuper = true)
@TableName("blog_comment")
@Data
public class BlogComment extends BaseEntity {

    private Long articleId;

    private Long parentId;

    private String nickname;

    private String email;

    private String website;

    private String content;

    private String quoteText;

    private Integer quoteStart;

    private Integer quoteEnd;

    private String ipAddress;

    private String ipRegion;

    private Integer status;

    private Integer isAdmin;

    private Integer isDeleted;
}
