package com.app.repository;

import com.app.model.Transaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Transaction> findByCode(String code);
    @Query("""
        select t from Transaction t
        where t.project.id = :projectId
          and (:personId is null or t.fromPerson.id = :personId or t.toPerson.id = :personId)
          and (:fromDate is null or t.dateRegistered >= :fromDate)
          and (:toDate is null or t.dateRegistered <= :toDate)
        order by t.dateRegistered asc, t.id asc
        """)
    List<Transaction> ledger(
            @Param("projectId") Long projectId,
            @Param("personId") Long personId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
        select coalesce(sum(t.amountPaid),0) from Transaction t
        where t.project.id = :projectId
          and t.toPerson.id = :personId
        """)
    BigDecimal sumIn(@Param("projectId") Long projectId, @Param("personId") Long personId);

    @Query("""
        select coalesce(sum(t.amountPaid),0) from Transaction t
        where t.project.id = :projectId
          and t.fromPerson.id = :personId
        """)
    BigDecimal sumOut(@Param("projectId") Long projectId, @Param("personId") Long personId);

    @Query("""
        select coalesce(sum(t.amountPaid),0) from Transaction t
        where t.project.id = :projectId
          and t.fromPerson.id = :fromId
          and t.toPerson.id = :toId
        """)
    BigDecimal sumFromTo(@Param("projectId") Long projectId,
                         @Param("fromId") Long fromId,
                         @Param("toId") Long toId);
}


