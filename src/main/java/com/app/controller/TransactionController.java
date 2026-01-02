package com.app.controller;

import com.app.dto.transaction.*;
import com.app.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // ---------------- CRUD ----------------

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@RequestBody TransactionCreateRequest req) {
        TransactionResponse created = transactionService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
                                                      @RequestBody TransactionUpdateRequest req) {
        return ResponseEntity.ok(transactionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Reports / Extras ----------------

    /**
     * دفتر حساب شخص در پروژه (ورودی/خروجی + مانده تجمعی)
     * from/to اختیاری هستند (فیلتر بر اساس date_due)
     *
     * مثال:
     * /api/v1/transactions/ledger?projectId=1&personId=10&from=2025-01-01&to=2025-12-31
     */
    @GetMapping("/ledger")
    public ResponseEntity<List<LedgerRowResponse>> ledger(@RequestParam Long projectId,
                                                          @RequestParam Long personId,
                                                          @RequestParam(required = false) LocalDate from,
                                                          @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(transactionService.ledger(projectId, personId, from, to));
    }

    /**
     * جمع کل دریافتی/پرداختی شخص در یک پروژه + مانده
     *
     * مثال:
     * /api/v1/transactions/person-balance?projectId=1&personId=10
     */
    @GetMapping("/person-balance")
    public ResponseEntity<PersonBalanceResponse> personBalance(@RequestParam Long projectId,
                                                               @RequestParam Long personId) {
        return ResponseEntity.ok(transactionService.personBalance(projectId, personId));
    }

    /**
     * مانده خالص بین دو شخص در پروژه:
     * (جمع پرداخت‌های from->to) - (جمع پرداخت‌های to->from)
     *
     * مثال:
     * /api/v1/transactions/pair-balance?projectId=1&fromPersonId=5&toPersonId=10
     */
    @GetMapping("/pair-balance")
    public ResponseEntity<PairBalanceResponse> pairBalance(@RequestParam Long projectId,
                                                           @RequestParam Long fromPersonId,
                                                           @RequestParam Long toPersonId) {
        return ResponseEntity.ok(transactionService.pairBalance(projectId, fromPersonId, toPersonId));
    }
}
