package com.app.controller;

import com.app.dto.transactiontrack.*;
import com.app.service.TransactionTrackService;
import jakarta.validation.Valid;
import org.springframework.http.*;
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
    @PostMapping("/{debtId}/allocations")
    public ResponseEntity<AllocationResponse> allocate(@PathVariable Long debtId,
                                                       @Valid @RequestBody AllocationCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.allocate(debtId, req));
    }

    // GET /api/v1/debts/{debtId}/allocations
    @GetMapping("/{debtId}/allocations")
    public ResponseEntity<List<AllocationResponse>> list(@PathVariable Long debtId) {
        return ResponseEntity.ok(trackService.listByDebt(debtId));
    }

    // DELETE /api/v1/debts/{debtId}/allocations/{allocationId}
    @DeleteMapping("/{debtId}/allocations/{allocationId}")
    public ResponseEntity<Void> delete(@PathVariable Long debtId, @PathVariable Long allocationId) {
        trackService.delete(debtId, allocationId);
        return ResponseEntity.noContent().build();
    }
}
