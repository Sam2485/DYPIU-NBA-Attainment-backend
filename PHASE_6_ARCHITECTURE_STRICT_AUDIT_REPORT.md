# PHASE 6 — STRICT READ-ONLY ARCHITECTURE AUDIT REPORT
**Authoritative Architectural Inspection & Baseline Audit**

---

## 1. Executive Summary

This report presents the independent, read-only architectural audit of the DYPIU NBA Attainment Backend codebase following Phase 5 completion. The audit inspects all 14 mandatory architectural areas, verifying domain terminology, identifier scoping, entity ownership, approval state machines, security preservation, mathematical calculation integrity, and screen-based API boundaries.

### Overall Compliance Status:
- **Domain Hierarchy & Relationships**: **PASS**
- **Domain Entity Renaming**: **PASS**
- **Identifier Scoping (DB & Entities)**: **PASS**
- **Approval Workflow & State Machine**: **PASS**
- **Security & Authorization Invariants**: **PASS**
- **Calculation Business Logic Preservation**: **PASS**
- **Historical Batch Model**: **PASS**
- **API Identifier Modernization (Screen/DTO layer)**: **NEEDS ALIGNMENT (Part B)**

---

## 2. Domain Hierarchy Verification

The audited hierarchy in code matches the target specification exactly:

```
School (sch-...)
  └── Department (dept-...)
       └── MasterProgramme (prog-...)
            ├── MasterCourse (crs-...) [Reusable Catalogue]
            │
            └── ProgrammeBatch (batch-...) [Specific Batch e.g. 2022-2026]
                 ├── Programme Coordinator (user / email)
                 ├── PO (ProgrammeOutcome) [uk_batch_po_code]
                 ├── PSO (ProgrammeSpecificOutcome) [uk_batch_pso_code]
                 ├── PEO (PeoOutcome) [uk_batch_peo_code]
                 ├── Programme Targets (po/pso target numeric columns)
                 ├── Programme ATR (ProgrammeAtr) [uk_programme_batch_atr]
                 │
                 └── ProgrammeBatchCourse (off-...) [Specific Course Instance]
                      ├── Course Coordinator (course_coordinator_id)
                      ├── Assigned Faculty (assigned_faculty)
                      ├── Course Outcomes (CourseOutcome) [uk_batch_course_co_code]
                      ├── CO-PO / CO-PSO Mapping (CourseMappingKeyword)
                      ├── Attainment Configuration (AttainmentConfiguration) [uq_attainment_config_batch_course]
                      ├── Student CO Marks (StudentCoMark) [programme_batch_course_id]
                      ├── Direct / Indirect Attainment
                      └── Course ATR (CourseAtr) [uk_batch_course_co_atr]
```

---

## 3. Naming & Terminology Audit (Audit #1)

- **Entities**:
  - `MasterProgramme`: Confirmed.
  - `ProgrammeBatch`: Confirmed.
  - `MasterCourse`: Confirmed.
  - `ProgrammeBatchCourse`: Confirmed.
- **Repositories**:
  - `MasterProgrammeRepository`, `ProgrammeBatchRepository`, `MasterCourseRepository`, `ProgrammeBatchCourseRepository`: Confirmed.
- **Legacy Aliases / Path Mappings**:
  - Path mappings such as `/courses`, `/batches`, `/programmes`, `/course-offerings` exist in controllers for frontend backward compatibility. In Part B, canonical routes and screen-specific endpoints will be formalized.

---

## 4. Identifier Terminology Audit (Audit #2)

- **Database Columns & Entities**:
  - `master_programme_id`: Canonical across `programme_batches`, `master_courses`, `approval_requests`.
  - `programme_batch_id`: Canonical across `programme_batch_courses`, `programme_outcomes`, `programme_specific_outcomes`, `peo_outcomes`, `programme_atrs`, `pc_setup_progress`, `approval_requests`.
  - `master_course_id`: Canonical across `programme_batch_courses`, `approval_requests`.
  - `programme_batch_course_id`: Canonical across `course_outcomes`, `attainment_configurations`, `student_co_marks`, `course_atrs`, `cc_setup_progress`, `approval_requests`.
- **DTOs / API Parameters**:
  - In some existing controllers (`AttainmentController`, `ReportController`), method parameters and aliases like `@PathVariable String courseOfferingId` or `courseId` map internally to `programmeBatchCourseId`. Part B will standardize DTOs and screen-oriented contracts.

---

## 5. MasterProgramme Ownership Audit (Audit #3)

- **Ownership**: `MasterProgramme` belongs to `Department` via `departmentId`.
- **Isolation**: Verified that `MasterProgramme` does NOT own batch-specific data (POs, PSOs, PEOs, targets, batch ATRs, or attainment marks). These are strictly owned by `ProgrammeBatch`.

---

## 6. ProgrammeBatch Ownership Audit (Audit #4)

- **Ownership**: `ProgrammeBatch` $\rightarrow$ `MasterProgramme`.
- **Batch Isolation**: Verified that multiple batches (e.g. 2022–2026, 2023–2027) under the same `MasterProgramme` maintain independent:
  - Coordinator assignments
  - PO, PSO, and PEO definitions
  - Target benchmark levels
  - Attainment calculations
  - Programme ATR observations and recommendations

---

## 7. MasterCourse Reusability Audit (Audit #5)

- **Ownership**: `MasterCourse` $\rightarrow$ `MasterProgramme`.
- **Catalogue Independence**: `MasterCourse` represents reusable syllabus/catalogue data (code, name, credits, courseType) and does not store batch-specific student marks, attainment configurations, or coordinator assignments.

---

## 8. ProgrammeBatchCourse Scoping Audit (Audit #6)

- **Ownership**: `ProgrammeBatchCourse` $\rightarrow$ (`ProgrammeBatch`, `MasterCourse`).
- **Offering Instance**: All direct assessments, marks uploads, student marks, and course ATRs strictly require `programmeBatchCourseId`.

---

## 9. Outcome Scoping Audit (Audit #7)

- **PO/PSO/PEO**: Scoped to `programmeBatchId`.
- **CO**: Scoped to `programmeBatchCourseId`.
- Verified that different batches and offerings under the same programme have completely isolated outcome definitions and targets.

---

## 10. ATR Scoping Audit (Audit #8)

- **Course ATR**: Scoped to `programmeBatchCourseId` (`uk_batch_course_co_atr`).
- **Programme ATR**: Scoped to `programmeBatchId` (`uk_programme_batch_atr`).
- No ATR data leaks to parent `MasterProgramme` or `MasterCourse`.

---

## 11. Attainment Scoping Audit (Audit #9)

- **Attainment Configuration**: Unique per `programmeBatchCourseId`.
- **Direct Assessment Marks**: Scoped to `programmeBatchCourseId` via `StudentCoMark`.
- **Indirect Survey Ratings**: Scoped to `programmeBatchCourseId`.
- **Programme-Level Attainment**: Aggregated dynamically by `programmeBatchId`.

---

## 12. Historical Batch & Report Model Audit (Audits #10 & #11)

- Historical batches retain all past data unchanged when newer batches are introduced.
- Historical reports for previous batches can be viewed by authorized users without modifying or overwriting past attainment records.

---

## 13. Approval Architecture & State Machine Audit (Audit #12)

### 13.1. The 6 Approval Resources
1. `ATTAINMENT_CONFIGURATION` (`programmeBatchCourseId`) — Submitter: CC, Approver: PC
2. `CO_DEFINITION` / `CO_TARGETS` (`programmeBatchCourseId`) — Submitter: CC, Approver: PC
3. `COURSE_ATR` (`programmeBatchCourseId`) — Submitter: CC, Approver: PC
4. `COURSE_ALLOCATION` (`programmeBatchId`) — Submitter: PC, Approver: HOD
5. `PO_PSO_TARGETS` (`programmeBatchId`) — Submitter: PC, Approver: HOD
6. `PROGRAMME_ATR` (`programmeBatchId`) — Submitter: PC, Approver: HOD

### 13.2. State Machine Rules
- **States**: `DRAFT`, `PENDING`, `APPROVED`, `NEEDS_REVISION`.
- **Forbidden**: Frontend cannot directly set status; self-approval is rejected (`403 Forbidden`); approving a rejected/revision-requested item directly without resubmission is blocked (`409 Conflict`).

---

## 14. Security & Authorization Audit

- All core security infrastructure classes are intact:
  - `User`, `UserRepository`, `UserRole`
  - `AuthController`, `AuthService`, `CustomUserDetailsService`
  - `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`
  - `CurrentUserScope`, `CurrentUserScopeService`
- Server-side scope enforcement is applied at all service boundaries for Director, HOD, PC, and CC.

---

## 15. Calculation Preservation Audit (Audit #13)

Verified mathematical calculations in `AttainmentCalculationService`:
- Direct Attainment: Component weighted average based on attainment level (0–3).
- Indirect Attainment: Survey questionnaire rating average (0–3).
- Combined Attainment: $0.80 \times \text{Direct} + 0.20 \times \text{Indirect}$.
- CO-PO/PSO Matrix Mapping: Preserved formula based on matrix weights.

---

## 16. Fake Data / Fallback Audit (Audit #14)

- No hardcoded calculation results, fabricated survey responses, or simulated attainment scores exist.
- Standard structural empty containers (`Collections.emptyList()`, `Collections.emptyMap()`) are returned when no database records exist.

---

## 17. Defect & Observation Summary

| ID | Category | Severity | Description | Phase 6 Resolution |
|---|---|---|---|---|
| D-01 | API DTOs | Low | Parameter name aliases like `courseOfferingId` present in some endpoints. | Reconstruct clean screen-specific DTOs with canonical identifiers in Part B. |
| D-02 | REST Endpoints | Low | Legacy multi-purpose endpoints exist alongside specialized endpoints. | Formalize screen-based, single-purpose REST endpoints in Part B. |

---

**Audit Result:** **READY FOR PART B (API CONTRACT RECONSTRUCTION)**
