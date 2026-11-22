package com.youlai.boot.module.blog.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 博客站点配置表单
 */
@Data
@Schema(description = "博客站点配置表单")
public class BlogConfigForm {

    @Schema(description = "站点标题")
    @NotBlank(message = "站点标题不能为空")
    private String siteTitle;

    @Schema(description = "站点副标题")
    private String siteSubtitle;

    @Schema(description = "博主昵称")
    @NotBlank(message = "博主昵称不能为空")
    private String authorName;

    @Schema(description = "博主简介")
    private String authorBio;

    @Schema(description = "博主头像")
    private String authorAvatar;

    @Schema(description = "GitHub 主页")
    private String githubUrl;

    @Schema(description = "ICP 备案号")
    private String icpNumber;

    @Schema(description = "评论是否需要审核(0否 1是)")
    private String commentAudit = "0";

    @Schema(description = "是否启用 Giscus(0否 1是)")
    private String giscusEnabled = "0";

    @Schema(description = "Giscus 仓库(owner/repo)")
    private String giscusRepo;

    @Schema(description = "Giscus Repository ID")
    private String giscusRepoId;

    @Schema(description = "Giscus 分类名称")
    private String giscusCategory;

    @Schema(description = "Giscus Category ID")
    private String giscusCategoryId;

    @Schema(description = "Giscus 映射方式")
    private String giscusMapping = "pathname";

    @Schema(description = "关于页面 Markdown 内容")
    private String aboutContent;
}
