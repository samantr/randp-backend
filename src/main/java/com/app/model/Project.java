package com.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Self reference FK -> projects(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Project parent;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "dsc", length = 4000)
    private String dsc;

    public Project() {}

    public Long getId() { return id; }

    public Project getParent() { return parent; }
    public void setParent(Project parent) { this.parent = parent; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
