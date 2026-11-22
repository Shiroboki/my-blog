package com.youlai.boot.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客站点配置实体
 */
@EqualsAndHashCode(callSuper = true)
@TableName("blog_config")
@Data
public class BlogConfig extends BaseEntity {

    private String configKey;

    private String configValue;

    private String configDesc;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    private Integer isDeleted;
}
