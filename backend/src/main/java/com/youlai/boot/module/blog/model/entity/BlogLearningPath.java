package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客学习路径与思维导图实体
 */
@EqualsAndHashCode(callSuper = true)
@TableName("blog_learning_path")
@Data
public class BlogLearningPath extends BaseEntity {

    private String title;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String slug;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String coverColor;

    private String treeJson;

    private Integer sort;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    private Integer isDeleted;
}
