package com.dentocure.model;

import java.math.BigDecimal;

/**
 * Represents one line item on an invoice.
 * Not a JPA entity — stored as a JSON array in the Invoice.items column.
 */
public class InvoiceItem {

    private String name;
    private int qty;
    private BigDecimal unitPrice;
    /** Per-item discount amount (not percentage) */
    private BigDecimal discount = BigDecimal.ZERO;
    /** Server-computed: qty × unitPrice − discount */
    private BigDecimal lineTotal;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
