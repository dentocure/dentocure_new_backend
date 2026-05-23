package com.dentocure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request body for recording a payment against an invoice")
public class PaymentRequest {

    @Schema(description = "Amount paid", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Schema(description = "Payment method",
            example = "UPI",
            allowableValues = {"Cash", "UPI", "Card", "Net Banking", "Insurance"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @Schema(description = "Optional notes", example = "Paid via Google Pay — ref #XYZ123")
    private String notes;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
