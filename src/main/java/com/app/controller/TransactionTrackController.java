package com.app.controller;

import com.app.dto.transactiontrack.AllocationCreateRequest;
import com.app.dto.transactiontrack.AllocationResponse;
import com.app.dto.transactiontrack.AllocationUpdateRequest;
import com.app.dto.transactiontrack.TransactionCandidateResponse;
import com.app.service.TransactionTrackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/debts")
public class TransactionTrackController {

    private final TransactionTrackService trackService;

    public TransactionTrackController(TransactionTrackService trackService) {
        this.trackService = trackService;
    }

    // POST /api/v1/debts/{debtId}/allocations
    @PostMapping(
            value = "/{debtId}/allocations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AllocationResponse> allocate(@PathVariable Long debtId,
                                                       @Valid @RequestBody AllocationCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.allocate(debtId, req));
    }

    // PUT /api/v1/debts/{debtId}/allocations/{allocationId}
    @PutMapping(
            value = "/{debtId}/allocations/{allocationId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AllocationResponse> update(@PathVariable Long debtId,
                                                     @PathVariable Long allocationId,
                                                     @Valid @RequestBody AllocationUpdateRequest req) {
        return ResponseEntity.ok(trackService.updateFromDebt(debtId, allocationId, req));
    }

    // GET /api/v1/debts/{debtId}/allocations
    @GetMapping(
            value = "/{debtId}/allocations",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<AllocationResponse>> list(@PathVariable Long debtId) {
        return ResponseEntity.ok(trackService.listByDebt(debtId));
    }

    // DELETE /api/v1/debts/{debtId}/allocations/{allocationId}
    @DeleteMapping("/{debtId}/allocations/{allocationId}")
    public ResponseEntity<Void> delete(@PathVariable Long debtId,
                                       @PathVariable Long allocationId) {
        trackService.delete(debtId, allocationId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/debts/{debtId}/allocation-candidates/transactions?allocationId=123
    @GetMapping(
            value = "/{debtId}/allocation-candidates/transactions",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<TransactionCandidateResponse>> transactionCandidates(
            @PathVariable Long debtId,
            @RequestParam(required = false) Long allocationId
    ) {
        return ResponseEntity.ok(trackService.transactionCandidatesForDebt(debtId, allocationId));
    }
}
