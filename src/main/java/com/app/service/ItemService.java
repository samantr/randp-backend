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
        if (req == null) throw new IllegalArgumentException("اطلاعات کالا/خدمت ارسال نشده است.");

        String code = trimToNull(req.code());
        String title = trimToNull(req.title());

        if (code == null) throw new IllegalArgumentException("کد کالا/خدمت الزامی است.");
        if (title == null) throw new IllegalArgumentException("عنوان کالا/خدمت الزامی است.");
        if (req.categoryId() == null) throw new IllegalArgumentException("دسته‌بندی الزامی است.");

        if (repo.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("این کد قبلاً ثبت شده است: " + code);
        }
        if (repo.existsByTitleIgnoreCase(title)) {
            throw new IllegalArgumentException("این عنوان قبلاً ثبت شده است: " + title);
        }

        ItemCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("دسته‌بندی یافت نشد. (شناسه: " + req.categoryId() + ")"));

        Item item = new Item(null, category, code, title, trimToNull(req.dsc()));
        return toResponse(repo.save(item));
    }

    public List<ItemResponse> getAll() {
        return repo.findAllWithCategory().stream().map(this::toResponse).toList();
    }

    public ItemResponse getById(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه کالا/خدمت الزامی است.");

        Item item = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("کالا/خدمت یافت نشد. (شناسه: " + id + ")"));

        // ensure category is loaded for response
        item.getCategory().getTitle();
        return toResponse(item);
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest req) {
        if (id == null) throw new IllegalArgumentException("شناسه کالا/خدمت الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش کالا/خدمت ارسال نشده است.");

        Item item = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("کالا/خدمت یافت نشد. (شناسه: " + id + ")"));

        String code = trimToNull(req.code());
        String title = trimToNull(req.title());

        if (code == null) throw new IllegalArgumentException("کد کالا/خدمت الزامی است.");
        if (title == null) throw new IllegalArgumentException("عنوان کالا/خدمت الزامی است.");
        if (req.categoryId() == null) throw new IllegalArgumentException("دسته‌بندی الزامی است.");

        repo.findByCodeIgnoreCase(code)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new IllegalArgumentException("این کد قبلاً ثبت شده است: " + code); });

        repo.findByTitleIgnoreCase(title)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new IllegalArgumentException("این عنوان قبلاً ثبت شده است: " + title); });

        ItemCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("دسته‌بندی یافت نشد. (شناسه: " + req.categoryId() + ")"));

        item.setCategory(category);
        item.setCode(code);
        item.setTitle(title);
        item.setDsc(trimToNull(req.dsc()));
        return toResponse(item);
    }

    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه کالا/خدمت الزامی است.");

        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("کالا/خدمت یافت نشد. (شناسه: " + id + ")");
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

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
