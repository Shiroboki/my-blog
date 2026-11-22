package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 博客文章实体
 */
@EqualsAndHashCode(callSuper = true)
@TableName("blog_article")
@Data
public class BlogArticle extends BaseEntity {

    private String title;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String slug;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;

    private String content;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String coverImage;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long categoryId;

    private Integer status;

    private Integer isTop;

    private Integer allowComment;

    private Long viewCount;

    private Long likeCount;

    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    private Integer isDeleted;
}
