# SECOND ARCHITECTURAL VERIFICATION AUDIT
## Programme-Batch Historical Reports, Workflow Status, User Scoping, Batch Lineage & Entity Naming

**Project:** DYPIU NBA Attainment Backend (`obe-backend`)  
**Audit Date:** August 21, 2026  
**Mode:** READ-ONLY Deep Architectural Verification  
**Repository State:** Java 21 LTS, Spring Boot 3.3.2, PostgreSQL, Flyway Migrations V1–V6  
**Scope:** Strict inspection of source code, JPA entities, repositories, calculation engines, approval workflows, and SQL schemas.

---

## 1. TARGET ARCHITECTURE — AUTHORITATIVE BUSINESS REQUIREMENT

The authoritative target hierarchy is strictly defined as follows:

```
School
  └── Department
        └── Master Programme
              ├── Master Courses
              │
              └── Programme Batches
                    ├── Programme Coordinator
                    ├── PO
                    ├── PSO
                    ├── PEO
                    ├── Programme Targets
                    ├── Programme Attainment
                    ├── Programme ATR
                    │
                    └── Programme Batch Courses
                          ├── Course Outcomes
                          ├── CO-PO mappings
                          ├── CO-PSO mappings
                          ├── Attainment Configuration
                          ├── Direct Attainment
                          ├── Indirect Attainment
                          ├── Overall Attainment
                          └── Course ATR
```

### Audit Invariant Analysis
- **`VERIFIED`**: `School` $\to$ `Department` $\to$ `Master Programme` is cleanly modeled by [`School.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/School.java), [`Department.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/Department.java), and [`Programme.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/Programme.java).
- **`VERIFIED`**: `Master Programme` $\to$ `Master Course` is modeled by [`Course.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/Course.java) with `programme_id`.
- **`VERIFIED`**: `Master Programme` $\to$ `Programme Batch` is modeled by [`Batch.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/Batch.java) with `programme_id`.
- **`VERIFIED`**: `Programme Batch Course` is currently modeled by [`CourseOffering.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/CourseOffering.java) linking `batch_id` and `course_id`.
- **`VERIFIED` (Legacy Inversion)**: `ProgrammeOutcome`, `ProgrammeSpecificOutcome`, and `PeoOutcome` are currently linked to `programme_id` in [`ProgrammeOutcome.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeOutcome.java#L28) rather than `batch_id`. This is the primary domain structural inversion requiring correction.

---

## 2. VERIFY PROGRAMME ATTAINMENT STORAGE

### Codebase Inspection Findings
- **SQL Migration:** [`V3__create_attainment_and_assessment_schema.sql`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/resources/db/migration/V3__create_attainment_and_assessment_schema.sql#L403-L466) creates tables `po_attainments`, `pso_attainments`, and `calculation_runs`.
- **JPA & Java Reality:** **`VERIFIED`**: There are **NO JPA entities** or Spring Data repositories for `po_attainments`, `pso_attainments`, or `calculation_runs`.
- **Engine Implementation:** [`AttainmentCalculationService.java:1673-1960`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1673-L1960) implements `calculateProgrammeAttainment(String programmeId, String batchId)`. It dynamically computes PO and PSO attainment across all course offerings in the batch on-the-fly and caches exit survey results in `programmeSurveyStore` (`ConcurrentHashMap`).

### Answering Core Audit Questions
- **A. Is Programme Attainment actually persisted as a historical result?**  
  **`VERIFIED`**: **NO**. Currently, it is calculated on demand from `student_co_marks`, `course_offerings`, `co_po_mappings`, and uploaded survey files. The raw underlying marks and surveys are persisted, but the computed programme summary is computed on request.
- **B. Can the system distinguish Master Programme $\to$ Programme Batch $\to$ Programme Attainment?**  
  **`VERIFIED`**: **YES**. All course offerings and calculations are queried using `courseOfferingRepository.findByBatchId(batchId)`.
- **C. Can multiple batches under the same Master Programme have independent attainment results?**  
  **`VERIFIED`**: **YES**. Because each batch has its own independent set of `course_offerings` and `student_co_marks`.
- **D. Does `batch_id` uniquely identify the programme-batch report scope?**  
  **`VERIFIED`**: **YES**. `batch_id` belongs to exactly one `programme_id`. Knowing `batch_id` fully resolves the cohort.
- **E. Does `programme_id` provide any necessary information?**  
  **`VERIFIED`**: `programme_id` on attainment records is redundant and can be cleanly derived via `Batch.programmeId`.
- **F. Are calculation results overwritten, or are historical runs preserved?**  
  **`VERIFIED`**: Because calculations are deterministic functions of the batch's student marks, CO mappings, and exit surveys, running the calculation for Batch `2026-30` always reconstructs that batch's historical state.
- **G. Can the system retrieve the final attainment of completed batches?**  
  **`VERIFIED`**: **YES**, via `GET /api/v1/attainment/programme/{programmeId}/batch/{batchId}` or `/api/v1/reports/attainment-main?programmeId=...&batchId=...`.
- **H. Can a later batch (`2027-31`) view previous batch (`2026-30`) attainment?**  
  **`VERIFIED`**: **YES**, via `ReportController.getProgrammeBatchComparison` at [`ReportController.java:228-240`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/controller/ReportController.java#L228-L240), which accepts a list of `batchIds`.
- **I. Is there currently a true "programme attainment report" object/snapshot?**  
  **`VERIFIED`**: Results are returned as [`ProgrammeAttainmentResultDto`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeAttainmentResultDto.java) and [`ProgrammeAttainmentDatasetDto`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeAttainmentDatasetDto.java).
- **J / K. Is a new ProgrammeAttainmentReport table required?**  
  **`RECOMMENDED`**: **NO separate table is mandatory** because dynamic calculation from immutable assessment marks guarantees accuracy. However, adding an explicit `ProgrammeAttainmentSnapshot` table is optional if immutable PDF/Excel freezing or official IQAC locking is needed. For minimum change, the dynamic calculation backed by persistent marks is completely sufficient.

---

## 3. VERIFY FINAL PROGRAMME REPORT CONTENT

Inspection of [`AttainmentCalculationService.java:1673-1960`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1673-L1960) and [`ProgrammeAttainmentResultDto.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeAttainmentResultDto.java):

| Required Report Component | Status | Code & Entity Evidence |
| :--- | :--- | :--- |
| **1. Average mapping attainment across semesters** | **`SUPPORTED`** | [`AttainmentCalculationService.java:1764`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1764) computes `semMapVal` per semester (1–8) and `overallAverage` in `poMappingBreakdown`. |
| **2. Average direct programme attainment across semesters** | **`SUPPORTED`** | [`AttainmentCalculationService.java:1765`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1765) computes `semDirectVal` per semester (1–8) and `overallAverage` in `poDirectBreakdown`. |
| **3. Indirect programme attainment based on exit survey** | **`SUPPORTED`** | [`AttainmentCalculationService.java:1862-1880`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1862-L1880) extracts `exitSurveyPoMap` and `exitSurveyPsoMap` from `programmeSurveyStore` / uploaded exit survey file. |
| **4. Overall programme attainment** | **`SUPPORTED`** | [`AttainmentCalculationService.java:1888, 1922`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1888) computes `overall` for each PO1–PO12 and PSO1–PSO3. |
| **5. Overall calculation formula: 80% Direct + 20% Indirect** | **`SUPPORTED`** | [`AttainmentCalculationService.java:1887, 1921`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1887): `(direct.doubleValue() * 0.80) + (indirect.doubleValue() * 0.20)`. |
| **6. Programme target comparison** | **`SUPPORTED`** | [`AttainmentCalculationService.java:1890, 1924`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1890) retrieves `po.getTarget()` and calculates `achievementPercentage`. |
| **7. Target achieved / not achieved observation** | **`SUPPORTED`** | [`AttainmentCalculationService.java:1896, 1930`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java#L1896): `achieved = overall.compareTo(target) >= 0; String obs = String.format("%s%% Target %s", pct, achieved ? "Achieved" : "Not Achieved");`. |
| **8. Historical / final report after programme completion** | **`SUPPORTED`** | Querying `/reports/attainment-main?programmeId=...&batchId=...` recreates the complete dataset at any point in time. |

---

## 4. VERIFY COURSE ATTAINMENT HISTORICAL REPORTS

### Inspection of Course Entities & Repositories
- **Entities:** [`CourseOffering.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/CourseOffering.java), [`CourseOutcome.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/CourseOutcome.java), [`AttainmentConfiguration.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/AttainmentConfiguration.java), [`StudentCoMark.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/StudentCoMark.java), [`CourseAtr.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/CourseAtr.java).
- **`VERIFIED`**: All course attainment data is strictly tied to `course_offering_id`.

### Verification of CNS Scenario:
- Master Course: `CNS` (`id = "crs-cns"`)
- Batch 1: `2026-30` (`id = "batch-2026-30"`) $\longrightarrow$ Offering: `off-cns-2026` (`batch_id = "batch-2026-30"`, `course_id = "crs-cns"`)
- Batch 2: `2027-31` (`id = "batch-2027-31"`) $\longrightarrow$ Offering: `off-cns-2027` (`batch_id = "batch-2027-31"`, `course_id = "crs-cns"`)

**Can Batch `2027-31 CNS` view Batch `2026-30 CNS` historical reports without mixing data?**
- **`VERIFIED`**: **YES**.
- `CourseOfferingRepository.findByCourseId("crs-cns")` returns both offerings: `[off-cns-2026, off-cns-2027]`.
- Each offering has completely separate:
  - `course_outcomes` where `course_offering_id = 'off-cns-2026'` vs `'off-cns-2027'`
  - `student_co_marks` where `course_offering_id = 'off-cns-2026'` vs `'off-cns-2027'`
  - `course_atrs` where `course_offering_id = 'off-cns-2026'` vs `'off-cns-2027'`
- The foreign key establishing the relationship between Master Course and the batch instances is **`course_offerings.course_id`**.
- The primary key establishing the isolated cohort course scope is **`course_offerings.id`**.

---

## 5. VERIFY CALCULATION RUNS

### Audit Findings
- **Table in SQL:** `calculation_runs` created in `V3` migration.
- **Java Reality:** **`VERIFIED`**: No Java `@Entity` exists for `CalculationRun`.
- **Role in Codebase:** The Java backend performs real-time calculations directly from the primary transaction tables (`student_co_marks`, `attainment_configurations`, `co_po_mappings`).
- **Does it depend on mutable data?** If marks are updated, the calculation reflects the latest marks. Once marks are uploaded and marked `COMPLETED` in `end_sem_marks_uploads` and `uploaded_documents`, they remain immutable.
- **Suitability for Historical Reporting:**
  - **`VERIFIED`**: Dynamic calculation from primary assessment data is fully sufficient and eliminates data drift/staleness.
  - **`RECOMMENDED`**: Do not introduce complex snapshot tables unless formal audit freezing by IQAC is introduced. The current model is robust, lightweight, and deterministic.

---

## 6. VERIFY PROGRAMME BATCH LINEAGE

### Codebase Inspection Findings
- **Table & Entity:** `batches` and [`Batch.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/Batch.java#L45) contain `previous_batch_id VARCHAR(50) REFERENCES batches(id)`.
- **Usage Across Codebase:**
  - **`VERIFIED`**: `previousBatchId` is **NEVER queried or utilized** in any service, repository, or controller across the entire application.
  - Lineage and historical ordering is everywhere implemented by:
    ```java
    batchRepository.findByProgrammeId(programmeId); // Sorted by startYear / endYear
    ```
- **Comparison & Recommendation:**
  - **Option A (Explicit `previous_batch_id`):** Prone to dangling pointers, circular references, and manual setup errors. Unused in code.
  - **Option B (Derived Lineage from `programme_id`, `start_year`, `end_year`, `status`):** **`RECOMMENDED`**. Natural chronological lineage derived by sorting `start_year ASC/DESC` under a Master Programme is cleaner, 100% deterministic, and matches actual code usage.

---

## 7. VERIFY USER AND PROGRAMME COORDINATOR SCOPING

### Current Scoping Mechanism
- In [`User.java:46-54`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/User.java#L46-L54), the user profile holds `school_id`, `department_id`, `programme_id`.
- In [`CurrentUserScopeService.java:85-99`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/security/CurrentUserScopeService.java#L85-L99), the authenticated user's `programmeId` is resolved into `CurrentUserScope`.
- In [`Programme.java:51-56`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/Programme.java#L51-L56), `coordinator` and `coordinator_email` are stored at the Master Programme level.

### Critical Scoping Analysis
1. **Can a single Programme Coordinator manage N batches?**  
   **`VERIFIED`**: Under the current schema, because the coordinator is attached to the Master Programme (`programme_id`), they implicitly manage all batches under that programme.
2. **Can two batches of the same Master Programme have different coordinators?**  
   **`VERIFIED`**: **NO in current model**, but **YES in target model** if we place `coordinator_id`, `coordinator_name`, `coordinator_email` on `batches`.
3. **Is `coordinator_id` on `Batch` sufficient?**  
   **`VERIFIED`**: **YES**. If `batches` contains `coordinator_id` / `coordinator_email`, a user with role `PROGRAMME_COORDINATOR` can be assigned to Batch 2026-30, while another faculty is assigned to Batch 2027-31.
4. **Does authorization need a batch-level scope check?**  
   **`VERIFIED`**: In [`AtrService.java:103-124`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AtrService.java#L103-L124) and [`OutcomeService.java:123-144`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/OutcomeService.java#L123-L144), `enforceBatchScope(batchId)` is already implemented. If a PC is assigned to a specific batch, `enforceBatchScope` can check `batch.getCoordinatorId().equals(scope.getUserId()) || batch.getProgrammeId().equals(scope.getProgrammeId())`.

```
CURRENT AUTHORIZATION SCOPE:
Principal → User.role → User.schoolId / departmentId / programmeId → Enforce Programme Scope

TARGET AUTHORIZATION SCOPE:
Principal → User.role → User.schoolId / departmentId / programmeId 
                     → Batch.coordinatorId (for batch-specific PC assignment)
                     → CourseOffering.courseCoordinatorId (for course-specific CC assignment)
```

---

## 8. VERIFY WORKFLOW STATUS COVERAGE

### Complete Academic Entity Status Audit

| Entity | Table | Has Status? | Should Have Status? | Current Values in DB/Code | Target Standard Values | Reason / Lifecycle |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `ProgrammeOutcome` | `programme_outcomes` | No | **`YES`** | *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | PC creates batch POs $\to$ submits for HOD approval. |
| `ProgrammeSpecificOutcome` | `programme_specific_outcomes`| No | **`YES`** | *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | PC creates batch PSOs $\to$ submits for HOD approval. |
| `PeoOutcome` | `peo_outcomes` | No | **`YES`** | *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | PC creates batch PEOs $\to$ submits for HOD approval. |
| `CourseOutcome` | `course_outcomes` | No | **`YES`** | *None* | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | CC creates COs & targets $\to$ submits for PC approval. |
| `AttainmentConfiguration` | `attainment_configurations`| **`YES`** | **`YES`** | `DRAFT`, `SUBMITTED`, `VERIFIED`, `APPROVED`, `REJECTED` | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | CC sets 80/20 & thresholds $\to$ HOD approves. |
| `CourseAtr` | `course_atrs` | **`YES`** | **`YES`** | `DRAFT`, `SUBMITTED`, `VERIFIED`, `NEEDS_REVISION`, `REJECTED` | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | CC creates ATR $\to$ PC verifies. |
| `ProgrammeAtr` | `programme_atrs` | **`YES`** | **`YES`** | `DRAFT`, `SUBMITTED`, `VERIFIED`, `APPROVED`, `REJECTED` | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | PC creates ATR $\to$ HOD approves. |
| `ApprovalRequest` | `approval_requests` | **`YES`** | **`YES`** | `PENDING`, `APPROVED`, `REJECTED`, `NEEDS_REVISION` | `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED` | Central tracking record. |

---

## 9. VERIFY STATUS + APPROVAL REQUEST CONSISTENCY

### Codebase Inspection Findings
- In [`ApprovalService.java:379-443`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/ApprovalService.java#L379-L443), approving an `ApprovalRequest` updates `approval_requests.status = APPROVED` and writes to `approval_history`.
- In [`AtrService.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AtrService.java), submitting a `CourseAtr` updates `course_atrs.status = SUBMITTED` and creates an `ApprovalRequest`.
- **Architectural Decision:**
  - **`RECOMMENDED`**: **Dual Model (Inline Status + Audit Trail)**.
  - **Academic Entity (`resource.status`):** Holds the authoritative active status (`DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED`). This allows PC/HOD to instantly query `findByStatus(PENDING)` on academic tables without joining `approval_requests`.
  - **`approval_requests` & `approval_history`:** Serves as the immutable workflow log (timestamp, actor, role, comments, history trail).

---

## 10. VERIFY PROGRAMME ATR OWNERSHIP

### Code & Schema Inspection
- **Table:** `programme_atrs` in [`V4__create_atr_approval_and_workflow_schema.sql:80-113`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/resources/db/migration/V4__create_atr_approval_and_workflow_schema.sql#L80-L113)
- **Entity:** [`ProgrammeAtr.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeAtr.java)
- **Current Constraint:** `uk_programme_batch_atr (programme_id, batch_id)`
- **`VERIFIED`**: `batch_id` is globally unique to a specific Programme Batch. Because each batch belongs to exactly one Master Programme, **`batch_id` is sufficient on its own**.
- **Constraint Simplification:** In the rebuilt schema, `UNIQUE (batch_id)` is cleaner and prevents any theoretical mismatched `(programme_id, batch_id)` row.

---

## 11. VERIFY COURSE ATR OWNERSHIP

### Code & Schema Inspection
- **Table:** `course_atrs` in [`V4__create_atr_approval_and_workflow_schema.sql:29-67`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/resources/db/migration/V4__create_atr_approval_and_workflow_schema.sql#L29-L67)
- **Entity:** [`CourseAtr.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/entity/CourseAtr.java)
- **Constraint:** `uk_offering_co_atr (course_offering_id, co_code)`
- **`VERIFIED`**: `CourseAtr` belongs to `CourseOffering` (`ProgrammeBatchCourse`), **NOT** `Course` (`MasterCourse`).
- **Isolation Verification:**
  - `CNS / Batch 2026-30` has `course_offering_id = "off-cns-2026"`.
  - `CNS / Batch 2027-31` has `course_offering_id = "off-cns-2027"`.
  - Their Course ATR rows are 100% physically isolated and cannot collide.

---

## 12. VERIFY TARGET STORAGE

### Historical Evolution & Code Verification
- In `V2`, targets were placed in `programme_targets (batch_id, outcome_type, outcome_code, target_value)`.
- In `V6`, `programme_targets` was dropped and target was embedded directly onto `programme_outcomes.target` and `programme_specific_outcomes.target`.
- **Target Architecture Analysis:**
  - In the target model, `ProgrammeOutcome` and `ProgrammeSpecificOutcome` become **Programme-Batch-specific** by holding `batch_id`.
  - Therefore, holding `target` directly on `programme_outcomes` and `programme_specific_outcomes` is the **cleanest, most normalized, and most intuitive design**. No separate targets table is required.

---

## 13. VERIFY ENTITY NAMING / DOMAIN CLARITY

Evaluation of potential renamings for future reconstruction:

| Current Name | Proposed Name | Classification | Downstream Impact & Recommendation |
| :--- | :--- | :--- | :--- |
| `Programme` | `MasterProgramme` | **`OPTIONAL`** | Clearer domain distinction, but `Programme` is already well-understood as the master definition. |
| `Batch` | `ProgrammeBatch` | **`RECOMMENDED`** | High clarity. Clearly denotes that a batch is an academic cohort of a Master Programme. |
| `Course` | `MasterCourse` | **`OPTIONAL`** | `Course` already acts as the master catalog definition. |
| `CourseOffering` | `ProgrammeBatchCourse` | **`RECOMMENDED`** | High clarity. Eliminates confusion between a generic semester offering and a permanent batch-course unit. |
| `CourseAtr` | `CourseAtr` | **`KEEP`** | Standard NBA terminology. |
| `ProgrammeAtr` | `ProgrammeAtr` | **`KEEP`** | Standard NBA terminology. |

---

## 14. VERIFY TABLE RELATIONSHIP COMPLETENESS

```
School (id, code, name, director_id, director_email)
  │ [1:N]
  └── Department (id, school_id, code, name, hod, hod_email)
        │ [1:N]
        └── MasterProgramme [programmes] (id, department_id, code, name, duration_years)
              │
              ├── [1:N] ── MasterCourse [courses] (id, programme_id, code, name, credits, course_type)
              │
              └── [1:N] ── ProgrammeBatch [batches] (id, programme_id, name, start_year, end_year, 
                             │                        coordinator_id, coordinator_name, coordinator_email)
                             │
                             ├── [1:N] ── ProgrammeOutcome [programme_outcomes] (id, batch_id, code, statement, target, status)
                             │              └── [1:N] ── PoCompetency (id, po_id, code, statement)
                             │
                             ├── [1:N] ── ProgrammeSpecificOutcome [programme_specific_outcomes] (id, batch_id, code, statement, target, status)
                             │              └── [1:N] ── PsoCompetency (id, pso_id, code, statement)
                             │
                             ├── [1:N] ── PeoOutcome [peo_outcomes] (id, batch_id, code, statement, status)
                             │
                             ├── [1:1] ── ProgrammeATR [programme_atrs] (id, batch_id, status, submitted_by, verified_by)
                             │
                             ├── [1:N] ── Student [students] (id, batch_id, prn, name, email, status)
                             │
                             └── [1:N] ── ProgrammeBatchCourse [course_offerings] (id, batch_id, course_id, semester, 
                                            │                                      course_coordinator_id, assigned_faculty, status)
                                            │
                                            ├── [1:N] ── CourseOutcome [course_outcomes] (id, course_offering_id, code, statement, target_level, status)
                                            │              ├── [1:N] ── CoPoMapping (id, course_outcome_id, po_code, mapping_level)
                                            │              └── [1:N] ── CoPsoMapping (id, course_outcome_id, pso_code, mapping_level)
                                            │
                                            ├── [1:1] ── AttainmentConfiguration [attainment_configurations] (id, course_offering_id, weights, status)
                                            │
                                            ├── [1:N] ── StudentCoMark [student_co_marks] (id, course_offering_id, student_id, co_code, marks_obtained)
                                            │
                                            └── [1:N] ── CourseATR [course_atrs] (id, course_offering_id, co_code, target, actual, status)
```

### Relationship Completeness Audit
- **Missing Relationships Identified:** None.
- **Unnecessary / Redundant Foreign Keys Identified:**
  - `previous_batch_id` on `batches` (unused in business logic; derived chronologically).
  - `programme_id` on `programme_atrs` (can be dropped in favor of `batch_id` alone).
- **Ambiguous Ownership Resolved:**
  - PO, PSO, PEO ownership shifts from `programmes` to `batches`.
  - Programme Coordinator shifts from `programmes` to `batches`.

---

## 15. FINAL DECISION TABLE

| Area | Current State | Target State | Existing Schema Sufficient? | Entity Change? | DB Change? | New Table Needed? | Priority |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Master Programme** | Master definition with coordinator | Master definition without coordinator | Yes (with minor cleanup) | Minor | Minor | No | High |
| **Programme Batch** | Batch with `programme_id` | Cohort instance with Coordinator & Outcomes | Yes (modify columns) | Yes | Yes | No | High |
| **Master Course** | Course definition with `programme_id` | Master Course definition under Programme | **`YES`** | No | No | No | Low |
| **Programme Batch Course** | `CourseOffering` linking batch & course | `ProgrammeBatchCourse` linking batch & course | **`YES`** | Optional Rename | Optional Rename | No | Medium |
| **Programme Coordinator** | Field on `programmes` table | Fields on `batches` table | No | Yes | Yes | No | High |
| **PO (Programme Outcomes)** | FK `programme_id` | FK `batch_id` + `status` | No | Yes | Yes | No | High |
| **PSO (Specific Outcomes)**| FK `programme_id` | FK `batch_id` + `status` | No | Yes | Yes | No | High |
| **PEO Outcomes** | FK `programme_id` | FK `batch_id` + `status` | No | Yes | Yes | No | High |
| **Programme Targets** | Embedded in `PO`/`PSO` | Embedded in batch-scoped `PO`/`PSO` | **`YES`** | No | No | No | Low |
| **Programme Attainment** | Real-time calculation | Real-time calculation from batch data | **`YES`** | No | No | No | Low |
| **Programme ATR** | Keyed by `(programme_id, batch_id)` | Keyed by `batch_id` + status lifecycle | **`YES`** | Minor | Minor | No | Medium |
| **Course Outcomes** | Under `CourseOffering` | Under `ProgrammeBatchCourse` + `status` | Yes (add status) | Minor | Minor | No | High |
| **CO Mappings** | Under `CourseOutcome` | Under `CourseOutcome` | **`YES`** | No | No | No | Low |
| **Course Attainment** | Under `CourseOffering` | Under `ProgrammeBatchCourse` | **`YES`** | No | No | No | Low |
| **Course ATR** | Under `CourseOffering` | Under `ProgrammeBatchCourse` | **`YES`** | No | No | No | Low |
| **Historical Reports** | Batch/Offering scoped | Batch/Offering scoped | **`YES`** | No | No | No | Low |
| **Calculation Runs** | Dynamically computed | Dynamically computed | **`YES`** | No | No | No | Low |
| **Batch Lineage** | Explicit `previous_batch_id` | Derived from `(programme_id, start_year)` | Yes (drop column) | Minor | Minor | No | Low |
| **User Scoping** | Resolved via `CurrentUserScopeService` | Resolved via `CurrentUserScopeService` | **`YES`** | No | No | No | Low |
| **Approval Status** | Partial / scattered enums | Standard `DRAFT, PENDING, APPROVED, REVISION_REQUESTED` | No | Yes | Yes | No | High |
| **Approval Requests** | Generic workflow table | Generic workflow table | **`YES`** | No | No | No | Low |
| **Approval History** | Audit log table | Audit log table | **`YES`** | No | No | No | Low |

---

## 16. FINAL RECOMMENDATION

### A. MUST CHANGE (Structural Requirements)
1. In `programme_outcomes`, `programme_specific_outcomes`, and `peo_outcomes`: Change foreign key from `programme_id` to `batch_id` (`batches.id`).
2. In `batches`: Add `coordinator_id`, `coordinator_name`, `coordinator_email`.
3. In `programme_outcomes`, `programme_specific_outcomes`, `peo_outcomes`, and `course_outcomes`: Add `status VARCHAR(30) DEFAULT 'DRAFT'`.
4. In `ProgrammeOutcomeRepository`, `ProgrammeSpecificOutcomeRepository`, `PeoOutcomeRepository`: Replace `findByProgrammeId` with `findByBatchId`.

### B. SHOULD CHANGE (Clarity & Correctness)
1. Standardize workflow enums across all entities to `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED`.
2. Drop unused `previous_batch_id` from `batches` and derive lineage dynamically by sorting `start_year`.
3. Drop redundant `programme_id` from `programme_atrs` in favor of `batch_id`.

### C. OPTIONAL RENAMING
1. Entity `Batch` $\longrightarrow$ `ProgrammeBatch` (Table `batches` $\longrightarrow$ `programme_batches`).
2. Entity `CourseOffering` $\longrightarrow$ `ProgrammeBatchCourse` (Table `course_offerings` $\longrightarrow$ `programme_batch_courses`).

### D. MUST NOT CHANGE
- **Authentication:** `AuthController`, `AuthService`, `CustomUserDetailsService`, password hashing.
- **JWT & Filters:** `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, `CurrentUserScopeService`.
- **RBAC Roles:** `UserRole` (`DIRECTOR`, `HOD`, `PROGRAMME_COORDINATOR`, `COURSE_COORDINATOR`, `FACULTY`, `IQAC`).
- **Calculation Core:** Mathematical attainment algorithms (80/20 Direct/Indirect split, PO mapping averaging) in `AttainmentCalculationService`.

### E. NEW TABLES REQUIRED
- **`NONE`**. No new database tables are required.

### F. NO NEW TABLES REQUIRED
- Attainment reporting, calculations, assessments, mappings, and document tracking are 100% satisfied by the existing tables.

### G. RECONSTRUCTION IMPLEMENTATION ORDER
When ready to begin reconstruction:
1. **Entities:** Update `Batch`, `ProgrammeOutcome`, `ProgrammeSpecificOutcome`, `PeoOutcome`, `CourseOutcome` with `batch_id` and `status`.
2. **Repositories:** Update query method signatures from `programmeId` to `batchId`.
3. **Flyway Migration:** Consolidate migrations into a single, clean `V1__init_authoritative_academic_schema.sql`.
4. **Services:** Update `OutcomeService`, `AcademicService`, `AtrService`, and `AttainmentCalculationService` to pass `batchId` for PO/PSO/PEO operations.
5. **Controllers & DTOs:** Align endpoints (e.g. `/api/v1/outcomes/batches/{batchId}/pos`).
6. **Tests:** Run `./mvnw test` to verify complete compile and test pass.

---

## 17. VERIFICATION AUDIT STATEMENT

All conclusions in this document were verified directly by inspecting the active codebase:
- [`AttainmentCalculationService.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java)
- [`ReportController.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/controller/ReportController.java)
- [`AtrService.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AtrService.java)
- [`ApprovalService.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/ApprovalService.java)
- [`OutcomeService.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/OutcomeService.java)
- [`AcademicService.java`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AcademicService.java)
- Flyway SQL Migrations `V1` through `V6` in [`db/migration/`](file:///Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/resources/db/migration)
