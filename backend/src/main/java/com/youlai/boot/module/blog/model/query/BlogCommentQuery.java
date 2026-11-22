package com.youlai.boot.module.blog.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "评论查询参数")
public class BlogCommentQuery extends BaseQuery {

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "关键字（昵称/内容）")
    private String keywords;

    @Schema(description = "状态(0待审核 1已通过 2已拒绝)")
    private Integer status;
}
