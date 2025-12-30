package com.app.repository;

import com.app.model.DebtDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtDetailRepository extends JpaRepository<DebtDetail, Long> {

    List<DebtDetail> findByDebtHeader_IdOrderByIdAsc(Long debtHeaderId);

    void deleteByDebtHeader_Id(Long debtHeaderId);

    boolean existsByDebtHeader_Id(Long debtHeaderId);
}
