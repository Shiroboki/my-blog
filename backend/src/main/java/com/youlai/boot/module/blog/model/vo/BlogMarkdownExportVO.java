package com.youlai.boot.module.blog.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Markdown 导出结果对象
 */
@Data
@AllArgsConstructor
public class BlogMarkdownExportVO {

    private String fileName;

    private String content;
}
