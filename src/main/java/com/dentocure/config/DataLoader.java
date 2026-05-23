package com.dentocure.config;

import com.dentocure.model.*;
import com.dentocure.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Loads sample data from CSV files into the H2 in-memory database at startup.
 *
 * CSV files are located at: src/main/resources/data/
 *   - doctors.csv
 *   - patients.csv
 *   - appointments.csv
 *
 * Column order is positional — see each load method for the expected schema.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public DataLoader(DoctorRepository doctorRepository,
                      PatientRepository patientRepository,
                      AppointmentRepository appointmentRepository,
                      UserRepository userRepository,
                      InvoiceRepository invoiceRepository,
                      PaymentRepository paymentRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        loadUsers();
        loadDoctors();
        loadPatients();
        loadAppointments();
        loadInvoices();
        loadPayments();
        log.info("Sample data loaded: {} users, {} doctors, {} patients, {} appointments, {} invoices, {} payments",
                userRepository.count(), doctorRepository.count(), patientRepository.count(), appointmentRepository.count(),
                invoiceRepository.count(), paymentRepository.count());
    }

    // ── Create sample users with authentication ───────────────────────────────

    private void loadUsers() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

        // Admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@dentocure.com");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        // Doctor user
        User doctor = new User();
        doctor.setUsername("doctor");
        doctor.setPassword(passwordEncoder.encode("doctor123"));
        doctor.setEmail("doctor@dentocure.com");
        doctor.setRole(Role.DOCTOR);
        doctor.setActive(true);
        userRepository.save(doctor);

        // Receptionist user
        User receptionist = new User();
        receptionist.setUsername("receptionist");
        receptionist.setPassword(passwordEncoder.encode("receptionist123"));
        receptionist.setEmail("receptionist@dentocure.com");
        receptionist.setRole(Role.RECEPTIONIST);
        receptionist.setActive(true);
        userRepository.save(receptionist);
    }

    // ── CSV: id, name, specialization, phone, email, color, active ───────────

    private void loadDoctors() throws Exception {
        List<String[]> rows = readCsv("data/doctors.csv");
        for (String[] row : rows) {
            if (row.length < 7) continue;

            Doctor d = new Doctor();
            d.setId(col(row, 0));
            d.setName(col(row, 1));
            d.setSpecialization(col(row, 2));
            d.setPhone(col(row, 3));
            d.setEmail(col(row, 4));
            d.setColor(col(row, 5));
            d.setActive(Boolean.parseBoolean(col(row, 6)));
            d.setWorkingHours(parseWorkingHours(col(row, 7))); // convert "09:00-17:00" → JSON
            d.setCreatedAt(LocalDateTime.now());
            doctorRepository.save(d);
        }
    }

    // ── CSV: id, name, phone, email, dob, gender, blood_group,
    //         allergies (semicolon-separated), emerg_contact, referred_by, notes, active

    private void loadPatients() throws Exception {
        List<String[]> rows = readCsv("data/patients.csv");
        for (String[] row : rows) {
            if (row.length < 4) continue;

            Patient p = new Patient();
            p.setId(col(row, 0));
            p.setName(col(row, 1));
            p.setPhone(col(row, 2));
            p.setEmail(col(row, 3));

            String dob = col(row, 4);
            if (!dob.isBlank()) p.setDob(LocalDate.parse(dob));

            p.setGender(col(row, 5));
            p.setBloodGroup(col(row, 6));

            String allergies = col(row, 7);
            if (!allergies.isBlank()) {
                p.setAllergies(Arrays.asList(allergies.split(";")));
            }

            p.setEmergContact(col(row, 8));
            p.setReferredBy(col(row, 9));
            p.setNotes(col(row, 10));
            p.setActive(Boolean.parseBoolean(col(row, 11)));
            p.setCreatedAt(LocalDateTime.now());
            patientRepository.save(p);
        }
    }

    // ── CSV: id, patient_id, doctor_id, type, date, time, duration, status, notes, emergency

    private void loadAppointments() throws Exception {
        List<String[]> rows = readCsv("data/appointments.csv");
        for (String[] row : rows) {
            if (row.length < 8) continue;

            Appointment a = new Appointment();
            a.setId(col(row, 0));
            a.setPatientId(col(row, 1));
            a.setDoctorId(col(row, 2));
            a.setType(col(row, 3));
            a.setDate(LocalDate.parse(col(row, 4)));
            a.setTime(LocalTime.parse(col(row, 5)));
            a.setDuration(Integer.parseInt(col(row, 6)));
            a.setStatus(col(row, 7));
            a.setNotes(col(row, 8));
            a.setEmergency(Boolean.parseBoolean(col(row, 9)));
            a.setCreatedAt(LocalDateTime.now());
            appointmentRepository.save(a);
        }
    }

    // ── CSV: id, invoice_number, patient_id, appointment_id, date, items,
    //         subtotal, gst_rate, gst_amount, grand_total, paid_amount, balance_due, status, notes

    private void loadInvoices() throws Exception {
        List<String[]> rows = readCsv("data/invoices.csv");
        for (String[] row : rows) {
            if (row.length < 13) continue;

            Invoice inv = new Invoice();
            inv.setId(col(row, 0));
            inv.setInvoiceNumber(col(row, 1));
            inv.setPatientId(col(row, 2));
            String apptId = col(row, 3);
            inv.setAppointmentId(apptId.isBlank() ? null : apptId);
            inv.setDate(LocalDate.parse(col(row, 4)));
            inv.setItems(col(row, 5));
            inv.setSubtotal(new BigDecimal(col(row, 6)));
            inv.setGstRate(new BigDecimal(col(row, 7)));
            inv.setGstAmount(new BigDecimal(col(row, 8)));
            inv.setGrandTotal(new BigDecimal(col(row, 9)));
            inv.setPaidAmount(new BigDecimal(col(row, 10)));
            inv.setBalanceDue(new BigDecimal(col(row, 11)));
            inv.setStatus(col(row, 12));
            inv.setNotes(col(row, 13));
            inv.setCreatedAt(LocalDateTime.now());
            invoiceRepository.save(inv);
        }
    }

    // ── CSV: id, invoice_id, amount, payment_method, notes

    private void loadPayments() throws Exception {
        List<String[]> rows = readCsv("data/payments.csv");
        for (String[] row : rows) {
            if (row.length < 4) continue;

            Payment p = new Payment();
            p.setId(col(row, 0));
            p.setInvoiceId(col(row, 1));
            p.setAmount(new BigDecimal(col(row, 2)));
            p.setPaymentMethod(col(row, 3));
            p.setNotes(col(row, 4));
            p.setPaidAt(LocalDateTime.now());
            paymentRepository.save(p);
        }
    }

    /** Reads all data rows (skipping the header) from a classpath CSV resource. */
    private List<String[]> readCsv(String resourcePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            List<String[]> all = reader.readAll();
            return all.size() > 1 ? all.subList(1, all.size()) : List.of(); // skip header
        }
    }

    /** Safe column accessor — returns empty string if index is out of bounds. */
    private String col(String[] row, int index) {
        if (index >= row.length) return "";
        return row[index] == null ? "" : row[index].trim();
    }

    /**
     * Converts CSV working_hours format "09:00-17:00" to JSON
     * {"start":"09:00","end":"17:00"} stored in the database.
     */
    private String parseWorkingHours(String raw) {
        if (raw == null || raw.isBlank()) return "{\"start\":\"09:00\",\"end\":\"17:00\"}";
        String[] parts = raw.split("-");
        if (parts.length == 2) {
            return String.format("{\"start\":\"%s\",\"end\":\"%s\"}", parts[0].trim(), parts[1].trim());
        }
        return "{\"start\":\"09:00\",\"end\":\"17:00\"}";
    }
}

