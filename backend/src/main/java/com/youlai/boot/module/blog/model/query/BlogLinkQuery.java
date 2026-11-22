package com.youlai.boot.module.blog.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 友情链接查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "友情链接查询参数")
public class BlogLinkQuery extends BaseQuery {

    @Schema(description = "关键字（名称/地址）")
    private String keywords;

    @Schema(description = "状态(1启用 0停用)")
    private Integer status;
}
