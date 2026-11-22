package com.youlai.boot.module.blog.controller;

import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.module.blog.model.form.BlogConfigForm;
import com.youlai.boot.module.blog.service.BlogConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 博客站点配置管理控制层
 */
@Tag(name = "18.博客站点设置")
@RestController
@RequestMapping("/api/v1/blog/configs")
@RequiredArgsConstructor
public class BlogConfigController {

    private final BlogConfigService blogConfigService;

    @Operation(summary = "获取站点设置")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:config:list')")
    public Result<BlogConfigForm> getConfigForm() {
        return Result.success(blogConfigService.getConfigForm());
    }

    @Operation(summary = "保存站点设置")
    @PutMapping
    @PreAuthorize("@ss.hasPerm('blog:config:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> saveConfigForm(@RequestBody @Valid BlogConfigForm form) {
        return Result.judge(blogConfigService.saveConfigForm(form));
    }
}
