package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 友情链接视图对象
 */
@Data
@Schema(description = "友情链接视图对象")
public class BlogLinkVO {

    @Schema(description = "友链ID")
    private Long id;

    @Schema(description = "站点名称")
    private String name;

    @Schema(description = "站点地址")
    private String url;

    @Schema(description = "站点图标")
    private String avatar;

    @Schema(description = "站点简介")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态(1启用 0停用)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
