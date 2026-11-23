package com.youlai.boot.module.blog.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客学习路径分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "学习路径查询参数")
public class BlogLearningPathQuery extends BaseQuery {

    @Schema(description = "关键字（标题/别名/简介）")
    private String keywords;

    @Schema(description = "状态(0草稿 1已发布)")
    private Integer status;
}
