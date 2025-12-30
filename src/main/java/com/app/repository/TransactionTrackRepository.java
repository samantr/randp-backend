package com.app.repository;

import com.app.model.TransactionTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionTrackRepository extends JpaRepository<TransactionTrack, Long> {

    boolean existsByDebtHeader_IdAndTransaction_Id(Long debtHeaderId, Long transactionId);

    List<TransactionTrack> findByDebtHeader_IdOrderByIdDesc(Long debtHeaderId);

    @Query("select coalesce(sum(tt.coveredAmount),0) from TransactionTrack tt where tt.debtHeader.id = :debtId")
    BigDecimal sumCoveredByDebt(@Param("debtId") Long debtId);

    @Query("select coalesce(sum(tt.coveredAmount),0) from TransactionTrack tt where tt.transaction.id = :txId")
    BigDecimal sumCoveredByTransaction(@Param("txId") Long txId);
}
