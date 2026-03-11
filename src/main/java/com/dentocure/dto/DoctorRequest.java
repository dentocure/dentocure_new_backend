package com.dentocure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for creating or updating a doctor profile")
public class DoctorRequest {

    @Schema(description = "Full name of the doctor", example = "Dr. Arjun Bose", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "Medical specialization", example = "Orthodontist")
    private String specialization;

    @Schema(description = "10-digit mobile number", example = "9876500001")
    private String phone;

    @Schema(description = "Professional email address", example = "arjun.bose@dentocure.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Hex color for calendar / timeline display", example = "#6366F1")
    private String color;

    @Schema(description = "Working hours as JSON string", example = "{\"start\":\"09:00\",\"end\":\"17:00\"}")
    private String workingHours;

    @Schema(description = "Whether the doctor is currently active", example = "true", defaultValue = "true")
    private boolean active = true;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
