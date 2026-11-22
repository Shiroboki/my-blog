package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客友情链接实体
 */
@EqualsAndHashCode(callSuper = true)
@TableName("blog_link")
@Data
public class BlogLink extends BaseEntity {

    private String name;

    private String url;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String avatar;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    private Integer sort;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    private Integer isDeleted;
}
