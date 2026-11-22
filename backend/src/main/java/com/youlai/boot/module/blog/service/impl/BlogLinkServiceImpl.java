package com.youlai.boot.module.blog.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.blog.mapper.BlogLinkMapper;
import com.youlai.boot.module.blog.model.entity.BlogLink;
import com.youlai.boot.module.blog.model.form.BlogLinkForm;
import com.youlai.boot.module.blog.model.query.BlogLinkQuery;
import com.youlai.boot.module.blog.model.vo.BlogLinkVO;
import com.youlai.boot.module.blog.service.BlogLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 博客友情链接业务实现类
 */
@Service
@RequiredArgsConstructor
public class BlogLinkServiceImpl implements BlogLinkService {

    private final BlogLinkMapper blogLinkMapper;

    @Override
    public Page<BlogLinkVO> getLinkPage(BlogLinkQuery queryParams) {
        LambdaQueryWrapper<BlogLink> wrapper = new LambdaQueryWrapper<BlogLink>()
                .eq(queryParams.getStatus() != null, BlogLink::getStatus, queryParams.getStatus())
                .and(StrUtil.isNotBlank(queryParams.getKeywords()), q -> q
                        .like(BlogLink::getName, queryParams.getKeywords().trim())
                        .or()
                        .like(BlogLink::getUrl, queryParams.getKeywords().trim()))
                .orderByAsc(BlogLink::getSort)
                .orderByDesc(BlogLink::getId);
        Page<BlogLink> page = blogLinkMapper.selectPage(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        Page<BlogLinkVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public List<BlogLinkVO> listPublicLinks() {
        List<BlogLink> links = blogLinkMapper.selectList(new LambdaQueryWrapper<BlogLink>()
                .eq(BlogLink::getStatus, 1)
                .orderByAsc(BlogLink::getSort)
                .orderByDesc(BlogLink::getId));
        return links.stream().map(this::toVO).toList();
    }

    @Override
    public BlogLinkForm getLinkForm(Long id) {
        BlogLink entity = blogLinkMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("友链不存在");
        }
        BlogLinkForm form = new BlogLinkForm();
        form.setId(entity.getId());
        form.setName(entity.getName());
        form.setUrl(entity.getUrl());
        form.setAvatar(entity.getAvatar());
        form.setDescription(entity.getDescription());
        form.setSort(entity.getSort());
        form.setStatus(entity.getStatus());
        return form;
    }

    @Override
    public boolean saveLink(BlogLinkForm form) {
        BlogLink entity = new BlogLink();
        applyForm(entity, form);
        return blogLinkMapper.insert(entity) > 0;
    }

    @Override
    public boolean updateLink(Long id, BlogLinkForm form) {
        BlogLink existing = blogLinkMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("友链不存在");
        }
        applyForm(existing, form);
        return blogLinkMapper.updateById(existing) > 0;
    }

    @Override
    public boolean deleteLinks(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }
        return blogLinkMapper.deleteByIds(ids) > 0;
    }

    private void applyForm(BlogLink target, BlogLinkForm form) {
        String normalizedUrl = normalizeHttpUrl(form.getUrl());
        if (normalizedUrl == null) {
            throw new BusinessException("站点地址必须以 http:// 或 https:// 开头");
        }
        target.setName(form.getName().trim());
        target.setUrl(normalizedUrl);
        target.setAvatar(StrUtil.isBlank(form.getAvatar()) ? null : form.getAvatar().trim());
        target.setDescription(StrUtil.isBlank(form.getDescription()) ? null : form.getDescription().trim());
        target.setSort(form.getSort() != null ? form.getSort() : 0);
        target.setStatus(Objects.equals(form.getStatus(), 0) ? 0 : 1);
    }

    private String normalizeHttpUrl(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.contains(":")) {
            return null;
        }
        return "https://" + trimmed;
    }

    private BlogLinkVO toVO(BlogLink entity) {
        BlogLinkVO vo = new BlogLinkVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setUrl(entity.getUrl());
        vo.setAvatar(entity.getAvatar());
        vo.setDescription(entity.getDescription());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
