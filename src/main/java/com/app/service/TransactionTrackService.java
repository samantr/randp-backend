package com.app.service;

import com.app.dto.transactiontrack.*;
import com.app.model.DebtHeader;
import com.app.model.Transaction;
import com.app.model.TransactionTrack;
import com.app.repository.DebtHeaderRepository;
import com.app.repository.TransactionRepository;
import com.app.repository.TransactionTrackRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات تخصیص ارسال نشده است.");
        if (req.transactionId() == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        validateAmount(req.coveredAmount());

        DebtHeader debt = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + debtId + ")"));

        Transaction tx = transactionRepository.findById(req.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + req.transactionId() + ")"));

        assertSamePersonForAllocation(debt, tx);

        if (trackRepository.existsByDebtHeader_IdAndTransaction_Id(debtId, req.transactionId())) {
            throw new IllegalArgumentException("این پرداخت قبلاً برای این بدهی تخصیص داده شده است.");
        }

        BigDecimal debtTotal = getDebtTotal(debtId);
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(debtId));
        BigDecimal debtRemaining = debtTotal.subtract(debtCovered);
        if (req.coveredAmount().compareTo(debtRemaining) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده بدهی بیشتر است. مانده قابل تخصیص: " + fmt(debtRemaining));
        }

        BigDecimal txTotal = nz(tx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(req.transactionId()));
        BigDecimal txRemaining = txTotal.subtract(txCovered);
        if (req.coveredAmount().compareTo(txRemaining) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده پرداخت بیشتر است. مانده قابل تخصیص: " + fmt(txRemaining));
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
            throw new IllegalArgumentException("ثبت تخصیص انجام نشد. احتمالاً این تخصیص تکراری است یا محدودیت دیتابیس وجود دارد.");
        }
    }

    // ---------------- CREATE (from TRANSACTION side) ----------------

    @Transactional
    public AllocationResponse allocateFromTransaction(Long txId, AllocationFromTransactionCreateRequest req) {
        if (txId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات تخصیص ارسال نشده است.");
        if (req.debtId() == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        validateAmount(req.coveredAmount());

        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + txId + ")"));

        DebtHeader debt = debtHeaderRepository.findById(req.debtId())
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + req.debtId() + ")"));

        assertSamePersonForAllocation(debt, tx);

        if (trackRepository.existsByDebtHeader_IdAndTransaction_Id(req.debtId(), txId)) {
            throw new IllegalArgumentException("این بدهی قبلاً برای این پرداخت تخصیص داده شده است.");
        }

        BigDecimal debtTotal = getDebtTotal(req.debtId());
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(req.debtId()));
        BigDecimal debtRemaining = debtTotal.subtract(debtCovered);
        if (req.coveredAmount().compareTo(debtRemaining) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده بدهی بیشتر است. مانده قابل تخصیص: " + fmt(debtRemaining));
        }

        BigDecimal txTotal = nz(tx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(txId));
        BigDecimal txRemaining = txTotal.subtract(txCovered);
        if (req.coveredAmount().compareTo(txRemaining) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده پرداخت بیشتر است. مانده قابل تخصیص: " + fmt(txRemaining));
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
            throw new IllegalArgumentException("ثبت تخصیص انجام نشد. احتمالاً این تخصیص تکراری است یا محدودیت دیتابیس وجود دارد.");
        }
    }

    // ---------------- LIST ----------------

    @Transactional(readOnly = true)
    public List<AllocationResponse> listByDebt(Long debtId) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        return trackRepository.findByDebtHeader_IdOrderByIdDesc(debtId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> listByTransaction(Long txId) {
        if (txId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        return trackRepository.findByTransaction_IdOrderByIdDesc(txId)
                .stream().map(this::toResponse).toList();
    }

    // ---------------- DELETE ----------------

    @Transactional
    public void delete(Long debtId, Long allocationId) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        if (allocationId == null) throw new IllegalArgumentException("شناسه تخصیص الزامی است.");

        TransactionTrack tr = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("تخصیص مورد نظر یافت نشد. (شناسه: " + allocationId + ")"));

        if (!tr.getDebtHeader().getId().equals(debtId)) {
            throw new IllegalArgumentException("این تخصیص متعلق به این بدهی نیست.");
        }

        trackRepository.delete(tr);
    }

    @Transactional
    public void deleteByTransaction(Long txId, Long allocationId) {
        if (txId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        if (allocationId == null) throw new IllegalArgumentException("شناسه تخصیص الزامی است.");

        TransactionTrack tr = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("تخصیص مورد نظر یافت نشد. (شناسه: " + allocationId + ")"));

        if (!tr.getTransaction().getId().equals(txId)) {
            throw new IllegalArgumentException("این تخصیص متعلق به این پرداخت نیست.");
        }

        trackRepository.delete(tr);
    }

    // ---------------- UPDATE (DEBT side) ----------------

    @Transactional
    public AllocationResponse updateFromDebt(Long debtId, Long allocationId, AllocationUpdateRequest req) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        if (allocationId == null) throw new IllegalArgumentException("شناسه تخصیص الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش تخصیص ارسال نشده است.");
        if (req.transactionId() == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        validateAmount(req.coveredAmount());

        TransactionTrack existing = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("تخصیص مورد نظر یافت نشد. (شناسه: " + allocationId + ")"));

        if (!existing.getDebtHeader().getId().equals(debtId)) {
            throw new IllegalArgumentException("این تخصیص متعلق به این بدهی نیست.");
        }

        DebtHeader debt = existing.getDebtHeader();

        Transaction newTx = transactionRepository.findById(req.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + req.transactionId() + ")"));

        assertSamePersonForAllocation(debt, newTx);

        Long oldTxId = existing.getTransaction().getId();
        if (!oldTxId.equals(req.transactionId())
                && trackRepository.existsByDebtHeader_IdAndTransaction_Id(debtId, req.transactionId())) {
            throw new IllegalArgumentException("این پرداخت قبلاً برای این بدهی تخصیص داده شده است.");
        }

        BigDecimal oldAmount = nz(existing.getCoveredAmount());
        BigDecimal newAmount = req.coveredAmount();

        BigDecimal debtTotal = getDebtTotal(debtId);
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(debtId));
        BigDecimal debtCoveredExcludingOld = debtCovered.subtract(oldAmount);
        BigDecimal debtRemainingForEdit = debtTotal.subtract(debtCoveredExcludingOld);
        if (newAmount.compareTo(debtRemainingForEdit) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده بدهی بیشتر است. مانده قابل تخصیص: " + fmt(debtRemainingForEdit));
        }

        BigDecimal txTotal = nz(newTx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(req.transactionId()));
        BigDecimal txCoveredExcludingOld = txCovered;
        if (oldTxId.equals(req.transactionId())) {
            txCoveredExcludingOld = txCovered.subtract(oldAmount);
        }
        BigDecimal txRemainingForEdit = txTotal.subtract(txCoveredExcludingOld);
        if (newAmount.compareTo(txRemainingForEdit) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده پرداخت بیشتر است. مانده قابل تخصیص: " + fmt(txRemainingForEdit));
        }

        existing.setTransaction(newTx);
        existing.setCoveredAmount(newAmount);
        existing.setDsc(trimToNull(req.dsc()));

        try {
            return toResponse(trackRepository.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("ویرایش تخصیص انجام نشد. احتمالاً این تخصیص تکراری است یا محدودیت دیتابیس وجود دارد.");
        }
    }

    // ---------------- UPDATE (TRANSACTION side) ----------------

    @Transactional
    public AllocationResponse updateFromTransaction(Long txId, Long allocationId, AllocationFromTransactionUpdateRequest req) {
        if (txId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        if (allocationId == null) throw new IllegalArgumentException("شناسه تخصیص الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش تخصیص ارسال نشده است.");
        if (req.debtId() == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        validateAmount(req.coveredAmount());

        TransactionTrack existing = trackRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("تخصیص مورد نظر یافت نشد. (شناسه: " + allocationId + ")"));

        if (!existing.getTransaction().getId().equals(txId)) {
            throw new IllegalArgumentException("این تخصیص متعلق به این پرداخت نیست.");
        }

        Transaction tx = existing.getTransaction();

        DebtHeader newDebt = debtHeaderRepository.findById(req.debtId())
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + req.debtId() + ")"));

        assertSamePersonForAllocation(newDebt, tx);

        Long oldDebtId = existing.getDebtHeader().getId();
        if (!oldDebtId.equals(req.debtId())
                && trackRepository.existsByDebtHeader_IdAndTransaction_Id(req.debtId(), txId)) {
            throw new IllegalArgumentException("این بدهی قبلاً برای این پرداخت تخصیص داده شده است.");
        }

        BigDecimal oldAmount = nz(existing.getCoveredAmount());
        BigDecimal newAmount = req.coveredAmount();

        BigDecimal txTotal = nz(tx.getAmountPaid());
        BigDecimal txCovered = nz(trackRepository.sumCoveredByTransaction(txId));
        BigDecimal txCoveredExcludingOld = txCovered.subtract(oldAmount);
        BigDecimal txRemainingForEdit = txTotal.subtract(txCoveredExcludingOld);
        if (newAmount.compareTo(txRemainingForEdit) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده پرداخت بیشتر است. مانده قابل تخصیص: " + fmt(txRemainingForEdit));
        }

        BigDecimal debtTotal = getDebtTotal(req.debtId());
        BigDecimal debtCovered = nz(trackRepository.sumCoveredByDebt(req.debtId()));
        BigDecimal debtCoveredExcludingOld = debtCovered;
        if (oldDebtId.equals(req.debtId())) {
            debtCoveredExcludingOld = debtCovered.subtract(oldAmount);
        }
        BigDecimal debtRemainingForEdit = debtTotal.subtract(debtCoveredExcludingOld);
        if (newAmount.compareTo(debtRemainingForEdit) > 0) {
            throw new IllegalArgumentException("مبلغ تخصیص از مانده بدهی بیشتر است. مانده قابل تخصیص: " + fmt(debtRemainingForEdit));
        }

        existing.setDebtHeader(newDebt);
        existing.setCoveredAmount(newAmount);
        existing.setDsc(trimToNull(req.dsc()));

        try {
            return toResponse(trackRepository.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("ویرایش تخصیص انجام نشد. احتمالاً این تخصیص تکراری است یا محدودیت دیتابیس وجود دارد.");
        }
    }

    // ---------------- CANDIDATES (ALL, including remaining=0) ----------------

    @Transactional(readOnly = true)
    public List<TransactionCandidateResponse> transactionCandidatesForDebt(Long debtId, Long allocationId) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");

        DebtHeader debt = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + debtId + ")"));

        Long editingTxId = null;
        BigDecimal editingOldAmount = BigDecimal.ZERO;
        if (allocationId != null) {
            TransactionTrack tr = trackRepository.findById(allocationId)
                    .orElseThrow(() -> new IllegalArgumentException("تخصیص مورد نظر یافت نشد. (شناسه: " + allocationId + ")"));
            if (!tr.getDebtHeader().getId().equals(debtId)) {
                throw new IllegalArgumentException("این تخصیص متعلق به این بدهی نیست.");
            }
            editingTxId = tr.getTransaction().getId();
            editingOldAmount = nz(tr.getCoveredAmount());
        }

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
        if (txId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");

        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + txId + ")"));

        Long editingDebtId = null;
        BigDecimal editingOldAmount = BigDecimal.ZERO;
        if (allocationId != null) {
            TransactionTrack tr = trackRepository.findById(allocationId)
                    .orElseThrow(() -> new IllegalArgumentException("تخصیص مورد نظر یافت نشد. (شناسه: " + allocationId + ")"));
            if (!tr.getTransaction().getId().equals(txId)) {
                throw new IllegalArgumentException("این تخصیص متعلق به این پرداخت نیست.");
            }
            editingDebtId = tr.getDebtHeader().getId();
            editingOldAmount = nz(tr.getCoveredAmount());
        }

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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("مبلغ تخصیص باید بزرگتر از صفر باشد.");
        }
    }

    // Allocation allowed only if debt.person_id == tx.to_person_id
    private void assertSamePersonForAllocation(DebtHeader debt, Transaction tx) {
        Long debtPersonId = debt.getPerson().getId();
        Long txToPersonId = tx.getToPerson().getId();
        if (!debtPersonId.equals(txToPersonId)) {
            throw new IllegalArgumentException("تخصیص مجاز نیست؛ شخصِ بدهی باید با گیرنده پرداخت (to_person) یکسان باشد.");
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

    private LocalDateTime toLocalDateTime(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0";
        return v.stripTrailingZeros().toPlainString();
    }
}
