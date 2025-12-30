package com.app.service;

import com.app.dto.document.DocumentMetaResponse;
import com.app.model.*;
import com.app.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentService {

    private final TransactionRepository transactionRepository;
    private final DebtHeaderRepository debtHeaderRepository;

    private final TransactionDocumentRepository transactionDocumentRepository;
    private final DebtDocumentRepository debtDocumentRepository;

    private final JdbcTemplate jdbcTemplate;

    public DocumentService(TransactionRepository transactionRepository,
                           DebtHeaderRepository debtHeaderRepository,
                           TransactionDocumentRepository transactionDocumentRepository,
                           DebtDocumentRepository debtDocumentRepository,
                           JdbcTemplate jdbcTemplate) {
        this.transactionRepository = transactionRepository;
        this.debtHeaderRepository = debtHeaderRepository;
        this.transactionDocumentRepository = transactionDocumentRepository;
        this.debtDocumentRepository = debtDocumentRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------- TRANSACTION DOCS -------------------

    @Transactional
    public Long uploadTransactionDoc(Long transactionId, byte[] bytes, String dsc) {
        if (transactionId == null) throw new IllegalArgumentException("transactionId is required.");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("file is empty.");

        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        TransactionDocument doc = new TransactionDocument();
        doc.setTransaction(tx);
        doc.setDoc(bytes);
        doc.setDsc(trimToNull(dsc));

        return transactionDocumentRepository.save(doc).getId();
    }

    @Transactional(readOnly = true)
    public List<DocumentMetaResponse> listTransactionDocs(Long transactionId) {
        if (transactionId == null) throw new IllegalArgumentException("transactionId is required.");
        // metadata query (no blob)
        return jdbcTemplate.query("""
                select id,
                       transaction_id as owner_id,
                       datalength(doc) as size_bytes,
                       dsc
                from transaction_documents
                where transaction_id = ?
                order by id desc
                """, (rs, rowNum) -> new DocumentMetaResponse(
                rs.getLong("id"),
                rs.getLong("owner_id"),
                rs.getLong("size_bytes"),
                rs.getString("dsc")
        ), transactionId);
    }

    @Transactional(readOnly = true)
    public TransactionDocument getTransactionDoc(Long docId) {
        return transactionDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction document not found: " + docId));
    }

    @Transactional
    public void deleteTransactionDoc(Long transactionId, Long docId) {
        TransactionDocument doc = getTransactionDoc(docId);
        if (!doc.getTransaction().getId().equals(transactionId)) {
            throw new IllegalArgumentException("Document does not belong to this transaction.");
        }
        transactionDocumentRepository.delete(doc);
    }

    // ------------------- DEBT DOCS -------------------

    @Transactional
    public Long uploadDebtDoc(Long debtId, byte[] bytes, String dsc) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("file is empty.");

        DebtHeader debt = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + debtId));

        DebtDocument doc = new DebtDocument();
        doc.setDebtHeader(debt);
        doc.setDoc(bytes);
        doc.setDsc(trimToNull(dsc));

        return debtDocumentRepository.save(doc).getId();
    }

    @Transactional(readOnly = true)
    public List<DocumentMetaResponse> listDebtDocs(Long debtId) {
        if (debtId == null) throw new IllegalArgumentException("debtId is required.");

        return jdbcTemplate.query("""
                select id,
                       debt_header_id as owner_id,
                       datalength(doc) as size_bytes,
                       dsc
                from debts_documents
                where debt_header_id = ?
                order by id desc
                """, (rs, rowNum) -> new DocumentMetaResponse(
                rs.getLong("id"),
                rs.getLong("owner_id"),
                rs.getLong("size_bytes"),
                rs.getString("dsc")
        ), debtId);
    }

    @Transactional(readOnly = true)
    public DebtDocument getDebtDoc(Long docId) {
        return debtDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Debt document not found: " + docId));
    }

    @Transactional
    public void deleteDebtDoc(Long debtId, Long docId) {
        DebtDocument doc = getDebtDoc(docId);
        if (!doc.getDebtHeader().getId().equals(debtId)) {
            throw new IllegalArgumentException("Document does not belong to this debt.");
        }
        debtDocumentRepository.delete(doc);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
