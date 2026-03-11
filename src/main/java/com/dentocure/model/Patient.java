package com.dentocure.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String email;
    private LocalDate dob;
    private String gender;

    @Column(name = "blood_group")
    private String bloodGroup;

    /** Stored as pipe-separated text: "Penicillin|Aspirin" */
    @Convert(converter = StringListConverter.class)
    @Column(name = "allergies", columnDefinition = "TEXT")
    private List<String> allergies = new ArrayList<>();

    @Column(name = "emerg_contact")
    private String emergContact;

    @Column(name = "referred_by")
    private String referredBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Age calculated server-side from dob — not persisted */
    @Transient
    public Integer getAge() {
        if (dob == null) return null;
        return Period.between(dob, LocalDate.now()).getYears();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
    public void setAllergies(List<String> allergies) {
        this.allergies = allergies != null ? allergies : new ArrayList<>();
    }

    public String getEmergContact() { return emergContact; }
    public void setEmergContact(String emergContact) { this.emergContact = emergContact; }

    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
