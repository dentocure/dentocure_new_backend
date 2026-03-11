package com.dentocure.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    private String id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    /** Appointment type label (e.g. "Checkup", "Root Canal") */
    private String type;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    /** Duration in minutes — must be a multiple of 15 */
    @Column(nullable = false)
    private int duration;

    /**
     * Status: Scheduled | In-Chair | Completed | Cancelled | No-show
     * Valid transitions: Scheduled → In-Chair → Completed
     *                    Scheduled → No-show
     *                    Scheduled → Cancelled
     */
    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private boolean emergency = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = "Scheduled";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean emergency) { this.emergency = emergency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
