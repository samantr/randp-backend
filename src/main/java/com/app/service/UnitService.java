package com.app.service;

import com.app.dto.unit.*;
import com.app.model.Unit;
import com.app.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnitService {

    private final UnitRepository repo;

    public UnitService(UnitRepository repo) {
        this.repo = repo;
    }

    public UnitResponse create(UnitCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("اطلاعات واحد ارسال نشده است.");

        String title = trimToNull(req.title());
        if (title == null) throw new IllegalArgumentException("عنوان واحد الزامی است.");

        if (repo.existsByTitleIgnoreCase(title)) {
            throw new IllegalArgumentException("این عنوان واحد قبلاً ثبت شده است: " + title);
        }

        Unit u = new Unit(null, title, trimToNull(req.dsc()));
        return toResponse(repo.save(u));
    }

    public List<UnitResponse> getAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public UnitResponse getById(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه واحد الزامی است.");

        return toResponse(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("واحد یافت نشد. (شناسه: " + id + ")")));
    }

    @Transactional
    public UnitResponse update(Long id, UnitUpdateRequest req) {
        if (id == null) throw new IllegalArgumentException("شناسه واحد الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش واحد ارسال نشده است.");

        Unit u = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("واحد یافت نشد. (شناسه: " + id + ")"));

        String title = trimToNull(req.title());
        if (title == null) throw new IllegalArgumentException("عنوان واحد الزامی است.");

        repo.findByTitleIgnoreCase(title)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new IllegalArgumentException("این عنوان واحد قبلاً ثبت شده است: " + title); });

        u.setTitle(title);
        u.setDsc(trimToNull(req.dsc()));
        return toResponse(u);
    }

    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه واحد الزامی است.");

        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("واحد یافت نشد. (شناسه: " + id + ")");
        }
        repo.deleteById(id);
    }

    private UnitResponse toResponse(Unit u) {
        return new UnitResponse(u.getId(), u.getTitle(), u.getDsc());
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
