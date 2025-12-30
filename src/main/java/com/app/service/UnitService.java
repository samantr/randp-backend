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
        if (repo.existsByTitleIgnoreCase(req.title())) {
            throw new IllegalArgumentException("Unit title already exists: " + req.title());
        }
        Unit u = new Unit(null, req.title().trim(), req.dsc());
        return toResponse(repo.save(u));
    }

    public List<UnitResponse> getAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public UnitResponse getById(Long id) {
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + id)));
    }

    @Transactional
    public UnitResponse update(Long id, UnitUpdateRequest req) {
        Unit u = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + id));

        // allow same title for same record, block duplicates for others
        repo.findByTitleIgnoreCase(req.title())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new IllegalArgumentException("Unit title already exists: " + req.title()); });

        u.setTitle(req.title().trim());
        u.setDsc(req.dsc());
        return toResponse(u);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Unit not found: " + id);
        }
        repo.deleteById(id);
    }

    private UnitResponse toResponse(Unit u) {
        return new UnitResponse(u.getId(), u.getTitle(), u.getDsc());
    }
}
