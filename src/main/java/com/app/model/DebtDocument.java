package com.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "debts_documents")
public class DebtDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> debts_header(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debt_header_id", nullable = false)
    private DebtHeader debtHeader;

    @Lob
    @Column(name = "doc", nullable = false)
    private byte[] doc;

    @Column(name = "dsc", length = 4000)
    private String dsc;

    public DebtDocument() {}

    public Long getId() { return id; }

    public DebtHeader getDebtHeader() { return debtHeader; }
    public void setDebtHeader(DebtHeader debtHeader) { this.debtHeader = debtHeader; }

    public byte[] getDoc() { return doc; }
    public void setDoc(byte[] doc) { this.doc = doc; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
