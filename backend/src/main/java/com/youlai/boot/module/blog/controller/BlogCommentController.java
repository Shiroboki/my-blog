package com.youlai.boot.module.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.framework.web.util.IPUtils;
import com.youlai.boot.module.blog.model.form.BlogCommentReplyForm;
import com.youlai.boot.module.blog.model.query.BlogCommentQuery;
import com.youlai.boot.module.blog.model.vo.BlogCommentVO;
import com.youlai.boot.module.blog.service.BlogCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 博客评论管理控制层
 */
@Tag(name = "17.博客评论管理")
@RestController
@RequestMapping("/api/v1/blog/comments")
@RequiredArgsConstructor
public class BlogCommentController {

    private final BlogCommentService blogCommentService;

    @Operation(summary = "评论分页列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:comment:list')")
    public PageResult<BlogCommentVO> getCommentPage(@Valid BlogCommentQuery queryParams) {
        Page<BlogCommentVO> page = blogCommentService.getCommentPage(queryParams);
        return PageResult.success(page);
    }

    @Operation(summary = "审核评论")
    @PutMapping("/{id}/audit")
    @PreAuthorize("@ss.hasPerm('blog:comment:audit')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> auditComment(
            @Parameter(description = "评论ID") @PathVariable Long id,
            @Parameter(description = "审核状态(1通过 2拒绝)") @RequestParam Integer status
    ) {
        return Result.judge(blogCommentService.auditComment(id, status));
    }

    @Operation(summary = "博主回复评论")
    @PostMapping("/reply")
    @PreAuthorize("@ss.hasPerm('blog:comment:reply')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<Void> replyComment(
            @RequestBody @Valid BlogCommentReplyForm form,
            HttpServletRequest request
    ) {
        return Result.judge(blogCommentService.replyComment(form, IPUtils.getIpAddr(request)));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('blog:comment:delete')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteComments(@Parameter(description = "评论ID，多个英文逗号分隔") @PathVariable String ids) {
        boolean result = blogCommentService.deleteComments(
                Arrays.stream(ids.split(",")).map(Long::valueOf).toList()
        );
        return Result.judge(result);
    }
}
