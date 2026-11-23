package com.youlai.boot.module.blog.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 思维导图树节点结构
 */
@Data
public class MindMapNodeDTO {

    private String id;

    private String label;

    private String description;

    private String tag;

    private Boolean collapsed;

    private List<MindMapResourceDTO> resources = new ArrayList<>();

    private List<MindMapNodeDTO> children = new ArrayList<>();
}
