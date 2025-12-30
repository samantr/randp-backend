package com.app.controller;

import com.app.dto.transaction.*;
import com.app.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // CRUD
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll() {
        return ResponseEntity.ok(transactionService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody TransactionUpdateRequest req) {
        return ResponseEntity.ok(transactionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Extra: Ledger (statement)
    // GET /api/v1/transactions/ledger?projectId=1&personId=10&from=2025-01-01&to=2025-12-31
    @GetMapping("/ledger")
    public ResponseEntity<List<LedgerRowResponse>> ledger(
            @RequestParam Long projectId,
            @RequestParam Long personId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(transactionService.ledger(projectId, personId, from, to));
    }

    // Extra: Person balance
    // GET /api/v1/transactions/balance/person?projectId=1&personId=10
    @GetMapping("/balance/person")
    public ResponseEntity<PersonBalanceResponse> personBalance(
            @RequestParam Long projectId,
            @RequestParam Long personId
    ) {
        return ResponseEntity.ok(transactionService.personBalance(projectId, personId));
    }

    // Extra: Pair balance (A vs B)
    // GET /api/v1/transactions/balance/pair?projectId=1&fromPersonId=10&toPersonId=12
    @GetMapping("/balance/pair")
    public ResponseEntity<PairBalanceResponse> pairBalance(
            @RequestParam Long projectId,
            @RequestParam Long fromPersonId,
            @RequestParam Long toPersonId
    ) {
        return ResponseEntity.ok(transactionService.pairBalance(projectId, fromPersonId, toPersonId));
    }
}
