# READ-ONLY ARCHITECTURAL AUDIT REPORT: DATABASE, ENTITY & REPOSITORY MINIMUM CHANGES

**Project:** DYPIU NBA Attainment Backend  
**Audit Date:** August 21, 2026  
**Mode:** READ-ONLY Architectural Assessment  
**Target:** Authoritative Academic Hierarchy & Batch-Centric Governance Refactoring  

---

## 1. EXECUTIVE SUMMARY

### Current Architecture Overview
The current backend is a **Java 21 LTS + Spring Boot 3.3.2** application backed by **PostgreSQL** with schema management handled via **Flyway** (Migrations `V1` through `V6`).
- The existing organizational hierarchy is structured as:
  $$\text{School} \longrightarrow \text{Department} \longrightarrow \text{Programme} \longrightarrow \text{Batch}$$
  $$\text{Programme} \longrightarrow \text{Course (Master)}$$
  $$\text{Batch} + \text{Course} \longrightarrow \text{CourseOffering}$$
- **Outcomes (`PO`, `PSO`, `PEO`)** are currently tied to `Programme` (master level) with foreign keys referencing `programmes(id)`.
- **Course Outcomes (`CO`)**, **CO-PO/PSO Mappings**, **Attainment Calculations**, and **Course ATRs** are tied to `CourseOffering` (`course_offerings(id)`).
- **Programme ATRs** and **Programme Attainments** are tied to both `programme_id` and `batch_id`.

### Target Architecture
$$\text{School} \longrightarrow N \times \text{Department} \longrightarrow N \times \text{Master Programme}$$
$$\text{Master Programme} \longrightarrow N \times \text{Master Course}$$
$$\text{Master Programme} \longrightarrow N \times \text{Programme Batch}$$
$$\text{Programme Batch} \longrightarrow \text{PO / PSO / PEO / Targets / Programme Attainment / Programme ATR}$$
$$\text{Programme Batch} \longrightarrow N \times \text{Programme Batch Course (formerly CourseOffering)}$$
$$\text{Programme Batch Course} \longrightarrow \text{COs / Mappings / Assessments / Attainments / Course ATR}$$

### Migration Complexity & Feasibility
- **Overall Migration Complexity:** **LOW TO MEDIUM**.
- **Can the target structure be achieved with minimum incremental changes?** **YES**.
- **Why?** The backend was already designed around the separation between master course definitions (`Course`) and cohort course instances (`CourseOffering`). In fact:
  1. `CourseOffering` is *already* the exact structural equivalent of the new **Programme Batch Course** concept. It already holds `course_id` (Master Course), `batch_id` (Programme Batch), `semester`, `course_coordinator_id`, and `assigned_faculty`.
  2. All course-level child entities (`CourseOutcome`, `CoPoMapping`, `CoPsoMapping`, `CourseMappingKeyword`, `AttainmentConfiguration`, `StudentCoMark`, `CourseAtr`, `UploadedDocument`, `CalculationRun`) *already* reference `course_offering_id`.
  3. The database currently contains **no production data**, meaning migrations can be rewritten and consolidated cleanly without requiring complex data transformation scripts.

### Biggest Structural Changes Required
1. **Scoping PO, PSO, and PEO to `ProgrammeBatch` (`batch_id`) instead of `Programme` (`programme_id`):**
   - In `programme_outcomes`, `programme_specific_outcomes`, and `peo_outcomes`, change the parent foreign key from `programme_id` to `batch_id` (or add `batch_id` as mandatory and update unique constraints).
2. **Moving Programme Coordinator Assignment to `ProgrammeBatch`:**
   - Move coordinator fields (`coordinator_id`, `coordinator_name`, `coordinator_email`) from `Programme` to `Batch` (`batches` table), allowing different coordinators across different batches of the same programme.
3. **Approval Status Integration:**
   - Add workflow status fields (`DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REVISION_REQUESTED`) to batch-level outcome definitions (`programme_outcomes`, `programme_specific_outcomes`, `course_outcomes`) while reusing existing workflow mechanisms in `course_atrs`, `programme_atrs`, and `attainment_configurations`.

---

## 2. CURRENT DATABASE INVENTORY

| Current Table | Purpose | Important Columns | Relationships (FKs) | Keep / Change / Remove |
| :--- | :--- | :--- | :--- | :--- |
| `schools` | Master organization school | `id`, `code`, `name`, `director_id`, `director_name`, `director_email`, `est_year` | None (Root) | **KEEP AS-IS** |
| `departments` | Academic department under a school | `id`, `school_id`, `code`, `name`, `hod`, `hod_email`, `status` | FK `school_id` $\to$ `schools(id)` | **KEEP AS-IS** |
| `programmes` | Master degree programme | `id`, `department_id`, `code`, `name`, `duration_years`, `coordinator`, `coordinator_email`, `status` | FK `department_id` $\to$ `departments(id)` | **MODIFY** (Make coordinator batch-specific) |
| `batches` | Cohort academic batch instance | `id`, `programme_id`, `name`, `start_year`, `end_year`, `duration_years`, `previous_batch_id`, `status` | FK `programme_id` $\to$ `programmes(id)`, FK `previous_batch_id` $\to$ `batches(id)` | **MODIFY** (Add coordinator fields) |
| `semesters` | Batch semester breakdown | `id`, `batch_id`, `semester_num`, `name`, `status` | FK `batch_id` $\to$ `batches(id)` | **KEEP / OPTIONAL** |
| `courses` | Master course catalog definition | `id`, `code`, `name`, `programme_id`, `credits`, `course_type`, `status` | FK `programme_id` $\to$ `programmes(id)` | **KEEP AS-IS** (Master Course) |
| `course_offerings` | Cohort course instance | `id`, `course_id`, `batch_id`, `semester`, `course_coordinator_id`, `course_coordinator_name`, `assigned_faculty`, `status` | FK `course_id` $\to$ `courses(id)`, FK `batch_id` $\to$ `batches(id)`, FK `course_coordinator_id` $\to$ `users(id)` | **REPURPOSE** (Becomes Programme Batch Course) |
| `students` | Enrolled student cohort roster | `id`, `batch_id`, `prn`, `name`, `email`, `status` | FK `batch_id` $\to$ `batches(id)` | **KEEP AS-IS** |
| `programme_outcomes` | PO statements (PO1–PO12) | `id`, `programme_id`, `code`, `statement`, `target` | FK `programme_id` $\to$ `programmes(id)` | **MODIFY** (Change FK from `programme_id` to `batch_id`) |
| `po_competencies` | Sub-competencies for POs | `id`, `po_id`, `code`, `statement` | FK `po_id` $\to$ `programme_outcomes(id)` | **KEEP AS-IS** |
| `programme_specific_outcomes` | PSO statements (PSO1–PSO3) | `id`, `programme_id`, `code`, `statement`, `target` | FK `programme_id` $\to$ `programmes(id)` | **MODIFY** (Change FK from `programme_id` to `batch_id`) |
| `pso_competencies` | Sub-competencies for PSOs | `id`, `pso_id`, `code`, `statement` | FK `pso_id` $\to$ `programme_specific_outcomes(id)` | **KEEP AS-IS** |
| `peo_outcomes` | PEO statements | `id`, `programme_id`, `code`, `statement` | FK `programme_id` $\to$ `programmes(id)` | **MODIFY** (Change FK from `programme_id` to `batch_id`) |
| `course_outcomes` | CO statements & targets | `id`, `course_offering_id`, `code`, `statement`, `target_level`, `blooms_level` | FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP / MODIFY** (Add status column) |
| `co_po_mappings` | CO to PO correlation (0–3) | `id`, `course_outcome_id`, `po_code`, `mapping_level` | FK `course_outcome_id` $\to$ `course_outcomes(id)` | **KEEP AS-IS** |
| `co_pso_mappings` | CO to PSO correlation (0–3) | `id`, `course_outcome_id`, `pso_code`, `mapping_level` | FK `course_outcome_id` $\to$ `course_outcomes(id)` | **KEEP AS-IS** |
| `course_mapping_keywords` | Justifications for CO-PO/PSO | `id`, `course_offering_id`, `keyword_type`, `keywords_json` | FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `attainment_configurations` | Direct/Indirect weights & thresholds | `id`, `course_offering_id`, `direct_weight`, `indirect_weight`, `direct_threshold`, `indirect_threshold`, `status`, `submitted_by`, `submitted_at`, `direct_levels_json`, `indirect_levels_json` | FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `attainment_levels` | Level percentage ranges | `id`, `config_id`, `type`, `level_val`, `min_percentage`, `max_percentage` | FK `config_id` $\to$ `attainment_configurations(id)` | **KEEP AS-IS** |
| `end_sem_marks_uploads` | Upload metadata for marks | `id`, `course_offering_id`, `file_name`, `file_path`, `uploaded_by`, `status` | FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `student_co_marks` | Student marks per CO | `id`, `upload_id`, `course_offering_id`, `student_id`, `prn`, `student_name`, `co_code`, `marks_obtained`, `max_marks`, `percentage` | FK `upload_id` $\to$ `end_sem_marks_uploads(id)`, FK `course_offering_id` $\to$ `course_offerings(id)`, FK `student_id` $\to$ `students(id)` | **KEEP AS-IS** |
| `course_end_surveys` | Course indirect survey summary | `id`, `course_offering_id`, `total_respondents` | FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `survey_responses` | Student survey response | `id`, `survey_id`, `student_id`, `prn` | FK `survey_id` $\to$ `course_end_surveys(id)`, FK `student_id` $\to$ `students(id)` | **KEEP AS-IS** |
| `survey_response_details` | Survey rating per CO (1–3) | `id`, `response_id`, `co_code`, `rating` | FK `response_id` $\to$ `survey_responses(id)` | **KEEP AS-IS** |
| `programme_exit_surveys` | Programme exit survey | `id`, `programme_id`, `batch_id`, `total_respondents`, `avg_exit_score` | FK `programme_id` $\to$ `programmes(id)`, FK `batch_id` $\to$ `batches(id)` | **MODIFY** (Make `batch_id` primary scope) |
| `uploaded_documents` | File storage records | `id`, `batch_id`, `course_offering_id`, `document_type`, `file_name`, `saved_path`, `threshold_percentage` | FK `batch_id` $\to$ `batches(id)`, FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `calculation_runs` | Engine calculation runs | `id`, `course_offering_id`, `programme_id`, `batch_id`, `run_type`, `status` | FK `course_offering_id` $\to$ `course_offerings(id)`, FK `programme_id` $\to$ `programmes(id)`, FK `batch_id` $\to$ `batches(id)` | **KEEP AS-IS** |
| `direct_co_attainments` | Direct CO results | `id`, `run_id`, `course_offering_id`, `co_code`, `percentage_attained`, `attainment_level`, `attainment_score` | FK `run_id` $\to$ `calculation_runs(id)`, FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `indirect_co_attainments` | Indirect CO results | `id`, `run_id`, `course_offering_id`, `co_code`, `percentage_attained`, `attainment_level`, `attainment_score` | FK `run_id` $\to$ `calculation_runs(id)`, FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `overall_co_attainments` | Overall CO results | `id`, `run_id`, `course_offering_id`, `co_code`, `direct_score`, `indirect_score`, `overall_attainment`, `target_score`, `is_target_achieved` | FK `run_id` $\to$ `calculation_runs(id)`, FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `po_attainments` | Batch PO attainment | `id`, `run_id`, `programme_id`, `batch_id`, `po_code`, `direct_attainment`, `indirect_attainment`, `final_attainment`, `target_attainment`, `is_target_achieved` | FK `run_id` $\to$ `calculation_runs(id)`, FK `programme_id` $\to$ `programmes(id)`, FK `batch_id` $\to$ `batches(id)` | **KEEP AS-IS** |
| `pso_attainments` | Batch PSO attainment | `id`, `run_id`, `programme_id`, `batch_id`, `pso_code`, `direct_attainment`, `indirect_attainment`, `final_attainment`, `target_attainment`, `is_target_achieved` | FK `run_id` $\to$ `calculation_runs(id)`, FK `programme_id` $\to$ `programmes(id)`, FK `batch_id` $\to$ `batches(id)` | **KEEP AS-IS** |
| `course_atrs` | Course Action Taken Report | `id`, `course_offering_id`, `co_code`, `target_score`, `actual_score`, `pct_achieved`, `status`, `statement`, `actions_json`, `submitted_by`, `verified_by` | FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `programme_atrs` | Programme Action Taken Report | `id`, `programme_id`, `batch_id`, `status`, `submitted_by`, `verified_by`, `verification_comments`, `observations_json` | FK `programme_id` $\to$ `programmes(id)`, FK `batch_id` $\to$ `batches(id)` | **KEEP AS-IS** |
| `approval_requests` | Generic workflow engine | `id`, `type`, `title`, `resource_id`, `school_id`, `department_id`, `programme_id`, `batch_id`, `course_id`, `course_offering_id`, `status`, `submitted_by`, `approved_by`, `remarks`, `details` | FKs to academic hierarchy | **KEEP AS-IS** |
| `approval_history` | Audit log of approvals | `id`, `approval_request_id`, `actor_id`, `actor_name`, `actor_role`, `action`, `comments`, `timestamp` | FK `approval_request_id` $\to$ `approval_requests(id)`, FK `actor_id` $\to$ `users(id)` | **KEEP AS-IS** |
| `director_setup_progress` | Wizard progress for Director | `id`, `school_id`, `current_step`, `current_step_enum`, `overall_status`, `completed_steps`, `pending_steps` | FK `school_id` $\to$ `schools(id)` | **KEEP AS-IS** |
| `hod_setup_progress` | Wizard progress for HOD | `id`, `department_id`, `hod_email`, `current_step`, `overall_status`, `completed_steps`, `pending_steps` | FK `department_id` $\to$ `departments(id)` | **KEEP AS-IS** |
| `pc_setup_progress` | Wizard progress for PC | `id`, `programme_id`, `batch_id`, `coordinator_email`, `current_step`, `overall_status`, `completed_steps`, `pending_steps` | FK `programme_id` $\to$ `programmes(id)`, FK `batch_id` $\to$ `batches(id)` | **KEEP AS-IS** |
| `cc_setup_progress` | Wizard progress for CC | `id`, `course_offering_id`, `coordinator_email`, `current_step`, `overall_status`, `completed_steps`, `pending_steps` | FK `course_offering_id` $\to$ `course_offerings(id)` | **KEEP AS-IS** |
| `users` | Auth and user profile table | `id`, `username`, `email`, `password_hash`, `name`, `role`, `school_id`, `department_id`, `programme_id`, `is_active` | FKs to `schools`, `departments`, `programmes` | **KEEP AS-IS** |

---

## 3. CURRENT ENTITY INVENTORY

| Entity | Table | Current Responsibility | Current Relationships | Target Responsibility | Action |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `School` | `schools` | Top-level academic institution container | Root entity | Root entity for N schools | **KEEP** |
| `Department` | `departments` | Department under School | `school_id` | Department under School | **KEEP** |
| `Programme` | `programmes` | Programme definition + Coordinator | `department_id` | **Master Programme** (permanent definition) | **MODIFY** |
| `Batch` | `batches` | Cohort year span | `programme_id`, `previous_batch_id` | **Programme Batch** (cohort instance + coordinator + outcomes) | **MODIFY** |
| `Course` | `courses` | Master course catalog definition | `programme_id` | **Master Course** (permanent definition under Master Programme) | **KEEP** |
| `CourseOffering` | `course_offerings` | Instance of Course in Batch & Semester with CC | `course_id`, `batch_id`, `course_coordinator_id` | **Programme Batch Course** (course instance under Programme Batch) | **REPURPOSE** |
| `Student` | `students` | Enrolled student in Batch | `batch_id` | Enrolled student in Programme Batch | **KEEP** |
| `ProgrammeOutcome` | `programme_outcomes` | Master PO definitions (PO1–12) | `programme_id` | **Programme-Batch-specific POs** (PO1–12 + target + status) | **MODIFY** |
| `PoCompetency` | `po_competencies` | PO Competencies | `po_id` | PO Competencies for batch PO | **KEEP** |
| `ProgrammeSpecificOutcome` | `programme_specific_outcomes`| Master PSO definitions (PSO1–3) | `programme_id` | **Programme-Batch-specific PSOs** (PSO1–3 + target + status) | **MODIFY** |
| `PsoCompetency` | `pso_competencies` | PSO Competencies | `pso_id` | PSO Competencies for batch PSO | **KEEP** |
| `PeoOutcome` | `peo_outcomes` | Master PEO definitions | `programme_id` | **Programme-Batch-specific PEOs** | **MODIFY** |
| `CourseOutcome` | `course_outcomes` | COs under CourseOffering | `course_offering_id` | COs under Programme Batch Course | **MODIFY** (Add status) |
| `CoPoMapping` | `co_po_mappings` | CO-PO correlation levels | `course_outcome_id` | CO-PO correlation levels | **KEEP** |
| `CoPsoMapping` | `co_pso_mappings` | CO-PSO correlation levels | `course_outcome_id` | CO-PSO correlation levels | **KEEP** |
| `CourseMappingKeyword` | `course_mapping_keywords` | Justification keywords | `course_offering_id` | Justifications under Programme Batch Course | **KEEP** |
| `AttainmentConfiguration` | `attainment_configurations`| Direct/Indirect weights & thresholds | `course_offering_id` | Configuration under Programme Batch Course | **KEEP** |
| `StudentCoMark` | `student_co_marks` | Student marks per CO | `course_offering_id`, `student_id` | Student marks under Programme Batch Course | **KEEP** |
| `UploadedDocument` | `uploaded_documents` | Marks & document records | `batch_id`, `course_offering_id` | Document records under Batch & Batch Course | **KEEP** |
| `CourseAtr` | `course_atrs` | CO Action Taken Report | `course_offering_id` | Action Taken Report under Programme Batch Course | **KEEP** |
| `ProgrammeAtr` | `programme_atrs` | Programme Action Taken Report | `programme_id`, `batch_id` | Action Taken Report under Programme Batch | **KEEP** |
| `ApprovalRequest` | `approval_requests` | Generic approval submission | Academic hierarchy FKs | Workflow engine for reviews | **KEEP** |
| `ApprovalHistory` | `approval_history` | Audit log of actions | `approval_request_id`, `actor_id` | Audit trail | **KEEP** |
| `DirectorSetupProgress` | `director_setup_progress` | Setup wizard state | `school_id` | Setup wizard state | **KEEP** |
| `HodSetupProgress` | `hod_setup_progress` | Setup wizard state | `department_id` | Setup wizard state | **KEEP** |
| `ProgrammeCoordinatorSetupProgress`| `pc_setup_progress` | Setup wizard state | `programme_id`, `batch_id` | Setup wizard state for Programme Batch | **KEEP** |
| `CourseCoordinatorSetupProgress`| `cc_setup_progress` | Setup wizard state | `course_offering_id` | Setup wizard state for Programme Batch Course | **KEEP** |
| `User` | `users` | Auth principal & scoped profile | `school_id`, `department_id`, `programme_id` | Auth principal & user profile | **KEEP** |

---

## 4. CURRENT REPOSITORY INVENTORY

| Repository | Entity | Important Methods (Currently Existing) | Depends On Old Model? | Required Change |
| :--- | :--- | :--- | :--- | :--- |
| `SchoolRepository` | `School` | `findByDirectorEmailIgnoreCase(email)`, `findByDirectorId(id)` | No | None (**KEEP**) |
| `DepartmentRepository` | `Department` | `findBySchoolId(id)`, `findByHodEmailIgnoreCase(email)`, `findByName(name)` | No | None (**KEEP**) |
| `ProgrammeRepository` | `Programme` | `findByDepartmentId(id)`, `findByDepartmentIdIn(ids)` | No | None (**KEEP**) |
| `BatchRepository` | `Batch` | `findByProgrammeId(id)`, `findByProgrammeIdIn(ids)` | No | Add queries for coordinator lookup if needed (**KEEP / EXTEND**) |
| `CourseRepository` | `Course` | `findByProgrammeId(id)`, `findByProgrammeIdIn(ids)` | No | None (**KEEP**) |
| `CourseOfferingRepository` | `CourseOffering` | `findByBatchId(id)`, `findByBatchIdIn(ids)`, `findByCourseId(id)`, `findByBatchIdAndCourseIdAndSemester(...)`, `findByCourseCoordinatorId(id)`, `findByCourseCoordinatorName...` | No (Already batch-centric) | None (**KEEP AS-IS**) |
| `StudentRepository` | `Student` | `findByBatchId(id)`, `findByPrn(prn)` | No | None (**KEEP**) |
| `ProgrammeOutcomeRepository` | `ProgrammeOutcome` | `findByProgrammeId(id)`, `findByProgrammeIdOrderByCodeAsc(id)` | **YES** (Uses `programme_id`) | **CHANGE** to `findByBatchId(id)` and `findByBatchIdOrderByCodeAsc(id)` |
| `PoCompetencyRepository` | `PoCompetency` | `findByPoId(id)`, `findByPoIdOrderByCodeAsc(id)`, `deleteByPoId(id)` | No | None (**KEEP**) |
| `ProgrammeSpecificOutcomeRepository`| `ProgrammeSpecificOutcome`| `findByProgrammeId(id)`, `findByProgrammeIdOrderByCodeAsc(id)` | **YES** (Uses `programme_id`) | **CHANGE** to `findByBatchId(id)` and `findByBatchIdOrderByCodeAsc(id)` |
| `PsoCompetencyRepository` | `PsoCompetency` | `findByPsoId(id)`, `findByPsoIdOrderByCodeAsc(id)`, `deleteByPsoId(id)` | No | None (**KEEP**) |
| `PeoOutcomeRepository` | `PeoOutcome` | `findByProgrammeId(id)`, `findByProgrammeIdOrderByCodeAsc(id)` | **YES** (Uses `programme_id`) | **CHANGE** to `findByBatchId(id)` and `findByBatchIdOrderByCodeAsc(id)` |
| `CourseOutcomeRepository` | `CourseOutcome` | `findByCourseOfferingId(id)`, `findByCourseOfferingIdOrderByCodeAsc(id)`, `findByCourseOfferingIdIn(ids)`, `findByCourseOfferingIdAndCode(...)`, `deleteByCourseOfferingId(id)` | No (Already offering-scoped) | None (**KEEP**) |
| `CoPoMappingRepository` | `CoPoMapping` | `findByCourseOutcomeId(id)`, `findByCourseOutcomeIdIn(ids)`, `deleteByCourseOutcomeId(id)`, `deleteByCourseOutcomeIdIn(ids)` | No | None (**KEEP**) |
| `CoPsoMappingRepository` | `CoPsoMapping` | `findByCourseOutcomeId(id)`, `findByCourseOutcomeIdIn(ids)`, `deleteByCourseOutcomeId(id)`, `deleteByCourseOutcomeIdIn(ids)` | No | None (**KEEP**) |
| `CourseMappingKeywordRepository` | `CourseMappingKeyword` | `findByCourseOfferingIdAndKeywordType(...)`, `findByCourseOfferingId(id)`, `deleteByCourseOfferingId(id)` | No | None (**KEEP**) |
| `AttainmentConfigurationRepository`| `AttainmentConfiguration`| `findByCourseOfferingId(id)`, `findByCourseOfferingIdIn(ids)`, `deleteByCourseOfferingId(id)` | No | None (**KEEP**) |
| `StudentCoMarkRepository` | `StudentCoMark` | `findByCourseOfferingId(id)`, `findByCourseOfferingIdAndCoCode(...)`, `findByCourseOfferingIdIn(ids)`, `deleteByCourseOfferingId(id)` | No | None (**KEEP**) |
| `UploadedDocumentRepository` | `UploadedDocument` | `findByCourseOfferingId(id)`, `findByBatchId(id)`, `findByBatchIdAndDocumentType(...)`, `findFirstByCourseOfferingId...`, `findFirstByBatchId...`, `deleteByCourseOfferingId...`, `deleteByBatchId...` | No | None (**KEEP**) |
| `CourseAtrRepository` | `CourseAtr` | `findByCourseOfferingId(id)`, `findByCourseOfferingIdIn(ids)`, `findByCourseOfferingIdAndCoCode(...)` | No | None (**KEEP**) |
| `ProgrammeAtrRepository` | `ProgrammeAtr` | `findByProgrammeId(id)`, `findByProgrammeIdAndBatchId(progId, batchId)`, `findByProgrammeIdIn(ids)`, `findByBatchId(batchId)` | Partial | Ensure `findByBatchId(batchId)` is primary (**KEEP**) |
| `ApprovalRequestRepository` | `ApprovalRequest` | `findBySchoolId(id)`, `findByProgrammeId(id)`, `findByStatus(status)` | No | Add `findByBatchId(id)` / `findByCourseOfferingId(id)` if needed (**KEEP / EXTEND**) |
| `ApprovalHistoryRepository` | `ApprovalHistory` | `findByApprovalRequestId(id)` | No | None (**KEEP**) |
| `DirectorSetupProgressRepository` | `DirectorSetupProgress`| `findBySchoolId(id)` | No | None (**KEEP**) |
| `HodSetupProgressRepository` | `HodSetupProgress` | `findByDepartmentId(id)`, `findByHodEmailIgnoreCase(email)` | No | None (**KEEP**) |
| `ProgrammeCoordinatorSetupProgressRepository`| `ProgrammeCoordinatorSetupProgress`| `findByProgrammeId(id)`, `findByProgrammeIdAndBatchId(progId, batchId)`, `findByCoordinatorEmailIgnoreCase(email)` | Partial | `findByBatchId(batchId)` (**KEEP / EXTEND**) |
| `CourseCoordinatorSetupProgressRepository`| `CourseCoordinatorSetupProgress`| `findByCourseOfferingId(id)`, `findByCoordinatorEmailAndCourseOfferingId(email, id)` | No | None (**KEEP**) |
| `UserRepository` | `User` | `findByUsername(...)`, `findByEmail(...)`, `findByUsernameOrEmail(...)`, `findByRole(...)`, `findBySchoolId(...)`, `findByRoleAndSchoolId(...)`, `existsByUsername(...)`, `existsByEmail(...)` | No | None (**KEEP**) |

---

## 5. TARGET ENTITY MODEL

```
School (id, code, name, director_id, director_email)
  │
  └── (1:N) ── Department (id, school_id, code, name, hod, hod_email)
                 │
                 └── (1:N) ── MasterProgramme [Table: programmes] (id, department_id, code, name, duration_years)
                                │
                                ├── (1:N) ── MasterCourse [Table: courses]
                                │              (id, programme_id, code, name, credits, course_type)
                                │
                                └── (1:N) ── ProgrammeBatch [Table: batches]
                                               (id, programme_id, name, start_year, end_year, duration_years,
                                                coordinator_id, coordinator_name, coordinator_email, previous_batch_id)
                                               │
                                               ├── (1:N) ── ProgrammeOutcome [Table: programme_outcomes]
                                               │              (id, batch_id, code, statement, target, status)
                                               │              └── (1:N) ── PoCompetency (id, po_id, code, statement)
                                               │
                                               ├── (1:N) ── ProgrammeSpecificOutcome [Table: programme_specific_outcomes]
                                               │              (id, batch_id, code, statement, target, status)
                                               │              └── (1:N) ── PsoCompetency (id, pso_id, code, statement)
                                               │
                                               ├── (1:N) ── PeoOutcome [Table: peo_outcomes]
                                               │              (id, batch_id, code, statement, status)
                                               │
                                               ├── (1:N) ── ProgrammeAttainment [Table: po_attainments / pso_attainments]
                                               │              (id, run_id, batch_id, po_code, direct, indirect, final, target)
                                               │
                                               ├── (1:1) ── ProgrammeATR [Table: programme_atrs]
                                               │              (id, batch_id, programme_id, status, submitted_by, verified_by)
                                               │
                                               ├── (1:N) ── Student [Table: students]
                                               │              (id, batch_id, prn, name, email, status)
                                               │
                                               └── (1:N) ── ProgrammeBatchCourse [Table: course_offerings]
                                                              (id, batch_id, course_id, semester, 
                                                               course_coordinator_id, course_coordinator_name, 
                                                               assigned_faculty, status)
                                                              │
                                                              ├── (1:N) ── CourseOutcome [Table: course_outcomes]
                                                              │              (id, course_offering_id, code, statement, 
                                                              │               target_level, blooms_level, status)
                                                              │              ├── (1:N) ── CoPoMapping (id, course_outcome_id, po_code, level)
                                                              │              └── (1:N) ── CoPsoMapping (id, course_outcome_id, pso_code, level)
                                                              │
                                                              ├── (1:N) ── CourseMappingKeyword [Table: course_mapping_keywords]
                                                              │              (id, course_offering_id, keyword_type, keywords_json)
                                                              │
                                                              ├── (1:1) ── AttainmentConfiguration [Table: attainment_configurations]
                                                              │              (id, course_offering_id, direct_weight, indirect_weight, status)
                                                              │
                                                              ├── (1:N) ── StudentCoMark [Table: student_co_marks]
                                                              │              (id, course_offering_id, student_id, prn, co_code, marks_obtained)
                                                              │
                                                              ├── (1:N) ── CourseAttainment [Table: direct/indirect/overall_co_attainments]
                                                              │              (id, run_id, course_offering_id, co_code, scores, status)
                                                              │
                                                              └── (1:N) ── CourseATR [Table: course_atrs]
                                                                             (id, course_offering_id, co_code, target, actual, status)
```

---

## 6. OLD $\to$ NEW MAPPING

| Current Entity / Table | New Concept | Action | Rationale |
| :--- | :--- | :--- | :--- |
| `School` / `schools` | School | **KEEP AS-IS** | Matches the target requirement for N Schools perfectly. |
| `Department` / `departments` | Department | **KEEP AS-IS** | Matches 1:N School-to-Department relationship with HOD assignment. |
| `Programme` / `programmes` | Master Programme | **KEEP / MODIFY** | Represents permanent programme definition. Remove or deprecate batch-specific coordinator fields from master table. |
| `Batch` / `batches` | Programme Batch | **MODIFY** | Add Programme Coordinator assignment directly to the batch (`coordinator_id`, `coordinator_name`, `coordinator_email`). |
| `Course` / `courses` | Master Course | **KEEP AS-IS** | Represents permanent course catalog definition under Master Programme. |
| `CourseOffering` / `course_offerings` | Programme Batch Course | **REPURPOSE** | Already links Master Course + Programme Batch + Semester + Course Coordinator. Keep table/entity structure to preserve zero churn on all child tables. |
| `Student` / `students` | Batch Enrolled Student | **KEEP AS-IS** | Already linked to `batch_id`. |
| `ProgrammeOutcome` / `programme_outcomes` | Batch-scoped PO | **MODIFY** | Switch foreign key from `programme_id` to `batch_id`. Add workflow status. |
| `ProgrammeSpecificOutcome` / `programme_specific_outcomes` | Batch-scoped PSO | **MODIFY** | Switch foreign key from `programme_id` to `batch_id`. Add workflow status. |
| `PeoOutcome` / `peo_outcomes` | Batch-scoped PEO | **MODIFY** | Switch foreign key from `programme_id` to `batch_id`. Add workflow status. |
| `CourseOutcome` / `course_outcomes` | Batch Course CO | **KEEP / MODIFY** | Already linked to `course_offering_id`. Add workflow status column. |
| `CoPoMapping` & `CoPsoMapping` | Batch Course Mappings | **KEEP AS-IS** | Already linked to `course_outcome_id`. |
| `AttainmentConfiguration` | Batch Course Attainment Config | **KEEP AS-IS** | Already linked to `course_offering_id` with existing approval status. |
| `StudentCoMark` | Batch Course Assessment Marks | **KEEP AS-IS** | Already linked to `course_offering_id` and `student_id`. |
| `CourseAtr` | Batch Course ATR | **KEEP AS-IS** | Already linked to `course_offering_id` with existing approval status. |
| `ProgrammeAtr` | Programme Batch ATR | **KEEP AS-IS** | Already scoped to `batch_id` (and `programme_id`) with approval status. |
| `ApprovalRequest` & `ApprovalHistory` | Governance & Approvals | **KEEP AS-IS** | Flexible polymorphic approval request engine already supporting all academic levels. |

---

## 7. COURSE OFFERING ANALYSIS

### Detailed Code & Schema Inspection
- **Current Table:** `course_offerings`
- **Current Fields:**
  - `id` (VARCHAR(50), PK)
  - `course_id` (VARCHAR(50), FK $\to$ `courses.id` ON DELETE CASCADE)
  - `batch_id` (VARCHAR(50), FK $\to$ `batches.id` ON DELETE CASCADE)
  - `semester` (INTEGER, NOT NULL)
  - `course_coordinator_id` (BIGINT, FK $\to$ `users.id` ON DELETE SET NULL)
  - `course_coordinator_name` (VARCHAR(255))
  - `assigned_faculty` (TEXT)
  - `status` (VARCHAR(30), DEFAULT 'ACTIVE')
  - `created_at`, `updated_at` (TIMESTAMP)
- **Current Unique Constraint:** `uk_batch_course_sem (batch_id, course_id, semester)`
- **Current Repositories & Consumers:**
  - `CourseOfferingRepository` (`findByBatchId`, `findByCourseId`, `findByCourseCoordinatorId`, etc.)
  - Referenced by FK in:
    - `course_outcomes (course_offering_id)`
    - `course_mapping_keywords (course_offering_id)`
    - `attainment_configurations (course_offering_id)`
    - `end_sem_marks_uploads (course_offering_id)`
    - `student_co_marks (course_offering_id)`
    - `course_end_surveys (course_offering_id)`
    - `uploaded_documents (course_offering_id)`
    - `calculation_runs (course_offering_id)`
    - `direct_co_attainments (course_offering_id)`
    - `indirect_co_attainments (course_offering_id)`
    - `overall_co_attainments (course_offering_id)`
    - `course_atrs (course_offering_id)`
    - `cc_setup_progress (course_offering_id)`
    - `approval_requests (course_offering_id)`

### What the New "Programme Batch Course" Needs
- Reference to Master Course (`course_id`) $\checkmark$ *Already present*
- Reference to Programme Batch (`batch_id`) $\checkmark$ *Already present*
- Semester number (`semester`) $\checkmark$ *Already present*
- Course Coordinator assignment (`course_coordinator_id`, `course_coordinator_name`) $\checkmark$ *Already present*
- Assigned faculty (`assigned_faculty`) $\checkmark$ *Already present*
- Associated COs, Mappings, Assessment Marks, Attainment Configurations, and Course ATRs $\checkmark$ *Already present*

### Recommendation: **REPURPOSE / REUSE AS-IS**
**Why?**
The existing `CourseOffering` entity and `course_offerings` table already contain **100% of the data structures, foreign keys, constraints, and child relationships** required by the new *Programme Batch Course* concept. Renaming or dropping this table would require touching 14 dependent tables and rewriting dozens of repository methods with zero functional gain. `CourseOffering` can simply be retained as the implementation entity for Programme Batch Course.

---

## 8. PO / PSO / PEO ANALYSIS

### Current State
1. `programme_outcomes` table has `programme_id VARCHAR(50) REFERENCES programmes(id)` and unique constraint `uq_programme_po (programme_id, code)`.
2. `programme_specific_outcomes` table has `programme_id VARCHAR(50) REFERENCES programmes(id)` and unique constraint `uq_programme_pso (programme_id, code)`.
3. `peo_outcomes` table has `programme_id VARCHAR(50) REFERENCES programmes(id)` and unique constraint `uq_programme_peo (programme_id, code)`.
4. In this current state, if Batch 2025-29 and Batch 2026-30 share the same Master Programme, they are forced to share the exact same PO/PSO/PEO definitions.

### Required Minimum Changes for Batch-Centric Scoping
1. **Schema Modifications:**
   - In `programme_outcomes`: Change `programme_id` to `batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE`.
   - In `programme_specific_outcomes`: Change `programme_id` to `batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE`.
   - In `peo_outcomes`: Change `programme_id` to `batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE`.
   - Update unique constraints to:
     - `uk_batch_po (batch_id, code)`
     - `uk_batch_pso (batch_id, code)`
     - `uk_batch_peo (batch_id, code)`
2. **Entity Modifications:**
   - In `ProgrammeOutcome.java`, `ProgrammeSpecificOutcome.java`, `PeoOutcome.java`:
     - Replace `private String programmeId;` with `private String batchId;` (or keep `programmeId` as a transient convenience accessor if needed).
     - Add `status` field (`ApprovalStatus` or `String`) defaulting to `'DRAFT'` / `'APPROVED'`.
3. **Repository Modifications:**
   - In `ProgrammeOutcomeRepository`: Replace `findByProgrammeId` with `findByBatchId(String batchId)`.
   - In `ProgrammeSpecificOutcomeRepository`: Replace `findByProgrammeId` with `findByBatchId(String batchId)`.
   - In `PeoOutcomeRepository`: Replace `findByProgrammeId` with `findByBatchId(String batchId)`.

---

## 9. TARGETS ANALYSIS

### Current State
- Originally in `V2`, targets were placed in a separate `programme_targets` table keyed by `(batch_id, outcome_type, outcome_code)`.
- In `V6__add_target_to_po_pso_and_drop_programme_targets.sql`, target columns were added directly to `programme_outcomes` (`target NUMERIC(4,2)`) and `programme_specific_outcomes` (`target NUMERIC(4,2)`), and `programme_targets` was dropped.

### Target Alignment
- By moving `programme_outcomes` and `programme_specific_outcomes` to `batch_id` (as described in Section 8), each Programme Batch has its own PO/PSO records containing its own explicit `target` value.
- **Minimum Change Required:** No new tables required. The `target` column already on `programme_outcomes` and `programme_specific_outcomes` naturally becomes batch-specific once the parent FK points to `batches(id)`.

---

## 10. PROGRAMME ATTAINMENT + PROGRAMME ATR

### Programme Attainment
- **Current Tables:** `po_attainments`, `pso_attainments`, `programme_exit_surveys`, `calculation_runs`.
- **Current State:** These tables *already* include `batch_id VARCHAR(50) REFERENCES batches(id)`.
- **Minimum Change Required:** Keep `batch_id` as the primary scoping identifier. In queries, query by `batch_id` directly.

### Programme ATR
- **Current Table:** `programme_atrs`
- **Current State:** Contains `programme_id`, `batch_id`, `status` (`DRAFT`, `SUBMITTED`, `APPROVED`, `REVISION_REQUESTED`), `submitted_by`, `verified_by`, `observations_json`.
- **Constraint:** `uk_programme_batch_atr (programme_id, batch_id)`.
- **Target Ownership:** Owned by Programme Batch (`batch_id`).
- **Minimum Change Required:** **KEEP AS-IS**. The existing table and entity already scope the ATR to the specific batch instance.

---

## 11. COURSE ATTAINMENT + COURSE ATR

### Course Attainment
- **Current Tables:** `attainment_configurations`, `student_co_marks`, `direct_co_attainments`, `indirect_co_attainments`, `overall_co_attainments`.
- **Current State:** Every single one of these tables is foreign-keyed to `course_offering_id` (`course_offerings.id`).
- **Target Ownership:** Must belong to the *Programme Batch Course*.
- **Minimum Change Required:** **KEEP AS-IS**. Since `course_offerings` represents the Programme Batch Course, all child attainment records are already properly scoped.

### Course ATR
- **Current Table:** `course_atrs`
- **Current State:** Foreign-keyed to `course_offering_id` with columns `co_code`, `target_score`, `actual_score`, `pct_achieved`, `status` (`DRAFT`, `SUBMITTED`, `VERIFIED`, `REVISION_REQUESTED`), `statement`, `actions_json`, `submitted_by`, `verified_by`.
- **Target Ownership:** Must belong to the *Programme Batch Course*.
- **Minimum Change Required:** **KEEP AS-IS**.

---

## 12. APPROVAL STATUS ANALYSIS

### Existing Approval Mechanism
The project contains both:
1. An inline `status` column on critical workflow entities (`AttainmentConfiguration`, `CourseAtr`, `ProgrammeAtr`).
2. A generic `approval_requests` + `approval_history` audit table for centralized submission, review, approval, and rejection across roles.

### Approval Status Inventory & Recommendation

| Entity | Current Status Column? | Needs Approval Status? | Recommended Field | Reason |
| :--- | :--- | :--- | :--- | :--- |
| `ProgrammeOutcome` | No | **YES** | `status VARCHAR(30) DEFAULT 'DRAFT'` | PO definitions require PC submission and HOD approval per batch. |
| `ProgrammeSpecificOutcome` | No | **YES** | `status VARCHAR(30) DEFAULT 'DRAFT'` | PSO definitions require PC submission and HOD approval per batch. |
| `PeoOutcome` | No | **YES** | `status VARCHAR(30) DEFAULT 'DRAFT'` | PEO definitions require PC submission and HOD approval per batch. |
| `CourseOutcome` | No | **YES** | `status VARCHAR(30) DEFAULT 'DRAFT'` | CO statements and target levels require CC submission and PC approval. |
| `AttainmentConfiguration` | Yes (`AttainmentConfigStatus`) | **YES** | Keep existing `status` | Direct/Indirect weightages require HOD approval before marks processing. |
| `CourseAtr` | Yes (`CourseAtrStatus`) | **YES** | Keep existing `status` | Course ATR requires PC verification. |
| `ProgrammeAtr` | Yes (`ProgrammeAtrStatus`) | **YES** | Keep existing `status` | Programme ATR requires HOD verification. |
| `ApprovalRequest` | Yes (`ApprovalStatus`) | **YES** | Keep existing `status` | Central workflow tracker. |

---

## 13. DATABASE CHANGES (SCHEMA-LEVEL SPECIFICATION)

### Table Modifications Summary

#### 1. ALTER TABLE `programmes`
- **DROP COLUMN (Optional/Deprecated):** `coordinator`, `coordinator_email` (moved to `batches`).
- **KEEP:** `id`, `department_id`, `code`, `name`, `duration_years`, `status`, `created_at`, `updated_at`.

#### 2. ALTER TABLE `batches`
- **ADD COLUMN:** `coordinator_id BIGINT REFERENCES users(id) ON DELETE SET NULL`
- **ADD COLUMN:** `coordinator_name VARCHAR(150)`
- **ADD COLUMN:** `coordinator_email VARCHAR(150)`

#### 3. ALTER TABLE `programme_outcomes`
- **DROP FK:** `programme_outcomes_programme_id_fkey`
- **DROP COLUMN:** `programme_id`
- **ADD COLUMN:** `batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE`
- **ADD COLUMN:** `status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'`
- **DROP CONSTRAINT:** `uq_programme_po`
- **ADD UNIQUE CONSTRAINT:** `uk_batch_po_code UNIQUE (batch_id, code)`
- **ADD INDEX:** `idx_programme_outcomes_batch ON programme_outcomes(batch_id)`

#### 4. ALTER TABLE `programme_specific_outcomes`
- **DROP FK:** `programme_specific_outcomes_programme_id_fkey`
- **DROP COLUMN:** `programme_id`
- **ADD COLUMN:** `batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE`
- **ADD COLUMN:** `status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'`
- **DROP CONSTRAINT:** `uq_programme_pso`
- **ADD UNIQUE CONSTRAINT:** `uk_batch_pso_code UNIQUE (batch_id, code)`
- **ADD INDEX:** `idx_programme_pso_batch ON programme_specific_outcomes(batch_id)`

#### 5. ALTER TABLE `peo_outcomes`
- **DROP FK:** `peo_outcomes_programme_id_fkey`
- **DROP COLUMN:** `programme_id`
- **ADD COLUMN:** `batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE`
- **ADD COLUMN:** `status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'`
- **DROP CONSTRAINT:** `uq_programme_peo`
- **ADD UNIQUE CONSTRAINT:** `uk_batch_peo_code UNIQUE (batch_id, code)`
- **ADD INDEX:** `idx_peo_outcomes_batch ON peo_outcomes(batch_id)`

#### 6. ALTER TABLE `course_outcomes`
- **ADD COLUMN:** `status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'`

#### 7. Retained Tables (No Structural Schema Alterations Required)
- `schools`, `departments`, `courses`, `course_offerings`, `students`, `po_competencies`, `pso_competencies`, `co_po_mappings`, `co_pso_mappings`, `course_mapping_keywords`, `attainment_configurations`, `attainment_levels`, `end_sem_marks_uploads`, `student_co_marks`, `course_end_surveys`, `survey_responses`, `survey_response_details`, `programme_exit_surveys`, `uploaded_documents`, `calculation_runs`, `direct_co_attainments`, `indirect_co_attainments`, `overall_co_attainments`, `po_attainments`, `pso_attainments`, `course_atrs`, `programme_atrs`, `approval_requests`, `approval_history`, `director_setup_progress`, `hod_setup_progress`, `pc_setup_progress`, `cc_setup_progress`, `users`.

---

## 14. ENTITY CHANGES

| Entity | Field / Relationship | Current State | Target State | Specific Change |
| :--- | :--- | :--- | :--- | :--- |
| `Programme` | Coordinator info | Fields on Master Programme | Deprecated / Optional | Keep class as Master Programme; coordinator shifts to `Batch`. |
| `Batch` | Coordinator info | None | `coordinatorId`, `coordinatorName`, `coordinatorEmail` | Add columns for Batch-specific Programme Coordinator. |
| `ProgrammeOutcome` | Parent relationship | `programmeId` (String) | `batchId` (String) | Change field name, column mapping, and unique constraint. |
| `ProgrammeOutcome` | Workflow Status | None | `status` (ApprovalStatus / String) | Add `@Enumerated` or `@Column` for approval status. |
| `ProgrammeSpecificOutcome`| Parent relationship | `programmeId` (String) | `batchId` (String) | Change field name, column mapping, and unique constraint. |
| `ProgrammeSpecificOutcome`| Workflow Status | None | `status` (ApprovalStatus / String) | Add `@Enumerated` or `@Column` for approval status. |
| `PeoOutcome` | Parent relationship | `programmeId` (String) | `batchId` (String) | Change field name, column mapping, and unique constraint. |
| `PeoOutcome` | Workflow Status | None | `status` (ApprovalStatus / String) | Add `@Enumerated` or `@Column` for approval status. |
| `CourseOutcome` | Workflow Status | None | `status` (ApprovalStatus / String) | Add `@Enumerated` or `@Column` for approval status. |
| `CourseOffering` | Batch Course role | Exists as `CourseOffering` | Keep as `CourseOffering` | No field changes needed; already references `batch_id` and `course_id`. |

---

## 15. REPOSITORY CHANGES

### Repositories with Method Changes

#### 1. `ProgrammeOutcomeRepository`
- **Methods to Remove/Change:**
  - `List<ProgrammeOutcome> findByProgrammeId(String programmeId)` $\longrightarrow$ Remove or mark obsolete.
  - `List<ProgrammeOutcome> findByProgrammeIdOrderByCodeAsc(String programmeId)` $\longrightarrow$ Remove or mark obsolete.
- **Methods to Add:**
  - `List<ProgrammeOutcome> findByBatchId(String batchId);`
  - `List<ProgrammeOutcome> findByBatchIdOrderByCodeAsc(String batchId);`
  - `List<ProgrammeOutcome> findByBatchIdIn(Collection<String> batchIds);`

#### 2. `ProgrammeSpecificOutcomeRepository`
- **Methods to Remove/Change:**
  - `List<ProgrammeSpecificOutcome> findByProgrammeId(String programmeId)` $\longrightarrow$ Remove or mark obsolete.
  - `List<ProgrammeSpecificOutcome> findByProgrammeIdOrderByCodeAsc(String programmeId)` $\longrightarrow$ Remove or mark obsolete.
- **Methods to Add:**
  - `List<ProgrammeSpecificOutcome> findByBatchId(String batchId);`
  - `List<ProgrammeSpecificOutcome> findByBatchIdOrderByCodeAsc(String batchId);`
  - `List<ProgrammeSpecificOutcome> findByBatchIdIn(Collection<String> batchIds);`

#### 3. `PeoOutcomeRepository`
- **Methods to Remove/Change:**
  - `List<PeoOutcome> findByProgrammeId(String programmeId)` $\longrightarrow$ Remove or mark obsolete.
  - `List<PeoOutcome> findByProgrammeIdOrderByCodeAsc(String programmeId)` $\longrightarrow$ Remove or mark obsolete.
- **Methods to Add:**
  - `List<PeoOutcome> findByBatchId(String batchId);`
  - `List<PeoOutcome> findByBatchIdOrderByCodeAsc(String batchId);`

#### 4. `BatchRepository`
- **Methods to Retain:**
  - `List<Batch> findByProgrammeId(String programmeId);`
  - `List<Batch> findByProgrammeIdIn(Collection<String> programmeIds);`
- **Methods to Add (Optional/Helper):**
  - `List<Batch> findByCoordinatorEmailIgnoreCase(String coordinatorEmail);`
  - `List<Batch> findByCoordinatorId(Long coordinatorId);`

---

## 16. MINIMUM CHANGE PLAN

Since the database has zero live production data, we can execute the migration in 6 clean, risk-free steps:

1. **Step 1: Update Entities**
   - Update `Batch.java` to add coordinator fields.
   - Update `ProgrammeOutcome.java`, `ProgrammeSpecificOutcome.java`, `PeoOutcome.java` to replace `programmeId` with `batchId` and add `status`.
   - Update `CourseOutcome.java` to add `status`.
2. **Step 2: Update Repositories**
   - Update `ProgrammeOutcomeRepository.java`, `ProgrammeSpecificOutcomeRepository.java`, `PeoOutcomeRepository.java` to query by `batchId`.
   - Update `BatchRepository.java` with coordinator lookup methods.
3. **Step 3: Update Flyway Migrations**
   - Rewrite / consolidate Flyway migrations `V1`–`V6` to reflect the clean batch-scoped outcomes and coordinator placement from day 1.
4. **Step 4: Keep Existing Core Entities & Repositories Untouched**
   - `School`, `Department`, `Course`, `CourseOffering`, `Student`, `AttainmentConfiguration`, `StudentCoMark`, `CourseAtr`, `ProgrammeAtr`, `ApprovalRequest`, `User`.
5. **Step 5: Verify Service & Controller Integration**
   - When later auditing services and controllers, adjust outcome queries to pass `batchId` rather than `programmeId`.

---

## 17. RISK ANALYSIS

| Risk Item | Severity | Mitigation Strategy |
| :--- | :--- | :--- |
| **Outcome Scoping Query Changes** | Medium | Changing `findByProgrammeId` to `findByBatchId` will produce compile errors in `OutcomeService` and `AcademicService` until those service methods are updated. |
| **Circular Relationships** | Low | None detected. The hierarchy is strictly acyclic: $\text{School} \to \text{Dept} \to \text{Prog} \to \text{Batch} \to \text{BatchCourse} \to \text{CO}$. |
| **Foreign Key / Cascade Risks** | Low | Deleting a `Batch` cleanly cascades to `ProgrammeOutcome`, `ProgrammeSpecificOutcome`, `PeoOutcome`, `CourseOffering`, `Student`, and `ProgrammeAtr`. |
| **Unique Constraint Inconsistencies** | Low | Replace `uq_programme_po (programme_id, code)` with `uk_batch_po (batch_id, code)`. This correctly allows duplicate codes (e.g. PO1) across different batches. |
| **Hibernate Mapping / Entity Naming Risks** | Low | By retaining the class name `CourseOffering` for Programme Batch Course, 14 dependent entities avoid cascade mapping edits. |
| **Lazy Loading & N+1 Query Risks** | Low | Keep JPA associations `@ManyToOne(fetch = FetchType.LAZY)` or use flat ID columns as currently implemented. The existing codebase relies primarily on clean flat ID columns (`batchId`, `courseOfferingId`), eliminating complex Hibernate proxy issues. |

---

## 18. WHAT MUST NOT CHANGE

The following core systems and modules have been audited and verified as 100% compatible with the target structure and **MUST REMAIN UNTOUCHED**:

1. **Authentication & User Management:**
   - `AuthController.java`, `AuthService.java`, `CustomUserDetailsService.java`
   - Password hashing with `BCryptPasswordEncoder`
   - Login, registration, token refresh, OTP verification
2. **JWT Infrastructure:**
   - `JwtTokenProvider.java`, `JwtAuthenticationFilter.java`
   - `JwtAuthenticationEntryPoint.java`, `JwtAccessDeniedHandler.java`
3. **Spring Security Architecture:**
   - `SecurityConfig.java`, `CurrentUserScope.java`, `CurrentUserScopeService.java`
   - Stateless session management, CORS configuration
4. **User Scoping & Roles:**
   - `UserRole.java` (`DIRECTOR`, `HOD`, `PROGRAMME_COORDINATOR`, `COURSE_COORDINATOR`, `FACULTY`, `IQAC`)
   - `User.java` and `UserRepository.java`
5. **Common Exception Handling & Response Wrappers:**
   - `GlobalExceptionHandler.java`, `BadRequestException.java`, `ResourceNotFoundException.java`
   - `ApiResponse.java`, `AuthResponse.java`
6. **File Storage & Excel Parsing:**
   - Apache POI and OpenPDF configuration

---

## 19. FILE-BY-FILE CHANGE PLAN

The following table lists the specific entity and repository files that will require changes in the upcoming implementation phase:

| File Path | Component | Exact Changes Needed |
| :--- | :--- | :--- |
| `src/main/java/com/dypiu/nba/entity/Batch.java` | Entity | Add `coordinatorId` (Long), `coordinatorName` (String), `coordinatorEmail` (String). |
| `src/main/java/com/dypiu/nba/entity/ProgrammeOutcome.java` | Entity | Replace `programmeId` with `batchId`. Update table unique constraint to `uk_batch_po_code (batch_id, code)`. Add `status` field. |
| `src/main/java/com/dypiu/nba/entity/ProgrammeSpecificOutcome.java` | Entity | Replace `programmeId` with `batchId`. Update table unique constraint to `uk_batch_pso_code (batch_id, code)`. Add `status` field. |
| `src/main/java/com/dypiu/nba/entity/PeoOutcome.java` | Entity | Replace `programmeId` with `batchId`. Update table unique constraint to `uk_batch_peo_code (batch_id, code)`. Add `status` field. |
| `src/main/java/com/dypiu/nba/entity/CourseOutcome.java` | Entity | Add `status` field (`ApprovalStatus` or `String`) defaulting to `'DRAFT'`. |
| `src/main/java/com/dypiu/nba/repository/ProgrammeOutcomeRepository.java` | Repository | Replace `findByProgrammeId...` with `findByBatchId(String batchId)` and `findByBatchIdOrderByCodeAsc(String batchId)`. |
| `src/main/java/com/dypiu/nba/repository/ProgrammeSpecificOutcomeRepository.java` | Repository | Replace `findByProgrammeId...` with `findByBatchId(String batchId)` and `findByBatchIdOrderByCodeAsc(String batchId)`. |
| `src/main/java/com/dypiu/nba/repository/PeoOutcomeRepository.java` | Repository | Replace `findByProgrammeId...` with `findByBatchId(String batchId)` and `findByBatchIdOrderByCodeAsc(String batchId)`. |
| `src/main/java/com/dypiu/nba/repository/BatchRepository.java` | Repository | Add `findByCoordinatorEmailIgnoreCase(String email)`. |
| `src/main/resources/db/migration/*` | Flyway SQL | Consolidate migrations `V1`–`V6` into a clean initial schema with `batch_id` on outcomes and coordinator on `batches`. |

---

## 20. FINAL MINIMUM-CHANGE SUMMARY

### Comprehensive Inventory of Changes

- **A. Tables to Keep (36 tables):**
  `schools`, `departments`, `programmes`, `semesters`, `courses`, `course_offerings`, `students`, `po_competencies`, `pso_competencies`, `co_po_mappings`, `co_pso_mappings`, `course_mapping_keywords`, `attainment_configurations`, `attainment_levels`, `end_sem_marks_uploads`, `student_co_marks`, `course_end_surveys`, `survey_responses`, `survey_response_details`, `programme_exit_surveys`, `uploaded_documents`, `calculation_runs`, `direct_co_attainments`, `indirect_co_attainments`, `overall_co_attainments`, `po_attainments`, `pso_attainments`, `course_atrs`, `programme_atrs`, `approval_requests`, `approval_history`, `director_setup_progress`, `hod_setup_progress`, `pc_setup_progress`, `cc_setup_progress`, `users`.
- **B. Tables to Modify (5 tables):**
  `batches` (add coordinator columns), `programme_outcomes` (switch `programme_id` $\to$ `batch_id`, add `status`), `programme_specific_outcomes` (switch `programme_id` $\to$ `batch_id`, add `status`), `peo_outcomes` (switch `programme_id` $\to$ `batch_id`, add `status`), `course_outcomes` (add `status`).
- **C. Tables to Remove (0 tables):**
  None (The legacy `programme_targets` table was already dropped in `V6`).
- **D. Tables to Add (0 tables):**
  None.
- **E. Entities to Keep (23 entities):**
  `School`, `Department`, `Course`, `CourseOffering`, `Student`, `PoCompetency`, `PsoCompetency`, `CoPoMapping`, `CoPsoMapping`, `CourseMappingKeyword`, `AttainmentConfiguration`, `StudentCoMark`, `UploadedDocument`, `CourseAtr`, `ProgrammeAtr`, `ApprovalRequest`, `ApprovalHistory`, `DirectorSetupProgress`, `HodSetupProgress`, `ProgrammeCoordinatorSetupProgress`, `CourseCoordinatorSetupProgress`, `User`, and all 11 Enums.
- **F. Entities to Modify (5 entities):**
  `Batch`, `ProgrammeOutcome`, `ProgrammeSpecificOutcome`, `PeoOutcome`, `CourseOutcome`.
- **G. Entities to Remove (0 entities):**
  None.
- **H. Entities to Add (0 entities):**
  None.
- **I. Repositories to Keep (24 repositories):**
  `SchoolRepository`, `DepartmentRepository`, `ProgrammeRepository`, `CourseRepository`, `CourseOfferingRepository`, `StudentRepository`, `PoCompetencyRepository`, `PsoCompetencyRepository`, `CoPoMappingRepository`, `CoPsoMappingRepository`, `CourseMappingKeywordRepository`, `AttainmentConfigurationRepository`, `StudentCoMarkRepository`, `UploadedDocumentRepository`, `CourseAtrRepository`, `ProgrammeAtrRepository`, `ApprovalRequestRepository`, `ApprovalHistoryRepository`, `DirectorSetupProgressRepository`, `HodSetupProgressRepository`, `ProgrammeCoordinatorSetupProgressRepository`, `CourseCoordinatorSetupProgressRepository`, `UserRepository`.
- **J. Repositories to Modify (4 repositories):**
  `BatchRepository`, `ProgrammeOutcomeRepository`, `ProgrammeSpecificOutcomeRepository`, `PeoOutcomeRepository`.
- **K. Repositories to Remove (0 repositories):**
  None.
- **L. Repositories to Add (0 repositories):**
  None.

### Overall Estimated Complexity: **LOW**
**Rationale:**
The existing codebase was already built with a cohort-oriented schema where course instances (`CourseOffering`), student marks (`StudentCoMark`), attainment calculations (`direct_co_attainments`), and Action Taken Reports (`CourseAtr`, `ProgrammeAtr`) are tied directly to specific batches and offerings. The only major structural requirement is shifting `PO`, `PSO`, and `PEO` ownership from `programme_id` to `batch_id` and storing the Programme Coordinator on `Batch`. All other infrastructure, authentication, security, and child table hierarchies remain intact.
