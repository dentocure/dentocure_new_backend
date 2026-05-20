# Running the DentaFlow Backend

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Java | 17+ | `java -version` |
| Maven | 3.6+ | `mvn -version` |

---

## Quick Start

```bash
# Clone / navigate to project
cd dentocure_new_backend

# Install dependencies & start server
mvn spring-boot:run
```

Server starts on **http://localhost:8080**

Sample data is loaded automatically on startup:
- 5 doctors
- 10 patients
- 15 appointments (10 for today)
- 8 invoices (Paid, Partially Paid, Unpaid, Draft, Void)
- 3 payment records

---

## Build & Run (Alternative)

```bash
# Build a runnable JAR
mvn clean package -DskipTests

# Run the JAR
java -jar target/dentocure-backend-0.0.1-SNAPSHOT.jar
```

---

## H2 Database Console

Browse the in-memory database at **http://localhost:8080/h2-console**

| Field | Value |
|-------|-------|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:dentocuredb` |
| Username | `sa` |
| Password | *(leave blank)* |

> **Note:** Data resets on every restart since the database is in-memory.

---

## API Documentation (Swagger UI)

Interactive API docs are available at **http://localhost:8080/swagger-ui.html**

---

## API Base URL

```
http://localhost:8080/api
```

---

## Endpoint Reference

### Doctors — `/api/doctors`

```
GET    /api/doctors                        List all doctors
GET    /api/doctors/{id}                   Get a single doctor
POST   /api/doctors                        Create a new doctor
PUT    /api/doctors/{id}                   Update doctor details
PATCH  /api/doctors/{id}/status            Toggle active/inactive
DELETE /api/doctors/{id}                   Soft-delete (sets active=false)
GET    /api/doctors/{id}/schedule          Appointments for a date
GET    /api/doctors/{id}/availability      Working hours info
POST   /api/doctors/{id}/leave             Mark leave dates
```

### Patients — `/api/patients`

```
GET    /api/patients                       List patients (paginated + searchable)
GET    /api/patients/{id}                  Get a single patient
POST   /api/patients                       Create a new patient
PUT    /api/patients/{id}                  Update patient details
DELETE /api/patients/{id}                  Soft-delete (sets active=false)
GET    /api/patients/{id}/appointments     All appointments for a patient
GET    /api/patients/{id}/invoices         All invoices for a patient
GET    /api/patients/{id}/reminders        Reminders (stub — returns empty list)
```

### Appointments — `/api/appointments`

```
GET    /api/appointments                   List appointments (filterable + paginated)
GET    /api/appointments/today             All appointments for today
GET    /api/appointments/slots             Available time slots for a doctor
GET    /api/appointments/{id}              Get a single appointment
POST   /api/appointments                   Create an appointment (conflict detection)
PUT    /api/appointments/{id}              Update appointment
PATCH  /api/appointments/{id}/status       Update status only
DELETE /api/appointments/{id}              Cancel appointment
```

### Billing & Invoices — `/api/invoices`

```
GET    /api/invoices                       List invoices (filterable + paginated)
GET    /api/invoices/{id}                  Get a single invoice with line items
POST   /api/invoices                       Create a new invoice
PUT    /api/invoices/{id}                  Update invoice (Draft status only)
DELETE /api/invoices/{id}                  Void an invoice
POST   /api/invoices/{id}/payments         Record a payment against an invoice
GET    /api/invoices/{id}/payments         List all payments for an invoice
GET    /api/invoices/{id}/pdf              Get PDF info (stub — full generation not yet implemented)
```

---

## Sample Requests

### List all doctors
```bash
curl http://localhost:8080/api/doctors
```

### Get today's appointments
```bash
curl http://localhost:8080/api/appointments/today
```

### Search patients by name or phone
```bash
curl "http://localhost:8080/api/patients?search=ravi&page=1&limit=20"
```

### Filter appointments by doctor and date
```bash
curl "http://localhost:8080/api/appointments?doctorId=DR001&date=2026-03-12"
```

### Check available slots for a doctor
```bash
curl "http://localhost:8080/api/appointments/slots?doctorId=DR001&date=2026-03-12"
```

### Create a new appointment
```bash
curl -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "PT001",
    "doctorId": "DR002",
    "type": "Checkup",
    "date": "2026-03-13",
    "time": "10:00",
    "duration": 30,
    "emergency": false
  }'
```

### Update appointment status
```bash
curl -X PATCH http://localhost:8080/api/appointments/AP001/status \
  -H "Content-Type: application/json" \
  -d '{"status": "In-Chair"}'
```

### Create a new patient
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Anil Kapoor",
    "phone": "9812345678",
    "dob": "1980-05-10",
    "gender": "Male",
    "bloodGroup": "O+",
    "allergies": ["Penicillin"]
  }'
```

### Toggle doctor active status
```bash
curl -X PATCH http://localhost:8080/api/doctors/DR005/status \
  -H "Content-Type: application/json" \
  -d '{"status": "active"}'
```

### List invoices filtered by status
```bash
curl "http://localhost:8080/api/invoices?status=Unpaid&page=1&limit=20"
```

### Create a new invoice
```bash
curl -X POST http://localhost:8080/api/invoices \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "PT001",
    "appointmentId": "AP001",
    "date": "2026-05-21",
    "items": [
      { "name": "Root Canal Treatment", "qty": 1, "unitPrice": 3500.00, "discount": 200.00 },
      { "name": "X-Ray", "qty": 2, "unitPrice": 300.00 }
    ],
    "notes": "Post-treatment care included",
    "status": "Draft"
  }'
```

### Record a payment against an invoice
```bash
curl -X POST http://localhost:8080/api/invoices/INV001/payments \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1500.00,
    "paymentMethod": "UPI",
    "notes": "Google Pay — ref #XYZ123"
  }'
```

### Get all payments for an invoice
```bash
curl http://localhost:8080/api/invoices/INV001/payments
```

### Get all invoices for a patient
```bash
curl http://localhost:8080/api/patients/PT001/invoices
```

---

## Query Parameters

### `GET /api/patients`
| Param | Type | Description |
|-------|------|-------------|
| `search` | string | Name or phone substring |
| `page` | int | Page number (default: 1) |
| `limit` | int | Page size (default: 20, max: 100) |
| `active` | boolean | Filter by active/inactive |

### `GET /api/appointments`
| Param | Type | Description |
|-------|------|-------------|
| `date` | YYYY-MM-DD | Filter by date |
| `doctorId` | string | Filter by doctor |
| `patientId` | string | Filter by patient |
| `status` | string | `Scheduled` / `In-Chair` / `Completed` / `Cancelled` / `No-show` |
| `type` | string | Appointment type label |
| `page` | int | Page number (default: 1) |
| `limit` | int | Page size (default: 20, max: 100) |

### `GET /api/doctors/{id}/schedule`
| Param | Type | Description |
|-------|------|-------------|
| `date` | YYYY-MM-DD | Date to fetch schedule for (default: today) |

### `GET /api/invoices`
| Param | Type | Description |
|-------|------|-------------|
| `status` | string | `Draft` / `Unpaid` / `Partially Paid` / `Paid` / `Void` |
| `patientId` | string | Filter by patient |
| `dateFrom` | YYYY-MM-DD | Start of date range |
| `dateTo` | YYYY-MM-DD | End of date range |
| `search` | string | Invoice number substring (e.g. `INV-001`) |
| `page` | int | Page number (default: 1) |
| `limit` | int | Page size (default: 20, max: 100) |

---

## Billing Calculations

All financial fields are computed server-side — never trusted from the client:

```
lineTotal   = qty × unitPrice − discount
subtotal    = Σ lineTotals
gstAmount   = subtotal × gstRate (default 18%)
grandTotal  = subtotal + gstAmount
balanceDue  = grandTotal − paidAmount
```

Invoice status is auto-derived after each payment:

| Condition | Status |
|-----------|--------|
| `balanceDue == 0` | `Paid` |
| `paidAmount > 0` and `balanceDue > 0` | `Partially Paid` |
| `paidAmount == 0` | `Unpaid` |

- Invoices can only be edited while in `Draft` status
- Voided invoices do not accept payments
- Accepted payment methods: `Cash`, `UPI`, `Card`, `Net Banking`, `Insurance`

---

## Response Format

All endpoints return a consistent envelope:

```json
{
  "data": { ... },
  "meta": { "page": 1, "limit": 20, "total": 42, "totalPages": 3 }
}
```

Errors follow this shape:

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Patient not found with id: XYZ"
  }
}
```

### HTTP Status Codes
| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Validation error / bad request |
| 404 | Resource not found |
| 409 | Conflict (duplicate phone, double-booking, already paid) |
| 500 | Internal server error |

---

## Sample Data Reference

### Doctors

| ID | Name | Specialization | Working Hours |
|----|------|---------------|---------------|
| DR001 | Dr. Priya Sharma | Orthodontist | 09:00–17:00 |
| DR002 | Dr. Rahul Mehta | Endodontist | 09:00–17:00 |
| DR003 | Dr. Sunita Patel | Periodontist | 10:00–18:00 |
| DR004 | Dr. Arun Kumar | Oral Surgeon | 08:00–16:00 |
| DR005 | Dr. Neha Joshi | Pedodontist | 09:00–17:00 (inactive) |

### Patients

| ID | Name | Phone | Notable |
|----|------|-------|---------|
| PT001 | Ravi Shankar | 9811111111 | Allergic: Penicillin |
| PT003 | Vikram Singh | 9833333333 | Allergic: Aspirin, Ibuprofen |
| PT005 | Suresh Patel | 9855555555 | Diabetic |
| PT007 | Arjun Reddy | 9877777777 | Allergic: Latex |
| PT009 | Ramesh Yadav | 9899999999 | Hypertensive |

### Invoices

| ID | Invoice No. | Patient | Grand Total | Status |
|----|-------------|---------|-------------|--------|
| INV001 | INV-0001 | PT001 – Ravi Shankar | ₹944.00 | Paid |
| INV002 | INV-0002 | PT002 – Anita Desai | ₹4,602.00 | Partially Paid |
| INV003 | INV-0003 | PT006 – Meena Krishnan | ₹1,416.00 | Paid |
| INV004 | INV-0004 | PT003 – Vikram Singh | ₹1,298.00 | Unpaid |
| INV005 | INV-0005 | PT005 – Suresh Patel | ₹2,124.00 | Unpaid |
| INV006 | INV-0006 | PT004 – Kavita Nair | ₹767.00 | Draft |
| INV007 | INV-0007 | PT009 – Ramesh Yadav | ₹354.00 | Void |
| INV008 | INV-0008 | PT007 – Arjun Reddy | ₹2,714.00 | Unpaid |
