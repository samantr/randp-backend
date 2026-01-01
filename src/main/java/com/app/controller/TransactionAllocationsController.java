package com.app.controller;

import com.app.dto.transactiontrack.*;
import com.app.service.TransactionTrackService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionAllocationsController {

    private final TransactionTrackService trackService;

    public TransactionAllocationsController(TransactionTrackService trackService) {
        this.trackService = trackService;
    }

    // POST /api/v1/transactions/{txId}/allocations
    @PostMapping("/{txId}/allocations")
    public ResponseEntity<AllocationResponse> allocate(@PathVariable Long txId,
                                                       @Valid @RequestBody AllocationFromTransactionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.allocateFromTransaction(txId, req));
    }

    // PUT /api/v1/transactions/{txId}/allocations/{allocationId}
    @PutMapping("/{txId}/allocations/{allocationId}")
    public ResponseEntity<AllocationResponse> update(@PathVariable Long txId,
                                                     @PathVariable Long allocationId,
                                                     @Valid @RequestBody AllocationFromTransactionUpdateRequest req) {
        return ResponseEntity.ok(trackService.updateFromTransaction(txId, allocationId, req));
    }

    // GET /api/v1/transactions/{txId}/allocations
    @GetMapping("/{txId}/allocations")
    public ResponseEntity<List<AllocationResponse>> list(@PathVariable Long txId) {
        return ResponseEntity.ok(trackService.listByTransaction(txId));
    }

    // DELETE /api/v1/transactions/{txId}/allocations/{allocationId}
    @DeleteMapping("/{txId}/allocations/{allocationId}")
    public ResponseEntity<Void> delete(@PathVariable Long txId, @PathVariable Long allocationId) {
        trackService.deleteByTransaction(txId, allocationId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/transactions/{txId}/allocation-candidates/debts?allocationId=123
    @GetMapping("/{txId}/allocation-candidates/debts")
    public ResponseEntity<List<DebtCandidateResponse>> debtCandidates(
            @PathVariable Long txId,
            @RequestParam(required = false) Long allocationId
    ) {
        return ResponseEntity.ok(trackService.debtCandidatesForTransaction(txId, allocationId));
    }
}
