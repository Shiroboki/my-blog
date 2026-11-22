package com.youlai.boot.module.blog.controller;

import com.youlai.boot.common.result.Result;
import com.youlai.boot.module.blog.model.vo.BlogDashboardVO;
import com.youlai.boot.module.blog.service.BlogDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 博客概览统计控制层
 */
@Tag(name = "21.博客概览统计")
@RestController
@RequestMapping("/api/v1/blog/dashboard")
@RequiredArgsConstructor
public class BlogDashboardController {

    private final BlogDashboardService blogDashboardService;

    @Operation(summary = "获取博客管理概览统计")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:dashboard:view')")
    public Result<BlogDashboardVO> getDashboardStats() {
        return Result.success(blogDashboardService.getDashboardStats());
    }
}
