package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客标签实体
 */
@EqualsAndHashCode(callSuper = true)
@TableName("blog_tag")
@Data
public class BlogTag extends BaseEntity {

    private String name;

    private String slug;

    private String color;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    private Integer isDeleted;
}
