package com.youlai.boot.module.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.module.blog.model.form.BlogArticleForm;
import com.youlai.boot.module.blog.model.query.BlogArticleQuery;
import com.youlai.boot.module.blog.model.vo.BlogArticlePageVO;
import com.youlai.boot.module.blog.model.vo.BlogMarkdownExportVO;
import com.youlai.boot.module.blog.service.BlogArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 博客文章管理控制层
 */
@Tag(name = "14.博客文章管理")
@RestController
@RequestMapping("/api/v1/blog/articles")
@RequiredArgsConstructor
public class BlogArticleController {

    private final BlogArticleService blogArticleService;

    @Operation(summary = "文章分页列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('blog:article:list')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.LIST)
    public PageResult<BlogArticlePageVO> getArticlePage(@Valid BlogArticleQuery queryParams) {
        Page<BlogArticlePageVO> page = blogArticleService.getArticlePage(queryParams);
        return PageResult.success(page);
    }

    @Operation(summary = "获取文章表单详情")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('blog:article:list')")
    public Result<BlogArticleForm> getArticleForm(@Parameter(description = "文章ID") @PathVariable Long id) {
        return Result.success(blogArticleService.getArticleForm(id));
    }

    @Operation(summary = "新增文章")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('blog:article:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<Void> saveArticle(@RequestBody @Valid BlogArticleForm form) {
        return Result.judge(blogArticleService.saveArticle(form));
    }

    @Operation(summary = "修改文章")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('blog:article:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateArticle(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @RequestBody @Valid BlogArticleForm form
    ) {
        return Result.judge(blogArticleService.updateArticle(id, form));
    }

    @Operation(summary = "修改文章发布状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('blog:article:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateArticleStatus(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @Parameter(description = "状态(0草稿 1已发布)") @RequestParam Integer status
    ) {
        return Result.judge(blogArticleService.updateArticleStatus(id, status));
    }

    @Operation(summary = "修改文章置顶状态")
    @PutMapping("/{id}/top")
    @PreAuthorize("@ss.hasPerm('blog:article:update')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateArticleTop(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @Parameter(description = "是否置顶(0否 1是)") @RequestParam Integer isTop
    ) {
        return Result.judge(blogArticleService.updateArticleTop(id, isTop));
    }

    @Operation(summary = "导出文章为 Markdown")
    @GetMapping("/{id}/export")
    @PreAuthorize("@ss.hasPerm('blog:article:list')")
    public ResponseEntity<byte[]> exportArticleMarkdown(@Parameter(description = "文章ID") @PathVariable Long id) {
        BlogMarkdownExportVO exportVO = blogArticleService.exportArticleMarkdown(id);
        String encodedName = UriUtils.encode(exportVO.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .body(exportVO.getContent().getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "导入 Markdown 文件生成草稿")
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('blog:article:create')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.INSERT)
    public Result<Long> importArticleMarkdown(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Markdown 文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("Markdown 文件大小不能超过 2MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (!lower.endsWith(".md") && !lower.endsWith(".markdown") && !lower.endsWith(".txt")) {
                throw new BusinessException("仅支持 .md、.markdown 或 .txt 文件");
            }
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return Result.success(blogArticleService.importArticleMarkdown(originalFilename, content));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("读取 Markdown 文件失败");
        }
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('blog:article:delete')")
    @Log(module = LogModuleEnum.BLOG, value = ActionTypeEnum.DELETE)
    public Result<Void> deleteArticles(@Parameter(description = "文章ID，多个英文逗号分隔") @PathVariable String ids) {
        boolean result = blogArticleService.deleteArticles(
                Arrays.stream(ids.split(",")).map(Long::valueOf).toList()
        );
        return Result.judge(result);
    }
}
