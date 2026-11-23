package com.youlai.boot.module.blog.controller;

import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.module.blog.model.form.BlogMcpSettingsForm;
import com.youlai.boot.module.blog.model.form.BlogMcpTokenCreateForm;
import com.youlai.boot.module.blog.model.vo.BlogMcpCreatedTokenVO;
import com.youlai.boot.module.blog.model.vo.BlogMcpOverviewVO;
import com.youlai.boot.module.blog.service.BlogMcpTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 博客 MCP 接入与 API 密钥管理控制层
 */
@Tag(name = "23.博客 MCP 接入管理")
@RestController
@RequestMapping("/api/v1/blog/mcp")
@RequiredArgsConstructor
public class BlogMcpController {

    private final BlogMcpTokenService blogMcpTokenService;

    @Operation(summary = "获取 MCP 接入总览、工具清单与密钥列表")
    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPerm('blog:mcp:list')")
    public Result<BlogMcpOverviewVO> getOverview() {
        return Result.success(blogMcpTokenService.getOverview());
    }

    @Operation(summary = "更新 MCP 接入与发布权限配置")
    @PutMapping("/settings")
    @PreAuthorize("@ss.hasPerm('blog:mcp:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateSettings(@RequestBody @Valid BlogMcpSettingsForm form) {
        return Result.judge(blogMcpTokenService.updateSettings(form));
    }

    @Operation(summary = "创建 MCP API 密钥")
    @PostMapping("/tokens")
    @PreAuthorize("@ss.hasPerm('blog:mcp:create')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<BlogMcpCreatedTokenVO> createToken(@RequestBody @Valid BlogMcpTokenCreateForm form) {
        return Result.success(blogMcpTokenService.createToken(form));
    }

    @Operation(summary = "切换 MCP API 密钥启停状态")
    @PatchMapping("/tokens/{id}/status")
    @PreAuthorize("@ss.hasPerm('blog:mcp:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateTokenStatus(
            @Parameter(description = "密钥ID") @PathVariable Long id,
            @Parameter(description = "状态(1启用 0禁用)") @RequestParam Integer status
    ) {
        return Result.judge(blogMcpTokenService.updateStatus(id, status));
    }

    @Operation(summary = "删除/吊销 MCP API 密钥")
    @DeleteMapping("/tokens/{id}")
    @PreAuthorize("@ss.hasPerm('blog:mcp:delete')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteToken(@Parameter(description = "密钥ID") @PathVariable Long id) {
        return Result.judge(blogMcpTokenService.deleteToken(id));
    }
}
