package com.dentocure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Request body for registering or updating a patient")
public class PatientRequest {

    @Schema(description = "Full name of the patient", example = "Anil Kapoor", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "10-digit mobile number — must be unique across active patients", example = "9812345678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Phone number is required")
    private String phone;

    @Schema(description = "Email address", example = "anil.kapoor@example.com")
    private String email;

    @Schema(description = "Date of birth (YYYY-MM-DD). Age is computed server-side.", example = "1980-05-10")
    private LocalDate dob;

    @Schema(description = "Biological sex", example = "Male", allowableValues = {"Male", "Female", "Other"})
    private String gender;

    @Schema(description = "ABO blood group", example = "O+",
        allowableValues = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"})
    private String bloodGroup;

    @Schema(description = "Known allergies (drug names, latex, etc.)", example = "[\"Penicillin\", \"Aspirin\"]")
    private List<String> allergies;

    @Schema(description = "Emergency contact phone number", example = "9812345679")
    private String emergContact;

    @Schema(description = "How the patient was referred", example = "Walk-in")
    private String referredBy;

    @Schema(description = "Clinical notes or special instructions", example = "Diabetic — handle anaesthesia carefully")
    private String notes;

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }

    public String getEmergContact() { return emergContact; }
    public void setEmergContact(String emergContact) { this.emergContact = emergContact; }

    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
