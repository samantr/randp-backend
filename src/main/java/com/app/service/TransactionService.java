package com.app.service;

import com.app.dto.transaction.*;
import com.app.model.*;
import com.app.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        return toResponse(transactionRepository.save(t));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        return transactionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionUpdateRequest req) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));

        validateCreateUpdate(req.projectId(), req.fromPersonId(), req.toPersonId(),
                req.code(), req.amountPaid(), req.paymentType(), req.transactionType(),
                req.dateDue(), req.dateRegistered(), id);

        apply(t, req.projectId(), req.fromPersonId(), req.toPersonId(),
                req.code(), req.dateDue(), req.amountPaid(),
                req.paymentType(), req.transactionType(), req.dateRegistered(), req.dsc());

        return toResponse(transactionRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));

        // referenced by transaction_tracks or documents?
        if (isTransactionReferenced(id)) {
            throw new IllegalArgumentException("Cannot delete transaction: referenced by tracks/documents.");
        }

        transactionRepository.delete(t);
    }

    // ---------------- Extra APIs ----------------

    @Transactional(readOnly = true)
    public List<LedgerRowResponse> ledger(Long projectId, Long personId, LocalDate from, LocalDate to) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required.");
        if (personId == null) throw new IllegalArgumentException("personId is required.");

        // ensure project & person exist (FKs exist in DB) :contentReference[oaicite:2]{index=2}
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        List<Transaction> list = transactionRepository.ledger(projectId, personId, from, to);

        BigDecimal running = BigDecimal.ZERO;
        List<LedgerRowResponse> out = new ArrayList<>();

        for (Transaction tr : list) {
            BigDecimal delta = BigDecimal.ZERO;

            if (tr.getToPerson().getId().equals(personId)) {
                delta = delta.add(tr.getAmountPaid()); // money received
            }
            if (tr.getFromPerson().getId().equals(personId)) {
                delta = delta.subtract(tr.getAmountPaid()); // money paid
            }

            running = running.add(delta);

            out.add(new LedgerRowResponse(
                    tr.getId(),
                    tr.getDateRegistered(),
                    tr.getCode(),
                    tr.getFromPerson().getId(),
                    tr.getToPerson().getId(),
                    tr.getAmountPaid(),
                    delta,
                    running,
                    tr.getDsc()
            ));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PersonBalanceResponse personBalance(Long projectId, Long personId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required.");
        if (personId == null) throw new IllegalArgumentException("personId is required.");

        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        BigDecimal in = transactionRepository.sumIn(projectId, personId);
        BigDecimal out = transactionRepository.sumOut(projectId, personId);
        BigDecimal net = in.subtract(out);

        return new PersonBalanceResponse(projectId, personId, in, out, net);
    }

    @Transactional(readOnly = true)
    public PairBalanceResponse pairBalance(Long projectId, Long fromId, Long toId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required.");
        if (fromId == null || toId == null) throw new IllegalArgumentException("fromPersonId/toPersonId are required.");
        if (fromId.equals(toId)) throw new IllegalArgumentException("fromPersonId and toPersonId cannot be the same.");

        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        personRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + fromId));
        personRepository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + toId));

        BigDecimal aToB = transactionRepository.sumFromTo(projectId, fromId, toId);
        BigDecimal bToA = transactionRepository.sumFromTo(projectId, toId, fromId);
        BigDecimal netFromToTo = aToB.subtract(bToA);

        return new PairBalanceResponse(projectId, fromId, toId, aToB, bToA, netFromToTo);
    }

    // ---------------- Helpers ----------------

    private void validateCreateUpdate(Long projectId, Long fromPersonId, Long toPersonId,
                                      String code, BigDecimal amountPaid, String paymentType, String transactionType,
                                      LocalDate dateDue, LocalDate dateRegistered, Long updatingId) {

        if (projectId == null) throw new IllegalArgumentException("projectId is required.");
        if (fromPersonId == null) throw new IllegalArgumentException("fromPersonId is required.");
        if (toPersonId == null) throw new IllegalArgumentException("toPersonId is required.");
        if (fromPersonId.equals(toPersonId)) throw new IllegalArgumentException("fromPersonId and toPersonId cannot be the same.");
        if (code == null || code.trim().isEmpty()) throw new IllegalArgumentException("code is required.");
        if (code.trim().length() > 50) throw new IllegalArgumentException("code max length is 50.");
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amountPaid must be > 0.");
        if (paymentType == null || paymentType.trim().length() != 3) throw new IllegalArgumentException("paymentType must be 3 chars.");
        if (transactionType == null || transactionType.trim().length() != 3) throw new IllegalArgumentException("transactionType must be 3 chars.");
        if (dateDue == null) throw new IllegalArgumentException("dateDue is required.");
        if (dateRegistered == null) throw new IllegalArgumentException("dateRegistered is required.");

        // existence checks (FKs in DB) :contentReference[oaicite:3]{index=3}
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        personRepository.findById(fromPersonId)
                .orElseThrow(() -> new IllegalArgumentException("From person not found: " + fromPersonId));
        personRepository.findById(toPersonId)
                .orElseThrow(() -> new IllegalArgumentException("To person not found: " + toPersonId));

        // optional uniqueness check for code (DB does NOT show a unique constraint for transactions.code)
        if (updatingId == null) {
            if (transactionRepository.existsByCodeIgnoreCase(code.trim())) {
                throw new IllegalArgumentException("Transaction code already exists.");
            }
        } else {
            if (transactionRepository.existsByCodeIgnoreCaseAndIdNot(code.trim(), updatingId)) {
                throw new IllegalArgumentException("Transaction code already exists.");
            }
        }
    }

    private void apply(Transaction t, Long projectId, Long fromPersonId, Long toPersonId,
                       String code, LocalDate dateDue, BigDecimal amountPaid,
                       String paymentType, String transactionType, LocalDate dateRegistered, String dsc) {

        Project project = projectRepository.findById(projectId).orElseThrow();
        Person from = personRepository.findById(fromPersonId).orElseThrow();
        Person to = personRepository.findById(toPersonId).orElseThrow();

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

    private TransactionResponse toResponse(Transaction t) {
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
                t.getDsc()
        );
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isTransactionReferenced(Long transactionId) {
        // transaction_tracks(transaction_id FK) + transaction_documents(transaction_id FK) :contentReference[oaicite:4]{index=4} :contentReference[oaicite:5]{index=5}
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
}
