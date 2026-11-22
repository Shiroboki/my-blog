package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.web.util.IPUtils;
import com.youlai.boot.module.blog.enums.ArticleStatusEnum;
import com.youlai.boot.module.blog.enums.CommentStatusEnum;
import com.youlai.boot.module.blog.mapper.BlogArticleMapper;
import com.youlai.boot.module.blog.mapper.BlogCommentMapper;
import com.youlai.boot.module.blog.model.entity.BlogArticle;
import com.youlai.boot.module.blog.model.entity.BlogComment;
import com.youlai.boot.module.blog.model.form.BlogCommentForm;
import com.youlai.boot.module.blog.model.form.BlogCommentReplyForm;
import com.youlai.boot.module.blog.model.query.BlogCommentQuery;
import com.youlai.boot.module.blog.model.vo.BlogCommentVO;
import com.youlai.boot.module.blog.service.BlogCommentService;
import com.youlai.boot.module.blog.service.BlogConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 博客评论业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogCommentServiceImpl implements BlogCommentService {

    private final BlogCommentMapper blogCommentMapper;
    private final BlogArticleMapper blogArticleMapper;
    private final BlogConfigService blogConfigService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Page<BlogCommentVO> getCommentPage(BlogCommentQuery queryParams) {
        Page<BlogCommentVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        return blogCommentMapper.getCommentPage(page, queryParams);
    }

    @Override
    public List<BlogCommentVO> listPublicArticleComments(Long articleId) {
        BlogArticle article = blogArticleMapper.selectById(articleId);
        if (article == null || !Objects.equals(article.getStatus(), ArticleStatusEnum.PUBLISHED.getValue())) {
            throw new BusinessException("文章不存在或尚未发布");
        }
        List<BlogComment> comments = blogCommentMapper.selectList(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getArticleId, articleId)
                .eq(BlogComment::getStatus, CommentStatusEnum.APPROVED.getValue())
                .orderByAsc(BlogComment::getCreateTime, BlogComment::getId));
        if (CollectionUtil.isEmpty(comments)) {
            return Collections.emptyList();
        }
        return buildCommentTree(comments);
    }

    @Override
    public BlogCommentVO submitPublicComment(BlogCommentForm form, String clientIp) {
        BlogArticle article = blogArticleMapper.selectById(form.getArticleId());
        if (article == null || !Objects.equals(article.getStatus(), ArticleStatusEnum.PUBLISHED.getValue())) {
            throw new BusinessException("文章不存在或尚未发布");
        }
        if (!Objects.equals(article.getAllowComment(), 1)) {
            throw new BusinessException("该文章暂未开启评论");
        }
        Long parentId = form.getParentId() != null ? form.getParentId() : 0L;
        if (parentId > 0) {
            BlogComment parent = blogCommentMapper.selectById(parentId);
            if (parent == null
                    || !Objects.equals(parent.getArticleId(), form.getArticleId())
                    || !Objects.equals(parent.getStatus(), CommentStatusEnum.APPROVED.getValue())) {
                throw new BusinessException("回复的目标评论不存在");
            }
        }

        boolean requireAudit = "1".equals(blogConfigService.getConfigValue("comment_audit", "0"));
        String quoteText = StrUtil.isBlank(form.getQuoteText()) ? null : form.getQuoteText().trim();
        Integer quoteStart = null;
        Integer quoteEnd = null;
        if (quoteText != null) {
            if (form.getQuoteStart() != null && form.getQuoteEnd() != null) {
                if (form.getQuoteStart() < 0 || form.getQuoteEnd() <= form.getQuoteStart()) {
                    throw new BusinessException("划词位置区间无效");
                }
                quoteStart = form.getQuoteStart();
                quoteEnd = form.getQuoteEnd();
            }
        }

        BlogComment entity = new BlogComment();
        entity.setArticleId(form.getArticleId());
        entity.setParentId(parentId);
        entity.setNickname(StrUtil.isBlank(form.getNickname()) ? "匿名读者" : form.getNickname().trim());
        entity.setEmail(StrUtil.isBlank(form.getEmail()) ? null : form.getEmail().trim());
        entity.setWebsite(normalizeWebsite(form.getWebsite()));
        entity.setContent(form.getContent().trim());
        entity.setQuoteText(quoteText);
        entity.setQuoteStart(quoteStart);
        entity.setQuoteEnd(quoteEnd);
        entity.setIpAddress(clientIp);
        entity.setIpRegion(resolveReadableRegion(clientIp));
        entity.setStatus(requireAudit ? CommentStatusEnum.PENDING.getValue() : CommentStatusEnum.APPROVED.getValue());
        entity.setIsAdmin(0);
        blogCommentMapper.insert(entity);

        if (!requireAudit) {
            evictArticleDetailCache(form.getArticleId());
        }
        return toPublicVO(entity);
    }

    @Override
    public boolean auditComment(Long id, Integer status) {
        BlogComment existing = blogCommentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("评论不存在");
        }
        if (!Objects.equals(status, CommentStatusEnum.APPROVED.getValue())
                && !Objects.equals(status, CommentStatusEnum.REJECTED.getValue())) {
            throw new BusinessException("无效的审核状态");
        }
        existing.setStatus(status);
        boolean ok = blogCommentMapper.updateById(existing) > 0;
        if (ok) {
            evictArticleDetailCache(existing.getArticleId());
        }
        return ok;
    }

    @Override
    public boolean replyComment(BlogCommentReplyForm form, String clientIp) {
        BlogComment parent = blogCommentMapper.selectById(form.getParentId());
        if (parent == null || !Objects.equals(parent.getArticleId(), form.getArticleId())) {
            throw new BusinessException("目标评论不存在");
        }
        String authorName = blogConfigService.getConfigValue("author_name", "博主");
        BlogComment entity = new BlogComment();
        entity.setArticleId(form.getArticleId());
        entity.setParentId(form.getParentId());
        entity.setNickname(authorName);
        entity.setContent(form.getContent().trim());
        entity.setIpAddress(clientIp);
        entity.setIpRegion(resolveReadableRegion(clientIp));
        entity.setStatus(CommentStatusEnum.APPROVED.getValue());
        entity.setIsAdmin(1);
        boolean ok = blogCommentMapper.insert(entity) > 0;
        if (ok) {
            evictArticleDetailCache(form.getArticleId());
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComments(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }
        List<BlogComment> targets = blogCommentMapper.selectByIds(ids);
        // 同时删除子回复，避免孤立评论残留
        blogCommentMapper.delete(new LambdaQueryWrapper<BlogComment>()
                .in(BlogComment::getParentId, ids));
        boolean ok = blogCommentMapper.deleteByIds(ids) > 0;
        if (ok) {
            targets.stream()
                    .map(BlogComment::getArticleId)
                    .distinct()
                    .forEach(this::evictArticleDetailCache);
        }
        return ok;
    }

    private List<BlogCommentVO> buildCommentTree(List<BlogComment> comments) {
        Map<Long, BlogCommentVO> nodeMap = new LinkedHashMap<>();
        List<BlogCommentVO> roots = new ArrayList<>();
        for (BlogComment comment : comments) {
            nodeMap.put(comment.getId(), toPublicVO(comment));
        }
        for (BlogComment comment : comments) {
            BlogCommentVO vo = nodeMap.get(comment.getId());
            Long parentId = comment.getParentId();
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(vo);
            } else {
                nodeMap.get(parentId).getChildren().add(vo);
            }
        }
        return roots;
    }

    private BlogCommentVO toPublicVO(BlogComment entity) {
        BlogCommentVO vo = new BlogCommentVO();
        vo.setId(entity.getId());
        vo.setArticleId(entity.getArticleId());
        vo.setParentId(entity.getParentId());
        vo.setNickname(entity.getNickname());
        // 公开响应绝不返回访客邮箱
        vo.setEmail(null);
        vo.setWebsite(entity.getWebsite());
        vo.setContent(entity.getContent());
        vo.setQuoteText(entity.getQuoteText());
        vo.setQuoteStart(entity.getQuoteStart());
        vo.setQuoteEnd(entity.getQuoteEnd());
        vo.setIpRegion(entity.getIpRegion());
        vo.setStatus(entity.getStatus());
        vo.setIsAdmin(entity.getIsAdmin());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String normalizeWebsite(String website) {
        if (StrUtil.isBlank(website)) {
            return null;
        }
        String trimmed = website.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.contains(":")) {
            return null;
        }
        return "https://" + trimmed;
    }

    private String resolveReadableRegion(String clientIp) {
        if (StrUtil.isBlank(clientIp)) {
            return "未知";
        }
        String raw = IPUtils.getRegion(clientIp);
        if (StrUtil.isBlank(raw)) {
            return "未知";
        }
        String[] parts = raw.split("\\|");
        List<String> valid = new ArrayList<>();
        for (int i = 0; i < parts.length && i < 4; i++) {
            String part = parts[i].trim();
            if (StrUtil.isNotBlank(part) && !"0".equals(part)) {
                valid.add(part);
            }
        }
        return valid.isEmpty() ? "本地" : String.join(" ", valid);
    }

    private void evictArticleDetailCache(Long articleId) {
        try {
            stringRedisTemplate.delete(StrUtil.format(RedisConstants.Blog.ARTICLE_DETAIL, articleId));
        } catch (Exception e) {
            log.warn("清理文章评论关联缓存失败, articleId={}: {}", articleId, e.getMessage());
        }
    }
}
