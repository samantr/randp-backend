package com.app.controller;

import com.app.dto.item.*;
import com.app.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody ItemCreateRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<ItemResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ItemResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /**
     * Non-CRUD: search by (title/code) with optional category filter.
     * Example: /api/v1/items/search?q=cement&categoryId=5
     */
    @GetMapping("/search")
    public List<ItemResponse> search(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Long categoryId) {
        return service.search(q, categoryId);
    }
}
