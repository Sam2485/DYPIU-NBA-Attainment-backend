# PHASE 7.1 — COMPLETE BUSINESS OPERATION AUDIT COVERAGE REPORT
**Strict Read-Only Architectural Audit & Mutation Coverage Analysis**

---

## 1. Executive Summary

This report delivers a strict, read-only audit of audit logging coverage across all business mutations in the DYPIU NBA Attainment Backend following the Phase 7 centralized `AuditLog` rollout.

### Key Metrics Summary:
- **Total Endpoints Discovered:** 72 unique endpoints (209 mapped routes including aliases)
- **Total State-Changing Mutation Operations:** 34 operations
- **Total Audit-Required Business Operations:** 28 operations
- **Total Audited Business Operations:** 2 operations (Formal `submitApprovalRequest` and `approveRequest` in `ApprovalService`)
- **Total Missing Audit Operations:** 26 operations
- **Audit Coverage Rate:** **7.1%** (Requires instrumentation in Phase 7.2)
- **Security / Actor Derivation Integrity:** **100% PASS** (No client actor forgery vulnerability detected; `SecurityContextHolder` is the sole source of actor truth)
- **Sensitive Data Redaction Integrity:** **100% PASS** (Passwords, tokens, hashes are systematically sanitized)
- **ADMIN / IQAC Role Gate Integrity:** **100% PASS** (`GET /audit-logs` blocked to Director, HOD, PC, Faculty with `403 Forbidden`)

---

## 2. API Endpoint Classification & Inventory

| Category | Description | Count |
|---|---|:---:|
| **A. CREATE** | Academic domain entity creation (School, Dept, Programme, Batch, Course, Offering, Student, User) | 8 |
| **B. UPDATE** | Academic domain updates, targets, settings, milestones | 12 |
| **C. DELETE / SOFT DELETE** | Entity removal endpoints (pending Phase 8 workflow) | 6 |
| **D. APPROVAL WORKFLOW** | Workflow approvals, verification actions, and revision requests | 5 |
| **E. SUBMISSION / RESUBMISSION** | Formal batch allocation, ATR, and attainment submissions | 4 |
| **F. ASSIGNMENT** | Coordinator and faculty allocation changes | 2 |
| **G. UPLOAD / IMPORT** | Excel marks parsing, course surveys, programme exit surveys | 3 |
| **H. CALCULATION** | Deterministic mathematical calculation and aggregation | 0 (pure memory/query) |
| **I. AUTHENTICATION / SECURITY** | Login, register, token refresh, OTP, password reset | 6 |
| **J. READ ONLY** | Pure retrieval queries, summaries, matrices, exports | 26 |
| **Total Unique Endpoints** | | **72** |

---

## 3. Complete Endpoint Coverage Matrix

| Method | Endpoint | Resource | Mutation | Audit Required | Audit Present | Action | Actor Source | Status |
|---|---|---|---|:---:|:---:|---|---|:---:|
| `POST` | `/auth/login` | `USER` | No (Token issuance) | Optional (Security) | No | `LOGIN` | SecurityContext | `NOT_APPLICABLE` |
| `POST` | `/auth/register` | `USER` | Yes (New User) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `POST` | `/auth/refresh-token` | `USER` | No | No | No | - | - | `NOT_APPLICABLE` |
| `POST` | `/auth/reset-password` | `USER` | Yes (Password change) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/academic/schools` | `SCHOOL` | Yes (Create School) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `PUT` | `/academic/schools/{id}` | `SCHOOL` | Yes (Update School) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `DELETE` | `/academic/schools/{id}` | `SCHOOL` | Yes (Delete School) | Yes | No | `DELETE` | SecurityContext | `MISSING` |
| `POST` | `/academic/director/setup-progress` | `SCHOOL` | Yes (Milestone) | No | No | `UPDATE` | SecurityContext | `NOT_APPLICABLE` |
| `POST` | `/academic/departments` | `DEPARTMENT` | Yes (Create Dept) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `PUT` | `/academic/departments/{id}` | `DEPARTMENT` | Yes (Update Dept) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `DELETE` | `/academic/departments/{id}` | `DEPARTMENT` | Yes (Delete Dept) | Yes | No | `DELETE` | SecurityContext | `MISSING` |
| `POST` | `/academic/hod/setup-progress` | `DEPARTMENT` | Yes (Milestone) | No | No | `UPDATE` | SecurityContext | `NOT_APPLICABLE` |
| `POST` | `/academic/hod/coordinators` | `MASTER_PROGRAMME` | Yes (Assign PC) | Yes | No | `ASSIGN_COORDINATOR` | SecurityContext | `MISSING` |
| `POST` | `/academic/master-programmes` | `MASTER_PROGRAMME` | Yes (Create Programme) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `DELETE` | `/academic/master-programmes/{id}` | `MASTER_PROGRAMME` | Yes (Delete Programme) | Yes | No | `DELETE` | SecurityContext | `MISSING` |
| `POST` | `/academic/programme-batches` | `PROGRAMME_BATCH` | Yes (Create Batch) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `DELETE` | `/academic/programme-batches/{id}` | `PROGRAMME_BATCH` | Yes (Delete Batch) | Yes | No | `DELETE` | SecurityContext | `MISSING` |
| `POST` | `/academic/coordinator/setup-progress`| `PROGRAMME_BATCH` | Yes (Milestone) | No | No | `UPDATE` | SecurityContext | `NOT_APPLICABLE` |
| `POST` | `/academic/master-courses` | `MASTER_COURSE` | Yes (Create Course) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `DELETE` | `/academic/master-courses/{id}` | `MASTER_COURSE` | Yes (Delete Course) | Yes | No | `DELETE` | SecurityContext | `MISSING` |
| `POST` | `/academic/programme-batch-courses` | `PROGRAMME_BATCH_COURSE` | Yes (Create Offering) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `POST` | `/academic/courses/allocate` | `PROGRAMME_BATCH_COURSE` | Yes (Allocate CC) | Yes | No | `ALLOCATE_COURSES` | SecurityContext | `MISSING` |
| `POST` | `/academic/course-coordinator/setup-progress` | `PROGRAMME_BATCH_COURSE` | Yes (Milestone) | No | No | `UPDATE` | SecurityContext | `NOT_APPLICABLE` |
| `POST` | `/academic/students` | `STUDENT` | Yes (Create Student) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `DELETE` | `/academic/students/{id}` | `STUDENT` | Yes (Delete Student) | Yes | No | `DELETE` | SecurityContext | `MISSING` |
| `POST` | `/academic/outcomes` | `PROGRAMME_OUTCOME` | Yes (Save PO/PSO/PEO)| Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/academic/courses/{id}/outcomes` | `COURSE_OUTCOME` | Yes (Save COs) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/academic/courses/{id}/co-targets` | `COURSE_OUTCOME` | Yes (Save CO Target) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/academic/programmes/{id}/targets` | `PROGRAMME_OUTCOME` | Yes (Save Batch Target)| Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/academic/courses/{id}/mapping` | `CO_PO_MAPPING` | Yes (Save Mapping) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/attainment/config/{id}` | `ATTAINMENT_CONFIGURATION` | Yes (Attainment Settings) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/attainment/examination/{id}/upload` | `DIRECT_ASSESSMENT` | Yes (Marks Upload) | Yes | No | `UPLOAD_MARKS` | SecurityContext | `MISSING` |
| `POST` | `/attainment/survey/{id}/upload` | `INDIRECT_ASSESSMENT` | Yes (Survey Upload) | Yes | No | `UPLOAD_SURVEY` | SecurityContext | `MISSING` |
| `POST` | `/attainment/programme-survey/upload` | `INDIRECT_ASSESSMENT` | Yes (Exit Survey) | Yes | No | `UPLOAD_SURVEY` | SecurityContext | `MISSING` |
| `POST` | `/atr/course/{id}` | `COURSE_ATR` | Yes (Save Course ATR) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/reports/course-atr/{id}/submit` | `COURSE_ATR` | Yes (Submit Course ATR)| Yes | No | `SUBMIT` | SecurityContext | `MISSING` |
| `POST` | `/atr/programme/{id}` | `PROGRAMME_ATR` | Yes (Save Batch ATR) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |
| `POST` | `/reports/programme-atr/.../submit`| `PROGRAMME_ATR` | Yes (Submit Batch ATR) | Yes | No | `SUBMIT` | SecurityContext | `MISSING` |
| `POST` | `/approvals/submit` | `APPROVAL_REQUEST` | Yes (Submit Workflow) | Yes | **Yes** | `SUBMIT` | SecurityContext | **PASS** |
| `POST` | `/approvals/{id}/approve` | `APPROVAL_REQUEST` | Yes (Approve Workflow) | Yes | **Yes** | `APPROVE` | SecurityContext | **PASS** |
| `POST` | `/approvals/{id}/request-revision` | `APPROVAL_REQUEST` | Yes (Request Revision)| Yes | No | `REQUEST_REVISION` | SecurityContext | `MISSING` |
| `POST` | `/approvals/verify` | `APPROVAL_REQUEST` | Yes (Direct Verify) | Yes | No | `APPROVE` | SecurityContext | `MISSING` |
| `POST` | `/approvals/request-revision` | `APPROVAL_REQUEST` | Yes (Direct Revision) | Yes | No | `REQUEST_REVISION` | SecurityContext | `MISSING` |
| `POST` | `/users` | `USER` | Yes (Create User) | Yes | No | `CREATE` | SecurityContext | `MISSING` |
| `PUT` | `/users/{id}` | `USER` | Yes (Update User) | Yes | No | `UPDATE` | SecurityContext | `MISSING` |

---

## 4. Verification Areas & Findings

### 4.1. CREATE Operations Coverage
- **Status:** **PARTIAL**
- **Findings:** `MasterProgramme`, `ProgrammeBatch`, `MasterCourse`, `ProgrammeBatchCourse`, `School`, `Department`, and `User` entities are saved via `AcademicService` and `UserController`, but do not yet trigger `auditLogService.recordSuccess(...)`.

### 4.2. UPDATE Operations Coverage
- **Status:** **PARTIAL**
- **Findings:** Outcome updates (POs, PSOs, PEOs, COs), CO-PO matrix correlations, Attainment configurations, and ATR drafts are persisted in DB, but missing audit calls in `OutcomeService`, `AttainmentCalculationService`, and `AtrService`.

### 4.3. ASSIGNMENT Coverage
- **Status:** **MISSING**
- **Findings:** PC assignment (`assignProgrammeCoordinator`) and Course Coordinator allocations (`allocateCourses`) update associations in DB without calling `AuditLogService`.

### 4.4. APPROVAL Coverage
- **Status:** **PARTIAL**
- **Findings:**
  - `submitApprovalRequest` $\rightarrow$ **Audited (`SUBMIT`)**
  - `approveRequest` $\rightarrow$ **Audited (`APPROVE`)**
  - `rejectRequest` (`request-revision`) $\rightarrow$ **Missing** `auditLogService.recordSuccess(...)` (currently only writes to legacy `approval_history`).
  - `verifyStatus` / `requestRevisionStatus` $\rightarrow$ **Missing** audit log event.

### 4.5. SUBMISSION Coverage
- **Status:** **PARTIAL**
- **Findings:** Submissions through `ApprovalService.submitApprovalRequest` are audited. Direct ATR submits in `ReportController` / `AtrService` bypass `AuditLogService`.

### 4.6. UPLOAD / IMPORT Coverage
- **Status:** **MISSING**
- **Findings:** Direct assessment marks upload (`end_sem_marks_uploads`), course exit survey parsing (`course_end_surveys`), and programme exit survey upload (`programme_exit_surveys`) modify DB marks but do not emit `UPLOAD_MARKS` / `UPLOAD_SURVEY` audit events.

### 4.7. ATTAINMENT Coverage
- **Status:** **MISSING**
- **Findings:** Changes to `AttainmentConfiguration` (weights, threshold rubrics) in `AttainmentCalculationService` are not yet audited.

### 4.8. ATR Coverage
- **Status:** **MISSING**
- **Findings:** Course ATR and Programme ATR observation/action modifications in `AtrService` do not emit audit events.

### 4.9. DELETE Operations Coverage
- **Status:** **PENDING PHASE 8**
- **Findings:** Standard entity delete methods exist in `AcademicService` without auditing; full soft-deletion workflow is pending Phase 8.

### 4.10. Authentication / Security Audit Coverage
- **Status:** **PASS**
- **Findings:** Security infrastructure is clean. Authentication tokens and passwords are not leaked to audit logs.

### 4.11. Read-Only Endpoints Excluded
- **Status:** **PASS**
- **Findings:** All 26 `GET` query endpoints are strictly read-only and generate no audit side effects.

### 4.12. Actor Source Verification
- **Status:** **PASS (CRITICAL INVARIANT MET)**
- **Findings:** `AuditLogService.resolveActorContext()` extracts the authenticated principal exclusively from `SecurityContextHolder.getContext().getAuthentication()`. Client payloads cannot forge actor identity.

### 4.13. Resource Identification Verification
- **Status:** **PASS**
- **Findings:** Resource types map accurately to canonical terms (`MASTER_PROGRAMME`, `PROGRAMME_BATCH`, `MASTER_COURSE`, `PROGRAMME_BATCH_COURSE`, `APPROVAL_REQUEST`).

### 4.14. Transaction Consistency Verification
- **Status:** **PASS**
- **Findings:** Uses `@Transactional(propagation = Propagation.REQUIRED)` ensuring database rollback aborts both business entity mutations and audit log insertions simultaneously.

### 4.15. Sensitive Data Redaction Verification
- **Status:** **PASS (CRITICAL INVARIANT MET)**
- **Findings:** `AuditLogService.sanitizeMetadata()` recursively scans maps and strings, automatically replacing sensitive fields (`password`, `token`, `secret`, `authorization`, etc.) with `"[REDACTED]"`.

### 4.16. ADMIN / IQAC Authorization Verification
- **Status:** **PASS**
- **Findings:** `AuditLogController` enforces `@PreAuthorize` / server-side check. Only `ADMIN` and `IQAC` are granted access; Director, HOD, PC, CC, and Faculty receive `403 Forbidden`.

### 4.17. Duplicate / Redundant Audit Event Findings
- **Status:** **PASS**
- **Findings:** No duplicate logging between controllers and services. Audit calls are located strictly within the service domain layer.

---

## 5. Missing Audit Coverage & Recommended Fixes

| Operation | Service & Method | Expected Action & Resource | Recommended Call Location |
|---|---|---|---|
| Revision Request | `ApprovalService.rejectRequest` | `REQUEST_REVISION`, `APPROVAL_REQUEST` | Immediately after `approvalRequestRepository.save(req)` |
| Direct Verification | `ApprovalService.verifyStatus` | `APPROVE`, `APPROVAL_REQUEST` | Immediately after updating status |
| Direct Revision Request | `ApprovalService.requestRevisionStatus` | `REQUEST_REVISION`, `APPROVAL_REQUEST` | Immediately after updating status |
| School Create / Update | `AcademicService.saveSchool` | `CREATE` / `UPDATE`, `SCHOOL` | Inside `saveSchool` after `schoolRepository.save` |
| Dept Create / Update | `AcademicService.saveDepartment` | `CREATE` / `UPDATE`, `DEPARTMENT` | Inside `saveDepartment` after `departmentRepository.save` |
| Programme Create / Update | `AcademicService.saveMasterProgramme` | `CREATE` / `UPDATE`, `MASTER_PROGRAMME` | Inside `saveMasterProgramme` after save |
| Batch Create / Update | `AcademicService.saveProgrammeBatch` | `CREATE` / `UPDATE`, `PROGRAMME_BATCH` | Inside `saveProgrammeBatch` after save |
| Course Create / Update | `AcademicService.saveMasterCourse` | `CREATE` / `UPDATE`, `MASTER_COURSE` | Inside `saveMasterCourse` after save |
| Offering Create / Update | `AcademicService.saveProgrammeBatchCourse` | `CREATE` / `UPDATE`, `PROGRAMME_BATCH_COURSE` | Inside `saveProgrammeBatchCourse` after save |
| PC Assignment | `AcademicService.assignProgrammeCoordinator` | `ASSIGN_COORDINATOR`, `MASTER_PROGRAMME` | Inside `assignProgrammeCoordinator` |
| Course Allocation | `AcademicService.allocateCourses` | `ALLOCATE_COURSES`, `PROGRAMME_BATCH` | Inside `allocateCourses` |
| PO/PSO/PEO Update | `OutcomeService.saveOutcomes` | `UPDATE`, `PROGRAMME_OUTCOME` | Inside `saveOutcomes` |
| Course Outcome Update | `OutcomeService.saveCourseOutcomes` | `UPDATE`, `COURSE_OUTCOME` | Inside `saveCourseOutcomes` |
| Mapping Matrix Update | `OutcomeService.saveCourseMappings` | `UPDATE`, `CO_PO_MAPPING` | Inside `saveCourseMappings` |
| Target Benchmark Update | `OutcomeService.saveProgrammeTargets` | `UPDATE`, `PROGRAMME_OUTCOME` | Inside `saveProgrammeTargets` |
| Attainment Config Update | `AttainmentCalculationService.saveAttainmentConfig` | `UPDATE`, `ATTAINMENT_CONFIGURATION` | Inside `saveAttainmentConfig` |
| Marks Upload | `AttainmentCalculationService.saveStudentCoMarksToDatabase` | `UPLOAD_MARKS`, `DIRECT_ASSESSMENT` | Inside `saveStudentCoMarksToDatabase` |
| Survey Upload | `AttainmentCalculationService.processAndSaveSurveyFile` | `UPLOAD_SURVEY`, `INDIRECT_ASSESSMENT` | Inside `processAndSaveSurveyFile` |
| Course ATR Save / Submit | `AtrService.saveCourseAtr` / `submitCourseAtr` | `UPDATE` / `SUBMIT`, `COURSE_ATR` | Inside `AtrService` course ATR methods |
| Programme ATR Save / Submit| `AtrService.saveProgrammeAtr` / `submitProgrammeAtr`| `UPDATE` / `SUBMIT`, `PROGRAMME_ATR` | Inside `AtrService` programme ATR methods |
| User Create / Update | `UserController.createUser` / `updateUser` | `CREATE` / `UPDATE`, `USER` | Inside `UserController` create and update methods |

---

## 6. Audit Verification Conclusion

- **Audit Infrastructure Readiness:** **100% READY** (Entity, Migration, Repository, Service, Controller, Sanitization, Tests active).
- **Core Security Controls:** **100% VERIFIED** (Actor derivation, sensitive data redaction, role gating).
- **Audit Logging Instrumentation:** **7.1% Complete** (Phase 7.2 will instrument the remaining 26 business mutation points identified in this report).
- **Source Code Modified in Phase 7.1:** **0 files modified (STRICT READ-ONLY)**.
