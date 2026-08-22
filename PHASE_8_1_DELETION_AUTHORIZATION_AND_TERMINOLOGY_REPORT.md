# PHASE 8.1 — DELETION AUTHORIZATION CORRECTION & DOMAIN TERMINOLOGY REPORT

**Phase:** Phase 8.1 — Deletion Authorization Correction & Domain Terminology Consistency  
**Status:** **COMPLETED & VERIFIED (218 / 218 TESTS PASSING)**  
**Build Status:** `./mvnw clean package` → **BUILD SUCCESS** (0 failures, 0 errors, 0 skipped)

---

## 1. Executive Summary

Phase 8.1 delivers two critical architectural alignments:
1. **Strict Hierarchical Deletion Authorization**: Ensures `ADMIN` and `IQAC` roles cannot bypass the academic deletion hierarchy. Only `PROGRAMME_COORDINATOR` can request `ProgrammeBatchCourse` deletion (reviewed/executed exclusively by `HOD`), and only `HOD` can request `ProgrammeBatch` deletion (reviewed/executed exclusively by `DIRECTOR`).
2. **Audit & Log Access Isolation**: `ADMIN` and `IQAC` retain full read access to centralized `AuditLog` records while having zero execution or deletion privileges.
3. **Canonical Domain Terminology**: Verified and standardized all deletion workflows, DTOs, messages, and audit resources to use `ProgrammeBatchCourse` and `ProgrammeBatch`.

---

## 2. Final Deletion Authorization Matrix

| Operation | Target Resource | PROGRAMME_COORDINATOR | HOD | DIRECTOR | ADMIN | IQAC | FACULTY |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Request Deletion** | `ProgrammeBatchCourse` | **ALLOWED (Within Scope)** | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) |
| **Reject Deletion** | `ProgrammeBatchCourse` | DENIED (`403`) | **ALLOWED (Within Dept)** | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) |
| **Execute Deletion** | `ProgrammeBatchCourse` | DENIED (`403`) | **ALLOWED (With Password)** | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) |
| **Request Deletion** | `ProgrammeBatch` | DENIED (`403`) | **ALLOWED (Within Scope)** | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) |
| **Reject Deletion** | `ProgrammeBatch` | DENIED (`403`) | DENIED (`403`) | **ALLOWED (Within School)** | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) |
| **Execute Deletion** | `ProgrammeBatch` | DENIED (`403`) | DENIED (`403`) | **ALLOWED (With Password)** | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) |
| **Read Audit Logs** | `AuditLog` | DENIED (`403`) | DENIED (`403`) | DENIED (`403`) | **ALLOWED (`200`)** | **ALLOWED (`200`)** | DENIED (`403`) |

---

## 3. Server-Side Scope & Security Invariants

- **Self-Approval Protection**: Requesters cannot review, reject, or execute their own deletion requests (`403 Forbidden`).
- **Cross-Scope Enforcement**:
  - HODs can only review/execute `ProgrammeBatchCourse` deletions where the course belongs to a batch within their assigned department.
  - Directors can only review/execute `ProgrammeBatch` deletions where the batch belongs to their assigned school.
- **Credential Verification**: Execution strictly requires the reviewer's own account password, authenticated via `PasswordEncoder` against the stored hash. Passwords are never logged or stored.
- **Permanent Soft-Deletion**: Records are marked with `deleted_at`, `deleted_by`, and `status = "DELETED"`. No physical `DELETE` or purge mechanisms exist.

---

## 4. Domain Terminology Audit Findings

| Context | Prior Terminology | Canonical Target Terminology | Status |
|---|---|---|:---:|
| Deletion Requests Table | `programme_batch_course_id` | `programme_batch_course_id` | **ALIGNED** |
| DTO Field Names | `programmeBatchCourseId` | `programmeBatchCourseId` | **ALIGNED** |
| ResourceType Enum | `PROGRAMME_BATCH_COURSE` | `PROGRAMME_BATCH_COURSE` | **ALIGNED** |
| Audit Actions | `DELETE_REQUESTED`, `DELETE_APPROVED`, `DELETE_REJECTED`, `DELETE_EXECUTED` | `DELETE_REQUESTED`, `DELETE_APPROVED`, `DELETE_REJECTED`, `DELETE_EXECUTED` | **ALIGNED** |
| Service Error Messages | "ProgrammeBatchCourse" | "ProgrammeBatchCourse" | **ALIGNED** |

---

## 5. Build and Test Verification

```bash
$ ./mvnw clean package
...
[INFO] Results:
[INFO] Tests run: 218, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] --- jar:3.4.2:jar (default-jar) @ obe-backend ---
[INFO] Building jar: /Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/target/obe-backend-1.0.0.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time: 31.760 s
```
