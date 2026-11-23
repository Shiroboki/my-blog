package com.youlai.boot.module.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.module.blog.model.form.BlogLearningPathForm;
import com.youlai.boot.module.blog.model.query.BlogLearningPathQuery;
import com.youlai.boot.module.blog.model.vo.BlogLearningPathVO;
import com.youlai.boot.module.blog.service.BlogLearningPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 博客学习路径与思维导图管理控制层
 */
@Tag(name = "22.博客学习路径管理")
@RestController
@RequestMapping("/api/v1/blog/learning-paths")
@RequiredArgsConstructor
public class BlogLearningPathController {

    private final BlogLearningPathService blogLearningPathService;

    @Operation(summary = "学习路径分页列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:learning-path:list')")
    public PageResult<BlogLearningPathVO> getPathPage(@Valid BlogLearningPathQuery queryParams) {
        Page<BlogLearningPathVO> page = blogLearningPathService.getPathPage(queryParams);
        return PageResult.success(page);
    }

    @Operation(summary = "获取学习路径表单详情")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('blog:learning-path:list')")
    public Result<BlogLearningPathForm> getPathForm(@Parameter(description = "路径ID") @PathVariable Long id) {
        return Result.success(blogLearningPathService.getPathForm(id));
    }

    @Operation(summary = "新增学习路径导图")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('blog:learning-path:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<Long> savePath(@RequestBody @Valid BlogLearningPathForm form) {
        return Result.success(blogLearningPathService.savePath(form));
    }

    @Operation(summary = "修改学习路径导图")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('blog:learning-path:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updatePath(
            @Parameter(description = "路径ID") @PathVariable Long id,
            @RequestBody @Valid BlogLearningPathForm form
    ) {
        return Result.judge(blogLearningPathService.updatePath(id, form));
    }

    @Operation(summary = "删除学习路径导图")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('blog:learning-path:delete')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.DELETE)
    public Result<Void> deletePaths(@Parameter(description = "路径ID，多个英文逗号分隔") @PathVariable String ids) {
        boolean ok = blogLearningPathService.deletePaths(
                Arrays.stream(ids.split(",")).map(Long::valueOf).toList()
        );
        return Result.judge(ok);
    }
}
