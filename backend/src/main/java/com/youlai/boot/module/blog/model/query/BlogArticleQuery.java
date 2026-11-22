package com.youlai.boot.module.blog.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文章查询参数")
public class BlogArticleQuery extends BaseQuery {

    @Schema(description = "关键字（标题/摘要）")
    private String keywords;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "状态(0草稿 1已发布)")
    private Integer status;
}
