package com.dentocure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Request body for creating or updating an invoice")
public class InvoiceRequest {

    @Schema(description = "ID of the patient being billed", example = "PT001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @Schema(description = "ID of the linked appointment (optional)", example = "AP001")
    private String appointmentId;

    @Schema(description = "Invoice date (YYYY-MM-DD)", example = "2026-05-21", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Invoice date is required")
    private LocalDate date;

    @Schema(description = "Line items — at least one required", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<InvoiceItemRequest> items;

    @Schema(description = "Optional notes for the invoice", example = "Post-treatment follow-up included")
    private String notes;

    @Schema(description = "Invoice status — defaults to Draft",
            allowableValues = {"Draft", "Unpaid"},
            defaultValue = "Draft")
    private String status;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<InvoiceItemRequest> getItems() { return items; }
    public void setItems(List<InvoiceItemRequest> items) { this.items = items; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
