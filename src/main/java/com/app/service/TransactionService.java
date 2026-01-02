package com.app.service;

import com.app.dto.transaction.*;
import com.app.model.*;
import com.app.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProjectRepository projectRepository;
    private final PersonRepository personRepository;
    private final JdbcTemplate jdbcTemplate;

    public TransactionService(TransactionRepository transactionRepository,
                              ProjectRepository projectRepository,
                              PersonRepository personRepository,
                              JdbcTemplate jdbcTemplate) {
        this.transactionRepository = transactionRepository;
        this.projectRepository = projectRepository;
        this.personRepository = personRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public TransactionResponse create(TransactionCreateRequest req) {
        validateCreateUpdate(req.projectId(), req.fromPersonId(), req.toPersonId(),
                req.code(), req.amountPaid(), req.paymentType(), req.transactionType(),
                req.dateDue(), req.dateRegistered(), null);

        Transaction t = new Transaction();
        apply(t, req.projectId(), req.fromPersonId(), req.toPersonId(),
                req.code(), req.dateDue(), req.amountPaid(),
                req.paymentType(), req.transactionType(), req.dateRegistered(), req.dsc());

        Transaction saved = transactionRepository.save(t);

        BigDecimal allocated = getAllocatedForTransaction(saved.getId());
        return toResponse(saved, allocated);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");

        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + id + ")"));

        BigDecimal allocated = getAllocatedForTransaction(id);
        return toResponse(t, allocated);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        List<Transaction> list = transactionRepository.findAll();
        if (list.isEmpty()) return List.of();

        List<Long> ids = list.stream().map(Transaction::getId).toList();
        Map<Long, BigDecimal> allocatedMap = getAllocatedMapForTransactions(ids);

        return list.stream()
                .map(t -> toResponse(t, allocatedMap.getOrDefault(t.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionUpdateRequest req) {
        if (id == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");

        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + id + ")"));

        validateCreateUpdate(req.projectId(), req.fromPersonId(), req.toPersonId(),
                req.code(), req.amountPaid(), req.paymentType(), req.transactionType(),
                req.dateDue(), req.dateRegistered(), id);

        apply(t, req.projectId(), req.fromPersonId(), req.toPersonId(),
                req.code(), req.dateDue(), req.amountPaid(),
                req.paymentType(), req.transactionType(), req.dateRegistered(), req.dsc());

        Transaction saved = transactionRepository.save(t);

        BigDecimal allocated = getAllocatedForTransaction(saved.getId());
        return toResponse(saved, allocated);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");

        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + id + ")"));

        if (isTransactionReferenced(id)) {
            throw new IllegalArgumentException("امکان حذف پرداخت وجود ندارد؛ برای این پرداخت تخصیص یا سند ثبت شده است.");
        }

        transactionRepository.delete(t);
    }

    // ---------------- Extra APIs ----------------

    @Transactional(readOnly = true)
    public List<LedgerRowResponse> ledger(Long projectId, Long personId, LocalDate from, LocalDate to) {
        if (projectId == null) throw new IllegalArgumentException("شناسه پروژه الزامی است.");
        if (personId == null) throw new IllegalArgumentException("شناسه شخص الزامی است.");

        projectRepository.findById(projectId).orElseThrow(() ->
                new IllegalArgumentException("پروژه مورد نظر یافت نشد. (شناسه: " + projectId + ")"));
        personRepository.findById(personId).orElseThrow(() ->
                new IllegalArgumentException("شخص مورد نظر یافت نشد. (شناسه: " + personId + ")"));

        StringBuilder sql = new StringBuilder("""
                select
                    t.id as transaction_id,
                    t.date_registered,
                    t.code,
                    t.from_person_id,
                    t.to_person_id,
                    t.amount_paid as amount,
                    case
                        when t.to_person_id = ? then t.amount_paid
                        when t.from_person_id = ? then -t.amount_paid
                        else 0
                    end as delta_for_person,
                    t.dsc
                from transactions t
                where t.project_id = ?
                  and (t.from_person_id = ? or t.to_person_id = ?)
                """);

        List<Object> args = new ArrayList<>();
        args.add(personId);
        args.add(personId);
        args.add(projectId);
        args.add(personId);
        args.add(personId);

        if (from != null) {
            sql.append(" and t.date_due >= ? ");
            args.add(from);
        }
        if (to != null) {
            sql.append(" and t.date_due <= ? ");
            args.add(to);
        }

        sql.append(" order by t.date_registered asc, t.id asc ");

        List<LedgerRowResponse> rows = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Long transactionId = rs.getLong("transaction_id");
            LocalDateTime dateRegistered = rs.getTimestamp("date_registered").toLocalDateTime();
            String code = rs.getString("code");
            Long fromPersonId = rs.getLong("from_person_id");
            Long toPersonId = rs.getLong("to_person_id");
            BigDecimal amount = rs.getBigDecimal("amount");
            BigDecimal delta = rs.getBigDecimal("delta_for_person");
            String dsc = rs.getString("dsc");

            return new LedgerRowResponse(
                    transactionId,
                    dateRegistered,
                    code,
                    fromPersonId,
                    toPersonId,
                    amount,
                    delta,
                    BigDecimal.ZERO, // runningBalance filled below
                    dsc
            );
        }, args.toArray());

        BigDecimal running = BigDecimal.ZERO;
        List<LedgerRowResponse> withBalance = new ArrayList<>();
        for (LedgerRowResponse r : rows) {
            running = running.add(r.deltaForPerson() == null ? BigDecimal.ZERO : r.deltaForPerson());
            withBalance.add(new LedgerRowResponse(
                    r.transactionId(),
                    r.dateRegistered(),
                    r.code(),
                    r.fromPersonId(),
                    r.toPersonId(),
                    r.amount(),
                    r.deltaForPerson(),
                    running,
                    r.dsc()
            ));
        }
        return withBalance;
    }

    @Transactional(readOnly = true)
    public PersonBalanceResponse personBalance(Long projectId, Long personId) {
        if (projectId == null) throw new IllegalArgumentException("شناسه پروژه الزامی است.");
        if (personId == null) throw new IllegalArgumentException("شناسه شخص الزامی است.");

        projectRepository.findById(projectId).orElseThrow(() ->
                new IllegalArgumentException("پروژه مورد نظر یافت نشد. (شناسه: " + projectId + ")"));
        personRepository.findById(personId).orElseThrow(() ->
                new IllegalArgumentException("شخص مورد نظر یافت نشد. (شناسه: " + personId + ")"));

        BigDecimal totalIn = jdbcTemplate.queryForObject("""
                        select coalesce(sum(amount_paid),0)
                        from transactions
                        where project_id = ? and to_person_id = ?
                        """, BigDecimal.class, projectId, personId);

        BigDecimal totalOut = jdbcTemplate.queryForObject("""
                        select coalesce(sum(amount_paid),0)
                        from transactions
                        where project_id = ? and from_person_id = ?
                        """, BigDecimal.class, projectId, personId);

        if (totalIn == null) totalIn = BigDecimal.ZERO;
        if (totalOut == null) totalOut = BigDecimal.ZERO;

        return new PersonBalanceResponse(projectId, personId, totalIn, totalOut, totalIn.subtract(totalOut));
    }

    @Transactional(readOnly = true)
    public PairBalanceResponse pairBalance(Long projectId, Long fromPersonId, Long toPersonId) {
        if (projectId == null) throw new IllegalArgumentException("شناسه پروژه الزامی است.");
        if (fromPersonId == null) throw new IllegalArgumentException("شناسه شخص پرداخت‌کننده (fromPerson) الزامی است.");
        if (toPersonId == null) throw new IllegalArgumentException("شناسه شخص دریافت‌کننده (toPerson) الزامی است.");

        projectRepository.findById(projectId).orElseThrow(() ->
                new IllegalArgumentException("پروژه مورد نظر یافت نشد. (شناسه: " + projectId + ")"));
        personRepository.findById(fromPersonId).orElseThrow(() ->
                new IllegalArgumentException("شخص پرداخت‌کننده یافت نشد. (شناسه: " + fromPersonId + ")"));
        personRepository.findById(toPersonId).orElseThrow(() ->
                new IllegalArgumentException("شخص دریافت‌کننده یافت نشد. (شناسه: " + toPersonId + ")"));

        BigDecimal fromToToTotal = jdbcTemplate.queryForObject("""
                        select coalesce(sum(amount_paid),0)
                        from transactions
                        where project_id = ? and from_person_id = ? and to_person_id = ?
                        """, BigDecimal.class, projectId, fromPersonId, toPersonId);

        BigDecimal toToFromTotal = jdbcTemplate.queryForObject("""
                        select coalesce(sum(amount_paid),0)
                        from transactions
                        where project_id = ? and from_person_id = ? and to_person_id = ?
                        """, BigDecimal.class, projectId, toPersonId, fromPersonId);

        if (fromToToTotal == null) fromToToTotal = BigDecimal.ZERO;
        if (toToFromTotal == null) toToFromTotal = BigDecimal.ZERO;

        return new PairBalanceResponse(projectId, fromPersonId, toPersonId,
                fromToToTotal, toToFromTotal, fromToToTotal.subtract(toToFromTotal));
    }

    // ---------------- Internals ----------------

    private void validateCreateUpdate(Long projectId, Long fromPersonId, Long toPersonId,
                                      String code, BigDecimal amountPaid, String paymentType, String transactionType,
                                      LocalDate dateDue, LocalDateTime dateRegistered, Long currentId) {

        if (projectId == null) throw new IllegalArgumentException("پروژه الزامی است.");
        if (fromPersonId == null) throw new IllegalArgumentException("شخص پرداخت‌کننده (fromPerson) الزامی است.");
        if (toPersonId == null) throw new IllegalArgumentException("شخص دریافت‌کننده (toPerson) الزامی است.");

        // منطقی: یک نفر نمی‌تواند هم پرداخت‌کننده و هم دریافت‌کننده همان تراکنش باشد
        if (fromPersonId != null && toPersonId != null && fromPersonId.equals(toPersonId)) {
            throw new IllegalArgumentException("پرداخت‌کننده و دریافت‌کننده نمی‌توانند یکسان باشند.");
        }

        if (code == null || code.trim().isEmpty()) throw new IllegalArgumentException("کد پرداخت الزامی است.");
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("مبلغ پرداخت باید بزرگتر از صفر باشد.");

        if (paymentType == null || paymentType.trim().length() != 3)
            throw new IllegalArgumentException("نوع پرداخت (paymentType) باید ۳ حرفی باشد.");
        if (transactionType == null || transactionType.trim().length() != 3)
            throw new IllegalArgumentException("نوع تراکنش (transactionType) باید ۳ حرفی باشد.");

        if (dateDue == null) throw new IllegalArgumentException("تاریخ سررسید الزامی است.");
        if (dateRegistered == null) throw new IllegalArgumentException("تاریخ ثبت الزامی است.");

        projectRepository.findById(projectId).orElseThrow(() ->
                new IllegalArgumentException("پروژه مورد نظر یافت نشد. (شناسه: " + projectId + ")"));
        personRepository.findById(fromPersonId).orElseThrow(() ->
                new IllegalArgumentException("شخص پرداخت‌کننده یافت نشد. (شناسه: " + fromPersonId + ")"));
        personRepository.findById(toPersonId).orElseThrow(() ->
                new IllegalArgumentException("شخص دریافت‌کننده یافت نشد. (شناسه: " + toPersonId + ")"));

        String normalizedCode = code.trim();

        Long existingId = transactionRepository.findByCode(normalizedCode).map(Transaction::getId).orElse(null);
        if (existingId != null && (currentId == null || !existingId.equals(currentId))) {
            throw new IllegalArgumentException("این کد پرداخت قبلاً ثبت شده است: " + normalizedCode);
        }
    }

    private void apply(Transaction t, Long projectId, Long fromPersonId, Long toPersonId,
                       String code, LocalDate dateDue, BigDecimal amountPaid,
                       String paymentType, String transactionType, LocalDateTime dateRegistered, String dsc) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("پروژه مورد نظر یافت نشد. (شناسه: " + projectId + ")"));
        Person from = personRepository.findById(fromPersonId)
                .orElseThrow(() -> new IllegalArgumentException("شخص پرداخت‌کننده یافت نشد. (شناسه: " + fromPersonId + ")"));
        Person to = personRepository.findById(toPersonId)
                .orElseThrow(() -> new IllegalArgumentException("شخص دریافت‌کننده یافت نشد. (شناسه: " + toPersonId + ")"));

        t.setProject(project);
        t.setFromPerson(from);
        t.setToPerson(to);
        t.setCode(code.trim());
        t.setDateDue(dateDue);
        t.setAmountPaid(amountPaid);
        t.setPaymentType(paymentType.trim().toUpperCase());
        t.setTransactionType(transactionType.trim().toUpperCase());
        t.setDateRegistered(dateRegistered);
        t.setDsc(trimToNull(dsc));
    }

    private TransactionResponse toResponse(Transaction t, BigDecimal allocatedAmount) {
        BigDecimal amountPaid = t.getAmountPaid() == null ? BigDecimal.ZERO : t.getAmountPaid();
        BigDecimal allocated = allocatedAmount == null ? BigDecimal.ZERO : allocatedAmount;
        BigDecimal remaining = amountPaid.subtract(allocated);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        return new TransactionResponse(
                t.getId(),
                t.getProject().getId(),
                t.getFromPerson().getId(),
                t.getToPerson().getId(),
                t.getCode(),
                t.getDateDue(),
                t.getAmountPaid(),
                t.getPaymentType(),
                t.getTransactionType(),
                t.getDateRegistered(),
                t.getDsc(),
                allocated,
                remaining
        );
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isTransactionReferenced(Long transactionId) {
        Integer trackCount = jdbcTemplate.queryForObject(
                "select count(1) from transaction_tracks where transaction_id = ?",
                Integer.class, transactionId
        );
        Integer docCount = jdbcTemplate.queryForObject(
                "select count(1) from transaction_documents where transaction_id = ?",
                Integer.class, transactionId
        );
        return (trackCount != null && trackCount > 0) || (docCount != null && docCount > 0);
    }

    private BigDecimal getAllocatedForTransaction(Long transactionId) {
        BigDecimal sum = jdbcTemplate.queryForObject("""
                select coalesce(sum(covered_amount), 0)
                from transaction_tracks
                where transaction_id = ?
                """, BigDecimal.class, transactionId);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private Map<Long, BigDecimal> getAllocatedMapForTransactions(List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) return Map.of();

        String placeholders = String.join(",", transactionIds.stream().map(x -> "?").toList());

        String sql = """
                select transaction_id, coalesce(sum(covered_amount), 0) as allocated
                from transaction_tracks
                where transaction_id in (""" + placeholders + """
                )
                group by transaction_id
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, transactionIds.toArray());

        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Long id = ((Number) r.get("transaction_id")).longValue();
            BigDecimal allocated = (BigDecimal) r.get("allocated");
            map.put(id, allocated == null ? BigDecimal.ZERO : allocated);
        }
        return map;
    }
}
