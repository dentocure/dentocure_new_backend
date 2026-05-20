package com.dentocure.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    private String id;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(nullable = false)
    private LocalDate date;

    /**
     * Line items stored as a JSON array of InvoiceItem.
     * Serialised/deserialised by InvoiceService via Jackson.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String items;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    /** GST rate snapshot at the time of invoice creation (e.g. 18.00) */
    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", precision = 10, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "grand_total", precision = 10, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 10, scale = 2)
    private BigDecimal balanceDue;

    /**
     * Status: Draft | Unpaid | Partially Paid | Paid | Void
     * Draft    — created but not yet finalised; can still be edited
     * Unpaid   — finalised, no payments received
     * Partially Paid — one or more payments, but balance > 0
     * Paid     — balance_due == 0
     * Void     — cancelled / soft-deleted
     */
    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = "Draft";
        }
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
