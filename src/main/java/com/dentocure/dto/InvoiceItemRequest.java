package com.dentocure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "A single line item on an invoice")
public class InvoiceItemRequest {

    @Schema(description = "Treatment or service name", example = "Root Canal Treatment", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Item name is required")
    private String name;

    @Schema(description = "Quantity", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "Quantity must be at least 1")
    private int qty;

    @Schema(description = "Unit price", example = "3500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be zero or positive")
    private BigDecimal unitPrice;

    @Schema(description = "Per-item discount amount (not percentage)", example = "200.00", defaultValue = "0")
    @PositiveOrZero(message = "Discount must be zero or positive")
    private BigDecimal discount = BigDecimal.ZERO;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
}
