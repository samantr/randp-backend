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
        if (req == null) throw new IllegalArgumentException("اطلاعات دسته‌بندی ارسال نشده است.");

        String title = trimToNull(req.title());
        if (title == null) throw new IllegalArgumentException("عنوان دسته‌بندی الزامی است.");

        if (repo.existsByTitleIgnoreCase(title)) {
            throw new IllegalArgumentException("این عنوان دسته‌بندی قبلاً ثبت شده است: " + title);
        }

        ItemCategory parent = null;
        if (req.parentId() != null) {
            parent = repo.findById(req.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("دسته‌بندی والد یافت نشد. (شناسه: " + req.parentId() + ")"));
        }

        ItemCategory c = new ItemCategory(null, title, parent, trimToNull(req.dsc()));
        return toResponse(repo.save(c));
    }

    public List<ItemCategoryResponse> getAll() {
        return repo.findAllByOrderByTitleAsc().stream().map(this::toResponse).toList();
    }

    public ItemCategoryResponse getById(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه دسته‌بندی الزامی است.");
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("دسته‌بندی یافت نشد. (شناسه: " + id + ")")));
    }

    @Transactional
    public ItemCategoryResponse update(Long id, ItemCategoryUpdateRequest req) {
        if (id == null) throw new IllegalArgumentException("شناسه دسته‌بندی الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش دسته‌بندی ارسال نشده است.");

        ItemCategory c = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("دسته‌بندی یافت نشد. (شناسه: " + id + ")"));

        String title = trimToNull(req.title());
        if (title == null) throw new IllegalArgumentException("عنوان دسته‌بندی الزامی است.");

        repo.findByTitleIgnoreCase(title)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new IllegalArgumentException("این عنوان دسته‌بندی قبلاً ثبت شده است: " + title); });

        ItemCategory newParent = null;
        if (req.parentId() != null) {
            if (Objects.equals(req.parentId(), id)) {
                throw new IllegalArgumentException("دسته‌بندی نمی‌تواند والدِ خودش باشد.");
            }
            newParent = repo.findById(req.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("دسته‌بندی والد یافت نشد. (شناسه: " + req.parentId() + ")"));

            // جلوگیری از چرخه
            ItemCategory cursor = newParent;
            while (cursor != null) {
                if (Objects.equals(cursor.getId(), id)) {
                    throw new IllegalArgumentException("انتخاب والد نامعتبر است؛ باعث ایجاد چرخه می‌شود.");
                }
                cursor = cursor.getParent();
            }
        }

        c.setTitle(title);
        c.setParent(newParent);
        c.setDsc(trimToNull(req.dsc()));
        return toResponse(c);
    }

    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه دسته‌بندی الزامی است.");

        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("دسته‌بندی یافت نشد. (شناسه: " + id + ")");
        }

        if (repo.existsByParent_Id(id)) {
            throw new IllegalArgumentException("امکان حذف وجود ندارد؛ این دسته‌بندی زیرمجموعه دارد.");
        }
        if (itemRepo.existsByCategory_Id(id)) {
            throw new IllegalArgumentException("امکان حذف وجود ندارد؛ برای این دسته‌بندی کالا/خدمت ثبت شده است.");
        }

        repo.deleteById(id);
    }

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
                if (parent != null) parent.getChildren().add(node);
                else roots.add(node);
            }
        }

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

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
