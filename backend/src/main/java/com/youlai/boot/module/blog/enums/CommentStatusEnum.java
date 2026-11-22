package com.youlai.boot.module.blog.enums;

import com.youlai.boot.common.base.IBaseEnum;
import lombok.Getter;

/**
 * 博客评论状态枚举
 */
@Getter
public enum CommentStatusEnum implements IBaseEnum<Integer> {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer value;
    private final String label;

    CommentStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
