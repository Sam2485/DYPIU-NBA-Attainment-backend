# PHASE 6 — API CONTRACT RECONSTRUCTION & SCREEN-BASED API DESIGN REPORT

**Phase:** Phase 6 — Strict Architecture Audit & API Contract Reconstruction  
**Status:** **AUDITED & VERIFIED (196 / 196 TESTS PASSING)**  
**Build Status:** `./mvnw test` → **BUILD SUCCESS** (0 failures, 0 errors, 0 skipped)

---

## 1. Executive Summary

Phase 6 completes the transition to a modern, screen-oriented, microservice-ready REST API architecture. Every API has been audited against the agreed academic hierarchy (`MasterProgramme`, `ProgrammeBatch`, `MasterCourse`, `ProgrammeBatchCourse`), ensuring server-side security authorization, lazy screen-based data loading, and deterministic approval state-machine transitions.

---

## 2. Screen-Based API Inventory & Design Matrix

| Screen / UI View | Consuming Persona | Purpose-Specific Endpoints | Screen-Focused Payload Summary | Unrelated Data Excluded? |
|---|---|---|---|:---:|
| **Director Dashboard** | Director / Admin | `GET /dashboard/director`<br>`GET /academic/director/school-summary`<br>`GET /academic/director/department-summary` | School-level aggregates, department list, director setup status | Yes |
| **Director School Structure** | Director / Admin | `GET /academic/schools/{id}`<br>`GET /academic/departments?schoolId=...` | School metadata, affiliated departments, department programmes | Yes |
| **Director Setup Wizard** | Director / Admin | `GET /academic/director/setup-progress`<br>`PUT /academic/director/setup-progress` | Setup step index, completion status, pending milestones | Yes |
| **HOD Dashboard** | HOD / Admin | `GET /dashboard/hod`<br>`GET /academic/hod/department-summary` | Department statistics, programme list, PC assignments | Yes |
| **HOD Coordinator Allocation** | HOD / Admin | `GET /academic/hod/coordinators`<br>`POST /academic/hod/coordinators` | Programmes under department and assigned coordinator faculty | Yes |
| **HOD Setup Wizard** | HOD / Admin | `GET /academic/hod/setup-progress`<br>`PUT /academic/hod/setup-progress` | HOD department milestone tracker | Yes |
| **PC Overview & Setup** | PC / HOD / Admin | `GET /academic/programme-coordinator/summary`<br>`GET /academic/coordinator/setup-progress`<br>`PUT /academic/coordinator/setup-progress` | Assigned `ProgrammeBatch`, batch courses count, setup progress | Yes |
| **PC Course Allocation** | PC / HOD / Admin | `GET /academic/courses?programmeId=...`<br>`POST /academic/courses/allocate` | Batch course offerings (`ProgrammeBatchCourse`) & assigned CC | Yes |
| **PC Outcome Framework** | PC / HOD / Admin | `GET /academic/outcomes?programmeId=...`<br>`POST /academic/outcomes` | Batch-scoped PO, PSO, and PEO outcomes & competencies | Yes |
| **PC Target Benchmarks** | PC / HOD / Admin | `GET /academic/programmes/{id}/targets`<br>`POST /academic/programmes/{id}/targets` | Target benchmark levels (0–3) for batch POs and PSOs | Yes |
| **PC Programme ATR** | PC / HOD / Admin | `GET /atr/programme/{programmeBatchId}`<br>`POST /atr/programme/{programmeBatchId}/submit` | Programme ATR observations, action items, recommendations | Yes |
| **CC / Faculty Dashboard** | CC / Faculty | `GET /dashboard/course-coordinator`<br>`GET /academic/course-coordinator/summary` | Assigned `ProgrammeBatchCourse` offerings list | Yes |
| **CC Setup Wizard** | CC / Faculty | `GET /academic/course-coordinator/setup-progress`<br>`PUT /academic/course-coordinator/setup-progress` | Offering setup milestone tracker (steps 0–4) | Yes |
| **CC Course Outcomes & Target** | CC / Faculty | `GET /academic/courses/{offeringId}/outcomes`<br>`POST /academic/courses/{offeringId}/outcomes` | Offering COs (statements, Blooms taxonomy, target level) | Yes |
| **CC CO-PO/PSO Mapping Matrix** | CC / Faculty | `GET /academic/courses/{offeringId}/mapping`<br>`POST /academic/courses/{offeringId}/mapping` | CO-PO and CO-PSO correlation matrices & averages | Yes |
| **CC Attainment Configuration** | CC / Faculty | `GET /attainment/config/{offeringId}`<br>`POST /attainment/config/{offeringId}` | Direct / Indirect weights (80/20) and rubric levels | Yes |
| **CC Direct Marks Assessment** | CC / Faculty | `GET /attainment/examination/{offeringId}`<br>`POST /attainment/examination/{offeringId}/upload` | Parsed student marks, calculated CO direct attainment | Yes |
| **CC Indirect Survey Assessment** | CC / Faculty | `GET /attainment/survey/{offeringId}`<br>`POST /attainment/survey/{offeringId}/upload` | Survey response averages, CO indirect attainment | Yes |
| **CC Course ATR** | CC / Faculty | `GET /atr/course/{offeringId}`<br>`POST /atr/course/{offeringId}/submit` | Course ATR report, actions proposed, verification comments | Yes |
| **Attainment Report Viewer** | All Authorized | `GET /reports/attainment-main/course/{offeringId}`<br>`GET /reports/attainment-main/course/{offeringId}/excel`<br>`GET /reports/attainment-main/course/{offeringId}/pdf` | Full consolidated direct + indirect attainment report | Yes |

---

## 3. Mandatory Approval Workflow & Verification Contract

| Resource | Submitter | Approver | Submit Endpoint | Approve Endpoint | Request Revision Endpoint | Resubmit Endpoint | Allowed Transitions |
|---|---|---|---|---|---|---|---|
| **1. Attainment Settings** | CC / Faculty | PC | `POST /approvals/submit` | `POST /approvals/{id}/approve` | `POST /approvals/{id}/reject` | `POST /approvals/submit` | `DRAFT → PENDING`<br>`PENDING → APPROVED`<br>`PENDING → NEEDS_REVISION`<br>`NEEDS_REVISION → PENDING` |
| **2. Course Outcomes + Target** | CC / Faculty | PC | `POST /approvals/submit` | `POST /approvals/{id}/approve` | `POST /approvals/{id}/reject` | `POST /approvals/submit` | `DRAFT → PENDING`<br>`PENDING → APPROVED`<br>`PENDING → NEEDS_REVISION`<br>`NEEDS_REVISION → PENDING` |
| **3. Course ATR** | CC / Faculty | PC | `POST /reports/course-atr/{id}/submit` | `POST /approvals/{id}/approve` | `POST /approvals/{id}/reject` | `POST /reports/course-atr/{id}/submit` | `DRAFT → PENDING`<br>`PENDING → APPROVED`<br>`PENDING → NEEDS_REVISION`<br>`NEEDS_REVISION → PENDING` |
| **4. Batch Course Allocation** | PC | HOD | `POST /approvals/submit` | `POST /approvals/{id}/approve` | `POST /approvals/{id}/reject` | `POST /approvals/submit` | `DRAFT → PENDING`<br>`PENDING → APPROVED`<br>`PENDING → NEEDS_REVISION`<br>`NEEDS_REVISION → PENDING` |
| **5. PO/PSO Targets** | PC | HOD | `POST /approvals/submit` | `POST /approvals/{id}/approve` | `POST /approvals/{id}/reject` | `POST /approvals/submit` | `DRAFT → PENDING`<br>`PENDING → APPROVED`<br>`PENDING → NEEDS_REVISION`<br>`NEEDS_REVISION → PENDING` |
| **6. Programme ATR** | PC | HOD | `POST /reports/programme-atr/{id}/submit` | `POST /approvals/{id}/approve` | `POST /approvals/{id}/reject` | `POST /reports/programme-atr/{id}/submit` | `DRAFT → PENDING`<br>`PENDING → APPROVED`<br>`PENDING → NEEDS_REVISION`<br>`NEEDS_REVISION → PENDING` |

---

## 4. Mandatory Security Verification Matrix

| Test Scenario | Evaluated Endpoint | Expected Response | Actual Response | Status |
|---|---|---|---|:---:|
| **Unauthenticated Request** | `GET /academic/schools` | `401 Unauthorized` | `401 Unauthorized` | **PASS** |
| **Wrong Role (CC approving PC item)** | `POST /approvals/{id}/approve` | `403 Forbidden` | `403 Forbidden` | **PASS** |
| **Wrong Role (HOD approving CC item)** | `POST /approvals/{id}/approve` | `403 Forbidden` | `403 Forbidden` | **PASS** |
| **Self-Approval Attempt** | `POST /approvals/{id}/approve` | `403 Forbidden` | `403 Forbidden` | **PASS** |
| **Wrong School Scope** | `GET /academic/departments?schoolId=sch-other` | `403 Forbidden` | `403 Forbidden` | **PASS** |
| **Wrong Department Scope** | `GET /academic/programmes?departmentId=dept-other` | `403 Forbidden` | `403 Forbidden` | **PASS** |
| **Wrong Programme Batch Scope** | `GET /academic/outcomes?programmeId=prog-other` | `403 Forbidden` | `403 Forbidden` | **PASS** |
| **Wrong Course Offering Scope** | `GET /attainment/examination/off-other` | `403 Forbidden` | `403 Forbidden` | **PASS** |
| **Spoofed Role in Payload Header** | `POST /approvals/{id}/action` | Ignored; Principal role used | Principal role used | **PASS** |
| **Status Spoofing in Direct Payload** | `PUT /academic/courses/{id}` | Status ignored; server-state used | Server-state used | **PASS** |
| **Approval after Rejection without Resubmission** | `POST /approvals/{id}/approve` | `409 Conflict` | `409 Conflict` | **PASS** |

---

## 5. Architecture Compliance Matrix

| Architecture Rule | Expected Invariant | Code Realization | Status |
|---|---|---|:---:|
| `MasterProgramme` | Reusable syllabus catalogue | `MasterProgramme` entity; owns code, name, duration | **PASS** |
| `ProgrammeBatch` | Batch-specific cohort container | `ProgrammeBatch` entity; owns start/end years, status | **PASS** |
| `MasterCourse` | Reusable course definition | `MasterCourse` entity; owns credits, courseType | **PASS** |
| `ProgrammeBatchCourse` | Batch-specific course offering | `ProgrammeBatchCourse` entity; owns faculty assignments | **PASS** |
| PO / PSO / PEO Scoping | Scoped strictly to batch | `programme_batch_id` foreign key & unique constraints | **PASS** |
| CO Scoping | Scoped strictly to offering | `programme_batch_course_id` foreign key | **PASS** |
| Attainment Scoping | Config & marks scoped to offering | `uq_attainment_config_batch_course` | **PASS** |
| Programme ATR Scoping | Scoped strictly to batch | `uk_programme_batch_atr` | **PASS** |
| Course ATR Scoping | Scoped strictly to offering | `uk_batch_course_co_atr` | **PASS** |
| Historical Reports | Retained indefinitely per batch | Previous batch records isolated and read-accessible | **PASS** |
| Status vs Authorization | Status is state only; server determines transition | Auth derived strictly from `SecurityContext` | **PASS** |
| Mathematical Integrity | Formulas 100% preserved | 80% Direct + 20% Indirect combination intact | **PASS** |

---

## 6. Microservice Readiness Assessment

1. **Identity & Authorization Boundary**: `User`, `UserRepository`, `UserRole`, `CurrentUserScopeService` are decoupled from attainment calculations.
2. **Academic Structure Boundary**: `School`, `Department`, `MasterProgramme`, `ProgrammeBatch`, `MasterCourse`, `ProgrammeBatchCourse` form a self-contained academic catalogue service domain.
3. **OBE Attainment Boundary**: `AttainmentCalculationService`, marks repositories, and survey evaluation engines consume batch and offering IDs without tight coupling to user models.
4. **Approval Workflow Boundary**: `ApprovalService` and `ApprovalRequest` operate as an independent state-machine coordinator.
