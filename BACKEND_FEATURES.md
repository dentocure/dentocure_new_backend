# DentaFlow — Backend Features Required

> **Project:** DentaFlow Dental Clinic EMR SaaS
> **Status:** Frontend MVP complete, backend not yet implemented
> **Stack Assumption:** Node.js (Express / Fastify) + PostgreSQL (or Supabase), REST API

---

## Table of Contents

1. [Authentication & Session Management](#1-authentication--session-management)
2. [Patient Management](#2-patient-management)
3. [Appointment Scheduling](#3-appointment-scheduling)
4. [Billing & Invoicing](#4-billing--invoicing)
5. [Reminder & Notification System](#5-reminder--notification-system)
6. [Doctors & Staff Management](#6-doctors--staff-management)
7. [Treatment & Appointment Type Catalog](#7-treatment--appointment-type-catalog)
8. [Clinic Settings & Configuration](#8-clinic-settings--configuration)
9. [Dashboard & Analytics](#9-dashboard--analytics)
10. [Third-Party Integrations](#10-third-party-integrations)
11. [Data Models / Database Schema](#11-data-models--database-schema)
12. [Non-Functional Requirements](#12-non-functional-requirements)

---

## 1. Authentication & Session Management

Currently hardcoded in `src/data/constants.js` with plaintext password. Must be replaced with a proper auth backend.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Authenticate with username + password |
| POST | `/api/auth/logout` | Invalidate session / revoke token |
| GET | `/api/auth/me` | Return current authenticated user profile |
| POST | `/api/auth/refresh` | Refresh JWT access token |
| PUT | `/api/auth/change-password` | Update password for logged-in user |

### Requirements

- **JWT-based auth** — issue short-lived access tokens (15 min) + long-lived refresh tokens (7 days)
- **Password hashing** — bcrypt (min cost factor 12)
- **Role-based access control (RBAC)** — at minimum: `admin`, `doctor`, `receptionist`
- **Multi-user support** — multiple staff accounts per clinic
- **Brute-force protection** — rate-limit login attempts (5 attempts, 15 min lockout)
- **Secure token storage guidance** — HttpOnly cookie or secure localStorage

---

## 2. Patient Management

Currently managed in React state (`patients` array in `App.jsx`). All CRUD is in-memory only.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/patients` | List all patients (paginated, searchable) |
| GET | `/api/patients/:id` | Get single patient by ID |
| POST | `/api/patients` | Create new patient |
| PUT | `/api/patients/:id` | Update patient details |
| DELETE | `/api/patients/:id` | Soft-delete (set `active = false`) |
| GET | `/api/patients/:id/appointments` | Get all appointments for a patient |
| GET | `/api/patients/:id/invoices` | Get all invoices for a patient |
| GET | `/api/patients/:id/reminders` | Get reminder history for a patient |

### Query Parameters (GET /api/patients)

- `search` — search by name or phone number
- `page`, `limit` — pagination (default limit: 20)
- `active` — filter active/inactive patients

### Requirements

- Phone number uniqueness validation
- Allergies stored as a text array / JSON column
- Age auto-calculated server-side from `dob` field
- Medical alerts (allergy warnings) returned with patient profile
- Soft delete only — never hard delete patient records
- Input sanitization for all string fields

---

## 3. Appointment Scheduling

Currently managed in React state (`appointments` array). Status changes, create/edit all update in-memory only.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/appointments` | List appointments (filterable, paginated) |
| GET | `/api/appointments/:id` | Get single appointment |
| POST | `/api/appointments` | Create new appointment |
| PUT | `/api/appointments/:id` | Update appointment details |
| PATCH | `/api/appointments/:id/status` | Update only the status field |
| DELETE | `/api/appointments/:id` | Cancel appointment (soft delete or status = Cancelled) |
| GET | `/api/appointments/today` | Get all appointments for today |
| GET | `/api/appointments/slots` | Get available time slots for a doctor on a date |

### Query Parameters (GET /api/appointments)

- `date` — filter by date (YYYY-MM-DD)
- `doctorId` — filter by doctor
- `status` — filter by status (Scheduled, Completed, Cancelled, No-show, In-Chair)
- `type` — filter by appointment type
- `patientId` — filter by patient
- `page`, `limit` — pagination

### Appointment Statuses

```
Scheduled → In-Chair → Completed
Scheduled → No-show
Scheduled → Cancelled
```

### Requirements

- **Conflict detection** — prevent double-booking a doctor in overlapping time slots
- **Availability checking** — `GET /api/appointments/slots` returns free slots based on working hours and existing bookings
- **Emergency flag** — `emergency: boolean` field supported
- **Duration validation** — duration must be in 15-min increments; slot must not overflow working hours
- **Cascade on cancel** — when appointment is cancelled, optionally trigger reminder cancellation

---

## 4. Billing & Invoicing

Currently managed in React state (`invoices` array). All calculations (GST, subtotals) are done in `src/utils/helpers.js` — these must be replicated and validated server-side.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/invoices` | List all invoices (filterable, paginated) |
| GET | `/api/invoices/:id` | Get single invoice with full line items |
| POST | `/api/invoices` | Create a new invoice |
| PUT | `/api/invoices/:id` | Update an invoice (only if status is Draft) |
| DELETE | `/api/invoices/:id` | Void an invoice (soft delete or status = Void) |
| POST | `/api/invoices/:id/payments` | Record a payment against an invoice |
| GET | `/api/invoices/:id/payments` | List all payment records for an invoice |
| GET | `/api/invoices/:id/pdf` | Generate and return PDF of the invoice |

### Query Parameters (GET /api/invoices)

- `status` — All / Paid / Partially Paid / Unpaid / Draft
- `patientId` — filter by patient
- `dateFrom`, `dateTo` — date range filter
- `search` — search by patient name or invoice number
- `page`, `limit` — pagination

### Calculation Requirements (Server-Side)

```
subtotal    = Σ (item.qty × item.price) − item.discount
gst         = subtotal × 0.18   (18% GST, configurable per clinic)
grand_total = subtotal + gst
balance_due = grand_total − paid_amount
status      = Paid | Partially Paid | Unpaid  (auto-derived from balance_due)
```

- GST rate must be read from clinic settings (not hardcoded)
- Invoice number auto-generated with configurable prefix (e.g., `INV-0001`)
- Prevent editing invoices once status is not `Draft`
- Payment method accepted: Cash, UPI, Card, Net Banking, Insurance
- Partial payment support — multiple payment records per invoice
- `balance_due` recomputed on every payment addition

### PDF Invoice Requirements

- Clinic name, address, GST number in header
- Line items table with qty, unit price, discount, line total
- Subtotal, GST breakdown, Grand Total
- Paid amount and balance due
- Payment method
- Footer text (configurable in settings)

---

## 5. Reminder & Notification System

Currently `handleSend()` in `RemindersPage.jsx` only creates a reminder record in memory. No actual message is sent.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/reminders` | List all reminders (filterable) |
| POST | `/api/reminders/send` | Send a reminder (triggers delivery via chosen channel) |
| GET | `/api/reminders/templates` | Get all reminder templates |
| POST | `/api/reminders/templates` | Create a custom template |
| PUT | `/api/reminders/templates/:id` | Update a template |
| DELETE | `/api/reminders/templates/:id` | Delete a template |
| GET | `/api/reminders/:id/status` | Check delivery status of a sent reminder |

### Query Parameters (GET /api/reminders)

- `patientId` — filter by patient
- `status` — Delivered / Read / Pending / Failed
- `channel` — SMS / WhatsApp / Email
- `page`, `limit` — pagination

### Variable Substitution (Server-Side)

The following template variables must be resolved server-side before sending:

| Variable | Source |
|---|---|
| `{patient_name}` | `patients.name` |
| `{doctor_name}` | `doctors.name` |
| `{appointment_date}` | `appointments.date` |
| `{time}` | `appointments.time` |
| `{clinic_name}` | `clinic_settings.name` |
| `{balance_due}` | Computed from invoice |

### Delivery Channels

- **WhatsApp** — via Twilio WhatsApp API or Interakt
- **SMS** — via Twilio SMS, AWS SNS, or MSG91
- **Email** — via SendGrid, AWS SES, or Resend

### Requirements

- Each sent reminder saved to DB with `delivery_status` (Pending initially)
- Webhook endpoint to receive delivery status callbacks from providers
- Async delivery — send via job queue (BullMQ / Agenda) to avoid blocking requests
- Retry logic for failed sends (max 3 retries with exponential backoff)
- Automated reminders (cron-based):
  - Day-before appointment reminders (run nightly)
  - 2-hour-before appointment reminders (run hourly)
  - Overdue payment reminders (configurable)

---

## 6. Doctors & Staff Management

Currently read-only in `SettingsPage.jsx` — add/edit marked as "coming in v1.1". Data seeded in `constants.js`.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/doctors` | List all doctors |
| GET | `/api/doctors/:id` | Get single doctor |
| POST | `/api/doctors` | Add a new doctor |
| PUT | `/api/doctors/:id` | Update doctor details |
| PATCH | `/api/doctors/:id/status` | Toggle active / inactive |
| DELETE | `/api/doctors/:id` | Soft-delete doctor |
| GET | `/api/doctors/:id/schedule` | Get doctor's appointment schedule |
| GET | `/api/doctors/:id/availability` | Get working hours / leave dates |
| POST | `/api/doctors/:id/leave` | Mark leave dates for a doctor |

### Doctor Schema Fields

- `name`, `specialization`, `phone`, `email`
- `color` — hex color used in timeline view
- `active` — boolean
- `working_hours` — JSON (start/end per day of week)

---

## 7. Treatment & Appointment Type Catalog

Currently hardcoded in `constants.js`. Needs to be editable through the Settings UI.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/treatments` | List all treatments |
| POST | `/api/treatments` | Add a new treatment |
| PUT | `/api/treatments/:id` | Update treatment name / price / category |
| DELETE | `/api/treatments/:id` | Delete a treatment |
| GET | `/api/appointment-types` | List all appointment types |
| POST | `/api/appointment-types` | Add a new type |
| PUT | `/api/appointment-types/:id` | Update type name / color / default duration |
| DELETE | `/api/appointment-types/:id` | Delete an appointment type |

### Treatment Fields

- `name`, `category`, `default_price`, `gst_applicable` (boolean)

### Appointment Type Fields

- `name`, `color` (hex), `default_duration` (minutes)

---

## 8. Clinic Settings & Configuration

Currently each tab in `SettingsPage.jsx` uses local state and a "Save" button with no API backing.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/settings` | Get all clinic settings |
| PUT | `/api/settings/clinic` | Update clinic profile (name, address, GST #, hours) |
| PUT | `/api/settings/billing` | Update billing rules (GST rate, invoice prefix, footer) |
| PUT | `/api/settings/notifications` | Update notification preferences |
| POST | `/api/settings/logo` | Upload clinic logo (multipart/form-data) |

### Settings Stored

**Clinic Profile:**
- `clinic_name`, `phone`, `address`, `email`, `gst_number`
- `working_hours` — start/end per weekday
- `logo_url`

**Billing:**
- `invoice_prefix` (e.g., `INV-`)
- `gst_rate` (default 18%)
- `invoice_footer_text`
- `apply_gst` (boolean)
- `allow_partial_payments` (boolean)
- `auto_whatsapp_receipt` (boolean)

**Notifications:**
- Channel API keys / webhook URLs (WhatsApp, SMS, Email)

---

## 9. Dashboard & Analytics

Currently all dashboard metrics are computed from in-memory data in `DashboardPage.jsx`. Must be backend-computed aggregations.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard/today` | Today's stats — appointment counts by status, revenue |
| GET | `/api/dashboard/revenue` | Revenue summary — total billed, collected, outstanding |
| GET | `/api/dashboard/doctor-load` | Appointment count per doctor for a given date |
| GET | `/api/dashboard/recent-patients` | Last N patients seen (default 4) |
| GET | `/api/dashboard/no-show-rate` | No-show rate (configurable period) |

### Today Stats Response Shape

```json
{
  "scheduled": 3,
  "inChair": 1,
  "completed": 5,
  "noShow": 1,
  "revenueTodayInr": 12500,
  "pendingPaymentsInr": 3200
}
```

### Revenue Stats Response Shape

```json
{
  "totalBilledInr": 150000,
  "totalCollectedInr": 130000,
  "outstandingInr": 20000,
  "unpaidInvoiceCount": 4
}
```

---

## 10. Third-Party Integrations

### 10.1 WhatsApp Business API

- Provider options: **Twilio**, **Interakt**, **360dialog**
- Use case: Send appointment reminders, payment due notices, post-treatment follow-ups, receipts
- Webhook: Receive delivery/read status callbacks → update `reminders.delivery_status`
- Template messages must be pre-approved by Meta for HSM (Highly Structured Messages)

### 10.2 SMS Gateway

- Provider options: **Twilio SMS**, **MSG91**, **AWS SNS**
- Use case: Appointment reminders (plain text)
- DLT registration required for Indian carriers (TRAI compliance)

### 10.3 Email Service

- Provider options: **SendGrid**, **AWS SES**, **Resend**
- Use case: Invoice PDF delivery, appointment confirmations, follow-ups
- Transactional templates with dynamic variables

### 10.4 PDF Invoice Generation

- Library options: **Puppeteer** (HTML → PDF), **PDFKit**, **React-PDF** (server-side)
- Triggered via `GET /api/invoices/:id/pdf`
- Store generated PDF in object storage (AWS S3 / Supabase Storage) and return URL

### 10.5 File / Image Storage

- Provider: **AWS S3**, **Cloudflare R2**, or **Supabase Storage**
- Use case: Clinic logo upload, invoice PDF storage, future X-ray / document attachments

### 10.6 Payment Gateway (Future)

- Provider: **Razorpay** or **PhonePe**
- Use case: Online payment links sent via WhatsApp/SMS, payment status webhook
- Payment link generation endpoint: `POST /api/invoices/:id/payment-link`

---

## 11. Data Models / Database Schema

### `users`
```sql
id            UUID PRIMARY KEY
clinic_id     UUID REFERENCES clinics(id)
name          TEXT NOT NULL
email         TEXT UNIQUE NOT NULL
password_hash TEXT NOT NULL
role          TEXT CHECK (role IN ('admin','doctor','receptionist'))
active        BOOLEAN DEFAULT true
created_at    TIMESTAMPTZ DEFAULT now()
```

### `clinics`
```sql
id               UUID PRIMARY KEY
name             TEXT NOT NULL
phone            TEXT
address          TEXT
email            TEXT
gst_number       TEXT
working_hours    JSONB
logo_url         TEXT
settings         JSONB
created_at       TIMESTAMPTZ DEFAULT now()
```

### `patients`
```sql
id              UUID PRIMARY KEY
clinic_id       UUID REFERENCES clinics(id)
name            TEXT NOT NULL
phone           TEXT NOT NULL
email           TEXT
dob             DATE
gender          TEXT
blood_group     TEXT
allergies       TEXT[]
emerg_contact   TEXT
referred_by     TEXT
notes           TEXT
active          BOOLEAN DEFAULT true
created_at      TIMESTAMPTZ DEFAULT now()
UNIQUE (clinic_id, phone)
```

### `doctors`
```sql
id              UUID PRIMARY KEY
clinic_id       UUID REFERENCES clinics(id)
name            TEXT NOT NULL
specialization  TEXT
phone           TEXT
email           TEXT
color           TEXT
working_hours   JSONB
active          BOOLEAN DEFAULT true
```

### `appointment_types`
```sql
id               UUID PRIMARY KEY
clinic_id        UUID REFERENCES clinics(id)
name             TEXT NOT NULL
color            TEXT
default_duration INTEGER DEFAULT 30
```

### `treatments`
```sql
id              UUID PRIMARY KEY
clinic_id       UUID REFERENCES clinics(id)
name            TEXT NOT NULL
category        TEXT
default_price   NUMERIC(10,2)
gst_applicable  BOOLEAN DEFAULT true
```

### `appointments`
```sql
id              UUID PRIMARY KEY
clinic_id       UUID REFERENCES clinics(id)
patient_id      UUID REFERENCES patients(id)
doctor_id       UUID REFERENCES doctors(id)
type            TEXT
date            DATE NOT NULL
time            TIME NOT NULL
duration        INTEGER NOT NULL
status          TEXT CHECK (status IN ('Scheduled','In-Chair','Completed','Cancelled','No-show'))
notes           TEXT
emergency       BOOLEAN DEFAULT false
created_at      TIMESTAMPTZ DEFAULT now()
```

### `invoices`
```sql
id              UUID PRIMARY KEY
clinic_id       UUID REFERENCES clinics(id)
invoice_number  TEXT UNIQUE NOT NULL
patient_id      UUID REFERENCES patients(id)
appointment_id  UUID REFERENCES appointments(id)
date            DATE NOT NULL
items           JSONB NOT NULL
subtotal        NUMERIC(10,2)
gst_amount      NUMERIC(10,2)
grand_total     NUMERIC(10,2)
paid_amount     NUMERIC(10,2) DEFAULT 0
balance_due     NUMERIC(10,2)
status          TEXT CHECK (status IN ('Draft','Unpaid','Partially Paid','Paid','Void'))
notes           TEXT
pdf_url         TEXT
created_at      TIMESTAMPTZ DEFAULT now()
```

### `payments`
```sql
id              UUID PRIMARY KEY
invoice_id      UUID REFERENCES invoices(id)
amount          NUMERIC(10,2) NOT NULL
payment_method  TEXT
paid_at         TIMESTAMPTZ DEFAULT now()
notes           TEXT
```

### `reminder_templates`
```sql
id          UUID PRIMARY KEY
clinic_id   UUID REFERENCES clinics(id)
name        TEXT NOT NULL
channel     TEXT CHECK (channel IN ('SMS','WhatsApp','Email'))
body        TEXT NOT NULL
is_default  BOOLEAN DEFAULT false
```

### `reminders`
```sql
id              UUID PRIMARY KEY
clinic_id       UUID REFERENCES clinics(id)
patient_id      UUID REFERENCES patients(id)
appointment_id  UUID REFERENCES appointments(id)
template_id     UUID REFERENCES reminder_templates(id)
channel         TEXT NOT NULL
message         TEXT NOT NULL
delivery_status TEXT CHECK (delivery_status IN ('Pending','Delivered','Read','Failed'))
sent_at         TIMESTAMPTZ DEFAULT now()
provider_msg_id TEXT
```

---

## 12. Non-Functional Requirements

### Security

- All endpoints require JWT authentication (except `/api/auth/login`)
- All data scoped to `clinic_id` — no cross-clinic data leaks
- Input validation + sanitization on every endpoint (use Zod or Joi)
- HTTPS only in production
- CORS restricted to known frontend origin(s)
- Secrets (API keys, JWT secret) stored in environment variables — never in code

### Performance

- Paginate all list endpoints (default `limit=20`, max `limit=100`)
- Index database on: `appointments.date`, `appointments.doctor_id`, `patients.phone`, `invoices.patient_id`, `invoices.status`
- Reminder sending via background job queue — not synchronous in request/response cycle

### Compliance (India)

- DLT registration for SMS (TRAI mandate)
- GST invoicing compliant with Indian GST rules
- ABHA (Ayushman Bharat Health Account) ID field on patients — future requirement
- Data residency: host in `ap-south-1` (Mumbai) region if using AWS

### Error Handling

- Consistent error response shape:
  ```json
  { "error": { "code": "VALIDATION_ERROR", "message": "...", "details": [] } }
  ```
- HTTP status codes used correctly (200, 201, 400, 401, 403, 404, 409, 422, 500)

### API Design

- RESTful conventions throughout
- Response envelopes:
  ```json
  { "data": {...}, "meta": { "page": 1, "total": 42 } }
  ```
- ISO 8601 dates (`YYYY-MM-DD`, `YYYY-MM-DDTHH:mm:ssZ`)
- Amounts in smallest currency unit OR as decimal strings to avoid float precision issues
