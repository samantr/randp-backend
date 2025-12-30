package com.app.repository;

import com.app.model.DebtDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtDocumentRepository extends JpaRepository<DebtDocument, Long> {
    List<DebtDocument> findByDebtHeader_IdOrderByIdDesc(Long debtId);
}
