package com.app.service;

import com.app.dto.item.*;
import com.app.model.Item;
import com.app.model.ItemCategory;
import com.app.repository.ItemCategoryRepository;
import com.app.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository repo;
    private final ItemCategoryRepository categoryRepo;

    public ItemService(ItemRepository repo, ItemCategoryRepository categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    public ItemResponse create(ItemCreateRequest req) {
        String code = req.code().trim();
        String title = req.title().trim();

        if (repo.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Item code already exists: " + code);
        }
        if (repo.existsByTitleIgnoreCase(title)) {
            throw new IllegalArgumentException("Item title already exists: " + title);
        }

        ItemCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));

        Item item = new Item(null, category, code, title, req.dsc());
        return toResponse(repo.save(item));
    }

    public List<ItemResponse> getAll() {
        return repo.findAllWithCategory().stream().map(this::toResponse).toList();
    }

    public ItemResponse getById(Long id) {
        Item item = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
        // Ensure category is available for response
        item.getCategory().getTitle();
        return toResponse(item);
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest req) {
        Item item = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));

        String code = req.code().trim();
        String title = req.title().trim();

        repo.findByCodeIgnoreCase(code)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new IllegalArgumentException("Item code already exists: " + code); });

        repo.findByTitleIgnoreCase(title)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new IllegalArgumentException("Item title already exists: " + title); });

        ItemCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));

        item.setCategory(category);
        item.setCode(code);
        item.setTitle(title);
        item.setDsc(req.dsc());
        return toResponse(item);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Item not found: " + id);
        }
        repo.deleteById(id);
    }

    public List<ItemResponse> search(String q, Long categoryId) {
        return repo.search(q == null ? null : q.trim(), categoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ItemResponse toResponse(Item i) {
        return new ItemResponse(
                i.getId(),
                i.getCategory().getId(),
                i.getCategory().getTitle(),
                i.getCode(),
                i.getTitle(),
                i.getDsc()
        );
    }
}
