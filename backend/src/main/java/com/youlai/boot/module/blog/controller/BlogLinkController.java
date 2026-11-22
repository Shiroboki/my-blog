package com.youlai.boot.module.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.module.blog.model.form.BlogLinkForm;
import com.youlai.boot.module.blog.model.query.BlogLinkQuery;
import com.youlai.boot.module.blog.model.vo.BlogLinkVO;
import com.youlai.boot.module.blog.service.BlogLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 博客友情链接管理控制层
 */
@Tag(name = "20.博客友链管理")
@RestController
@RequestMapping("/api/v1/blog/links")
@RequiredArgsConstructor
public class BlogLinkController {

    private final BlogLinkService blogLinkService;

    @Operation(summary = "友链分页列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:link:list')")
    public PageResult<BlogLinkVO> getLinkPage(@Valid BlogLinkQuery queryParams) {
        Page<BlogLinkVO> page = blogLinkService.getLinkPage(queryParams);
        return PageResult.success(page);
    }

    @Operation(summary = "获取友链表单详情")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('blog:link:list')")
    public Result<BlogLinkForm> getLinkForm(@Parameter(description = "友链ID") @PathVariable Long id) {
        return Result.success(blogLinkService.getLinkForm(id));
    }

    @Operation(summary = "新增友链")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('blog:link:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<Void> saveLink(@RequestBody @Valid BlogLinkForm form) {
        return Result.judge(blogLinkService.saveLink(form));
    }

    @Operation(summary = "修改友链")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('blog:link:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateLink(
            @Parameter(description = "友链ID") @PathVariable Long id,
            @RequestBody @Valid BlogLinkForm form
    ) {
        return Result.judge(blogLinkService.updateLink(id, form));
    }

    @Operation(summary = "删除友链")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('blog:link:delete')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteLinks(@Parameter(description = "友链ID，多个英文逗号分隔") @PathVariable String ids) {
        boolean ok = blogLinkService.deleteLinks(
                Arrays.stream(ids.split(",")).map(Long::valueOf).toList()
        );
        return Result.judge(ok);
    }
}
