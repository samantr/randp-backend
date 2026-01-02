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
    public Long uploadTransactionDoc(Long transactionId, byte[] bytes, String fileName, String contentType, String dsc) {
        if (transactionId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("فایل خالی است.");

        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("پرداخت مورد نظر یافت نشد. (شناسه: " + transactionId + ")"));

        TransactionDocument doc = new TransactionDocument();
        doc.setTransaction(tx);
        doc.setDoc(bytes);
        doc.setFileName(trimToNull(fileName));
        doc.setContentType(trimToNull(contentType));
        doc.setDsc(trimToNull(dsc));

        return transactionDocumentRepository.save(doc).getId();
    }

    @Transactional(readOnly = true)
    public List<DocumentMetaResponse> listTransactionDocs(Long transactionId) {
        if (transactionId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");

        return jdbcTemplate.query("""
                select id,
                       transaction_id as owner_id,
                       datalength(doc) as size_bytes,
                       file_name,
                       content_type,
                       convert(varchar(19), created_at, 120) as created_at,
                       dsc
                from transaction_documents
                where transaction_id = ?
                order by id desc
                """, (rs, rowNum) -> new DocumentMetaResponse(
                rs.getLong("id"),
                rs.getLong("owner_id"),
                rs.getLong("size_bytes"),
                rs.getString("file_name"),
                rs.getString("content_type"),
                rs.getString("created_at"),
                rs.getString("dsc")
        ), transactionId);
    }

    @Transactional(readOnly = true)
    public TransactionDocument getTransactionDoc(Long docId) {
        if (docId == null) throw new IllegalArgumentException("شناسه سند الزامی است.");

        return transactionDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("سند پرداخت یافت نشد. (شناسه: " + docId + ")"));
    }

    @Transactional
    public void deleteTransactionDoc(Long transactionId, Long docId) {
        if (transactionId == null) throw new IllegalArgumentException("شناسه پرداخت الزامی است.");
        if (docId == null) throw new IllegalArgumentException("شناسه سند الزامی است.");

        TransactionDocument doc = getTransactionDoc(docId);

        if (!doc.getTransaction().getId().equals(transactionId)) {
            throw new IllegalArgumentException("این سند متعلق به این پرداخت نیست.");
        }

        transactionDocumentRepository.delete(doc);
    }

    // ------------------- DEBT DOCS -------------------

    @Transactional
    public Long uploadDebtDoc(Long debtId, byte[] bytes, String fileName, String contentType, String dsc) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("فایل خالی است.");

        DebtHeader debt = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + debtId + ")"));

        DebtDocument doc = new DebtDocument();
        doc.setDebtHeader(debt);
        doc.setDoc(bytes);
        doc.setFileName(trimToNull(fileName));
        doc.setContentType(trimToNull(contentType));
        doc.setDsc(trimToNull(dsc));

        return debtDocumentRepository.save(doc).getId();
    }

    @Transactional(readOnly = true)
    public List<DocumentMetaResponse> listDebtDocs(Long debtId) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");

        return jdbcTemplate.query("""
                select id,
                       debt_header_id as owner_id,
                       datalength(doc) as size_bytes,
                       file_name,
                       content_type,
                       convert(varchar(19), created_at, 120) as created_at,
                       dsc
                from debts_documents
                where debt_header_id = ?
                order by id desc
                """, (rs, rowNum) -> new DocumentMetaResponse(
                rs.getLong("id"),
                rs.getLong("owner_id"),
                rs.getLong("size_bytes"),
                rs.getString("file_name"),
                rs.getString("content_type"),
                rs.getString("created_at"),
                rs.getString("dsc")
        ), debtId);
    }

    @Transactional(readOnly = true)
    public DebtDocument getDebtDoc(Long docId) {
        if (docId == null) throw new IllegalArgumentException("شناسه سند الزامی است.");

        return debtDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("سند بدهی یافت نشد. (شناسه: " + docId + ")"));
    }

    @Transactional
    public void deleteDebtDoc(Long debtId, Long docId) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        if (docId == null) throw new IllegalArgumentException("شناسه سند الزامی است.");

        DebtDocument doc = getDebtDoc(docId);

        if (!doc.getDebtHeader().getId().equals(debtId)) {
            throw new IllegalArgumentException("این سند متعلق به این بدهی نیست.");
        }

        debtDocumentRepository.delete(doc);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
