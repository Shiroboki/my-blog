package com.youlai.boot.module.blog.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 全局快捷搜索聚合结果（用于 Cmd/Ctrl+K 命令面板）
 */
@Data
@Schema(description = "全局搜索结果")
public class BlogSearchResultVO {

    @Schema(description = "匹配文章列表")
    private List<ArticleHit> articles;

    @Schema(description = "匹配学习路径及导图节点")
    private List<PathHit> paths;

    @Schema(description = "匹配归档年月")
    private List<ArchiveHit> archives;

    @Data
    public static class ArticleHit {
        private Long id;
        private String slug;
        private String title;
        private String categoryName;
        private LocalDateTime publishTime;
        private String snippet;
    }

    @Data
    public static class PathHit {
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private String matchedNodeId;
        private String matchedNodeLabel;
    }

    @Data
    public static class ArchiveHit {
        private String yearMonth;
        private int count;
    }
}
