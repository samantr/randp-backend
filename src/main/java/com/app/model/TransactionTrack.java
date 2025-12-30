package com.app.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "transaction_tracks",
        uniqueConstraints = {
                @UniqueConstraint(name = "IX_transaction_tracks", columnNames = {"transaction_id", "debt_header_id"})
        }
)
public class TransactionTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debt_header_id", nullable = false)
    private DebtHeader debtHeader;

    @Column(name = "covered_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal coveredAmount;

    @Column(name = "dsc", length = 5000)
    private String dsc;

    public TransactionTrack() {}

    public Long getId() { return id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public DebtHeader getDebtHeader() { return debtHeader; }
    public void setDebtHeader(DebtHeader debtHeader) { this.debtHeader = debtHeader; }

    public BigDecimal getCoveredAmount() { return coveredAmount; }
    public void setCoveredAmount(BigDecimal coveredAmount) { this.coveredAmount = coveredAmount; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
