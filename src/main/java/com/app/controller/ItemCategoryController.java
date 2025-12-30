package com.app.controller;

import com.app.dto.itemcategory.*;
import com.app.service.ItemCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/item-categories")
public class ItemCategoryController {

    private final ItemCategoryService service;

    public ItemCategoryController(ItemCategoryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCategoryResponse create(@Valid @RequestBody ItemCategoryCreateRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<ItemCategoryResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ItemCategoryResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public ItemCategoryResponse update(@PathVariable Long id, @Valid @RequestBody ItemCategoryUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /**
     * Non-CRUD: category tree for UI.
     */
    @GetMapping("/tree")
    public List<ItemCategoryTreeNodeResponse> getTree() {
        return service.getTree();
    }
}
