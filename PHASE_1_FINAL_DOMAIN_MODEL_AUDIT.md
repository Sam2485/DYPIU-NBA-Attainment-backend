# PHASE 1 — FINAL DOMAIN MODEL AUDIT AND FREEZE
## Authoritative Academic Domain Architecture, Ownership Specifications & Freeze Blueprint

**Project:** DYPIU NBA Attainment Backend (`obe-backend`)  
**Audit Phase:** Phase 1 — Domain Model Audit & Architecture Freeze  
**Execution Mode:** READ-ONLY Design & Verification Audit  
**Date:** August 21, 2026  
**Status:** FROZEN — Ready for Phase 2 Implementation  

---

## 1. EXECUTIVE SUMMARY

This audit establishes the **authoritative, frozen domain model** for the academic restructuring of the DYPIU NBA Attainment Backend.

### Key Audit Findings
1. **The Core Academic Inversion:**
   - In the existing codebase, Program Outcomes (`PO`), Program Specific Outcomes (`PSO`), and Program Educational Objectives (`PEO`) are owned by `Programme` (`programme_id`).
   - In the target architecture, these outcome definitions are cohort-specific and must be owned by **`ProgrammeBatch`** (`batch_id`).
   - Similarly, the **Programme Coordinator** assignment belongs to `ProgrammeBatch`, allowing different coordinators across different cohorts of the same degree programme.
2. **Course-Level Structural Alignment:**
   - The existing `CourseOffering` entity and `course_offerings` table already function as the exact conceptual implementation of **`ProgrammeBatchCourse`**.
   - All course-level child data (`CourseOutcome`, `CoPoMapping`, `CoPsoMapping`, `AttainmentConfiguration`, `StudentCoMark`, `CourseAtr`, `UploadedDocument`) are already tied to `course_offering_id`.
   - The structural hierarchy from `ProgrammeBatchCourse` downwards is already **100% correct** and requires zero relational re-wiring.
3. **Historical Reporting & Batch Lineage:**
   - Programme attainment is computed dynamically and deterministically from immutable batch records.
   - Batch lineage is naturally derived from `(programme_id, start_year)` without requiring mutable pointer fields (`previous_batch_id`).
4. **Authentication & Security:**
   - All authentication, JWT handling, Spring Security filter chains, role-based authorization, and user scoping mechanisms are **completely preserved and out of scope**.
5. **Data Status:**
   - The database contains no required production data. Flyway migrations will be reconstructed cleanly from scratch in subsequent phases.

---

## 2. CURRENT ACADEMIC ENTITY INVENTORY

| Entity | Table | Current Responsibility | Classification | Rationale |
| :--- | :--- | :--- | :--- | :--- |
| `School` | `schools` | Root institutional container (N Schools) | **KEEP AS-IS** | Perfectly matches target architecture. |
| `Department` | `departments` | Department under School with HOD | **KEEP AS-IS** | Perfectly matches target architecture. |
| `Programme` | `programmes` | Permanent degree programme definition | **RENAME ONLY / MINOR CHANGE** | Renamed conceptually to `MasterProgramme`. Remove master-level coordinator fields. |
| `Batch` | `batches` | Academic student cohort instance | **RELATIONSHIP & FIELD CHANGE** | Renamed conceptually to `ProgrammeBatch`. Becomes the direct owner of Coordinator, POs, PSOs, PEOs, and ATR. |
| `Course` | `courses` | Master course catalog definition | **RENAME ONLY / KEEP AS-IS** | Renamed conceptually to `MasterCourse`. Belongs to `MasterProgramme`. |
| `CourseOffering` | `course_offerings` | Cohort course instance under Batch & Semester | **RENAME ONLY / KEEP AS-IS** | Renamed conceptually to `ProgrammeBatchCourse`. Already links `batch_id` and `course_id`. |
| `Student` | `students` | Enrolled student cohort roster | **KEEP AS-IS** | Already belongs to `batch_id`. |
| `ProgrammeOutcome` | `programme_outcomes` | PO statements (PO1–PO12) + targets | **RELATIONSHIP CHANGE REQUIRED** | Move ownership from `programme_id` to `batch_id`. Add approval status. |
| `PoCompetency` | `po_competencies` | Sub-competencies under PO | **KEEP AS-IS** | Already belongs to `po_id`. |
| `ProgrammeSpecificOutcome`| `programme_specific_outcomes`| PSO statements (PSO1–PSO3) + targets | **RELATIONSHIP CHANGE REQUIRED** | Move ownership from `programme_id` to `batch_id`. Add approval status. |
| `PsoCompetency` | `pso_competencies` | Sub-competencies under PSO | **KEEP AS-IS** | Already belongs to `pso_id`. |
| `PeoOutcome` | `peo_outcomes` | PEO statements | **RELATIONSHIP CHANGE REQUIRED** | Move ownership from `programme_id` to `batch_id`. Add approval status. |
| `CourseOutcome` | `course_outcomes` | Course Outcomes under batch course | **FIELD CHANGE REQUIRED** | Already belongs to `course_offering_id`. Add approval status. |
| `CoPoMapping` | `co_po_mappings` | CO to PO correlation matrix (0–3) | **KEEP AS-IS** | Already belongs to `course_outcome_id`. |
| `CoPsoMapping` | `co_pso_mappings` | CO to PSO correlation matrix (0–3) | **KEEP AS-IS** | Already belongs to `course_outcome_id`. |
| `CourseMappingKeyword` | `course_mapping_keywords` | Justifications for CO-PO/PSO mappings | **KEEP AS-IS** | Already belongs to `course_offering_id`. |
| `AttainmentConfiguration` | `attainment_configurations`| Direct/Indirect weights & thresholds | **KEEP AS-IS** | Already belongs to `course_offering_id` with approval status. |
| `StudentCoMark` | `student_co_marks` | Student marks per CO | **KEEP AS-IS** | Already belongs to `course_offering_id` and `student_id`. |
| `UploadedDocument` | `uploaded_documents` | File storage metadata for marks/surveys | **KEEP AS-IS** | Already references `batch_id` and `course_offering_id`. |
| `CourseAtr` | `course_atrs` | Course Action Taken Report | **KEEP AS-IS** | Already belongs to `course_offering_id` with approval status. |
| `ProgrammeAtr` | `programme_atrs` | Programme Action Taken Report | **FIELD / CONSTRAINT CHANGE** | Belongs to `batch_id`. Drop redundant `programme_id` column/constraint. |
| `ApprovalRequest` | `approval_requests` | Workflow submission & state tracker | **KEEP AS-IS** | Flexible polymorphic approval request engine. |
| `ApprovalHistory` | `approval_history` | Audit log of review actions | **KEEP AS-IS** | Immutable audit trail. |
| `User` | `users` | Authenticated principal & profile scope | **KEEP AS-IS** | Scopes users to School, Dept, Programme. |

---

## 3. CURRENT $\to$ TARGET NAMING MAP

| Current Domain Concept | Target Domain Concept | Java Class Target | Database Table Target | Recommendation Level |
| :--- | :--- | :--- | :--- | :--- |
| `Programme` | **`MasterProgramme`** | `Programme` (or `MasterProgramme`) | `programmes` (or `master_programmes`) | `RECOMMENDED` |
| `Batch` | **`ProgrammeBatch`** | `Batch` (or `ProgrammeBatch`) | `batches` (or `programme_batches`) | `RECOMMENDED` |
| `Course` | **`MasterCourse`** | `Course` (or `MasterCourse`) | `courses` (or `master_courses`) | `OPTIONAL` |
| `CourseOffering` | **`ProgrammeBatchCourse`** | `CourseOffering` (or `ProgrammeBatchCourse`) | `course_offerings` (or `programme_batch_courses`)| `RECOMMENDED` |
| `ProgrammeOutcome` | **`ProgrammeOutcome`** | `ProgrammeOutcome` | `programme_outcomes` | `KEEP AS-IS` |
| `ProgrammeSpecificOutcome`| **`ProgrammeSpecificOutcome`**| `ProgrammeSpecificOutcome` | `programme_specific_outcomes` | `KEEP AS-IS` |
| `PeoOutcome` | **`PeoOutcome`** | `PeoOutcome` | `peo_outcomes` | `KEEP AS-IS` |
| `CourseOutcome` | **`CourseOutcome`** | `CourseOutcome` | `course_outcomes` | `KEEP AS-IS` |
| `CourseAtr` | **`CourseAtr`** | `CourseAtr` | `course_atrs` | `KEEP AS-IS` |
| `ProgrammeAtr` | **`ProgrammeAtr`** | `ProgrammeAtr` | `programme_atrs` | `KEEP AS-IS` |

---

## 4. FINAL TARGET HIERARCHY

```
School
  │
  └── (1:N) ── Department
                 │
                 └── (1:N) ── MasterProgramme
                                │
                                ├── (1:N) ── MasterCourse
                                │
                                └── (1:N) ── ProgrammeBatch
                                               │
                                               ├── Programme Coordinator (Assignment)
                                               ├── (1:N) ── ProgrammeOutcome (PO1–PO12 + target + status)
                                               │              └── (1:N) ── PoCompetency
                                               ├── (1:N) ── ProgrammeSpecificOutcome (PSO1–PSO3 + target + status)
                                               │              └── (1:N) ── PsoCompetency
                                               ├── (1:N) ── PeoOutcome (PEO1–PEO4 + status)
                                               ├── (1:1) ── ProgrammeATR (Status + Observations + Actions)
                                               ├── (1:N) ── Student (Cohort Roster)
                                               │
                                               └── (1:N) ── ProgrammeBatchCourse
                                                              │
                                                              ├── Course Coordinator (Assignment)
                                                              ├── (1:N) ── CourseOutcome (CO1–CO6 + target + status)
                                                              │              ├── (1:N) ── CoPoMapping (Correlation Level 0–3)
                                                              │              └── (1:N) ── CoPsoMapping (Correlation Level 0–3)
                                                              ├── (1:1) ── AttainmentConfiguration (80/20 Weights + Thresholds + Status)
                                                              ├── (1:N) ── StudentCoMark (Student Assessment Scores)
                                                              └── (1:N) ── CourseATR (CO-level ATR + Actions + Status)
```

---

## 5. ENTITY-BY-ENTITY OWNERSHIP ANALYSIS

### 1. `School`
- **Current Owner:** Root container.
- **Target Owner:** Root container.
- **Action:** **KEEP AS-IS**.
- **Reason:** Manages multi-school isolation perfectly. Supports N Schools.
- **Future Change:** None.

### 2. `Department`
- **Current Owner:** `School` (`school_id`).
- **Target Owner:** `School` (`school_id`).
- **Action:** **KEEP AS-IS**.
- **Reason:** Properly encapsulates department data and HOD assignment.
- **Future Change:** None.

### 3. `MasterProgramme` (`Programme`)
- **Current Owner:** `Department` (`department_id`).
- **Target Owner:** `Department` (`department_id`).
- **Action:** **MODIFY**.
- **Reason:** Must serve strictly as the permanent, reusable degree definition (e.g. B.Tech Computer Engineering). Must not contain batch-specific coordinator fields.
- **Future Change:** Remove `coordinator` and `coordinator_email` fields.

### 4. `MasterCourse` (`Course`)
- **Current Owner:** `MasterProgramme` (`programme_id`).
- **Target Owner:** `MasterProgramme` (`programme_id`).
- **Action:** **KEEP AS-IS**.
- **Reason:** Correctly represents the permanent course definition (code, name, credits, course type).
- **Future Change:** None.

### 5. `ProgrammeBatch` (`Batch`)
- **Current Owner:** `MasterProgramme` (`programme_id`).
- **Target Owner:** `MasterProgramme` (`programme_id`).
- **Action:** **MODIFY**.
- **Reason:** Becomes the authoritative parent scope for all cohort academic assets (Coordinator, POs, PSOs, PEOs, Programme ATR).
- **Future Change:** Add `coordinator_id`, `coordinator_name`, `coordinator_email`. Remove `previous_batch_id`.

### 6. `ProgrammeBatchCourse` (`CourseOffering`)
- **Current Owner:** `ProgrammeBatch` (`batch_id`) + `MasterCourse` (`course_id`).
- **Target Owner:** `ProgrammeBatch` (`batch_id`) + `MasterCourse` (`course_id`).
- **Action:** **REPURPOSE / KEEP AS-IS**.
- **Reason:** Already correctly links Master Course, Programme Batch, Semester, Course Coordinator, and Assigned Faculty.
- **Future Change:** None required structurally.

### 7. `ProgrammeOutcome`
- **Current Owner:** `MasterProgramme` (`programme_id`).
- **Target Owner:** `ProgrammeBatch` (`batch_id`).
- **Action:** **RELATIONSHIP CHANGE REQUIRED**.
- **Reason:** PO definitions and their target values vary across batches.
- **Future Change:** Replace `programme_id` with `batch_id`. Add `status`.

### 8. `ProgrammeSpecificOutcome`
- **Current Owner:** `MasterProgramme` (`programme_id`).
- **Target Owner:** `ProgrammeBatch` (`batch_id`).
- **Action:** **RELATIONSHIP CHANGE REQUIRED**.
- **Reason:** PSO definitions and their target values vary across batches.
- **Future Change:** Replace `programme_id` with `batch_id`. Add `status`.

### 9. `PeoOutcome`
- **Current Owner:** `MasterProgramme` (`programme_id`).
- **Target Owner:** `ProgrammeBatch` (`batch_id`).
- **Action:** **RELATIONSHIP CHANGE REQUIRED**.
- **Reason:** PEO statements vary across batches.
- **Future Change:** Replace `programme_id` with `batch_id`. Add `status`.

### 10. `CourseOutcome`
- **Current Owner:** `ProgrammeBatchCourse` (`course_offering_id`).
- **Target Owner:** `ProgrammeBatchCourse` (`course_offering_id`).
- **Action:** **FIELD CHANGE REQUIRED**.
- **Reason:** Ownership is already correct. Requires approval lifecycle status.
- **Future Change:** Add `status`.

### 11. `CoPoMapping` & `CoPsoMapping`
- **Current Owner:** `CourseOutcome` (`course_outcome_id`).
- **Target Owner:** `CourseOutcome` (`course_outcome_id`).
- **Action:** **KEEP AS-IS**.
- **Reason:** Correctly maps COs to PO/PSO codes.
- **Future Change:** None.

### 12. `AttainmentConfiguration`
- **Current Owner:** `ProgrammeBatchCourse` (`course_offering_id`).
- **Target Owner:** `ProgrammeBatchCourse` (`course_offering_id`).
- **Action:** **KEEP AS-IS**.
- **Reason:** Correctly configures direct/indirect weights and thresholds per course offering.
- **Future Change:** Align status enum values.

### 13. `StudentCoMark`
- **Current Owner:** `ProgrammeBatchCourse` (`course_offering_id`) + `Student` (`student_id`).
- **Target Owner:** `ProgrammeBatchCourse` (`course_offering_id`) + `Student` (`student_id`).
- **Action:** **KEEP AS-IS**.
- **Reason:** Correctly isolates marks per student and course offering.
- **Future Change:** None.

### 14. `CourseAtr`
- **Current Owner:** `ProgrammeBatchCourse` (`course_offering_id`).
- **Target Owner:** `ProgrammeBatchCourse` (`course_offering_id`).
- **Action:** **KEEP AS-IS**.
- **Reason:** Correctly isolates course-level continuous improvement actions per offering.
- **Future Change:** Align status enum values.

### 15. `ProgrammeAtr`
- **Current Owner:** `MasterProgramme` (`programme_id`) + `ProgrammeBatch` (`batch_id`).
- **Target Owner:** `ProgrammeBatch` (`batch_id`).
- **Action:** **FIELD / CONSTRAINT CHANGE**.
- **Reason:** `batch_id` uniquely identifies the cohort. `programme_id` is redundant.
- **Future Change:** Make `batch_id` the unique foreign key.

---

## 6. RELATIONSHIP CHANGES REQUIRED

```
1. ProgrammeOutcome:
   Current:  programme_outcomes.programme_id  ──FK──>  programmes.id
   Target:   programme_outcomes.batch_id      ──FK──>  batches.id

2. ProgrammeSpecificOutcome:
   Current:  programme_specific_outcomes.programme_id  ──FK──>  programmes.id
   Target:   programme_specific_outcomes.batch_id      ──FK──>  batches.id

3. PeoOutcome:
   Current:  peo_outcomes.programme_id  ──FK──>  programmes.id
   Target:   peo_outcomes.batch_id      ──FK──>  batches.id

4. ProgrammeBatch (Coordinator Assignment):
   Current:  programmes.coordinator / coordinator_email (stored as loose strings on master programme)
   Target:   batches.coordinator_id  ──FK──>  users.id (stored directly on programme batch)
```

---

## 7. FIELDS THAT MUST BE REMOVED

| Table / Entity | Field / Column | Reason for Removal |
| :--- | :--- | :--- |
| `programmes` / `Programme` | `coordinator`, `coordinator_email` | Coordinator is batch-specific and moved to `batches`. |
| `batches` / `Batch` | `previous_batch_id` | Unused in business logic; batch lineage is derived chronologically from `start_year`. |
| `programme_outcomes` / `ProgrammeOutcome` | `programme_id` | Replaced by `batch_id`. |
| `programme_specific_outcomes` / `ProgrammeSpecificOutcome` | `programme_id` | Replaced by `batch_id`. |
| `peo_outcomes` / `PeoOutcome` | `programme_id` | Replaced by `batch_id`. |
| `programme_atrs` / `ProgrammeAtr` | `programme_id` | Redundant; `batch_id` is the primary unique identifier. |

---

## 8. FIELDS THAT MUST BE ADDED

| Table / Entity | Field / Column | Type | Purpose |
| :--- | :--- | :--- | :--- |
| `batches` / `Batch` | `coordinator_id` | `BIGINT REFERENCES users(id)` | User ID of the assigned Programme Coordinator. |
| `batches` / `Batch` | `coordinator_name` | `VARCHAR(150)` | Full display name of the Programme Coordinator. |
| `batches` / `Batch` | `coordinator_email` | `VARCHAR(150)` | Email of the Programme Coordinator for lookup & notifications. |
| `programme_outcomes` / `ProgrammeOutcome` | `batch_id` | `VARCHAR(50) REFERENCES batches(id)` | Foreign key scoping PO to specific Programme Batch. |
| `programme_outcomes` / `ProgrammeOutcome` | `status` | `VARCHAR(30) DEFAULT 'DRAFT'` | Workflow approval lifecycle status. |
| `programme_specific_outcomes` / `ProgrammeSpecificOutcome`| `batch_id` | `VARCHAR(50) REFERENCES batches(id)` | Foreign key scoping PSO to specific Programme Batch. |
| `programme_specific_outcomes` / `ProgrammeSpecificOutcome`| `status` | `VARCHAR(30) DEFAULT 'DRAFT'` | Workflow approval lifecycle status. |
| `peo_outcomes` / `PeoOutcome` | `batch_id` | `VARCHAR(50) REFERENCES batches(id)` | Foreign key scoping PEO to specific Programme Batch. |
| `peo_outcomes` / `PeoOutcome` | `status` | `VARCHAR(30) DEFAULT 'DRAFT'` | Workflow approval lifecycle status. |
| `course_outcomes` / `CourseOutcome` | `status` | `VARCHAR(30) DEFAULT 'DRAFT'` | Workflow approval lifecycle status. |

---

## 9. CONSTRAINTS THAT MUST CHANGE

| Table | Current Constraint | Target Constraint | Purpose |
| :--- | :--- | :--- | :--- |
| `programme_outcomes` | `uq_programme_po UNIQUE (programme_id, code)` | `uk_batch_po_code UNIQUE (batch_id, code)` | Allows same PO code (e.g. PO1) across different batches. |
| `programme_specific_outcomes`| `uq_programme_pso UNIQUE (programme_id, code)`| `uk_batch_pso_code UNIQUE (batch_id, code)` | Allows same PSO code (e.g. PSO1) across different batches. |
| `peo_outcomes` | `uq_programme_peo UNIQUE (programme_id, code)`| `uk_batch_peo_code UNIQUE (batch_id, code)` | Allows same PEO code (e.g. PEO1) across different batches. |
| `programme_atrs` | `uk_programme_batch_atr UNIQUE (programme_id, batch_id)`| `uk_batch_atr UNIQUE (batch_id)` | Guarantees exactly one Programme ATR per Programme Batch. |
| `batches` | `uk_batch_programme_start_year UNIQUE (programme_id, start_year)` | `uk_batch_programme_start_year UNIQUE (programme_id, start_year)` | **KEEP AS-IS** (Guarantees unique cohort start year per programme). |
| `course_offerings` | `uk_batch_course_sem UNIQUE (batch_id, course_id, semester)` | `uk_batch_course_sem UNIQUE (batch_id, course_id, semester)` | **KEEP AS-IS** (Guarantees unique course instance per semester in a batch). |

---

## 10. STATUS CHANGES REQUIRED

All workflow-controlled academic resources will follow a standardized 4-state lifecycle:

$$\mathbf{DRAFT} \longrightarrow \mathbf{PENDING} \longrightarrow \mathbf{APPROVED} \quad \Big( \text{or} \longrightarrow \mathbf{REVISION\_REQUESTED} \longrightarrow \mathbf{DRAFT} \Big)$$

### Status Enum Unification Matrix

| Domain Entity | Current Status Representation | Target Standard Status |
| :--- | :--- | :--- |
| `ProgrammeOutcome` | *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |
| `ProgrammeSpecificOutcome`| *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |
| `PeoOutcome` | *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |
| `CourseOutcome` | *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |
| `AttainmentConfiguration` | `AttainmentConfigStatus` (`DRAFT, SUBMITTED, VERIFIED, APPROVED, REJECTED`)| Standardized to `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |
| `CourseAtr` | `CourseAtrStatus` (`DRAFT, SUBMITTED, VERIFIED, NEEDS_REVISION, REJECTED`)| Standardized to `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |
| `ProgrammeAtr` | `ProgrammeAtrStatus` (`DRAFT, SUBMITTED, VERIFIED, APPROVED, REJECTED`)| Standardized to `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |
| `ApprovalRequest` | `ApprovalStatus` (`PENDING, APPROVED, REJECTED, NEEDS_REVISION`)| Standardized to `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` |

---

## 11. HISTORICAL REPORTING RELATIONSHIP

Historical reports are guaranteed by the permanent hierarchy without requiring artificial pointer fields:

```
MasterProgramme (id: "prog-btech-cse")
  │
  ├── ProgrammeBatch 1 (id: "batch-2026-30", start_year: 2026) ──> Historical Reports (2026-30)
  │     └── ProgrammeBatchCourse (course: "CNS") ───────────────> Historical Course Report (2026-30 CNS)
  │
  └── ProgrammeBatch 2 (id: "batch-2027-31", start_year: 2027) ──> Active Reports (2027-31)
        └── ProgrammeBatchCourse (course: "CNS") ───────────────> Active Course Report (2027-31 CNS)
```

1. **How Batch `2027-31` finds previous batch `2026-30`:**
   - Query: `batchRepository.findByProgrammeIdOrderByStartYearDesc(programmeId)`.
   - The immediate predecessor is the first batch with `startYear < currentBatch.startYear`.
2. **How Batch Course `2027-31 CNS` finds previous `2026-30 CNS` report:**
   - Query: `courseOfferingRepository.findByCourseId(courseId)`.
   - Filter offerings by predecessor batches under the same Master Programme.

---

## 12. THINGS THAT ARE ALREADY CORRECT AND MUST NOT BE CHANGED

1. **Multi-School Isolation:** `School` and `Department` models and repository lookups.
2. **Master Course Catalog:** `Course` table structure (`id`, `code`, `name`, `credits`, `course_type`, `programme_id`).
3. **Cohort Course Structure:** `CourseOffering` table structure (`id`, `course_id`, `batch_id`, `semester`, `course_coordinator_id`, `assigned_faculty`).
4. **Student Mark Storage:** `StudentCoMark` (`course_offering_id`, `student_id`, `co_code`, `marks_obtained`, `max_marks`).
5. **Attainment Configuration:** `AttainmentConfiguration` (`course_offering_id`, `direct_weight`, `indirect_weight`, `direct_threshold`, `indirect_threshold`).
6. **CO Mappings:** `CoPoMapping` and `CoPsoMapping` (`course_outcome_id`, `po_code` / `pso_code`, `mapping_level`).
7. **Document Management:** `UploadedDocument` file metadata tracking for Excel uploads.
8. **Mathematical Engine:** 80% Direct + 20% Indirect attainment formulas and semester aggregation matrices in `AttainmentCalculationService`.
9. **Export Engine:** Apache POI Excel template populator and OpenPDF report generator in `AttainmentReportExportService`.

---

## 13. AUTHENTICATION / AUTHORIZATION PRESERVATION CONFIRMATION

> [!IMPORTANT]
> **STRICT PRESERVATION CONFIRMATION:**
> Authentication, authorization, Spring Security, JWT token handling, and user scoping are **STRICTLY OUT OF SCOPE** and have **NOT** been modified in any way.

The following production components remain 100% untouched:
- `SecurityConfig.java` (Security filter chain, stateless session, CORS rules)
- `JwtAuthenticationFilter.java` & `JwtTokenProvider.java`
- `JwtAuthenticationEntryPoint.java` & `JwtAccessDeniedHandler.java`
- `CurrentUserScopeService.java` & `CurrentUserScope.java`
- `CustomUserDetailsService.java` & `AuthService.java` & `AuthController.java`
- `User.java`, `UserRepository.java`, `UserRole.java`

---

## 14. MINIMUM-CHANGE IMPLEMENTATION ORDER (PHASE 2 PREVIEW)

When Phase 2 begins, the exact sequence of modifications will be:

```
Step 1: Reconstruct Database Migration
        └── Consolidate into V1__init_authoritative_academic_schema.sql
            (Clean schema with batch_id on outcomes, coordinator on batches)

Step 2: Update JPA Entities
        ├── Batch.java (Add coordinator fields, remove previousBatchId)
        ├── Programme.java (Remove coordinator fields)
        ├── ProgrammeOutcome.java (Change programmeId -> batchId, add status)
        ├── ProgrammeSpecificOutcome.java (Change programmeId -> batchId, add status)
        ├── PeoOutcome.java (Change programmeId -> batchId, add status)
        └── CourseOutcome.java (Add status)

Step 3: Update JPA Repositories
        ├── ProgrammeOutcomeRepository.java (findByBatchId...)
        ├── ProgrammeSpecificOutcomeRepository.java (findByBatchId...)
        ├── PeoOutcomeRepository.java (findByBatchId...)
        └── BatchRepository.java (findByCoordinatorEmail...)

Step 4: Update Domain Services
        ├── OutcomeService.java (Update PO/PSO/PEO operations to use batchId)
        ├── AcademicService.java (Update batch coordinator assignment)
        ├── AtrService.java (Update scope enforcement & ATR lookups)
        └── AttainmentCalculationService.java (Pass batchId to outcome repository queries)

Step 5: Update REST Controllers & DTOs
        └── Update OutcomeController endpoints to accept /batches/{batchId}/pos

Step 6: Verification & Test Suite
        └── Run ./mvnw test to confirm complete green build
```

---

## 15. RISKS / AMBIGUITIES

### Issues Found — NOT MODIFIED (To be addressed in Phase 2):
1. **Legacy Endpoint Parameter Ambiguity:**
   - In `OutcomeService.java`, certain helper methods (`resolveOfferingId`) contained fallback logic when `courseId` was passed instead of `courseOfferingId`.
   - *Phase 2 Fix:* Enforce explicit `batchId` / `courseOfferingId` on all outcome and calculation endpoints.
2. **Default PO Generation Logic:**
   - In `AttainmentCalculationService.java:1680-1691`, if no POs were found in the database, 12 dummy POs were auto-generated in memory.
   - *Phase 2 Fix:* Read POs strictly from the database for the given `batchId`.

---

## 16. FINAL ENTITY RELATIONSHIP DIAGRAM (TEXT FORM)

```
====================================================================================================
                                 FINAL ENTITY RELATIONSHIP DIAGRAM
====================================================================================================

[ schools ]
    id (PK)
    code, name
    director_id, director_name, director_email
    │
    └──< (1:N) >── [ departments ]
                       id (PK)
                       school_id (FK -> schools.id)
                       code, name, hod, hod_email, status
                       │
                       └──< (1:N) >── [ programmes ] (Master Programme)
                                          id (PK)
                                          department_id (FK -> departments.id)
                                          code, name, duration_years, status
                                          │
                                          ├──< (1:N) >── [ courses ] (Master Course)
                                          │                  id (PK)
                                          │                  programme_id (FK -> programmes.id)
                                          │                  code, name, credits, course_type, status
                                          │
                                          └──< (1:N) >── [ batches ] (Programme Batch)
                                                             id (PK)
                                                             programme_id (FK -> programmes.id)
                                                             name, start_year, end_year, duration_years, status
                                                             coordinator_id (FK -> users.id), coordinator_name, coordinator_email
                                                             │
                                                             ├──< (1:N) >── [ programme_outcomes ]
                                                             │                  id (PK), batch_id (FK -> batches.id)
                                                             │                  code, statement, target, status
                                                             │                  │
                                                             │                  └──< (1:N) >── [ po_competencies ]
                                                             │                                     id (PK), po_id (FK)
                                                             │
                                                             ├──< (1:N) >── [ programme_specific_outcomes ]
                                                             │                  id (PK), batch_id (FK -> batches.id)
                                                             │                  code, statement, target, status
                                                             │                  │
                                                             │                  └──< (1:N) >── [ pso_competencies ]
                                                             │                                     id (PK), pso_id (FK)
                                                             │
                                                             ├──< (1:N) >── [ peo_outcomes ]
                                                             │                  id (PK), batch_id (FK -> batches.id)
                                                             │                  code, statement, status
                                                             │
                                                             ├──< (1:1) >── [ programme_atrs ]
                                                             │                  id (PK), batch_id (FK -> batches.id UNIQUE)
                                                             │                  status, submitted_by, verified_by, observations_json
                                                             │
                                                             ├──< (1:N) >── [ students ]
                                                             │                  id (PK), batch_id (FK -> batches.id)
                                                             │                  prn, name, email, status
                                                             │
                                                             └──< (1:N) >── [ course_offerings ] (Programme Batch Course)
                                                                                id (PK)
                                                                                batch_id (FK -> batches.id)
                                                                                course_id (FK -> courses.id)
                                                                                semester, status
                                                                                course_coordinator_id (FK -> users.id), assigned_faculty
                                                                                │
                                                                                ├──< (1:N) >── [ course_outcomes ]
                                                                                │                  id (PK), course_offering_id (FK)
                                                                                │                  code, statement, target_level, blooms_level, status
                                                                                │                  │
                                                                                │                  ├──< (1:N) >── [ co_po_mappings ]
                                                                                │                  │                  id (PK), course_outcome_id (FK), po_code, mapping_level
                                                                                │                  │
                                                                                │                  └──< (1:N) >── [ co_pso_mappings ]
                                                                                │                                     id (PK), course_outcome_id (FK), pso_code, mapping_level
                                                                                │
                                                                                ├──< (1:1) >── [ attainment_configurations ]
                                                                                │                  id (PK), course_offering_id (FK UNIQUE)
                                                                                │                  direct_weight, indirect_weight, thresholds, status
                                                                                │
                                                                                ├──< (1:N) >── [ student_co_marks ]
                                                                                │                  id (PK), course_offering_id (FK), student_id (FK)
                                                                                │                  prn, co_code, marks_obtained, max_marks
                                                                                │
                                                                                └──< (1:N) >── [ course_atrs ]
                                                                                                   id (PK), course_offering_id (FK)
                                                                                                   co_code, target_score, actual_score, pct_achieved, status
====================================================================================================
```

---

## 17. EXACT FILES THAT WOULD NEED MODIFICATION IN PHASE 2

> [!NOTE]
> None of the files below have been modified during Phase 1. They are cataloged here as the execution checklist for Phase 2.

### 1. Migrations
- `obe-backend/src/main/resources/db/migration/*` (Consolidate into `V1__init_authoritative_academic_schema.sql`)

### 2. JPA Entities
- `obe-backend/src/main/java/com/dypiu/nba/entity/Batch.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/Programme.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeSpecificOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/PeoOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/CourseOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeAtr.java`

### 3. Repositories
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeOutcomeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeSpecificOutcomeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/PeoOutcomeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/BatchRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeAtrRepository.java`

### 4. Domain Services
- `obe-backend/src/main/java/com/dypiu/nba/service/OutcomeService.java`
- `obe-backend/src/main/java/com/dypiu/nba/service/AcademicService.java`
- `obe-backend/src/main/java/com/dypiu/nba/service/AtrService.java`
- `obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java`

### 5. Controllers & DTOs
- `obe-backend/src/main/java/com/dypiu/nba/controller/OutcomeController.java`
- `obe-backend/src/main/java/com/dypiu/nba/controller/AcademicController.java`
