package com.app.service;

import com.app.dto.transactiontrack.*;
import com.app.model.*;
import com.app.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ---------------- CREATE (from DEBT side) ----------------

    @Transactional
    public AllocationResponse allocate(Long debtId, AllocationCreateRequest req) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");
        if (req.transactionId() == null) throw new IllegalArgumentException("transactionId is required.");
        validateAmount(req.coveredAmount());

        DebtHeader debt = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + debtId));

        Transaction tx = transactionRepository.findById(req.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + req.transactionId()));

        //assertSameProject(debt, tx);
        assertSamePersonForAllocation(debt, tx); // ✅ NEW VALIDATION

        if (trackRepository.existsByDebtHeader_IdAndTransaction_Id(debtId, req.transactionId())) {
            throw new IllegalArgumentException("This transaction is already allocated to this debt.");
        }

        // validate against remaining
        BigDecimal debtTotal = getDebtTotal(debtId);
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(debtId));
        BigDecimal debtRemaining = debtTotal.subtract(debtCovered);
        if (req.coveredAmount().compareTo(debtRemaining) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds debt remaining. Remaining: " + debtRemaining);
        }

        BigDecimal txTotal = nz(tx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(req.transactionId()));
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
            throw new IllegalArgumentException("Allocation could not be saved (DB constraint).");
        }
    }

    // ---------------- CREATE (from TRANSACTION side) ----------------

    @Transactional
    public AllocationResponse allocateFromTransaction(Long txId, AllocationFromTransactionCreateRequest req) {
        if (txId == null) throw new IllegalArgumentException("transactionId is required.");
        if (req.debtId() == null) throw new IllegalArgumentException("debtId is required.");
        validateAmount(req.coveredAmount());

        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));

        DebtHeader debt = debtHeaderRepository.findById(req.debtId())
                .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + req.debtId()));

        //assertSameProject(debt, tx);
        assertSamePersonForAllocation(debt, tx); // ✅ NEW VALIDATION

        if (trackRepository.existsByDebtHeader_IdAndTransaction_Id(req.debtId(), txId)) {
            throw new IllegalArgumentException("This debt is already allocated to this transaction.");
        }

        // validate remaining
        BigDecimal debtTotal = getDebtTotal(req.debtId());
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(req.debtId()));
        BigDecimal debtRemaining = debtTotal.subtract(debtCovered);
        if (req.coveredAmount().compareTo(debtRemaining) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds debt remaining. Remaining: " + debtRemaining);
        }

        BigDecimal txTotal = nz(tx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(txId));
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
            throw new IllegalArgumentException("Allocation could not be saved (DB constraint).");
        }
    }

    // ---------------- LIST ----------------

    @Transactional(readOnly = true)
    public List<AllocationResponse> listByDebt(Long debtId) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");
        return trackRepository.findByDebtHeader_IdOrderByIdDesc(debtId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> listByTransaction(Long txId) {
        if (txId == null) throw new IllegalArgumentException("transactionId is required.");
        return trackRepository.findByTransaction_IdOrderByIdDesc(txId)
                .stream().map(this::toResponse).toList();
    }

    // ---------------- DELETE ----------------

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

    @Transactional
    public void deleteByTransaction(Long txId, Long allocationId) {
        if (txId == null) throw new IllegalArgumentException("transactionId is required.");
        if (allocationId == null) throw new IllegalArgumentException("allocationId is required.");

        TransactionTrack tr = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found: " + allocationId));

        if (!tr.getTransaction().getId().equals(txId)) {
            throw new IllegalArgumentException("Allocation does not belong to this transaction.");
        }

        trackRepository.delete(tr);
    }

    // ---------------- UPDATE (DEBT side) ----------------

    @Transactional
    public AllocationResponse updateFromDebt(Long debtId, Long allocationId, AllocationUpdateRequest req) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");
        if (allocationId == null) throw new IllegalArgumentException("allocationId is required.");
        if (req.transactionId() == null) throw new IllegalArgumentException("transactionId is required.");
        validateAmount(req.coveredAmount());

        TransactionTrack existing = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found: " + allocationId));

        if (!existing.getDebtHeader().getId().equals(debtId)) {
            throw new IllegalArgumentException("Allocation does not belong to this debt.");
        }

        DebtHeader debt = existing.getDebtHeader();

        Transaction newTx = transactionRepository.findById(req.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + req.transactionId()));

        //assertSameProject(debt, newTx);
        assertSamePersonForAllocation(debt, newTx); // ✅ NEW VALIDATION

        // prevent duplicate pair (debtId + txId) except itself
        Long oldTxId = existing.getTransaction().getId();
        if (!oldTxId.equals(req.transactionId())
                && trackRepository.existsByDebtHeader_IdAndTransaction_Id(debtId, req.transactionId())) {
            throw new IllegalArgumentException("This transaction is already allocated to this debt.");
        }

        BigDecimal oldAmount = nz(existing.getCoveredAmount());
        BigDecimal newAmount = req.coveredAmount();

        // debt remaining excluding old allocation
        BigDecimal debtTotal = getDebtTotal(debtId);
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(debtId));
        BigDecimal debtCoveredExcludingOld = debtCovered.subtract(oldAmount);
        BigDecimal debtRemainingForEdit = debtTotal.subtract(debtCoveredExcludingOld);
        if (newAmount.compareTo(debtRemainingForEdit) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds debt remaining. Remaining: " + debtRemainingForEdit);
        }

        // transaction remaining excluding old allocation only if old tx == new tx
        BigDecimal txTotal = nz(newTx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(req.transactionId()));
        BigDecimal txCoveredExcludingOld = txCovered;
        if (oldTxId.equals(req.transactionId())) {
            txCoveredExcludingOld = txCovered.subtract(oldAmount);
        }
        BigDecimal txRemainingForEdit = txTotal.subtract(txCoveredExcludingOld);
        if (newAmount.compareTo(txRemainingForEdit) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds transaction remaining. Remaining: " + txRemainingForEdit);
        }

        existing.setTransaction(newTx);
        existing.setCoveredAmount(newAmount);
        existing.setDsc(trimToNull(req.dsc()));

        try {
            return toResponse(trackRepository.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Allocation could not be saved (DB constraint).");
        }
    }

    // ---------------- UPDATE (TRANSACTION side) ----------------

    @Transactional
    public AllocationResponse updateFromTransaction(Long txId, Long allocationId, AllocationFromTransactionUpdateRequest req) {
        if (txId == null) throw new IllegalArgumentException("transactionId is required.");
        if (allocationId == null) throw new IllegalArgumentException("allocationId is required.");
        if (req.debtId() == null) throw new IllegalArgumentException("debtId is required.");
        validateAmount(req.coveredAmount());

        TransactionTrack existing = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found: " + allocationId));

        if (!existing.getTransaction().getId().equals(txId)) {
            throw new IllegalArgumentException("Allocation does not belong to this transaction.");
        }

        Transaction tx = existing.getTransaction();

        DebtHeader newDebt = debtHeaderRepository.findById(req.debtId())
                .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + req.debtId()));

        //assertSameProject(newDebt, tx);
        assertSamePersonForAllocation(newDebt, tx); // ✅ NEW VALIDATION

        Long oldDebtId = existing.getDebtHeader().getId();
        if (!oldDebtId.equals(req.debtId())
                && trackRepository.existsByDebtHeader_IdAndTransaction_Id(req.debtId(), txId)) {
            throw new IllegalArgumentException("This debt is already allocated to this transaction.");
        }

        BigDecimal oldAmount = nz(existing.getCoveredAmount());
        BigDecimal newAmount = req.coveredAmount();

        // transaction remaining excluding old allocation
        BigDecimal txTotal = nz(tx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(txId));
        BigDecimal txCoveredExcludingOld = txCovered.subtract(oldAmount);
        BigDecimal txRemainingForEdit = txTotal.subtract(txCoveredExcludingOld);
        if (newAmount.compareTo(txRemainingForEdit) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds transaction remaining. Remaining: " + txRemainingForEdit);
        }

        // debt remaining excluding old allocation only if old debt == new debt
        BigDecimal debtTotal = getDebtTotal(req.debtId());
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(req.debtId()));
        BigDecimal debtCoveredExcludingOld = debtCovered;
        if (oldDebtId.equals(req.debtId())) {
            debtCoveredExcludingOld = debtCovered.subtract(oldAmount);
        }
        BigDecimal debtRemainingForEdit = debtTotal.subtract(debtCoveredExcludingOld);
        if (newAmount.compareTo(debtRemainingForEdit) > 0) {
            throw new IllegalArgumentException("Covered amount exceeds debt remaining. Remaining: " + debtRemainingForEdit);
        }

        existing.setDebtHeader(newDebt);
        existing.setCoveredAmount(newAmount);
        existing.setDsc(trimToNull(req.dsc()));

        try {
            return toResponse(trackRepository.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Allocation could not be saved (DB constraint).");
        }
    }

    // ---------------- CANDIDATES (ALL, including remaining=0) ----------------

    @Transactional(readOnly = true)
    public List<TransactionCandidateResponse> transactionCandidatesForDebt(Long debtId, Long allocationId) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");

        DebtHeader debt = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + debtId));

        // if editing, we add old covered back to editableRemaining for its linked tx
        Long editingTxId = null;
        BigDecimal editingOldAmount = BigDecimal.ZERO;
        if (allocationId != null) {
            TransactionTrack tr = trackRepository.findById(allocationId)
                    .orElseThrow(() -> new IllegalArgumentException("Allocation not found: " + allocationId));
            if (!tr.getDebtHeader().getId().equals(debtId)) {
                throw new IllegalArgumentException("Allocation does not belong to this debt.");
            }
            editingTxId = tr.getTransaction().getId();
            editingOldAmount = nz(tr.getCoveredAmount());
        }

        // ✅ FILTER by person rule: t.to_person_id must match debt.person_id
        String sql = """
            select
                t.id,
                t.code,
                t.date_registered,
                t.amount_paid,
                coalesce(tt.covered,0) as allocated_amount
            from transactions t
            left join (
                select transaction_id, coalesce(sum(covered_amount),0) as covered
                from transaction_tracks
                group by transaction_id
            ) tt on tt.transaction_id = t.id
            where  t.to_person_id = ?
            order by t.date_registered desc, t.id desc
            """;

        //Long projectId = debt.getProject().getId();
        Long debtPersonId = debt.getPerson().getId();

        Long finalEditingTxId = editingTxId;
        BigDecimal finalEditingOldAmount = editingOldAmount;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long id = rs.getLong("id");
            String code = rs.getString("code");
            LocalDateTime dateRegistered = toLocalDateTime(rs.getTimestamp("date_registered"));
            BigDecimal amountPaid = nz(rs.getBigDecimal("amount_paid"));
            BigDecimal allocated = nz(rs.getBigDecimal("allocated_amount"));
            BigDecimal remaining = amountPaid.subtract(allocated);

            BigDecimal editableRemaining = remaining;
            if (finalEditingTxId != null && id.equals(finalEditingTxId)) {
                editableRemaining = remaining.add(finalEditingOldAmount);
            }

            return new TransactionCandidateResponse(
                    id, code, dateRegistered, amountPaid, allocated, remaining, editableRemaining
            );
        }, debtPersonId);
    }

    @Transactional(readOnly = true)
    public List<DebtCandidateResponse> debtCandidatesForTransaction(Long txId, Long allocationId) {
        if (txId == null) throw new IllegalArgumentException("transactionId is required.");

        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));

        // if editing, add old covered back to editableRemaining for its linked debt
        Long editingDebtId = null;
        BigDecimal editingOldAmount = BigDecimal.ZERO;
        if (allocationId != null) {
            TransactionTrack tr = trackRepository.findById(allocationId)
                    .orElseThrow(() -> new IllegalArgumentException("Allocation not found: " + allocationId));
            if (!tr.getTransaction().getId().equals(txId)) {
                throw new IllegalArgumentException("Allocation does not belong to this transaction.");
            }
            editingDebtId = tr.getDebtHeader().getId();
            editingOldAmount = nz(tr.getCoveredAmount());
        }

        // ✅ FILTER by person rule: dh.person_id must match tx.to_person_id
        String sql = """
            select
                dh.id as debt_id,
                dh.date_registered,
                case
                    when p.is_legal = 1 then coalesce(p.company_name,'')
                    else ltrim(rtrim(coalesce(p.name,'') + ' ' + coalesce(p.last_name,'')))
                end as person_title,
                coalesce(sum(cast(dd.qnt as decimal(18,3)) * cast(dd.unit_price as decimal(18,0))),0) as total_amount,
                coalesce(tt.covered,0) as allocated_amount
            from debts_header dh
            join persons p on p.id = dh.person_id
            left join debts_detail dd on dd.debt_header_id = dh.id
            left join (
                select debt_header_id, coalesce(sum(covered_amount),0) as covered
                from transaction_tracks
                group by debt_header_id
            ) tt on tt.debt_header_id = dh.id
            where  dh.person_id = ?
            group by dh.id, dh.date_registered, p.is_legal, p.company_name, p.name, p.last_name, tt.covered
            order by dh.date_registered desc, dh.id desc
            """;

        //Long projectId = tx.getProject().getId();
        Long txToPersonId = tx.getToPerson().getId();

        Long finalEditingDebtId = editingDebtId;
        BigDecimal finalEditingOldAmount = editingOldAmount;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long id = rs.getLong("debt_id");
            String personTitle = rs.getString("person_title");
            LocalDateTime dateRegistered = toLocalDateTime(rs.getTimestamp("date_registered"));
            BigDecimal total = nz(rs.getBigDecimal("total_amount"));
            BigDecimal allocated = nz(rs.getBigDecimal("allocated_amount"));
            BigDecimal remaining = total.subtract(allocated);

            BigDecimal editableRemaining = remaining;
            if (finalEditingDebtId != null && id.equals(finalEditingDebtId)) {
                editableRemaining = remaining.add(finalEditingOldAmount);
            }

            return new DebtCandidateResponse(
                    id, personTitle, dateRegistered, total, allocated, remaining, editableRemaining
            );
        }, txToPersonId);
    }

    // -------- helpers --------

    private BigDecimal getDebtTotal(Long debtId) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "select coalesce(sum(cast(qnt as decimal(18,2)) * cast(unit_price as decimal(18,2))), 0) " +
                        "from debts_detail where debt_header_id = ?",
                BigDecimal.class,
                debtId
        );
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

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("coveredAmount must be > 0.");
    }

    private void assertSameProject(DebtHeader debt, Transaction tx) {
        Long debtProjectId = debt.getProject().getId();
        Long txProjectId = tx.getProject().getId();
        if (!debtProjectId.equals(txProjectId)) {
            throw new IllegalArgumentException("Debt and transaction must belong to the same project.");
        }
    }

    // ✅ NEW: person validation rule
    // Allocation allowed only if debt.person_id == tx.to_person_id
    private void assertSamePersonForAllocation(DebtHeader debt, Transaction tx) {
        Long debtPersonId = debt.getPerson().getId();
        Long txToPersonId = tx.getToPerson().getId();
        if (!debtPersonId.equals(txToPersonId)) {
            throw new IllegalArgumentException(
                    "Allocation is not allowed: debt person and transaction گیرنده (to_person) must be the same."
            );
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
