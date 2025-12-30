package com.app.controller;

import com.app.dto.debt.*;
import com.app.service.DebtService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debts")
public class DebtController {

    private final DebtService debtService;

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    // CRUD
    @PostMapping
    public ResponseEntity<DebtHeaderResponse> create(@Valid @RequestBody DebtCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DebtHeaderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(debtService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DebtHeaderResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody DebtUpdateRequest req) {
        return ResponseEntity.ok(debtService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        debtService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // View (header + lines + allocations + totals)
    @GetMapping("/{id}/view")
    public ResponseEntity<DebtViewResponse> view(@PathVariable Long id) {
        return ResponseEntity.ok(debtService.view(id));
    }

    // Open debts
    // GET /api/v1/debts/open?projectId=1&personId=10
    @GetMapping("/open")
    public ResponseEntity<List<Map<String, Object>>> openDebts(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long personId
    ) {
        return ResponseEntity.ok(debtService.openDebts(projectId, personId));
    }
}
