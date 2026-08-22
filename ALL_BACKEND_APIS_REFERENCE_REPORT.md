# DYPIU NBA ATTAINMENT BACKEND — COMPLETE AUTHORITATIVE API REFERENCE REPORT

**Total Distinct Endpoint Groups:** 19  
**Architecture Hierarchy:** School $\rightarrow$ Department $\rightarrow$ MasterProgramme $\rightarrow$ (MasterCourse, ProgrammeBatch $\rightarrow$ ProgrammeBatchCourse)  
**Standard Response Envelope:** All JSON responses follow the `ApiResponse<T>` wrapper:
```json
{
  "success": true,
  "message": "Optional human-readable message",
  "data": { ... }
}
```

---

## TABLE OF CONTENTS
1. [Authentication & Account Management APIs (`/auth`)](#1-authentication--account-management-apis-auth)
2. [School Management & Director APIs (`/academic/schools`, `/academic/director`)](#2-school-management--director-apis-academicschools-academicdirector)
3. [Department Management & HOD APIs (`/academic/departments`, `/academic/hod`)](#3-department-management--hod-apis-academicdepartments-academichod)
4. [Master Programme Management APIs (`/academic/programmes`, `/academic/master-programmes`)](#4-master-programme-management-apis-academicprogrammes-academicmaster-programmes)
5. [Programme Batch Management APIs (`/academic/batches`, `/academic/programme-batches`)](#5-programme-batch-management-apis-academicbatches-academicprogramme-batches)
6. [Master Course (Catalogue) APIs (`/academic/courses`, `/academic/master-courses`)](#6-master-course-catalogue-apis-academiccourses-academicmaster-courses)
7. [Programme Batch Course (Offering) & Allocation APIs (`/academic/course-offerings`, `/academic/programme-batch-courses`)](#7-programme-batch-course-offering--allocation-apis-academiccourse-offerings-academicprogramme-batch-courses)
8. [Student Management APIs (`/academic/students`, `/academic/batches/.../students`)](#8-student-management-apis-academicstudents-academicbatchesstudents)
9. [Outcome Framework APIs — PO, PSO, PEO & CO (`/academic/outcomes`, `/outcomes`)](#9-outcome-framework-apis--po-pso-peo--co-academicoutcomes-outcomes)
10. [CO-PO & CO-PSO Mapping Matrix APIs (`/academic/courses/.../mapping`, `/mappings`)](#10-co-po--co-pso-mapping-matrix-apis-academiccoursesmapping-mappings)
11. [Attainment Configuration & Calculation APIs (`/attainment`)](#11-attainment-configuration--calculation-apis-attainment)
12. [Action Taken Reports (ATR) APIs — Course & Programme (`/atr`, `/reports/*-atr`)](#12-action-taken-reports-atr-apis--course--programme-atr-reportsatr)
13. [Reports, Analytics & File Export APIs (`/reports`)](#13-reports-analytics--file-export-apis-reports)
14. [Approval Workflow & Verification APIs (`/approvals`)](#14-approval-workflow--verification-apis-approvals)
15. [Role-Scoped Dashboard APIs (`/dashboard`)](#15-role-scoped-dashboard-apis-dashboard)
16. [User & Academic Staff Management APIs (`/users`, `/academic/users`)](#16-user--academic-staff-management-apis-users-academicusers)
17. [System & Health Monitoring APIs (`/health`)](#17-system--health-monitoring-apis-health)
18. [Centralized Audit Logging APIs (`/audit-logs`)](#18-centralized-audit-logging-apis-audit-logs)
19. [Hierarchical Deletion Request & Soft Delete APIs (`/deletion-requests`)](#19-hierarchical-deletion-request--soft-delete-apis-deletion-requests)

---

## 1. Authentication & Account Management APIs (`/auth`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `POST /auth/login` | **JSON Body:**<br>```json{"usernameOrEmail": "string", "password": "string"}``` | **JSON Response:**<br>```json{"success": true, "data": {"token": "jwt_token", "refreshToken": "jwt_refresh", "userId": 1, "username": "user", "name": "Dr. Name", "email": "email@dypiu.ac.in", "role": "HOD", "schoolId": "sch-1", "departmentId": "dept-1", "programmeId": "prog-1"}}``` | Authenticates user credentials and returns JWT bearer tokens with scoped organizational claims. |
| `POST /auth/register` | **JSON Body:**<br>```json{"username": "string", "email": "string", "password": "string", "name": "string", "role": "FACULTY", "schoolId": "sch-1", "departmentId": "dept-1", "programmeId": "prog-1"}``` | **JSON Response:**<br>```json{"success": true, "message": "User registered successfully", "data": {"id": 1, "username": "user", "email": "email@dypiu.ac.in", "role": "FACULTY"}}``` | Registers a new user account with assigned organizational scope. |
| `POST /auth/refresh-token` | **JSON Body:**<br>```json{"refreshToken": "string"}``` | **JSON Response:**<br>```json{"success": true, "data": {"token": "new_jwt_token", "refreshToken": "new_refresh_token"}}``` | Generates a fresh JWT access token from a valid unexpired refresh token. |
| `POST /auth/forgot-password` | **JSON Body:**<br>```json{"email": "string"}``` | **JSON Response:**<br>```json{"success": true, "message": "Password reset OTP sent to registered email", "data": null}``` | Dispatches a 6-digit one-time password (OTP) to the user's registered email address. |
| `POST /auth/verify-otp` | **JSON Body:**<br>```json{"email": "string", "otp": "123456"}``` | **JSON Response:**<br>```json{"success": true, "message": "OTP verified successfully", "data": {"verified": true}}``` | Validates the OTP for password recovery. |
| `POST /auth/reset-password` | **JSON Body:**<br>```json{"email": "string", "otp": "123456", "newPassword": "string"}``` | **JSON Response:**<br>```json{"success": true, "message": "Password reset successfully. You can now login.", "data": null}``` | Resets the account password upon successful OTP verification. |

---

## 2. School Management & Director APIs (`/academic/schools`, `/academic/director`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/schools` | *Query Params (Optional):* None | **JSON Response:**<br>```json{"success": true, "data": [{"id": "sch-1", "code": "SOE", "name": "School of Engineering", "directorName": "Dr. Patil", "directorEmail": "director.soe@dypiu.ac.in", "status": "ACTIVE"}]}``` | Returns all schools accessible within the caller's authorized scope. |
| `GET /academic/schools/{id}` | *Path Variable:* `id` (String) | **JSON Response:**<br>```json{"success": true, "data": {"id": "sch-1", "code": "SOE", "name": "School of Engineering", "directorName": "Dr. Patil", "directorEmail": "director.soe@dypiu.ac.in", "status": "ACTIVE"}}``` | Retrieves detailed metadata for a specific school by its ID. |
| `POST /academic/schools` | **JSON Body:**<br>```json{"code": "SOE", "name": "School of Engineering", "directorName": "Dr. Patil", "directorEmail": "director.soe@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "message": "School saved successfully", "data": {"id": "sch-1", "code": "SOE", "name": "School of Engineering"}}``` | Creates a new school and links the director user. |
| `PUT /academic/schools/{id}` | *Path Variable:* `id`<br>**JSON Body:**<br>```json{"name": "School of Engineering Updated", "directorName": "Dr. New Director", "directorEmail": "new.director@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "message": "School updated successfully", "data": {"id": "sch-1", "name": "School of Engineering Updated"}}``` | Updates existing school metadata and director assignment. |
| `DELETE /academic/schools/{id}` | *Path Variable:* `id` (String) | **JSON Response:**<br>```json{"success": true, "message": "School deleted successfully", "data": null}``` | Deletes a school if caller has ADMIN / IQAC permissions. |
| `GET /academic/director/school-summary` | *Query Params (Optional):* `directorEmail` | **JSON Response:**<br>```json{"success": true, "data": {"schoolId": "sch-1", "schoolCode": "SOE", "schoolName": "School of Engineering", "directorEmail": "director@dypiu.ac.in", "totalDepartments": 4, "totalProgrammes": 8, "totalFaculty": 45, "setupProgress": {...}}}``` | Screen-oriented API for Director Dashboard overview. |
| `GET /academic/director/department-summary` | *Query Params (Optional):* `schoolId`, `directorEmail` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "dept-1", "name": "CSE", "code": "CSE", "hodName": "Dr. Joshi", "hodEmail": "hod.cse@dypiu.ac.in", "programmeCount": 3, "facultyCount": 20}]}``` | Screen-oriented API listing department summary statistics for director view. |
| `GET /academic/director/setup-progress` | *Query Params (Optional):* `directorEmail`, `schoolId` | **JSON Response:**<br>```json{"success": true, "data": {"id": "dsp-1", "schoolId": "sch-1", "currentStep": 1, "completedSteps": ["0","1"], "pendingSteps": ["2","3"], "overallStatus": "IN_PROGRESS"}}``` | Retrieves Director onboarding and setup wizard milestone progress. |
| `POST /academic/director/setup-progress`<br>`PUT /academic/director/setup-progress` | **JSON Body:**<br>```json{"stepNumber": 2, "completedSteps": ["0","1","2"], "directorEmail": "director@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "data": {"id": "dsp-1", "schoolId": "sch-1", "currentStep": 2, "completedSteps": ["0","1","2"], "overallStatus": "IN_PROGRESS"}}``` | Updates Director onboarding setup progress and step status. |

---

## 3. Department Management & HOD APIs (`/academic/departments`, `/academic/hod`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/departments` | *Query Params (Optional):* `schoolId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "dept-1", "schoolId": "sch-1", "code": "CSE", "name": "Computer Science & Engineering", "hod": "Dr. Joshi", "hodEmail": "hod.cse@dypiu.ac.in", "status": "ACTIVE"}]}``` | Returns departments scoped by caller authority or filtered by `schoolId`. |
| `GET /academic/departments/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "data": {"id": "dept-1", "schoolId": "sch-1", "code": "CSE", "name": "Computer Science & Engineering", "hod": "Dr. Joshi", "hodEmail": "hod.cse@dypiu.ac.in"}}``` | Retrieves detailed metadata for a specific department. |
| `POST /academic/departments` | **JSON Body:**<br>```json{"schoolId": "sch-1", "code": "CSE", "name": "Computer Science & Engineering", "hod": "Dr. Joshi", "hodEmail": "hod.cse@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "message": "Department saved successfully", "data": {"id": "dept-1", "code": "CSE", "name": "Computer Science & Engineering"}}``` | Creates a new department under a school and synchronizes HOD user. |
| `PUT /academic/departments/{id}` | *Path Variable:* `id`<br>**JSON Body:**<br>```json{"name": "Computer Science Updated", "hod": "Dr. New HOD", "hodEmail": "new.hod@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "message": "Department updated successfully", "data": {"id": "dept-1", "name": "Computer Science Updated"}}``` | Updates department information and assigned HOD. |
| `DELETE /academic/departments/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "message": "Department deleted successfully", "data": null}``` | Deletes a department if within caller's administrative scope. |
| `GET /academic/hod/department-summary` | *Query Params (Optional):* `hodEmail`, `deptId` | **JSON Response:**<br>```json{"success": true, "data": {"deptId": "dept-1", "deptCode": "CSE", "deptName": "Computer Science", "hodEmail": "hod@dypiu.ac.in", "programmeCount": 3, "facultyCount": 20, "studentCount": 450, "setupProgress": {...}}}``` | Screen-oriented API for HOD Dashboard overview. |
| `GET /academic/hod/setup-progress` | *Query Params (Optional):* `hodEmail`, `deptId` | **JSON Response:**<br>```json{"success": true, "data": {"id": "hsp-1", "departmentId": "dept-1", "currentStep": 1, "completedSteps": ["0"], "pendingSteps": ["1","2","3"], "overallStatus": "IN_PROGRESS"}}``` | Retrieves HOD onboarding setup progress. |
| `POST /academic/hod/setup-progress`<br>`PUT /academic/hod/setup-progress` | **JSON Body:**<br>```json{"stepNumber": 2, "completedSteps": ["0","1"], "hodEmail": "hod@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "data": {"id": "hsp-1", "departmentId": "dept-1", "currentStep": 2, "overallStatus": "IN_PROGRESS"}}``` | Updates HOD onboarding setup step completion. |
| `GET /academic/hod/coordinators` | *Query Params (Optional):* `departmentId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "prog-1", "programmeCode": "BTECH-CSE", "programmeName": "B.Tech CSE", "coordinator": "Dr. Rahul", "coordinatorEmail": "rahul@dypiu.ac.in", "assignedDate": "2025-06-15"}]}``` | Returns list of programmes and their assigned Programme Coordinators for HOD screen. |
| `POST /academic/hod/coordinators`<br>`PUT /academic/hod/coordinators` | **JSON Body:**<br>```json{"programmeId": "prog-1", "coordinatorName": "Dr. Rahul Verma", "coordinatorEmail": "rahul@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "message": "Programme Coordinator assigned successfully", "data": {...}}``` | Assigns or updates a Programme Coordinator to a MasterProgramme. |

---

## 4. Master Programme Management APIs (`/academic/programmes`, `/academic/master-programmes`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/programmes`<br>`GET /academic/master-programmes` | *Query Params (Optional):* `departmentId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "prog-1", "departmentId": "dept-1", "code": "BTECH-CSE", "name": "B.Tech Computer Science", "durationYears": 4, "coordinator": "Dr. Rahul", "coordinatorEmail": "rahul@dypiu.ac.in", "status": "ACTIVE"}]}``` | Returns reusable MasterProgramme syllabus catalogue definitions. |
| `GET /academic/programmes/{id}`<br>`GET /academic/master-programmes/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "data": {"id": "prog-1", "departmentId": "dept-1", "code": "BTECH-CSE", "name": "B.Tech Computer Science", "durationYears": 4, "coordinator": "Dr. Rahul"}}``` | Retrieves detailed MasterProgramme definition by ID. |
| `POST /academic/programmes`<br>`POST /academic/master-programmes` | **JSON Body:**<br>```json{"departmentId": "dept-1", "code": "BTECH-CSE", "name": "B.Tech Computer Science", "durationYears": 4, "coordinator": "Dr. Rahul", "coordinatorEmail": "rahul@dypiu.ac.in"}``` | **JSON Response:**<br>```json{"success": true, "message": "MasterProgramme saved successfully", "data": {"id": "prog-1", "code": "BTECH-CSE", "name": "B.Tech Computer Science"}}``` | Creates or updates a MasterProgramme catalogue entry. |
| `DELETE /academic/programmes/{id}`<br>`DELETE /academic/master-programmes/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "message": "MasterProgramme deleted successfully", "data": null}``` | Deletes a MasterProgramme if within caller's scope and has no active dependencies. |

---

## 5. Programme Batch Management APIs (`/academic/batches`, `/academic/programme-batches`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/batches`<br>`GET /academic/programme-batches` | *Query Params (Optional):* `programmeId`, `masterProgrammeId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "batch-1", "masterProgrammeId": "prog-1", "name": "2022-2026", "startYear": 2022, "endYear": 2026, "academicYear": "2022-26", "status": "ACTIVE"}]}``` | Returns list of ProgrammeBatch cohort containers. |
| `GET /academic/batches/{id}`<br>`GET /academic/programme-batches/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "data": {"id": "batch-1", "masterProgrammeId": "prog-1", "name": "2022-2026", "startYear": 2022, "endYear": 2026, "status": "ACTIVE"}}``` | Retrieves specific ProgrammeBatch cohort details. |
| `POST /academic/batches`<br>`POST /academic/programme-batches` | **JSON Body:**<br>```json{"masterProgrammeId": "prog-1", "name": "2022-2026", "startYear": 2022, "endYear": 2026, "status": "ACTIVE"}``` | **JSON Response:**<br>```json{"success": true, "message": "ProgrammeBatch saved successfully", "data": {"id": "batch-1", "name": "2022-2026"}}``` | Creates a new cohort ProgrammeBatch under a MasterProgramme. |
| `DELETE /academic/batches/{id}`<br>`DELETE /academic/programme-batches/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "message": "ProgrammeBatch deleted successfully", "data": null}``` | Deletes a ProgrammeBatch cohort if authorized. |
| `GET /academic/programme-coordinator/summary` | *Query Params (Optional):* `coordinatorEmail`, `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "data": {"masterProgrammeId": "prog-1", "programmeBatchId": "batch-1", "programmeName": "B.Tech CSE", "programmeCode": "BTCS", "totalCoursesCount": 42, "activePOsCount": 12, "activePSOsCount": 3, "setupProgress": {...}}}``` | Screen-oriented API for Programme Coordinator Dashboard overview. |
| `GET /academic/coordinator/setup-progress` | *Query Params (Optional):* `coordinatorEmail`, `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "data": {"id": "pcsp-1", "masterProgrammeId": "prog-1", "programmeBatchId": "batch-1", "currentStep": 1, "completedSteps": ["0"], "pendingSteps": ["1","2","3"], "overallStatus": "IN_PROGRESS"}}``` | Retrieves PC setup wizard milestone progress. |
| `POST /academic/coordinator/setup-progress`<br>`PUT /academic/coordinator/setup-progress` | **JSON Body:**<br>```json{"programmeId": "prog-1", "batchId": "batch-1", "stepNumber": 2, "completedSteps": ["0","1"]}``` | **JSON Response:**<br>```json{"success": true, "data": {"id": "pcsp-1", "masterProgrammeId": "prog-1", "programmeBatchId": "batch-1", "currentStep": 2, "overallStatus": "IN_PROGRESS"}}``` | Updates PC onboarding setup step status. |
| `POST /academic/coordinator/complete-setup` | *Query Params (Optional):* `coordinatorEmail`, `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "message": "Programme Coordinator setup completed successfully", "data": {"overallStatus": "COMPLETED"}}``` | Marks all PC setup wizard steps as COMPLETED. |

---

## 6. Master Course (Catalogue) APIs (`/academic/courses`, `/academic/master-courses`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/courses`<br>`GET /academic/master-courses` | *Query Params (Optional):* `programmeId`, `masterProgrammeId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "crs-1", "masterProgrammeId": "prog-1", "code": "CS301", "name": "Data Structures", "credits": 4, "courseType": "THEORY", "status": "ACTIVE"}]}``` | Returns reusable MasterCourse catalogue items. |
| `GET /academic/courses/{id}`<br>`GET /academic/master-courses/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "data": {"id": "crs-1", "masterProgrammeId": "prog-1", "code": "CS301", "name": "Data Structures", "credits": 4, "courseType": "THEORY"}}``` | Retrieves detailed MasterCourse definition. |
| `POST /academic/courses`<br>`POST /academic/master-courses` | **JSON Body:**<br>```json{"masterProgrammeId": "prog-1", "code": "CS301", "name": "Data Structures", "credits": 4, "courseType": "THEORY", "status": "ACTIVE"}``` | **JSON Response:**<br>```json{"success": true, "message": "MasterCourse saved successfully", "data": {"id": "crs-1", "code": "CS301", "name": "Data Structures"}}``` | Creates or updates a MasterCourse catalogue item. |
| `DELETE /academic/courses/{id}`<br>`DELETE /academic/master-courses/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "message": "MasterCourse deleted successfully", "data": null}``` | Deletes a MasterCourse catalogue entry. |

---

## 7. Programme Batch Course (Offering) & Allocation APIs (`/academic/course-offerings`, `/academic/programme-batch-courses`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/course-offerings`<br>`GET /academic/programme-batch-courses` | *Query Params (Optional):* `batchId`, `programmeBatchId`, `courseId`, `masterCourseId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "off-1", "programmeBatchId": "batch-1", "masterCourseId": "crs-1", "semester": 5, "academicYear": "2024-25", "courseCoordinatorId": 12, "courseCoordinatorName": "Prof. Smith", "status": "ACTIVE"}]}``` | Returns batch-specific course offering instances. |
| `GET /academic/course-offerings/{offeringId}`<br>`GET /academic/programme-batch-courses/{offeringId}` | *Path Variable:* `offeringId` | **JSON Response:**<br>```json{"success": true, "data": {"id": "off-1", "programmeBatchId": "batch-1", "masterCourseId": "crs-1", "semester": 5, "courseCoordinatorName": "Prof. Smith"}}``` | Retrieves a specific ProgrammeBatchCourse offering instance. |
| `POST /academic/course-offerings`<br>`POST /academic/programme-batch-courses` | **JSON Body:**<br>```json{"programmeBatchId": "batch-1", "masterCourseId": "crs-1", "semester": 5, "academicYear": "2024-25", "courseCoordinatorId": 12, "assignedFaculty": ["prof.smith@dypiu.ac.in"]}``` | **JSON Response:**<br>```json{"success": true, "message": "Course Offering saved successfully", "data": {"id": "off-1", "semester": 5}}``` | Creates or updates a ProgrammeBatchCourse instance and faculty allocation. |
| `POST /academic/courses/allocate`<br>`POST /academic/courses/allocate-submit` | **JSON Body:**<br>```json{"programmeId": "prog-1", "batchId": "batch-1", "allocations": [{"courseId": "crs-1", "coordinator": "Prof. Smith", "coordinatorEmail": "smith@dypiu.ac.in"}], "submit": true}``` | **JSON Response:**<br>```json{"success": true, "message": "Course allocations saved/submitted successfully", "data": {"allocatedCount": 5, "approvalRequestId": "app-1"}}``` | Saves or formally submits batch course allocations with approval request trigger. |
| `GET /academic/course-coordinator/summary` | *Query Params (Optional):* `coordinatorEmail` | **JSON Response:**<br>```json{"success": true, "data": {"coordinatorEmail": "smith@dypiu.ac.in", "coordinatorName": "Prof. Smith", "courseCode": "CS301", "courseName": "Data Structures", "assignedCourseCount": 2, "courseOutcomesCount": 6, "setupProgress": {...}}}``` | Screen-oriented API for Course Coordinator / Faculty Dashboard. |
| `GET /academic/course-coordinator/setup-progress` | *Query Params (Optional):* `coordinatorEmail`, `courseId` | **JSON Response:**<br>```json{"success": true, "data": {"id": "ccsp-1", "programmeBatchCourseId": "off-1", "currentStep": 1, "completedSteps": ["0"], "pendingSteps": ["1","2","3","4"], "updatedAt": "2026-08-21T10:00:00Z"}}``` | Retrieves Course Coordinator onboarding setup progress. |
| `POST /academic/course-coordinator/setup-progress`<br>`PUT /academic/course-coordinator/setup-progress` | **JSON Body:**<br>```json{"courseId": "off-1", "stepNumber": 2, "completedSteps": ["0","1"]}``` | **JSON Response:**<br>```json{"success": true, "data": {"id": "ccsp-1", "programmeBatchCourseId": "off-1", "currentStep": 2}}``` | Updates Course Coordinator onboarding step progress. |
| `POST /academic/course-coordinator/complete-setup` | *Query Params (Optional):* `coordinatorEmail`, `courseId` | **JSON Response:**<br>```json{"success": true, "message": "Course Coordinator setup completed successfully", "data": null}``` | Marks all Course Coordinator setup steps as completed. |

---

## 8. Student Management APIs (`/academic/students`, `/academic/batches/.../students`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/students` | *Query Params (Optional):* `batchId`, `programmeBatchId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "std-1", "prn": "20241413001", "name": "Alice Johnson", "batchId": "batch-1", "email": "alice@dypiu.ac.in", "rollNo": "01", "status": "ACTIVE"}]}``` | Returns students enrolled in a ProgrammeBatch cohort. |
| `GET /academic/students/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "data": {"id": "std-1", "prn": "20241413001", "name": "Alice Johnson", "batchId": "batch-1"}}``` | Retrieves individual student record by ID. |
| `POST /academic/students` | **JSON Body:**<br>```json{"prn": "20241413001", "name": "Alice Johnson", "batchId": "batch-1", "email": "alice@dypiu.ac.in", "rollNo": "01"}``` | **JSON Response:**<br>```json{"success": true, "message": "Student saved successfully", "data": {"id": "std-1", "prn": "20241413001", "name": "Alice Johnson"}}``` | Creates or updates a student record. |
| `DELETE /academic/students/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "message": "Student deleted successfully", "data": null}``` | Deletes a student record. |
| `GET /academic/batches/{batchId}/students` | *Path Variable:* `batchId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "std-1", "prn": "20241413001", "name": "Alice Johnson", "batchId": "batch-1"}]}``` | Returns students scoped specifically to a ProgrammeBatch. |

---

## 9. Outcome Framework APIs — PO, PSO, PEO & CO (`/academic/outcomes`, `/outcomes`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/outcomes`<br>`GET /outcomes` | *Query Params:* `programmeId` (or `batchId`) | **JSON Response:**<br>```json{"success": true, "data": {"pos": [{"id": "po-1", "code": "PO1", "statement": "Engineering Knowledge", "target": 2.50}], "psos": [{"id": "pso-1", "code": "PSO1", "statement": "Software Architecture", "target": 2.40}], "peos": [{"id": "peo-1", "code": "PEO1", "statement": "Career Growth"}]}}``` | Returns batch-scoped POs, PSOs, and PEOs with target benchmark levels. |
| `POST /academic/outcomes`<br>`POST /outcomes` | **JSON Body:**<br>```json{"programmeId": "prog-1", "batchId": "batch-1", "pos": [{"code": "PO1", "statement": "Stmt...", "target": 2.50}], "psos": [{"code": "PSO1", "statement": "Stmt...", "target": 2.40}], "peos": [{"code": "PEO1", "statement": "Stmt..."}]}``` | **JSON Response:**<br>```json{"success": true, "message": "Outcomes saved successfully", "data": {"pos": [...], "psos": [...], "peos": [...]}}``` | Saves batch-scoped POs, PSOs, and PEOs for a ProgrammeBatch. |
| `GET /outcomes/pos` | *Query Params:* `programmeId` (or `batchId`) | **JSON Response:**<br>```json{"success": true, "data": [{"id": "po-1", "code": "PO1", "statement": "...", "target": 2.50}]}``` | Returns PO outcomes list. |
| `POST /outcomes/pos` | *Query Params:* `programmeId`<br>**JSON Body:** List of `ProgrammeOutcome` | **JSON Response:**<br>```json{"success": true, "data": [...]}``` | Saves PO outcomes. |
| `GET /outcomes/psos` | *Query Params:* `programmeId` (or `batchId`) | **JSON Response:**<br>```json{"success": true, "data": [{"id": "pso-1", "code": "PSO1", "statement": "...", "target": 2.40}]}``` | Returns PSO outcomes list. |
| `POST /outcomes/psos` | *Query Params:* `programmeId`<br>**JSON Body:** List of `ProgrammeSpecificOutcome` | **JSON Response:**<br>```json{"success": true, "data": [...]}``` | Saves PSO outcomes. |
| `GET /outcomes/peos` | *Query Params:* `programmeId` (or `batchId`) | **JSON Response:**<br>```json{"success": true, "data": [{"id": "peo-1", "code": "PEO1", "statement": "..."}]}``` | Returns PEO outcomes list. |
| `POST /outcomes/peos` | *Query Params:* `programmeId`<br>**JSON Body:** List of `PeoOutcome` | **JSON Response:**<br>```json{"success": true, "data": [...]}``` | Saves PEO outcomes. |
| `GET /academic/courses/{courseId}/outcomes`<br>`GET /outcomes/courses/{courseId}/cos` | *Path Variable:* `courseId` (programmeBatchCourseId) | **JSON Response:**<br>```json{"success": true, "data": [{"id": "co-1", "programmeBatchCourseId": "off-1", "code": "CO1", "statement": "Understand Data Structures", "bloomsLevel": "UNDERSTAND", "targetLevel": 2.50}]}``` | Returns Course Outcomes for a specific course offering. |
| `POST /academic/courses/{courseId}/outcomes`<br>`POST /outcomes/courses/{courseId}/cos` | *Path Variable:* `courseId`<br>**JSON Body:** List of `CourseOutcome` | **JSON Response:**<br>```json{"success": true, "message": "Course Outcomes saved successfully", "data": [...]}``` | Saves Course Outcomes for a specific course offering. |
| `GET /academic/courses/{courseId}/co-targets` | *Path Variable:* `courseId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "co-1", "code": "CO1", "targetLevel": 2.50}]}``` | Retrieves CO target levels. |
| `POST /academic/courses/{courseId}/co-targets`<br>`PUT /academic/courses/{courseId}/co-targets` | *Path Variable:* `courseId`<br>**JSON Body:** Map of `{ "CO1": 2.50, "CO2": 2.80 }` | **JSON Response:**<br>```json{"success": true, "message": "CO targets saved successfully", "data": [...]}``` | Updates CO target levels for an offering. |
| `GET /academic/programmes/{programmeId}/targets` | *Path Variable:* `programmeId` | **JSON Response:**<br>```json{"success": true, "data": {"masterProgrammeId": "prog-1", "programmeBatchId": "batch-1", "poTargets": {"PO1": 2.50, "PO2": 2.60}, "psoTargets": {"PSO1": 2.40}}}``` | Returns PO and PSO target benchmark levels for a batch. |
| `POST /academic/programmes/{programmeId}/targets`<br>`PUT /academic/programmes/{programmeId}/targets` | *Path Variable:* `programmeId`<br>**JSON Body:** `ProgrammeTargetDto` | **JSON Response:**<br>```json{"success": true, "message": "Programme targets saved successfully", "data": {...}}``` | Saves PO and PSO target benchmark levels. |

---

## 10. CO-PO & CO-PSO Mapping Matrix APIs (`/academic/courses/.../mapping`, `/mappings`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /academic/courses/{courseId}/mapping`<br>`GET /mappings/{courseId}` | *Path Variable:* `courseId` (programmeBatchCourseId) | **JSON Response:**<br>```json{"success": true, "data": {"courseId": "off-1", "matrix": {"CO1": {"PO1": 3, "PO2": 2, "PSO1": 3}}, "poAverages": {"PO1": 2.8, "PO2": 2.0}, "psoAverages": {"PSO1": 2.5}}}``` | Returns CO-PO and CO-PSO correlation matrix and calculated averages. |
| `POST /academic/courses/{courseId}/mapping`<br>`POST /mappings/{courseId}` | *Path Variable:* `courseId`<br>**JSON Body:** `CourseMappingMatrixDto` | **JSON Response:**<br>```json{"success": true, "message": "Mappings saved successfully", "data": {...}}``` | Saves CO-PO and CO-PSO correlation weights (1, 2, 3 or null). |
| `GET /mappings/keywords` | *Query Params (Optional):* `bloomsLevel` | **JSON Response:**<br>```json{"success": true, "data": [{"id": 1, "bloomsLevel": "APPLY", "keyword": "implement", "suggestedWeight": 3}]}``` | Returns Bloom taxonomy mapping keyword suggestions. |

---

## 11. Attainment Configuration & Calculation APIs (`/attainment`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /attainment/config/{courseId}`<br>`GET /attainment/configurations/{courseId}` | *Path Variable:* `courseId` (programmeBatchCourseId) | **JSON Response:**<br>```json{"success": true, "data": {"id": "cfg-1", "programmeBatchCourseId": "off-1", "directWeight": 0.80, "indirectWeight": 0.20, "directThreshold": 60.00, "indirectThreshold": 60.00, "status": "APPROVED"}}``` | Returns attainment configuration (direct/indirect weights, threshold rubrics). |
| `POST /attainment/config/{courseId}`<br>`PUT /attainment/config/{courseId}` | *Path Variable:* `courseId`<br>**JSON Body:** `AttainmentConfiguration` | **JSON Response:**<br>```json{"success": true, "message": "Attainment Configuration saved successfully", "data": {"id": "cfg-1", "directWeight": 0.80, "indirectWeight": 0.20}}``` | Saves attainment settings and evaluation weights for a course offering. |
| `GET /attainment/examination/{courseOfferingId}` | *Path Variable:* `courseOfferingId` | **JSON Response:**<br>```json{"success": true, "data": {"courseOfferingId": "off-1", "coDirectAttainment": {"CO1": 2.65, "CO2": 2.40}, "totalStudents": 45, "attainmentLevels": {"CO1": 3, "CO2": 2}}}``` | Returns calculated Direct (Examination) Attainment results. |
| `POST /attainment/examination/{courseOfferingId}` | *Path Variable:* `courseOfferingId`<br>**JSON Body:** `ExaminationMarksPayloadDto` | **JSON Response:**<br>```json{"success": true, "data": {"coDirectAttainment": {"CO1": 2.65}}}``` | Manually calculates and saves Direct Attainment marks. |
| `POST /attainment/examination/{courseOfferingId}/upload` | *Path Variable:* `courseOfferingId`<br>*Multipart:* `file` (Excel Marks xlsx)<br>*Form Field:* `thresholdPercentage` (60.0) | **JSON Response:**<br>```json{"success": true, "message": "Examination marks processed successfully", "data": {"studentsCount": 45, "coDirectAttainment": {"CO1": 2.65}}}``` | Parses internal marks Excel sheet, validates PRN and max marks, and calculates direct CO attainment. |
| `GET /attainment/survey/{courseOfferingId}` | *Path Variable:* `courseOfferingId` | **JSON Response:**<br>```json{"success": true, "data": {"courseOfferingId": "off-1", "coIndirectAttainment": {"CO1": 2.80, "CO2": 2.70}, "surveyResponsesCount": 42}}``` | Returns calculated Indirect (Course Exit Survey) Attainment results. |
| `POST /attainment/survey/{courseOfferingId}` | *Path Variable:* `courseOfferingId`<br>**JSON Body:** `SurveyMarksPayloadDto` | **JSON Response:**<br>```json{"success": true, "data": {"coIndirectAttainment": {"CO1": 2.80}}}``` | Manually calculates and saves Indirect Attainment survey marks. |
| `POST /attainment/survey/{courseOfferingId}/upload` | *Path Variable:* `courseOfferingId`<br>*Multipart:* `file` (Excel Survey xlsx)<br>*Form Field:* `thresholdPercentage` (60.0) | **JSON Response:**<br>```json{"success": true, "message": "Survey marks processed successfully", "data": {"responsesCount": 42, "coIndirectAttainment": {"CO1": 2.80}}}``` | Parses course exit survey responses and calculates indirect CO attainment. |
| `POST /attainment/programme-survey/upload` | *Query Params:* `programmeId`, `batchId`<br>*Multipart:* `file` (Excel Survey xlsx) | **JSON Response:**<br>```json{"success": true, "message": "Programme exit survey processed successfully", "data": {"poIndirectAttainment": {"PO1": 2.75}, "psoIndirectAttainment": {"PSO1": 2.60}}}``` | Parses Programme End Survey with strict PO/PSO header validation. |
| `GET /attainment/co-attainment/{courseOfferingId}` | *Path Variable:* `courseOfferingId` | **JSON Response:**<br>```json{"success": true, "data": {"courseOfferingId": "off-1", "directAttainment": {"CO1": 2.65}, "indirectAttainment": {"CO1": 2.80}, "overallAttainment": {"CO1": 2.68}}}``` | Returns combined Overall CO Attainment ($0.80 \times \text{Direct} + 0.20 \times \text{Indirect}$). |

---

## 12. Action Taken Reports (ATR) APIs — Course & Programme (`/atr`, `/reports/*-atr`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /atr/course/{courseId}`<br>`GET /reports/course-atr/{courseOfferingId}` | *Path Variable:* `courseId` (programmeBatchCourseId) | **JSON Response:**<br>```json{"success": true, "data": {"courseOfferingId": "off-1", "courseCode": "CS301", "courseName": "Data Structures", "items": [{"coCode": "CO1", "targetScore": 2.50, "actualScore": 2.68, "pctAchieved": 107.2, "statement": "...", "actions": ["Introduce advanced lab tutorials"]}], "status": "DRAFT"}}``` | Retrieves the complete Course ATR report for an offering. |
| `POST /atr/course/{courseId}`<br>`PUT /atr/course/{courseId}`<br>`POST /reports/course-atr` | *Path Variable:* `courseId`<br>**JSON Body:** `CourseAtrReportDto` | **JSON Response:**<br>```json{"success": true, "message": "Course ATR saved successfully", "data": {...}}``` | Saves draft Course ATR actions and observations. |
| `POST /reports/course-atr/{courseOfferingId}/submit` | *Path Variable:* `courseOfferingId` | **JSON Response:**<br>```json{"success": true, "message": "Course ATR submitted for verification", "data": {"status": "PENDING"}}``` | Formally submits Course ATR to Programme Coordinator for approval. |
| `GET /reports/course-atr/{courseOfferingId}/export-data` | *Path Variable:* `courseOfferingId` | **JSON Response:**<br>```json{"success": true, "data": {...}}``` | Exports raw Course ATR dataset for report compilation. |
| `GET /atr/programme/{programmeId}`<br>`GET /reports/programme-atr/{programmeId}/batch/{batchId}` | *Path Variables:* `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "data": {"programmeId": "prog-1", "batchId": "batch-1", "items": [{"poCode": "PO1", "targetScore": 2.50, "actualScore": 2.45, "pctAchieved": 98.0, "observations": "Strengthen practical sessions"}], "status": "DRAFT"}}``` | Retrieves the complete Programme ATR report for a batch cohort. |
| `POST /atr/programme/{programmeId}`<br>`PUT /atr/programme/{programmeId}`<br>`POST /reports/programme-atr` | *Path Variable:* `programmeId`<br>**JSON Body:** `ProgrammeAtrReportDto` | **JSON Response:**<br>```json{"success": true, "message": "Programme ATR saved successfully", "data": {...}}``` | Saves draft Programme ATR observations and action items. |
| `POST /reports/programme-atr/{programmeId}/batch/{batchId}/submit` | *Path Variables:* `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "message": "Programme ATR submitted for verification", "data": {"status": "PENDING"}}``` | Formally submits Programme ATR to HOD for approval. |
| `GET /reports/programme-atr/{programmeId}/batch/{batchId}/export-data` | *Path Variables:* `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "data": {...}}``` | Exports raw Programme ATR dataset for reporting. |

---

## 13. Reports, Analytics & File Export APIs (`/reports`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /reports/summary` | *Query Params (Optional):* `schoolId`, `departmentId`, `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "data": {"totalProgrammes": 5, "totalBatches": 12, "completedAtrs": 8, "overallAttainmentAverage": 2.54}}``` | High-level summary metrics across the academic hierarchy. |
| `GET /reports/attainment-main` | *Query Params:* `programmeId`, `batchId` | **JSON Response:**<br>```json{"success": true, "data": {"programmeId": "prog-1", "batchId": "batch-1", "courses": [...], "poAttainment": {"PO1": 2.65}, "psoAttainment": {"PSO1": 2.45}}}``` | Returns full consolidated batch attainment dataset. |
| `GET /reports/attainment-main/course/{courseOfferingId}` | *Path Variable:* `courseOfferingId` | **JSON Response:**<br>```json{"success": true, "data": {"courseOfferingId": "off-1", "courseCode": "CS301", "directAttainment": {...}, "indirectAttainment": {...}, "overallAttainment": {...}}}``` | Full course offering attainment breakdown report. |
| `GET /reports/attainment-main/course/{courseOfferingId}/excel` | *Path Variable:* `courseOfferingId` | **Binary Stream:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Generates and downloads formatted Excel Course Attainment Sheet. |
| `GET /reports/attainment-main/course/{courseOfferingId}/pdf` | *Path Variable:* `courseOfferingId` | **Binary Stream:** `application/pdf` | Generates and downloads print-ready PDF Course Attainment Report. |
| `GET /reports/programmes/{programmeId}/batch-comparison` | *Path Variable:* `programmeId`<br>*Query Params (Optional):* `batchIds` | **JSON Response:**<br>```json{"success": true, "data": {"programmeId": "prog-1", "batches": ["2022-26", "2023-27"], "poComparison": {"PO1": [2.45, 2.60]}}}``` | Longitudinal batch comparison analytics for NBA accreditation criteria. |
| `GET /reports/batch/{batchId}/summary` | *Path Variable:* `batchId` | **JSON Response:**<br>```json{"success": true, "data": {"batchId": "batch-1", "academicYear": "2022-26", "poCount": 12, "psoCount": 3, "courseCount": 40}}``` | Contextual summary for a specific batch. |
| `GET /reports/export/excel` | *Query Params (Optional):* `programmeId`, `batchId` | **Binary Stream:** Excel Sheet | Bulk export of academic reports in Excel format. |
| `GET /reports/export/pdf` | *Query Params (Optional):* `programmeId`, `batchId` | **Binary Stream:** PDF Document | Bulk export of academic reports in PDF format. |

---

## 14. Approval Workflow & Verification APIs (`/approvals`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /approvals` | *Query Params (Optional):* `role`, `status`, `type`, `schoolId`, `programmeId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "app-1", "type": "CO_DEFINITION", "title": "CO Approval for CS301", "status": "PENDING", "submittedBy": "Prof. Smith", "submittedAt": "2026-08-21T12:00:00Z"}]}``` | Returns approval requests filtered by status, type, and caller scope. |
| `GET /approvals/pending` | *Query Params (Optional):* `role`, `schoolId`, `programmeId` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "app-1", "type": "COURSE_ALLOCATION", "status": "PENDING"}]}``` | Returns only PENDING approval items requiring action by the caller's role. |
| `GET /approvals/{id}` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "data": {"id": "app-1", "type": "COURSE_ATR", "status": "PENDING", "details": "{...}", "remarks": ""}}``` | Retrieves specific approval request details. |
| `GET /approvals/{id}/history` | *Path Variable:* `id` | **JSON Response:**<br>```json{"success": true, "data": [{"id": "aph-1", "action": "SUBMITTED", "actorName": "Prof. Smith", "actorRole": "FACULTY", "comments": "Submitted for review", "createdAt": "2026-08-21T12:00:00Z"}]}``` | Returns complete immutable audit history for an approval request. |
| `POST /approvals/submit` | **JSON Body:**<br>```json{"type": "ATTAINMENT_CONFIGURATION", "title": "Attainment Config CS301", "resourceId": "off-1", "programmeBatchCourseId": "off-1", "masterProgrammeId": "prog-1"}``` | **JSON Response:**<br>```json{"success": true, "message": "Approval request submitted", "data": {"id": "app-1", "status": "PENDING"}}``` | Submits an academic resource for multi-tier approval. |
| `POST /approvals/{id}/approve` | *Path Variable:* `id`<br>**JSON Body (Optional):**<br>```json{"comments": "Approved after review"}``` | **JSON Response:**<br>```json{"success": true, "message": "Request approved successfully", "data": {"id": "app-1", "status": "APPROVED", "approvedBy": "Dr. Rahul"}}``` | Approves request if caller has valid role and scope authority. Self-approval blocked. |
| `POST /approvals/{id}/request-revision`<br>`POST /approvals/{id}/reject` | *Path Variable:* `id`<br>**JSON Body:**<br>```json{"remarks": "Please revise CO2 Blooms level taxonomy."}``` | **JSON Response:**<br>```json{"success": true, "message": "Revision requested", "data": {"id": "app-1", "status": "REVISION_REQUESTED", "remarks": "Please revise..."}}``` | Requests revision on an approval item with mandatory remarks. |
| `POST /approvals/{id}/action` | *Path Variable:* `id`<br>**JSON Body:**<br>```json{"action": "APPROVE", "comments": "Good"}``` | **JSON Response:**<br>```json{"success": true, "message": "Action APPROVE executed successfully", "data": {"status": "APPROVED"}}``` | Unified action endpoint supporting `APPROVE` and `REQUEST_REVISION`. |
| `GET /approvals/verification-status` | *Query Params:* `key` (or `id`) | **JSON Response:**<br>```json{"success": true, "data": {"configStatus": "APPROVED", "coStatus": "APPROVED", "atrStatus": "DRAFT", "verifiedBy": "Dr. Rahul"}}``` | Returns real-time workflow status and verification comments for any resource key. |
| `POST /approvals/verify`<br>`PUT /approvals/verify` | **JSON Body:**<br>```json{"key": "off-1", "statusType": "coStatus", "statusValue": "APPROVED", "remarks": "Approved"}``` | **JSON Response:**<br>```json{"success": true, "message": "Status updated successfully.", "data": {"coStatus": "APPROVED"}}``` | Directly verifies and approves a resource key with server-side role validation. |
| `POST /approvals/request-revision`<br>`PUT /approvals/request-revision` | **JSON Body:**<br>```json{"key": "off-1", "statusType": "coStatus", "remarks": "Revise Blooms taxonomy for CO3"}``` | **JSON Response:**<br>```json{"success": true, "message": "Revision request recorded. Coordinator notified.", "data": {"coStatus": "REVISION_REQUESTED"}}``` | Directly marks a resource key as `REVISION_REQUESTED` and records comments. |

---

## 15. Role-Scoped Dashboard APIs (`/dashboard`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /dashboard/director` | *Query Params (Optional):* `schoolId`, `directorEmail` | **JSON Response:**<br>```json{"success": true, "data": {"schoolName": "School of Engineering", "departmentCount": 4, "programmeCount": 8, "facultyCount": 45, "pendingApprovalsCount": 2}}``` | Dedicated Director Dashboard screen metrics. |
| `GET /dashboard/hod` | *Query Params (Optional):* `departmentId`, `hodEmail` | **JSON Response:**<br>```json{"success": true, "data": {"departmentName": "CSE", "programmes": [...], "facultyCount": 20, "pendingCourseAllocations": 1, "pendingProgrammeAtrs": 0}}``` | Dedicated HOD Dashboard screen metrics. |
| `GET /dashboard/programme-coordinator` | *Query Params (Optional):* `programmeId`, `coordinatorEmail` | **JSON Response:**<br>```json{"success": true, "data": {"programmeName": "B.Tech CSE", "batch": "2022-2026", "coursesAllocated": 40, "pendingCoApprovals": 2, "pendingAtrApprovals": 1}}``` | Dedicated Programme Coordinator Dashboard screen metrics. |
| `GET /dashboard/course-coordinator` | *Query Params (Optional):* `courseId`, `coordinatorEmail` | **JSON Response:**<br>```json{"success": true, "data": {"assignedOfferings": [{"offeringId": "off-1", "courseCode": "CS301", "coStatus": "APPROVED", "configStatus": "APPROVED", "attainmentCalculated": true}]}}``` | Dedicated Course Coordinator Dashboard screen metrics. |
| `GET /dashboard/faculty` | *Query Params (Optional):* None | **JSON Response:**<br>```json{"success": true, "data": {"assignedCourses": [...], "pendingTasks": [...]}}``` | Dedicated Faculty Dashboard view. |

---

## 16. User & Academic Staff Management APIs (`/users`, `/academic/users`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /users`<br>`GET /academic/users` | *Query Params (Optional):* `role` | **JSON Response:**<br>```json{"success": true, "data": [{"id": 1, "username": "dr.verma", "name": "Dr. Rahul Verma", "email": "rahul@dypiu.ac.in", "role": "PROGRAMME_COORDINATOR", "schoolId": "sch-1", "departmentId": "dept-1", "programmeId": "prog-1"}]}``` | Returns academic user profiles filtered by role and scoped by caller authority. |
| `GET /users/{id}` | *Path Variable:* `id` (Long) | **JSON Response:**<br>```json{"success": true, "data": {"id": 1, "username": "dr.verma", "name": "Dr. Rahul Verma", "email": "rahul@dypiu.ac.in", "role": "PROGRAMME_COORDINATOR"}}``` | Retrieves detailed user profile by ID. |
| `POST /users` | **JSON Body:**<br>```json{"email": "faculty@dypiu.ac.in", "name": "Dr. John Doe", "username": "johndoe", "password": "Password@123", "role": "FACULTY", "schoolId": "sch-1", "departmentId": "dept-1"}``` | **JSON Response:**<br>```json{"success": true, "message": "Academic member registered successfully.", "data": {"id": 2, "name": "Dr. John Doe", "role": "FACULTY"}}``` | Creates and assigns an academic staff member within caller's organizational scope. |
| `PUT /users/{id}` | *Path Variable:* `id`<br>**JSON Body:**<br>```json{"name": "Dr. John Doe Updated", "role": "COURSE_COORDINATOR"}``` | **JSON Response:**<br>```json{"success": true, "message": "User updated successfully.", "data": {"id": 2, "name": "Dr. John Doe Updated"}}``` | Updates academic staff profile, credentials, or assigned role. |

---

## 17. System & Health Monitoring APIs (`/health`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /health` | *Query Params:* None | **JSON Response:**<br>```json{"status": "UP", "timestamp": "2026-08-22T00:30:00Z", "service": "DYPIU NBA Attainment Backend"}``` | Health check endpoint for uptime and service readiness monitoring. |

---

## 18. Centralized Audit Logging APIs (`/audit-logs`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `GET /audit-logs` | *Query Params (Optional):*<br>`actorId` (String)<br>`actorRole` (String)<br>`action` (String, e.g. `CREATE`, `APPROVE`, `REQUEST_REVISION`, `DELETE_APPROVED`)<br>`resourceType` (String, e.g. `COURSE_OUTCOME`, `PROGRAMME_BATCH`)<br>`resourceId` (String)<br>`success` (Boolean)<br>`from` (ISO DateTime)<br>`to` (ISO DateTime)<br>`page` (Integer, default `0`)<br>`size` (Integer, default `20`) | **JSON Response:**<br>```json{"success": true, "data": {"content": [{"id": 101, "action": "APPROVE", "actorId": "2", "actorName": "Dr. Joshi", "actorRole": "HOD", "actorEmail": "hod.cse@dypiu.ac.in", "resourceType": "COURSE_OUTCOME", "resourceId": "off-cs101", "oldStatus": "PENDING", "newStatus": "APPROVED", "success": true, "remarks": "Approved by HOD", "createdAt": "2026-08-22T02:00:00Z"}], "pageNumber": 0, "pageSize": 20, "totalElements": 48, "totalPages": 3, "last": false}}``` | Paginated, filtered inspection of institutional audit logs. Strictly restricted to `ADMIN` and `IQAC` roles. |
| `GET /audit-logs/{id}` | *Path Variable:* `id` (Long) | **JSON Response:**<br>```json{"success": true, "data": {"id": 101, "action": "APPROVE", "actorId": "2", "actorName": "Dr. Joshi", "actorRole": "HOD", "actorEmail": "hod.cse@dypiu.ac.in", "resourceType": "COURSE_OUTCOME", "resourceId": "off-cs101", "oldStatus": "PENDING", "newStatus": "APPROVED", "success": true, "remarks": "Approved by HOD", "metadata": "{\"batchId\":\"batch-2024\"}", "ipAddress": "192.168.1.1", "userAgent": "Mozilla/5.0...", "createdAt": "2026-08-22T02:00:00Z"}}``` | Retrieves detailed record for a single audit log entry by ID. Accessible only to `ADMIN` and `IQAC`. |
| `GET /audit-logs/resources/{resourceType}/{resourceId}` | *Path Variables:* `resourceType` (e.g. `PROGRAMME_BATCH_COURSE`), `resourceId` (String) | **JSON Response:**<br>```json{"success": true, "data": [{"id": 98, "action": "CREATE", "actorName": "Prof. Alan", "actorRole": "PROGRAMME_COORDINATOR", "createdAt": "2026-08-20T10:00:00Z"}, {"id": 101, "action": "APPROVE", "actorName": "Dr. Joshi", "actorRole": "HOD", "createdAt": "2026-08-22T02:00:00Z"}]}``` | Retrieves chronological audit event history for a specific business resource. |

---

## 19. Hierarchical Deletion Request & Soft Delete APIs (`/deletion-requests`)

| API (Method & Path) | Request Format | Response Format | Description |
|---|---|---|---|
| `POST /deletion-requests` | **JSON Body:**<br>```json{"resourceType": "PROGRAMME_BATCH_COURSE", "resourceId": "off-cs101", "reason": "Curriculum updated, replaced by CS102", "metadata": "{\"academicYear\":\"2024-25\"}"}``` | **JSON Response:**<br>```json{"success": true, "message": "Deletion request created successfully.", "data": {"id": 1, "resourceType": "PROGRAMME_BATCH_COURSE", "resourceId": "off-cs101", "resourceName": "CS101 - Programming", "status": "PENDING", "requestedBy": "Prof. Alan", "requestedByRole": "PROGRAMME_COORDINATOR", "requestedAt": "2026-08-22T02:00:00Z", "reason": "Curriculum updated..."}}``` | Submits a deletion request: Programme Coordinator for Course Offering, or HOD for Programme Batch. |
| `GET /deletion-requests` | *Query Params (Optional):*<br>`status` (String, e.g. `PENDING`, `REJECTED`, `EXECUTED`)<br>`resourceType` (String, e.g. `PROGRAMME_BATCH`, `PROGRAMME_BATCH_COURSE`) | **JSON Response:**<br>```json{"success": true, "data": [{"id": 1, "resourceType": "PROGRAMME_BATCH_COURSE", "resourceId": "off-cs101", "resourceName": "CS101", "status": "PENDING", "requestedBy": "Prof. Alan", "requestedAt": "2026-08-22T02:00:00Z"}]}``` | Lists deletion requests within the caller's organizational scope (`HOD` reviews course requests; `DIRECTOR` reviews batch requests). |
| `GET /deletion-requests/{id}` | *Path Variable:* `id` (Long) | **JSON Response:**<br>```json{"success": true, "data": {"id": 1, "resourceType": "PROGRAMME_BATCH_COURSE", "resourceId": "off-cs101", "status": "PENDING", "reason": "Curriculum update", "requestedBy": "Prof. Alan", "requestedAt": "2026-08-22T02:00:00Z"}}``` | Retrieves detailed information on a specific deletion request by its ID. |
| `POST /deletion-requests/{id}/reject` | *Path Variable:* `id`<br>**JSON Body (Optional):**<br>```json{"remarks": "Cannot delete active course with enrolled students."}``` | **JSON Response:**<br>```json{"success": true, "message": "Deletion request rejected successfully.", "data": {"id": 1, "status": "REJECTED", "reviewedBy": "Dr. Joshi", "reviewedByRole": "HOD", "reviewedAt": "2026-08-22T02:15:00Z", "remarks": "Cannot delete..."}}``` | Rejects a deletion request (`HOD` for courses, `DIRECTOR` for batches). |
| `POST /deletion-requests/{id}/execute` | *Path Variable:* `id`<br>**JSON Body:**<br>```json{"password": "ReviewerPassword@123", "remarks": "Verified and approved for permanent soft-deletion"}``` | **JSON Response:**<br>```json{"success": true, "message": "Deletion request executed successfully with soft-delete.", "data": {"id": 1, "status": "EXECUTED", "reviewedBy": "Dr. Joshi", "reviewedByRole": "HOD", "executedAt": "2026-08-22T02:20:00Z", "remarks": "Verified..."}}``` | Executes permanent soft-deletion after verifying authenticated reviewer's password. Soft-deletes target entity without physical data loss. |

