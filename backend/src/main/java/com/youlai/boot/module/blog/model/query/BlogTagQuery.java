package com.youlai.boot.module.blog.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "标签查询参数")
public class BlogTagQuery extends BaseQuery {

    @Schema(description = "关键字（名称/别名）")
    private String keywords;
}
