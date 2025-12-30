package com.app.dto.itemcategory;

import java.util.ArrayList;
import java.util.List;

public class ItemCategoryTreeNodeResponse {

    private Long id;
    private String title;
    private Long parentId;
    private String dsc;
    private List<ItemCategoryTreeNodeResponse> children = new ArrayList<>();

    public ItemCategoryTreeNodeResponse() {
    }

    public ItemCategoryTreeNodeResponse(Long id, String title, Long parentId, String dsc) {
        this.id = id;
        this.title = title;
        this.parentId = parentId;
        this.dsc = dsc;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }

    public List<ItemCategoryTreeNodeResponse> getChildren() {
        return children;
    }

    public void setChildren(List<ItemCategoryTreeNodeResponse> children) {
        this.children = children;
    }
}
