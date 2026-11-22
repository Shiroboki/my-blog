package com.youlai.boot.module.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.module.blog.model.form.BlogTagForm;
import com.youlai.boot.module.blog.model.query.BlogTagQuery;
import com.youlai.boot.module.blog.model.vo.BlogTagVO;
import com.youlai.boot.module.blog.service.BlogTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 博客标签管理控制层
 */
@Tag(name = "16.博客标签管理")
@RestController
@RequestMapping("/api/v1/blog/tags")
@RequiredArgsConstructor
public class BlogTagController {

    private final BlogTagService blogTagService;

    @Operation(summary = "标签分页列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:tag:list')")
    public PageResult<BlogTagVO> getTagPage(@Valid BlogTagQuery queryParams) {
        Page<BlogTagVO> page = blogTagService.getTagPage(queryParams);
        return PageResult.success(page);
    }

    @Operation(summary = "标签下拉选项")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listTagOptions() {
        return Result.success(blogTagService.listTagOptions());
    }

    @Operation(summary = "获取标签表单详情")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('blog:tag:list')")
    public Result<BlogTagForm> getTagForm(@Parameter(description = "标签ID") @PathVariable Long id) {
        return Result.success(blogTagService.getTagForm(id));
    }

    @Operation(summary = "新增标签")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('blog:tag:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<Void> saveTag(@RequestBody @Valid BlogTagForm form) {
        return Result.judge(blogTagService.saveTag(form));
    }

    @Operation(summary = "修改标签")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('blog:tag:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateTag(
            @Parameter(description = "标签ID") @PathVariable Long id,
            @RequestBody @Valid BlogTagForm form
    ) {
        return Result.judge(blogTagService.updateTag(id, form));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('blog:tag:delete')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteTags(@Parameter(description = "标签ID，多个英文逗号分隔") @PathVariable String ids) {
        boolean result = blogTagService.deleteTags(
                Arrays.stream(ids.split(",")).map(Long::valueOf).toList()
        );
        return Result.judge(result);
    }
}
