package com.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "debts_detail",
        uniqueConstraints = {
                @UniqueConstraint(name = "IX_debts_detail", columnNames = {"debts_header_id", "item_id"})
        }
)
public class DebtDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> debts_header(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debts_header_id", nullable = false)
    private DebtHeader debtHeader;

    // FK -> items(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // decimal(18,3)
    @Column(name = "qnt", nullable = false, precision = 18, scale = 3)
    private BigDecimal qnt;

    // FK -> units(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    // decimal(18,0)
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "dsc", length = 4000)
    private String dsc;

    public DebtDetail() {}

    public Long getId() { return id; }

    public DebtHeader getDebtHeader() { return debtHeader; }
    public void setDebtHeader(DebtHeader debtHeader) { this.debtHeader = debtHeader; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public BigDecimal getQnt() { return qnt; }
    public void setQnt(BigDecimal qnt) { this.qnt = qnt; }

    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
