# PHASE 6.1 — API TERMINOLOGY & APPROVAL CONTRACT CLEANUP REPORT

**Phase:** Phase 6.1 — API Terminology & Approval Contract Cleanup  
**Status:** **COMPLETED & VERIFIED (196 / 196 TESTS PASSING)**  
**Build Status:** `./mvnw test` → **BUILD SUCCESS** (0 failures, 0 errors, 0 skipped)

---

## 1. Objective

Phase 6.1 performed a controlled cleanup of API layer identifier terminology and normalized the approval workflow contract to the final four-state machine (`DRAFT`, `PENDING`, `APPROVED`, `REVISION_REQUESTED`) without modifying core domain entities, database migrations, authentication, or mathematical calculations.

---

## 2. Files Inspected & Modified

### Modified Files:
- `obe-backend/src/main/java/com/dypiu/nba/dto/CourseCoordinatorSetupProgressDto.java`
- `obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeCoordinatorSetupProgressDto.java`
- `obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeTargetDto.java`
- `obe-backend/src/main/java/com/dypiu/nba/service/ApprovalService.java`
- `obe-backend/src/main/java/com/dypiu/nba/controller/DashboardController.java`
- `obe-backend/src/test/java/com/dypiu/nba/security/ApprovalWorkflowSecurityTest.java`
- `obe-backend/src/test/java/com/dypiu/nba/service/FrontendContractHardeningIntegrationTest.java`

---

## 3. Legacy Identifiers & Semantic Mapping

| Legacy Identifier | Canonical Identifier | Semantic Resource | Status |
|---|---|---|:---:|
| `courseOfferingId` / `courseId` | `programmeBatchCourseId` | `ProgrammeBatchCourse` instance | **Canonicalized** (Backward compatibility getters/setters preserved) |
| `programmeId` | `masterProgrammeId` | `MasterProgramme` syllabus catalogue | **Canonicalized** (Backward compatibility getters/setters preserved) |
| `batchId` | `programmeBatchId` | `ProgrammeBatch` cohort | **Canonicalized** (Backward compatibility getters/setters preserved) |
| `courseId` | `masterCourseId` | `MasterCourse` course definition | **Canonicalized** |

---

## 4. Approval Terminology & State Machine Model

### 4.1. Authoritative States
- `DRAFT`: Initial draft state prior to formal submission.
- `PENDING`: Formally submitted and awaiting designated role evaluation.
- `APPROVED`: Formally verified and locked by authorized approver.
- `REVISION_REQUESTED`: Returned by approver for specific coordinator corrections.

### 4.2. Eliminated States
- `NEEDS_REVISION` $\rightarrow$ Consolidated to canonical `REVISION_REQUESTED`.
- `REJECTED` $\rightarrow$ Eliminated from canonical workflow in favor of `REVISION_REQUESTED`.

### 4.3. Authoritative State Transitions
```
   [ DRAFT ] ── submit ──► [ PENDING ] ── approve ──► [ APPROVED ]
                              │
                        request-revision
                              ▼
                   [ REVISION_REQUESTED ]
                              │
                           resubmit
                              ▼
                         [ PENDING ]
```

---

## 5. Security Regression & Invariant Verification

- **Server-Side Verification**: Status is strictly state; authorization is derived from `SecurityContext`.
- **Role Authority**:
  - CC / Faculty can submit `ATTAINMENT_CONFIGURATION`, `CO_DEFINITION`/`CO_TARGETS`, `COURSE_ATR`.
  - PC approves CC submissions, and submits `COURSE_ALLOCATION`, `PO_PSO_TARGETS`, `PROGRAMME_ATR`.
  - HOD approves PC submissions.
  - Cross-role approvals and self-approvals return `403 Forbidden`.
- **JWT & Auth Preservation**: All Spring Security infrastructure and user scoping intact.

---

## 6. Mathematical Calculation & Database Preservation

- Direct Attainment formula ($0.80 \times \text{Direct} + 0.20 \times \text{Indirect}$) unchanged.
- Marksheets parsing, Bloom taxonomy weighting, and survey calculation engines unchanged.
- Database migrations (`V1__init_authoritative_academic_schema.sql`) and JPA entities unchanged.

---

## 7. Test Results

```bash
$ ./mvnw test
[INFO] Results:
[INFO] Tests run: 196, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
