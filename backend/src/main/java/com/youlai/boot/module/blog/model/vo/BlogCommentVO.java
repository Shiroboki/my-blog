package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 博客评论视图对象
 */
@Data
@Schema(description = "博客评论视图对象")
public class BlogCommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "文章标题（后台列表展示）")
    private String articleTitle;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱（仅后台展示）")
    private String email;

    @Schema(description = "个人主页")
    private String website;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "划词引用原文")
    private String quoteText;

    @Schema(description = "划词起始字符偏移")
    private Integer quoteStart;

    @Schema(description = "划词结束字符偏移")
    private Integer quoteEnd;

    @Schema(description = "IP属地")
    private String ipRegion;

    @Schema(description = "状态(0待审核 1已通过 2已拒绝)")
    private Integer status;

    @Schema(description = "是否博主回复(0否 1是)")
    private Integer isAdmin;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;

    @Schema(description = "子回复列表")
    private List<BlogCommentVO> children = new ArrayList<>();
}
