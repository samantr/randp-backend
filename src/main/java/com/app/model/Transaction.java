package com.app.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> projects(id) NOT NULL
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // FK -> persons(id) NOT NULL
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_person_id", nullable = false)
    private Person fromPerson;

    // FK -> persons(id) NOT NULL
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_person_id", nullable = false)
    private Person toPerson;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "date_due", nullable = false)
    private LocalDate dateDue;

    @Column(name = "amount_paid", nullable = false, precision = 18, scale = 0)
    private BigDecimal amountPaid;

    // char(3)
    @Column(name = "payment_type", nullable = false, length = 3)
    private String paymentType;

    // char(3)
    @Column(name = "transaction_type", nullable = false, length = 3)
    private String transactionType;

    // CHANGED: date -> datetime
    @Column(name = "date_registered", nullable = false)
    private LocalDateTime dateRegistered;

    @Column(name = "dsc", length = 4000)
    private String dsc;

    public Transaction() {}

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Person getFromPerson() { return fromPerson; }
    public void setFromPerson(Person fromPerson) { this.fromPerson = fromPerson; }

    public Person getToPerson() { return toPerson; }
    public void setToPerson(Person toPerson) { this.toPerson = toPerson; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDate getDateDue() { return dateDue; }
    public void setDateDue(LocalDate dateDue) { this.dateDue = dateDue; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public LocalDateTime getDateRegistered() { return dateRegistered; }
    public void setDateRegistered(LocalDateTime dateRegistered) { this.dateRegistered = dateRegistered; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
