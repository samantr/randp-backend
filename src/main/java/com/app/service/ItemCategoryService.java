package com.app.service;

import com.app.dto.itemcategory.*;
import com.app.model.ItemCategory;
import com.app.repository.ItemCategoryRepository;
import com.app.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ItemCategoryService {

    private final ItemCategoryRepository repo;
    private final ItemRepository itemRepo;

    public ItemCategoryService(ItemCategoryRepository repo, ItemRepository itemRepo) {
        this.repo = repo;
        this.itemRepo = itemRepo;
    }

    public ItemCategoryResponse create(ItemCategoryCreateRequest req) {
        String title = req.title().trim();
        if (repo.existsByTitleIgnoreCase(title)) {
            throw new IllegalArgumentException("Category title already exists: " + title);
        }

        ItemCategory parent = null;
        if (req.parentId() != null) {
            parent = repo.findById(req.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + req.parentId()));
        }

        ItemCategory c = new ItemCategory(null, title, parent, req.dsc());
        return toResponse(repo.save(c));
    }

    public List<ItemCategoryResponse> getAll() {
        return repo.findAllByOrderByTitleAsc().stream().map(this::toResponse).toList();
    }

    public ItemCategoryResponse getById(Long id) {
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id)));
    }

    @Transactional
    public ItemCategoryResponse update(Long id, ItemCategoryUpdateRequest req) {
        ItemCategory c = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        String title = req.title().trim();
        repo.findByTitleIgnoreCase(title)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> {
                    throw new IllegalArgumentException("Category title already exists: " + title);
                });

        ItemCategory newParent = null;
        if (req.parentId() != null) {
            if (Objects.equals(req.parentId(), id)) {
                throw new IllegalArgumentException("Category cannot be its own parent: " + id);
            }
            newParent = repo.findById(req.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + req.parentId()));

            // prevent cycles: walk up from newParent, ensure we don't reach current category
            ItemCategory cursor = newParent;
            while (cursor != null) {
                if (Objects.equals(cursor.getId(), id)) {
                    throw new IllegalArgumentException("Invalid parent. Cycle detected for category: " + id);
                }
                cursor = cursor.getParent();
            }
        }

        c.setTitle(title);
        c.setParent(newParent);
        c.setDsc(req.dsc());
        return toResponse(c);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Category not found: " + id);
        }

        if (repo.existsByParent_Id(id)) {
            throw new IllegalArgumentException("Category has subcategories and cannot be deleted: " + id);
        }
        if (itemRepo.existsByCategory_Id(id)) {
            throw new IllegalArgumentException("Category has items and cannot be deleted: " + id);
        }

        repo.deleteById(id);
    }

    /**
     * Returns category tree (parent -> children).
     * Uses a single DB query then builds in-memory (no N+1).
     */
    public List<ItemCategoryTreeNodeResponse> getTree() {
        List<ItemCategory> all = repo.findAll();
        Map<Long, ItemCategoryTreeNodeResponse> nodes = new HashMap<>();

        for (ItemCategory c : all) {
            nodes.put(c.getId(), new ItemCategoryTreeNodeResponse(
                    c.getId(),
                    c.getTitle(),
                    c.getParent() == null ? null : c.getParent().getId(),
                    c.getDsc()
            ));
        }

        List<ItemCategoryTreeNodeResponse> roots = new ArrayList<>();
        for (ItemCategory c : all) {
            ItemCategoryTreeNodeResponse node = nodes.get(c.getId());
            if (c.getParent() == null) {
                roots.add(node);
            } else {
                ItemCategoryTreeNodeResponse parent = nodes.get(c.getParent().getId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // orphan (shouldn't happen if data is consistent) - treat as root
                    roots.add(node);
                }
            }
        }

        // sort children by title recursively
        sortTreeByTitle(roots);
        return roots;
    }

    private void sortTreeByTitle(List<ItemCategoryTreeNodeResponse> nodes) {
        nodes.sort(Comparator.comparing(ItemCategoryTreeNodeResponse::getTitle, String.CASE_INSENSITIVE_ORDER));
        for (ItemCategoryTreeNodeResponse n : nodes) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortTreeByTitle(n.getChildren());
            }
        }
    }

    private ItemCategoryResponse toResponse(ItemCategory c) {
        return new ItemCategoryResponse(
                c.getId(),
                c.getTitle(),
                c.getParent() == null ? null : c.getParent().getId(),
                c.getDsc()
        );
    }
}
