package com.app.controller;

import com.app.dto.document.DocumentMetaResponse;
import com.app.model.DebtDocument;
import com.app.model.TransactionDocument;
import com.app.service.DocumentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // ---------------- TRANSACTION DOCS ----------------

    @PostMapping(value = "/transactions/{transactionId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadTransactionDoc(@PathVariable Long transactionId,
                                                  @RequestPart("file") MultipartFile file,
                                                  @RequestPart(value = "dsc", required = false) String dsc) throws IOException {

        Long docId = documentService.uploadTransactionDoc(transactionId, file.getBytes(), dscOrMeta(dsc, file));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                java.util.Map.of("documentId", docId)
        );
    }

    @GetMapping("/transactions/{transactionId}/documents")
    public ResponseEntity<List<DocumentMetaResponse>> listTransactionDocs(@PathVariable Long transactionId) {
        return ResponseEntity.ok(documentService.listTransactionDocs(transactionId));
    }

    @GetMapping("/transaction-documents/{docId}/download")
    public ResponseEntity<byte[]> downloadTransactionDoc(@PathVariable Long docId) {
        TransactionDocument doc = documentService.getTransactionDoc(docId);

        // We don't store content-type/filename in DB. Use generic download.
        String filename = "transaction_document_" + docId;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(doc.getDoc());
    }

    @DeleteMapping("/transactions/{transactionId}/documents/{docId}")
    public ResponseEntity<Void> deleteTransactionDoc(@PathVariable Long transactionId, @PathVariable Long docId) {
        documentService.deleteTransactionDoc(transactionId, docId);
        return ResponseEntity.noContent().build();
    }

    // ---------------- DEBT DOCS ----------------

    @PostMapping(value = "/debts/{debtId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDebtDoc(@PathVariable Long debtId,
                                           @RequestPart("file") MultipartFile file,
                                           @RequestPart(value = "dsc", required = false) String dsc) throws IOException {

        Long docId = documentService.uploadDebtDoc(debtId, file.getBytes(), dscOrMeta(dsc, file));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                java.util.Map.of("documentId", docId)
        );
    }

    @GetMapping("/debts/{debtId}/documents")
    public ResponseEntity<List<DocumentMetaResponse>> listDebtDocs(@PathVariable Long debtId) {
        return ResponseEntity.ok(documentService.listDebtDocs(debtId));
    }

    @GetMapping("/debt-documents/{docId}/download")
    public ResponseEntity<byte[]> downloadDebtDoc(@PathVariable Long docId) {
        DebtDocument doc = documentService.getDebtDoc(docId);

        String filename = "debt_document_" + docId;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(doc.getDoc());
    }

    @DeleteMapping("/debts/{debtId}/documents/{docId}")
    public ResponseEntity<Void> deleteDebtDoc(@PathVariable Long debtId, @PathVariable Long docId) {
        documentService.deleteDebtDoc(debtId, docId);
        return ResponseEntity.noContent().build();
    }

    // If user doesn't pass dsc, store some minimal metadata there.
    private String dscOrMeta(String dsc, MultipartFile file) {
        if (dsc != null && !dsc.trim().isEmpty()) return dsc.trim();
        return "filename=" + safe(file.getOriginalFilename()) + "; contentType=" + safe(file.getContentType());
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ").trim();
    }
}
