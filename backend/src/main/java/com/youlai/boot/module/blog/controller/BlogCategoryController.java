package com.youlai.boot.module.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.module.blog.model.form.BlogCategoryForm;
import com.youlai.boot.module.blog.model.query.BlogCategoryQuery;
import com.youlai.boot.module.blog.model.vo.BlogCategoryVO;
import com.youlai.boot.module.blog.service.BlogCategoryService;
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
 * 博客分类管理控制层
 */
@Tag(name = "15.博客分类管理")
@RestController
@RequestMapping("/api/v1/blog/categories")
@RequiredArgsConstructor
public class BlogCategoryController {

    private final BlogCategoryService blogCategoryService;

    @Operation(summary = "分类分页列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:category:list')")
    public PageResult<BlogCategoryVO> getCategoryPage(@Valid BlogCategoryQuery queryParams) {
        Page<BlogCategoryVO> page = blogCategoryService.getCategoryPage(queryParams);
        return PageResult.success(page);
    }

    @Operation(summary = "分类下拉选项")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listCategoryOptions() {
        return Result.success(blogCategoryService.listCategoryOptions());
    }

    @Operation(summary = "获取分类表单详情")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('blog:category:list')")
    public Result<BlogCategoryForm> getCategoryForm(@Parameter(description = "分类ID") @PathVariable Long id) {
        return Result.success(blogCategoryService.getCategoryForm(id));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('blog:category:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<Void> saveCategory(@RequestBody @Valid BlogCategoryForm form) {
        return Result.judge(blogCategoryService.saveCategory(form));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('blog:category:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @RequestBody @Valid BlogCategoryForm form
    ) {
        return Result.judge(blogCategoryService.updateCategory(id, form));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('blog:category:delete')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteCategories(@Parameter(description = "分类ID，多个英文逗号分隔") @PathVariable String ids) {
        boolean result = blogCategoryService.deleteCategories(
                Arrays.stream(ids.split(",")).map(Long::valueOf).toList()
        );
        return Result.judge(result);
    }
}
