package com.dentocure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Request body for creating or updating an appointment")
public class AppointmentRequest {

    @Schema(description = "ID of the patient being seen", example = "PT001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @Schema(description = "ID of the treating doctor", example = "DR002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @Schema(description = "Appointment type label", example = "Root Canal")
    private String type;

    @Schema(description = "Appointment date (YYYY-MM-DD)", example = "2026-03-13", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Date is required")
    private LocalDate date;

    @Schema(description = "Start time (HH:mm)", example = "10:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Time is required")
    private LocalTime time;

    @Schema(description = "Duration in minutes — must be a multiple of 15", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be a positive number")
    private Integer duration;

    @Schema(description = "Initial status — defaults to Scheduled", example = "Scheduled",
        allowableValues = {"Scheduled", "In-Chair", "Completed", "Cancelled", "No-show"},
        defaultValue = "Scheduled")
    private String status;

    @Schema(description = "Clinical notes for this appointment", example = "Second session — review X-ray first")
    private String notes;

    @Schema(description = "Mark as emergency / walk-in priority", example = "false", defaultValue = "false")
    private boolean emergency = false;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean emergency) { this.emergency = emergency; }
}
