package com.app.controller;

import com.app.dto.debt.*;
import com.app.service.DebtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // ---------------- CRUD ----------------

    // POST /api/v1/debts
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DebtHeaderResponse> create(@Valid @RequestBody DebtCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtService.create(req));
    }

    // GET /api/v1/debts/{id}
    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DebtHeaderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(debtService.getById(id));
    }

    // PUT /api/v1/debts/{id}
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DebtHeaderResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody DebtUpdateRequest req) {
        return ResponseEntity.ok(debtService.update(id, req));
    }

    // DELETE /api/v1/debts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        debtService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- VIEW ----------------

    // GET /api/v1/debts/{id}/view  (header + lines + allocations + totals)
    @GetMapping(
            value = "/{id}/view",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DebtViewResponse> view(@PathVariable Long id) {
        return ResponseEntity.ok(debtService.view(id));
    }

    // ---------------- LISTS ----------------

    // Open debts (remaining > 0)
    // GET /api/v1/debts/open?projectId=1&personId=10
    @GetMapping(
            value = "/open",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<Map<String, Object>>> openDebts(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long personId
    ) {
        return ResponseEntity.ok(debtService.openDebts(projectId, personId));
    }

    // All debts (no filter)
    // GET /api/v1/debts
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getAllDebts() {
        return ResponseEntity.ok(debtService.getAllDebts());
    }
}
