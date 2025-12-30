package com.app.service;

import com.app.dto.transactiontrack.*;
import com.app.model.*;
import com.app.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionTrackService {

    private final TransactionTrackRepository trackRepository;
    private final DebtHeaderRepository debtHeaderRepository;
    private final TransactionRepository transactionRepository;
    private final JdbcTemplate jdbcTemplate;

    public TransactionTrackService(TransactionTrackRepository trackRepository,
                                   DebtHeaderRepository debtHeaderRepository,
                                   TransactionRepository transactionRepository,
                                   JdbcTemplate jdbcTemplate) {
        this.trackRepository = trackRepository;
        this.debtHeaderRepository = debtHeaderRepository;
        this.transactionRepository = transactionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public AllocationResponse allocate(Long debtId, AllocationCreateRequest req) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");
        if (req.transactionId() == null) throw new IllegalArgumentException("transactionId is required.");
        if (req.coveredAmount() == null || req.coveredAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("coveredAmount must be > 0.");

        DebtHeader debt = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + debtId));

        Transaction tx = transactionRepository.findById(req.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + req.transactionId()));

        // Must be same project
        Long debtProjectId = debt.getProject().getId();
        Long txProjectId = tx.getProject().getId();
        if (!debtProjectId.equals(txProjectId)) {
            throw new IllegalArgumentException("Debt and transaction must belong to the same project.");
        }

        // Prevent duplicate (debtId + txId)
        if (trackRepository.existsByDebtHeader_IdAndTransaction_Id(debtId, req.transactionId())) {
            throw new IllegalArgumentException("This transaction is already allocated to this debt.");
        }

        // Debt total = SUM(qnt * unit_price) from debts_detail
        BigDecimal debtTotal = getDebtTotal(debtId);
        BigDecimal debtCovered = trackRepository.sumCoveredByDebt(debtId);
        BigDecimal debtRemaining = debtTotal.subtract(debtCovered);

        if (req.coveredAmount().compareTo(debtRemaining) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds debt remaining. Remaining: " + debtRemaining);
        }

        // Transaction remaining = amount_paid - SUM(covered_amount for this transaction across all debts)
        BigDecimal txTotal = tx.getAmountPaid();
        BigDecimal txCovered = trackRepository.sumCoveredByTransaction(req.transactionId());
        BigDecimal txRemaining = txTotal.subtract(txCovered);

        if (req.coveredAmount().compareTo(txRemaining) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds transaction remaining. Remaining: " + txRemaining);
        }

        TransactionTrack track = new TransactionTrack();
        track.setDebtHeader(debt);
        track.setTransaction(tx);
        track.setCoveredAmount(req.coveredAmount());
        track.setDsc(trimToNull(req.dsc()));

        try {
            TransactionTrack saved = trackRepository.save(track);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // In case DB unique constraint or FK blocks it
            throw new IllegalArgumentException("Allocation could not be saved (DB constraint).");
        }
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> listByDebt(Long debtId) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");
        return trackRepository.findByDebtHeader_IdOrderByIdDesc(debtId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long debtId, Long allocationId) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");
        if (allocationId == null) throw new IllegalArgumentException("allocationId is required.");

        TransactionTrack tr = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found: " + allocationId));

        if (!tr.getDebtHeader().getId().equals(debtId)) {
            throw new IllegalArgumentException("Allocation does not belong to this debt.");
        }

        trackRepository.delete(tr);
    }

    // -------- helpers --------

    private BigDecimal getDebtTotal(Long debtId) {
        // debts_detail: qnt * unit_price
        BigDecimal total = jdbcTemplate.queryForObject(
                "select coalesce(sum(cast(qnt as decimal(18,2)) * cast(unit_price as decimal(18,2))), 0) " +
                        "from debts_detail where debt_header_id = ?",
                BigDecimal.class,
                debtId
        );
        // Convert to scale 0 style if you want (Toman integer), but keeping BigDecimal is safe
        return total == null ? BigDecimal.ZERO : total;
    }

    private AllocationResponse toResponse(TransactionTrack t) {
        return new AllocationResponse(
                t.getId(),
                t.getDebtHeader().getId(),
                t.getTransaction().getId(),
                t.getCoveredAmount(),
                t.getDsc()
        );
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
