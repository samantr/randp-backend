package com.app.controller;

import com.app.dto.transactiontrack.*;
import com.app.service.TransactionTrackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionAllocationsController {

    private final TransactionTrackService trackService;

    public TransactionAllocationsController(TransactionTrackService trackService) {
        this.trackService = trackService;
    }

    // POST /api/v1/transactions/{transactionId}/allocations
    @PostMapping(
            value = "/{transactionId}/allocations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AllocationResponse> allocate(@PathVariable("transactionId") Long transactionId,
                                                       @Valid @RequestBody AllocationFromTransactionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.allocateFromTransaction(transactionId, req));
    }

    // PUT /api/v1/transactions/{transactionId}/allocations/{allocationId}
    @PutMapping(
            value = "/{transactionId}/allocations/{allocationId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AllocationResponse> update(@PathVariable("transactionId") Long transactionId,
                                                     @PathVariable Long allocationId,
                                                     @Valid @RequestBody AllocationFromTransactionUpdateRequest req) {
        return ResponseEntity.ok(trackService.updateFromTransaction(transactionId, allocationId, req));
    }

    // GET /api/v1/transactions/{transactionId}/allocations
    @GetMapping(
            value = "/{transactionId}/allocations",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<AllocationResponse>> list(@PathVariable("transactionId") Long transactionId) {
        return ResponseEntity.ok(trackService.listByTransaction(transactionId));
    }

    // DELETE /api/v1/transactions/{transactionId}/allocations/{allocationId}
    @DeleteMapping("/{transactionId}/allocations/{allocationId}")
    public ResponseEntity<Void> delete(@PathVariable("transactionId") Long transactionId,
                                       @PathVariable Long allocationId) {
        trackService.deleteByTransaction(transactionId, allocationId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/transactions/{transactionId}/allocation-candidates/debts?allocationId=123
    @GetMapping(
            value = "/{transactionId}/allocation-candidates/debts",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DebtCandidateResponse>> debtCandidates(
            @PathVariable("transactionId") Long transactionId,
            @RequestParam(required = false) Long allocationId
    ) {
        return ResponseEntity.ok(trackService.debtCandidatesForTransaction(transactionId, allocationId));
    }
}
