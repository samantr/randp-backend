package com.app.repository;

import com.app.model.TransactionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionDocumentRepository extends JpaRepository<TransactionDocument, Long> {
    List<TransactionDocument> findByTransaction_IdOrderByIdDesc(Long transactionId);
}
