# PHASE 3 — JPA ENTITY RECONSTRUCTION REPORT
## JPA Entity Model Alignment, Domain Renaming & Schema Mapping

**Project:** DYPIU NBA Attainment Backend (`obe-backend`)  
**Phase:** Phase 3 — JPA Entity Reconstruction + Domain Renaming  
**Date:** August 21, 2026  
**Status:** COMPLETE — JPA Entities Reconstructed and Mapped to Phase 2 Schema  

---

## 1. ENTITIES RENAMED

| OLD Class Name | NEW Class Name | Target Table Name | REASON |
| :--- | :--- | :--- | :--- |
| `Programme.java` | **`MasterProgramme.java`** | `master_programmes` | Represents the permanent master degree programme definition. |
| `Batch.java` | **`ProgrammeBatch.java`** | `programme_batches` | Represents the student cohort instance under a Master Programme. |
| `Course.java` | **`MasterCourse.java`** | `master_courses` | Represents reusable master course catalog data. |
| `CourseOffering.java` | **`ProgrammeBatchCourse.java`** | `programme_batch_courses` | Represents the cohort-specific course instance (Master Course + Programme Batch + Semester). |

---

## 2. ENTITIES MODIFIED

| Entity Class | Primary Modifications |
| :--- | :--- |
| `MasterProgramme.java` | Mapped to `master_programmes`. Removed obsolete coordinator fields (`coordinator`, `coordinatorEmail`). |
| `ProgrammeBatch.java` | Mapped to `programme_batches`. Mapped `masterProgrammeId` (`master_programme_id`). Added `coordinatorId` (`Long`), `coordinatorName`, `coordinatorEmail`. Removed `previousBatchId` and `programmeId`. |
| `MasterCourse.java` | Mapped to `master_courses`. Mapped `masterProgrammeId` (`master_programme_id`). |
| `ProgrammeBatchCourse.java`| Mapped to `programme_batch_courses`. Mapped `masterCourseId` (`master_course_id`) and `programmeBatchId` (`programme_batch_id`). |
| `ProgrammeOutcome.java` | Mapped to `programme_outcomes`. Changed parent scoping from `programmeId` to `programmeBatchId` (`programme_batch_id`). Added workflow `status` (`ApprovalStatus.DRAFT`). |
| `ProgrammeSpecificOutcome.java`| Mapped to `programme_specific_outcomes`. Changed parent scoping from `programmeId` to `programmeBatchId` (`programme_batch_id`). Added workflow `status` (`ApprovalStatus.DRAFT`). |
| `PeoOutcome.java` | Mapped to `peo_outcomes`. Changed parent scoping from `programmeId` to `programmeBatchId` (`programme_batch_id`). Added workflow `status` (`ApprovalStatus.DRAFT`). |
| `CourseOutcome.java` | Mapped to `course_outcomes`. Changed parent scoping from `courseOfferingId` to `programmeBatchCourseId` (`programme_batch_course_id`). Added workflow `status` (`ApprovalStatus.DRAFT`). |
| `ProgrammeAtr.java` | Mapped to `programme_atrs`. Scoped exclusively to `programmeBatchId` (`programme_batch_id`). Removed `programmeId`. |
| `CourseAtr.java` | Mapped to `course_atrs`. Scoped to `programmeBatchCourseId` (`programme_batch_course_id`). |
| `AttainmentConfiguration.java`| Mapped to `attainment_configurations`. Scoped to `programmeBatchCourseId` (`programme_batch_course_id`). |
| `StudentCoMark.java` | Mapped to `student_co_marks`. Scoped to `programmeBatchCourseId` (`programme_batch_course_id`). |
| `CourseMappingKeyword.java` | Mapped to `course_mapping_keywords`. Scoped to `programmeBatchCourseId` (`programme_batch_course_id`). |
| `UploadedDocument.java` | Mapped to `uploaded_documents`. Mapped `programmeBatchId` and `programmeBatchCourseId`. |
| `ApprovalRequest.java` | Mapped to `approval_requests`. Mapped `masterProgrammeId`, `programmeBatchId`, `masterCourseId`, `programmeBatchCourseId`. |
| `Student.java` | Mapped to `students`. Mapped `programmeBatchId` (`programme_batch_id`). |
| `ProgrammeCoordinatorSetupProgress.java` | Mapped to `pc_setup_progress`. Mapped `programmeBatchId` (`programme_batch_id`). |
| `CourseCoordinatorSetupProgress.java` | Mapped to `cc_setup_progress`. Mapped `programmeBatchCourseId` (`programme_batch_course_id`). |

---

## 3. FIELDS RENAMED

| Entity | OLD Field Name | NEW Field Name | Database Column Name | REASON |
| :--- | :--- | :--- | :--- | :--- |
| `ProgrammeBatch` | `programmeId` | `masterProgrammeId` | `master_programme_id` | Aligns with renamed `MasterProgramme` entity. |
| `MasterCourse` | `programmeId` | `masterProgrammeId` | `master_programme_id` | Aligns with renamed `MasterProgramme` entity. |
| `ProgrammeBatchCourse` | `batchId` | `programmeBatchId` | `programme_batch_id` | Aligns with renamed `ProgrammeBatch` entity. |
| `ProgrammeBatchCourse` | `courseId` | `masterCourseId` | `master_course_id` | Aligns with renamed `MasterCourse` entity. |
| `ProgrammeOutcome` | `programmeId` | `programmeBatchId` | `programme_batch_id` | Scopes PO to Programme Batch. |
| `ProgrammeSpecificOutcome` | `programmeId` | `programmeBatchId` | `programme_batch_id` | Scopes PSO to Programme Batch. |
| `PeoOutcome` | `programmeId` | `programmeBatchId` | `programme_batch_id` | Scopes PEO to Programme Batch. |
| `CourseOutcome` | `courseOfferingId` | `programmeBatchCourseId` | `programme_batch_course_id` | Scopes CO to Programme Batch Course. |
| `ProgrammeAtr` | `batchId` | `programmeBatchId` | `programme_batch_id` | Scopes ATR uniquely to Programme Batch. |
| `CourseAtr` | `courseOfferingId` | `programmeBatchCourseId` | `programme_batch_course_id` | Scopes Course ATR to Programme Batch Course. |
| `AttainmentConfiguration` | `courseOfferingId` | `programmeBatchCourseId` | `programme_batch_course_id` | Scopes Attainment Config to Programme Batch Course. |
| `StudentCoMark` | `courseOfferingId` | `programmeBatchCourseId` | `programme_batch_course_id` | Scopes Marks to Programme Batch Course. |
| `CourseMappingKeyword` | `courseOfferingId` | `programmeBatchCourseId` | `programme_batch_course_id` | Scopes Mapping Keywords to Programme Batch Course. |
| `UploadedDocument` | `batchId` / `courseOfferingId` | `programmeBatchId` / `programmeBatchCourseId` | `programme_batch_id` / `programme_batch_course_id` | Aligns document tracking. |
| `ApprovalRequest` | `programmeId` / `batchId` / `courseId` / `courseOfferingId` | `masterProgrammeId` / `programmeBatchId` / `masterCourseId` / `programmeBatchCourseId` | `master_programme_id` / `programme_batch_id` / `master_course_id` / `programme_batch_course_id` | Aligns workflow tracker. |
| `Student` | `batchId` | `programmeBatchId` | `programme_batch_id` | Scopes student roster to Programme Batch. |
| `ProgrammeCoordinatorSetupProgress` | `batchId` | `programmeBatchId` | `programme_batch_id` | Scopes setup progress to Programme Batch. |
| `CourseCoordinatorSetupProgress` | `courseOfferingId` | `programmeBatchCourseId` | `programme_batch_course_id` | Scopes setup progress to Programme Batch Course. |

---

## 4. FIELDS ADDED

| Entity | Added Field Name | Java Type | Database Column | REASON |
| :--- | :--- | :--- | :--- | :--- |
| `ProgrammeBatch` | `coordinatorId` | `Long` | `coordinator_id` | Matches `User.id` type (`BIGINT`) for Programme Coordinator assignment. |
| `ProgrammeBatch` | `coordinatorName` | `String` | `coordinator_name` | Full name cache for display. |
| `ProgrammeBatch` | `coordinatorEmail`| `String` | `coordinator_email` | Email identifier for authentication scoping and notifications. |
| `ProgrammeOutcome` | `status` | `ApprovalStatus` | `status` | 4-state workflow lifecycle (`DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED`). |
| `ProgrammeSpecificOutcome`| `status` | `ApprovalStatus` | `status` | 4-state workflow lifecycle. |
| `PeoOutcome` | `status` | `ApprovalStatus` | `status` | 4-state workflow lifecycle. |
| `CourseOutcome` | `status` | `ApprovalStatus` | `status` | 4-state workflow lifecycle. |

---

## 5. FIELDS REMOVED

| Entity | Removed Field Name | REASON |
| :--- | :--- | :--- |
| `MasterProgramme` | `coordinator` | Coordinator assignment is batch-specific and now lives on `ProgrammeBatch`. |
| `MasterProgramme` | `coordinatorEmail` | Coordinator assignment is batch-specific and now lives on `ProgrammeBatch`. |
| `ProgrammeBatch` | `previousBatchId` | Unused in business logic; batch lineage is dynamically derived from `startYear`. |
| `ProgrammeAtr` | `programmeId` | Redundant; `programmeBatchId` uniquely identifies the cohort instance. |

---

## 6. RELATIONSHIPS & CONSTRAINTS ALIGNMENT

```
[ master_programmes ]
      │
      ├──< (1:N) >── [ master_courses ]
      │                  master_programme_id  ──FK──>  master_programmes.id
      │
      └──< (1:N) >── [ programme_batches ]
                         master_programme_id  ──FK──>  master_programmes.id
                         coordinator_id        ──FK──>  users.id
                         │
                         ├──< (1:N) >── [ programme_outcomes ]
                         │                  programme_batch_id  ──FK──>  programme_batches.id
                         │
                         ├──< (1:N) >── [ programme_specific_outcomes ]
                         │                  programme_batch_id  ──FK──>  programme_batches.id
                         │
                         ├──< (1:N) >── [ peo_outcomes ]
                         │                  programme_batch_id  ──FK──>  programme_batches.id
                         │
                         ├──< (1:1) >── [ programme_atrs ]
                         │                  programme_batch_id  ──FK──>  programme_batches.id (UNIQUE)
                         │
                         └──< (1:N) >── [ programme_batch_courses ]
                                            programme_batch_id  ──FK──>  programme_batches.id
                                            master_course_id    ──FK──>  master_courses.id
                                            course_coordinator_id ──FK──>  users.id
                                            │
                                            ├──< (1:N) >── [ course_outcomes ]
                                            │                  programme_batch_course_id  ──FK──>  programme_batch_courses.id
                                            │
                                            ├──< (1:1) >── [ attainment_configurations ]
                                            │                  programme_batch_course_id  ──FK──>  programme_batch_courses.id (UNIQUE)
                                            │
                                            ├──< (1:N) >── [ student_co_marks ]
                                            │                  programme_batch_course_id  ──FK──>  programme_batch_courses.id
                                            │
                                            └──< (1:N) >── [ course_atrs ]
                                                               programme_batch_course_id  ──FK──>  programme_batch_courses.id
```

---

## 7. STATUS ENUM IMPLEMENTATION

- Reused existing shared enum: `com.dypiu.nba.entity.ApprovalStatus`
- Supported values: `DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED`
- Annotated with `@Enumerated(EnumType.STRING)` on `ProgrammeOutcome`, `ProgrammeSpecificOutcome`, `PeoOutcome`, and `CourseOutcome`.

---

## 8. EXISTING BEHAVIOR DELIBERATELY PRESERVED

- **Lombok Conventions:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` across all entity classes.
- **Transients & Calculation Helpers:** Kept helper methods (`getTarget()`, `setTarget()`, `getDirectLevels()`, `getIndirectLevels()`, `getType()`, `setType()`).
- **Compatibility Methods:** Included non-breaking getter/setter aliases (e.g. `getProgrammeId()`, `getCourseId()`, `getBatchId()`, `getCourseOfferingId()`) to provide smooth interop during subsequent service refactoring.

---

## 9. SECURITY & AUTHENTICATION CONFIRMATION

> [!IMPORTANT]
> **STRICT SECURITY PRESERVATION:**
> Authentication, authorization, JWT token handling, Spring Security filter chains, `User.java`, `UserRepository.java`, `UserRole.java`, `CurrentUserScope.java`, and `CurrentUserScopeService.java` were **NOT MODIFIED**.

---

## 10. COMPILATION RESULT & DOWNSTREAM SCOPE

- **Entity Layer:** 100% syntactically correct and fully aligned with Phase 2 database DDL.
- **Downstream Compilation Status:** As expected, `./mvnw test-compile` reports missing symbols in repositories (`ProgrammeRepository`, `BatchRepository`, `CourseRepository`, `CourseOfferingRepository`), services (`AcademicService`, `OutcomeService`, `AttainmentReportExportService`), and controllers (`ReportController`, `OutcomeController`) because they still reference old entity names.
- **Phase 4 Action:** Update Repositories (`MasterProgrammeRepository`, `ProgrammeBatchRepository`, `MasterCourseRepository`, `ProgrammeBatchCourseRepository`, `ProgrammeOutcomeRepository` with `findByProgrammeBatchId`).
- **Phase 5 Action:** Update Services, DTOs, Controllers, and API mappings.

---

## 11. MODIFIED FILES LIST

### Created Entities (Renamed)
- `obe-backend/src/main/java/com/dypiu/nba/entity/MasterProgramme.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeBatch.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/MasterCourse.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeBatchCourse.java`

### Deleted Legacy Entities
- `obe-backend/src/main/java/com/dypiu/nba/entity/Programme.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/Batch.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/Course.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/CourseOffering.java`

### Updated Entities
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeSpecificOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/PeoOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/CourseOutcome.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeAtr.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/CourseAtr.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/AttainmentConfiguration.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/StudentCoMark.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/CourseMappingKeyword.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/UploadedDocument.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ApprovalRequest.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/Student.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/ProgrammeCoordinatorSetupProgress.java`
- `obe-backend/src/main/java/com/dypiu/nba/entity/CourseCoordinatorSetupProgress.java`
