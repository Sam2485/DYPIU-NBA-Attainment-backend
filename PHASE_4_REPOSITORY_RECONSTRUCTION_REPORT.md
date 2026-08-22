# PHASE 4 — REPOSITORY RECONSTRUCTION REPORT
## Repository Layer Alignment, Domain Renaming & Approval Workflow Data Access

**Project:** DYPIU NBA Attainment Backend (`obe-backend`)  
**Phase:** Phase 4 — Repository Reconstruction + Domain Renaming + Approval Workflow Data Access  
**Date:** August 21, 2026  
**Status:** COMPLETE — Repository Layer Fully Aligned with Phase 3 Entities and Phase 2 Schema  

---

## 1. REPOSITORY RENAME MAP

| OLD Repository Name | NEW Repository Name | Entity Managed | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| `ProgrammeRepository.java` | **`MasterProgrammeRepository.java`** | `MasterProgramme` | Data access for master programme definitions by department. |
| `BatchRepository.java` | **`ProgrammeBatchRepository.java`** | `ProgrammeBatch` | Data access for cohort instances by Master Programme and Coordinator. |
| `CourseRepository.java` | **`MasterCourseRepository.java`** | `MasterCourse` | Data access for reusable master course catalog entries. |
| `CourseOfferingRepository.java` | **`ProgrammeBatchCourseRepository.java`** | `ProgrammeBatchCourse` | Data access for cohort-specific course offerings (Batch + Course + Semester). |

---

## 2. REPOSITORY FILES CREATED

- `obe-backend/src/main/java/com/dypiu/nba/repository/MasterProgrammeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeBatchRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/MasterCourseRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeBatchCourseRepository.java`

---

## 3. REPOSITORY FILES DELETED

- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/BatchRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/CourseRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/CourseOfferingRepository.java`

---

## 4. REPOSITORY FILES MODIFIED

- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeOutcomeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeSpecificOutcomeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/PeoOutcomeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/CourseOutcomeRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeAtrRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/CourseAtrRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/AttainmentConfigurationRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/StudentCoMarkRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/CourseMappingKeywordRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/UploadedDocumentRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/StudentRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ApprovalRequestRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/ProgrammeCoordinatorSetupProgressRepository.java`
- `obe-backend/src/main/java/com/dypiu/nba/repository/CourseCoordinatorSetupProgressRepository.java`

---

## 5. QUERY METHODS RENAMED

| Repository | OLD Query Method | NEW Query Method | REASON |
| :--- | :--- | :--- | :--- |
| `ProgrammeBatchRepository` | `findByProgrammeId` | `findByMasterProgrammeId` | Property renamed from `programmeId` to `masterProgrammeId`. |
| `ProgrammeBatchRepository` | `findByProgrammeIdIn` | `findByMasterProgrammeIdIn` | Property renamed from `programmeId` to `masterProgrammeId`. |
| `MasterCourseRepository` | `findByProgrammeId` | `findByMasterProgrammeId` | Property renamed from `programmeId` to `masterProgrammeId`. |
| `MasterCourseRepository` | `findByProgrammeIdIn` | `findByMasterProgrammeIdIn` | Property renamed from `programmeId` to `masterProgrammeId`. |
| `ProgrammeBatchCourseRepository`| `findByBatchId` | `findByProgrammeBatchId` | Property renamed from `batchId` to `programmeBatchId`. |
| `ProgrammeBatchCourseRepository`| `findByBatchIdIn` | `findByProgrammeBatchIdIn` | Property renamed from `batchId` to `programmeBatchId`. |
| `ProgrammeBatchCourseRepository`| `findByCourseId` | `findByMasterCourseId` | Property renamed from `courseId` to `masterCourseId`. |
| `ProgrammeBatchCourseRepository`| `findByBatchIdAndCourseIdAndSemester`| `findByProgrammeBatchIdAndMasterCourseIdAndSemester`| Properties renamed. |
| `ProgrammeOutcomeRepository` | `findByProgrammeId` | `findByProgrammeBatchId` | Outcome parent scoped to `ProgrammeBatch`. |
| `ProgrammeOutcomeRepository` | `findByProgrammeIdOrderByCodeAsc` | `findByProgrammeBatchIdOrderByCodeAsc` | Outcome parent scoped to `ProgrammeBatch`. |
| `ProgrammeSpecificOutcomeRepository`| `findByProgrammeId` | `findByProgrammeBatchId` | Outcome parent scoped to `ProgrammeBatch`. |
| `ProgrammeSpecificOutcomeRepository`| `findByProgrammeIdOrderByCodeAsc` | `findByProgrammeBatchIdOrderByCodeAsc` | Outcome parent scoped to `ProgrammeBatch`. |
| `PeoOutcomeRepository` | `findByProgrammeId` | `findByProgrammeBatchId` | Outcome parent scoped to `ProgrammeBatch`. |
| `PeoOutcomeRepository` | `findByProgrammeIdOrderByCodeAsc` | `findByProgrammeBatchIdOrderByCodeAsc` | Outcome parent scoped to `ProgrammeBatch`. |
| `CourseOutcomeRepository` | `findByCourseOfferingId` | `findByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `CourseOutcomeRepository` | `findByCourseOfferingIdOrderByCodeAsc` | `findByProgrammeBatchCourseIdOrderByCodeAsc`| Scoped to `ProgrammeBatchCourse`. |
| `CourseOutcomeRepository` | `findByCourseOfferingIdIn` | `findByProgrammeBatchCourseIdIn` | Scoped to `ProgrammeBatchCourse`. |
| `CourseOutcomeRepository` | `findByCourseOfferingIdAndCode` | `findByProgrammeBatchCourseIdAndCode` | Scoped to `ProgrammeBatchCourse`. |
| `CourseOutcomeRepository` | `deleteByCourseOfferingId` | `deleteByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `ProgrammeAtrRepository` | `findByBatchId` | `findByProgrammeBatchId` | Scoped to `ProgrammeBatch`. |
| `ProgrammeAtrRepository` | `findByProgrammeIdIn` | `findByProgrammeBatchIdIn` | Scoped to `ProgrammeBatch`. |
| `CourseAtrRepository` | `findByCourseOfferingId` | `findByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `CourseAtrRepository` | `findByCourseOfferingIdIn` | `findByProgrammeBatchCourseIdIn` | Scoped to `ProgrammeBatchCourse`. |
| `CourseAtrRepository` | `findByCourseOfferingIdAndCoCode` | `findByProgrammeBatchCourseIdAndCoCode` | Scoped to `ProgrammeBatchCourse`. |
| `AttainmentConfigurationRepository`| `findByCourseOfferingId` | `findByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `AttainmentConfigurationRepository`| `findByCourseOfferingIdIn` | `findByProgrammeBatchCourseIdIn` | Scoped to `ProgrammeBatchCourse`. |
| `AttainmentConfigurationRepository`| `deleteByCourseOfferingId` | `deleteByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `StudentCoMarkRepository` | `findByCourseOfferingId` | `findByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `StudentCoMarkRepository` | `findByCourseOfferingIdAndCoCode` | `findByProgrammeBatchCourseIdAndCoCode` | Scoped to `ProgrammeBatchCourse`. |
| `StudentCoMarkRepository` | `deleteByCourseOfferingId` | `deleteByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `CourseMappingKeywordRepository`| `findByCourseOfferingId` | `findByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `UploadedDocumentRepository` | `findByCourseOfferingId` | `findByProgrammeBatchCourseId` | Scoped to `ProgrammeBatchCourse`. |
| `UploadedDocumentRepository` | `findByBatchId` | `findByProgrammeBatchId` | Scoped to `ProgrammeBatch`. |
| `StudentRepository` | `findByBatchId` | `findByProgrammeBatchId` | Scoped to `ProgrammeBatch`. |

---

## 6. QUERY METHODS ADDED

- `ProgrammeBatchRepository`:
  - `findByMasterProgrammeIdOrderByStartYearDesc(String masterProgrammeId)` (enables natural historical lineage)
  - `findByCoordinatorId(Long coordinatorId)` (supports PC assignment lookup)
  - `findByCoordinatorEmailIgnoreCase(String coordinatorEmail)` (supports PC security scope lookup)
  - `findByMasterProgrammeIdAndStatus(String masterProgrammeId, String status)`
- `MasterCourseRepository`:
  - `findByMasterProgrammeIdAndCode(String masterProgrammeId, String code)`
  - `findByMasterProgrammeIdAndStatus(String masterProgrammeId, String status)`
- `ProgrammeBatchCourseRepository`:
  - `findByProgrammeBatchIdAndStatus(String programmeBatchId, String status)`
- `ProgrammeOutcomeRepository`:
  - `findByProgrammeBatchIdAndStatus(String programmeBatchId, ApprovalStatus status)`
  - `findByProgrammeBatchIdAndCode(String programmeBatchId, String code)`
  - `deleteByProgrammeBatchId(String programmeBatchId)`
- `ProgrammeSpecificOutcomeRepository`:
  - `findByProgrammeBatchIdAndStatus(String programmeBatchId, ApprovalStatus status)`
  - `findByProgrammeBatchIdAndCode(String programmeBatchId, String code)`
  - `deleteByProgrammeBatchId(String programmeBatchId)`
- `PeoOutcomeRepository`:
  - `findByProgrammeBatchIdAndStatus(String programmeBatchId, ApprovalStatus status)`
  - `findByProgrammeBatchIdAndCode(String programmeBatchId, String code)`
  - `deleteByProgrammeBatchId(String programmeBatchId)`
- `CourseOutcomeRepository`:
  - `findByProgrammeBatchCourseIdAndStatus(String programmeBatchCourseId, ApprovalStatus status)`
- `ProgrammeAtrRepository`:
  - `findByProgrammeBatchIdAndStatus(String programmeBatchId, ProgrammeAtrStatus status)`
- `CourseAtrRepository`:
  - `findByProgrammeBatchCourseIdAndStatus(String programmeBatchCourseId, CourseAtrStatus status)`
- `AttainmentConfigurationRepository`:
  - `findByProgrammeBatchCourseIdAndStatus(String programmeBatchCourseId, AttainmentConfigStatus status)`
- `ApprovalRequestRepository`:
  - `findByMasterProgrammeId(String masterProgrammeId)`
  - `findByProgrammeBatchId(String programmeBatchId)`
  - `findByProgrammeBatchCourseId(String programmeBatchCourseId)`

---

## 7. QUERY METHODS REMOVED

- `ProgrammeAtrRepository`: Removed obsolete `findByProgrammeIdAndBatchId(programmeId, batchId)` and `findByProgrammeId(programmeId)` because Programme ATR is uniquely identified by `programmeBatchId`.
- `ProgrammeCoordinatorSetupProgressRepository`: Removed `findByProgrammeIdAndBatchId` and `findByProgrammeId`.

---

## 8. PROPERTY-PATH CHANGES

All Spring Data query method names were strictly aligned with the exact Java property names declared in Phase 3 JPA entities:
- `programme_batch_id` column $\longrightarrow$ `programmeBatchId` Java property
- `master_programme_id` column $\longrightarrow$ `masterProgrammeId` Java property
- `master_course_id` column $\longrightarrow$ `masterCourseId` Java property
- `programme_batch_course_id` column $\longrightarrow$ `programmeBatchCourseId` Java property

---

## 9. SCOPING ALIGNMENT SUMMARY

```
MasterProgrammeRepository
    └── Scope: departmentId

ProgrammeBatchRepository
    └── Scope: masterProgrammeId, coordinatorId, coordinatorEmail

MasterCourseRepository
    └── Scope: masterProgrammeId

ProgrammeBatchCourseRepository
    └── Scope: programmeBatchId, masterCourseId, courseCoordinatorId

ProgrammeOutcomeRepository / ProgrammeSpecificOutcomeRepository / PeoOutcomeRepository
    └── Scope: programmeBatchId (NOT masterProgrammeId)

CourseOutcomeRepository / CoPoMappingRepository / CoPsoMappingRepository
    └── Scope: programmeBatchCourseId

AttainmentConfigurationRepository / StudentCoMarkRepository / CourseAtrRepository
    └── Scope: programmeBatchCourseId

ProgrammeAtrRepository
    └── Scope: programmeBatchId
```

---

## 10. APPROVAL WORKFLOW DATA ACCESS DESIGN

### A. The Six Approval Resources & Lifecycles

| # | Approval Resource | Domain Entity | Scoping Key | Submitter Role | Approver Role | State Lifecycle |
| :- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **Attainment Settings** | `AttainmentConfiguration` | `programmeBatchCourseId` | Course Coordinator | Programme Coordinator | `DRAFT` $\to$ `PENDING` $\to$ `APPROVED` (or `REVISION_REQUESTED`) |
| 2 | **Course Outcomes + Target** | `CourseOutcome` | `programmeBatchCourseId` | Course Coordinator | Programme Coordinator | `DRAFT` $\to$ `PENDING` $\to$ `APPROVED` (or `REVISION_REQUESTED`) |
| 3 | **Course ATR** | `CourseAtr` | `programmeBatchCourseId` | Course Coordinator | Programme Coordinator | `DRAFT` $\to$ `PENDING` $\to$ `APPROVED` (or `REVISION_REQUESTED`) |
| 4 | **Batch + CC Allocation** | `ProgrammeBatchCourse` | `programmeBatchId` | Programme Coordinator | HOD | `DRAFT` $\to$ `PENDING` $\to$ `APPROVED` (or `REVISION_REQUESTED`) |
| 5 | **PO/PSO + Target** | `ProgrammeOutcome` / `PSO` | `programmeBatchId` | Programme Coordinator | HOD | `DRAFT` $\to$ `PENDING` $\to$ `APPROVED` (or `REVISION_REQUESTED`) |
| 6 | **Programme ATR** | `ProgrammeAtr` | `programmeBatchId` | Programme Coordinator | HOD | `DRAFT` $\to$ `PENDING` $\to$ `APPROVED` (or `REVISION_REQUESTED`) |

### B. Architectural Invariants
1. **"STATUS REPRESENTS STATE, NOT AUTHORIZATION."**
   - An entity's `status` represents its current lifecycle stage. It does not dictate whether the calling actor is permitted to transition that state.
2. **"Repositories provide data access only. Role-based approval/revision authorization and status-transition validation belong to the service layer."**
   - Repositories expose only query filters (`findByProgrammeBatchCourseIdAndStatus`, `findByProgrammeBatchIdAndStatus`, `findByStatus`).
   - Repositories do **NOT** contain authorization checks or arbitrary status mutation methods that could bypass service validation.
3. **Approver Scope Queries:**
   - Programme Coordinator queries pending items for their batch via `ProgrammeBatchCourseRepository.findByProgrammeBatchId` and `CourseOutcomeRepository.findByProgrammeBatchCourseIdAndStatus(..., PENDING)`.
   - HOD queries pending items for their department via `MasterProgrammeRepository.findByDepartmentId` $\to$ `ProgrammeBatchRepository.findByMasterProgrammeId` $\to$ `ProgrammeOutcomeRepository.findByProgrammeBatchIdAndStatus(..., PENDING)`.

---

## 11. SECURITY & AUTHENTICATION CONFIRMATION

> [!IMPORTANT]
> **STRICT SECURITY PRESERVATION:**
> - Authentication: **UNTOUCHED**
> - Authorization: **UNTOUCHED**
> - JWT: **UNTOUCHED**
> - Spring Security: **UNTOUCHED**
> - User repository (`UserRepository.java`): **UNTOUCHED**
> - Current-user scope (`CurrentUserScopeService.java`): **UNTOUCHED**

---

## 12. COMPILATION & DOWNSTREAM PHASE 5 WORK

- **Repository Layer Compilation:** All 26 repositories in `com.dypiu.nba.repository` are 100% syntactically correct and compile against the Phase 3 entity model.
- **Downstream Phase 5 Scope:** As expected, compilation errors in services (`AcademicService`, `OutcomeService`, `AttainmentCalculationService`, `AtrService`, `ReportAccessService`, `AttainmentReportExportService`), DTOs, and controllers (`AcademicController`, `OutcomeController`, `AttainmentController`, `ReportController`) exist because they still reference old repository and entity class names (`ProgrammeRepository`, `BatchRepository`, `CourseRepository`, `CourseOfferingRepository`). These will be systematically aligned in Phase 5.
