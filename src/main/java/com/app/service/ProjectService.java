package com.app.service;

import com.app.dto.project.*;
import com.app.model.Project;
import com.app.repository.ProjectRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProjectService(ProjectRepository projectRepository, JdbcTemplate jdbcTemplate) {
        this.projectRepository = projectRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("اطلاعات پروژه ارسال نشده است.");

        String title = trimToNull(req.title());
        if (title == null) throw new IllegalArgumentException("عنوان پروژه الزامی است.");

        if (projectRepository.existsByTitleIgnoreCase(title)) {
            throw new IllegalArgumentException("این عنوان پروژه قبلاً ثبت شده است: " + title);
        }

        Project parent = resolveParent(req.parentId(), null);

        Project p = new Project();
        p.setTitle(title);
        p.setDsc(trimToNull(req.dsc()));
        p.setParent(parent);

        return toResponse(projectRepository.save(p));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه پروژه الزامی است.");

        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("پروژه یافت نشد. (شناسه: " + id + ")"));
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectUpdateRequest req) {
        if (id == null) throw new IllegalArgumentException("شناسه پروژه الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش پروژه ارسال نشده است.");

        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("پروژه یافت نشد. (شناسه: " + id + ")"));

        String title = trimToNull(req.title());
        if (title == null) throw new IllegalArgumentException("عنوان پروژه الزامی است.");

        if (projectRepository.existsByTitleIgnoreCaseAndIdNot(title, id)) {
            throw new IllegalArgumentException("این عنوان پروژه قبلاً ثبت شده است: " + title);
        }

        Project parent = resolveParent(req.parentId(), id);

        p.setTitle(title);
        p.setDsc(trimToNull(req.dsc()));
        p.setParent(parent);

        return toResponse(projectRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه پروژه الزامی است.");

        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("پروژه یافت نشد. (شناسه: " + id + ")"));

        if (projectRepository.countByParent_Id(id) > 0) {
            throw new IllegalArgumentException("امکان حذف پروژه وجود ندارد؛ این پروژه زیرپروژه دارد.");
        }

        if (isProjectReferenced(id)) {
            throw new IllegalArgumentException("امکان حذف پروژه وجود ندارد؛ برای این پروژه پرداخت یا بدهی ثبت شده است.");
        }

        projectRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public List<ProjectTreeNode> getTree() {
        List<Project> all = projectRepository.findAll();

        Map<Long, ProjectTreeNode> map = new HashMap<>();
        List<ProjectTreeNode> roots = new ArrayList<>();

        for (Project p : all) {
            Long parentId = (p.getParent() == null ? null : p.getParent().getId());
            map.put(p.getId(), new ProjectTreeNode(p.getId(), parentId, p.getTitle(), p.getDsc()));
        }

        for (Project p : all) {
            ProjectTreeNode node = map.get(p.getId());
            if (node.parentId == null) roots.add(node);
            else {
                ProjectTreeNode parent = map.get(node.parentId);
                if (parent != null) parent.children.add(node);
                else roots.add(node);
            }
        }

        return roots;
    }

    private Project resolveParent(Long parentId, Long selfId) {
        if (parentId == null) return null;

        if (selfId != null && parentId.equals(selfId)) {
            throw new IllegalArgumentException("پروژه نمی‌تواند والدِ خودش باشد.");
        }

        Project parent = projectRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("پروژه والد یافت نشد. (شناسه: " + parentId + ")"));

        if (selfId != null && createsCycle(selfId, parent)) {
            throw new IllegalArgumentException("انتخاب والد نامعتبر است؛ باعث ایجاد چرخه می‌شود.");
        }

        return parent;
    }

    private boolean createsCycle(Long selfId, Project newParent) {
        Project cur = newParent;
        while (cur != null) {
            if (selfId.equals(cur.getId())) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private boolean isProjectReferenced(Long projectId) {
        Integer txCount = jdbcTemplate.queryForObject(
                "select count(1) from transactions where project_id = ?",
                Integer.class, projectId
        );
        Integer debtCount = jdbcTemplate.queryForObject(
                "select count(1) from debts_header where project_id = ?",
                Integer.class, projectId
        );
        return (txCount != null && txCount > 0) || (debtCount != null && debtCount > 0);
    }

    private ProjectResponse toResponse(Project p) {
        Long parentId = (p.getParent() == null ? null : p.getParent().getId());
        return new ProjectResponse(p.getId(), parentId, p.getTitle(), p.getDsc());
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
