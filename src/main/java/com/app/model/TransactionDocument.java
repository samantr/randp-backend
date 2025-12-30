package com.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction_documents")
public class TransactionDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> transactions(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Lob
    @Column(name = "doc", nullable = false)
    private byte[] doc;

    @Column(name = "dsc", length = 4000)
    private String dsc;

    public TransactionDocument() {}

    public Long getId() { return id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public byte[] getDoc() { return doc; }
    public void setDoc(byte[] doc) { this.doc = doc; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
