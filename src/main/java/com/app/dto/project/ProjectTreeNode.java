package com.app.dto.project;

import java.util.ArrayList;
import java.util.List;

public class ProjectTreeNode {
    public Long id;
    public Long parentId;
    public String title;
    public String dsc;
    public List<ProjectTreeNode> children = new ArrayList<>();

    public ProjectTreeNode(Long id, Long parentId, String title, String dsc) {
        this.id = id;
        this.parentId = parentId;
        this.title = title;
        this.dsc = dsc;
    }
}
