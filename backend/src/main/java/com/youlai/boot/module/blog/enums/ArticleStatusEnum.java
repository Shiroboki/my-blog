package com.youlai.boot.module.blog.enums;

import com.youlai.boot.common.base.IBaseEnum;
import lombok.Getter;

/**
 * 博客文章状态枚举
 */
@Getter
public enum ArticleStatusEnum implements IBaseEnum<Integer> {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    private final Integer value;
    private final String label;

    ArticleStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
