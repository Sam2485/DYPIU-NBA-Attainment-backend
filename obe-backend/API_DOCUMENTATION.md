# API Contract Refactor Documentation

This document outlines the updated API endpoints and JSON request/response contracts for the frontend following the strict canonical naming migration. All legacy endpoints (`/academic/programmes`, `/academic/batches`, `/academic/courses`, `/academic/course-offerings`) and generic IDs (`programmeId`, `batchId`, `courseId`, `courseOfferingId`) have been completely replaced.

All responses follow the standard `ApiResponse` wrapper format:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... } // or [...]
}
```
*Note: In the JSON blocks below, only the `data` payload is shown for brevity.*

---

## 1. Master Programmes

### Endpoints
- `GET    /api/v1/academic/master-programmes`
- `GET    /api/v1/academic/master-programmes/{masterProgrammeId}` (Replaces legacy GET)
- `POST   /api/v1/academic/master-programmes`
- `PUT    /api/v1/academic/master-programmes/{masterProgrammeId}`
- `DELETE /api/v1/academic/master-programmes/{masterProgrammeId}`
- `PUT    /api/v1/academic/master-programmes/{masterProgrammeId}/coordinator`
- `GET    /api/v1/academic/master-programmes/{masterProgrammeId}/targets`
- `POST   /api/v1/academic/master-programmes/{masterProgrammeId}/targets`
- `GET    /api/v1/academic/master-programmes/{masterProgrammeId}/competencies`
- `POST   /api/v1/academic/master-programmes/{masterProgrammeId}/competencies`
- `GET    /api/v1/academic/master-programmes/{masterProgrammeId}/historical/attainment-reports`

### JSON Schema
**Request (POST / PUT)**
```json
{
  "departmentId": "dept-123",
  "code": "BTECH-CS",
  "name": "B.Tech Computer Science",
  "durationYears": 4
}
```

**Response (GET)**
```json
{
  "masterProgrammeId": "prog-1234",
  "departmentId": "dept-123",
  "code": "BTECH-CS",
  "name": "B.Tech Computer Science",
  "durationYears": 4,
  "status": "ACTIVE",
  "coordinator": "Dr. Smith",
  "coordinatorEmail": "smith@dypiu.edu.in",
  "createdAt": "2023-01-01T10:00:00Z"
}
```

---

## 2. Programme Batches

### Endpoints
- `GET    /api/v1/academic/programme-batches`
- `GET    /api/v1/academic/programme-batches/{programmeBatchId}`
- `POST   /api/v1/academic/programme-batches`
- `PUT    /api/v1/academic/programme-batches/{programmeBatchId}`
- `DELETE /api/v1/academic/programme-batches/{programmeBatchId}`
- `POST   /api/v1/academic/programme-batches/{programmeBatchId}/status`
- `GET    /api/v1/academic/programme-batches/{programmeBatchId}/context`
- `GET    /api/v1/academic/programme-batches/{programmeBatchId}/students`
- `POST   /api/v1/academic/programme-batches/{programmeBatchId}/students`
- `GET    /api/v1/academic/programme-batches/{programmeBatchId}/atr`
- `PUT    /api/v1/academic/programme-batches/{programmeBatchId}/atr`
- `POST   /api/v1/academic/programme-batches/{programmeBatchId}/atr/submit`
- `POST   /api/v1/academic/programme-batches/{programmeBatchId}/survey/upload`

### JSON Schema
**Request (POST / PUT)**
```json
{
  "masterProgrammeId": "prog-1234",
  "name": "2023-2027",
  "startYear": 2023,
  "endYear": 2027,
  "durationYears": 4
}
```

**Response (GET)**
```json
{
  "programmeBatchId": "batch-5678",
  "masterProgrammeId": "prog-1234",
  "name": "2023-2027",
  "startYear": 2023,
  "endYear": 2027,
  "status": "ACTIVE",
  "coordinatorName": "Dr. Allen",
  "coordinatorEmail": "allen@dypiu.edu.in"
}
```

---

## 3. Master Courses

### Endpoints
- `GET    /api/v1/academic/master-courses`
- `GET    /api/v1/academic/master-courses/{masterCourseId}`
- `POST   /api/v1/academic/master-courses`
- `PUT    /api/v1/academic/master-courses/{masterCourseId}`
- `DELETE /api/v1/academic/master-courses/{masterCourseId}`
- `POST   /api/v1/academic/master-courses/allocate`

### JSON Schema
**Request (POST / PUT)**
```json
{
  "masterProgrammeId": "prog-1234",
  "code": "CS101",
  "name": "Intro to Programming",
  "credits": 3,
  "courseType": "CORE"
}
```

**Response (GET)**
```json
{
  "masterCourseId": "course-9101",
  "masterProgrammeId": "prog-1234",
  "code": "CS101",
  "name": "Intro to Programming",
  "credits": 3,
  "courseType": "CORE",
  "status": "ACTIVE"
}
```

---

## 4. Programme-Batch Courses (Course Offerings)

### Endpoints
- `GET    /api/v1/academic/programme-batch-courses`
- `GET    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}`
- `POST   /api/v1/academic/programme-batch-courses`
- `PUT    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}`
- `DELETE /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}`
- `GET    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/course-outcomes`
- `POST   /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/course-outcomes`
- `GET    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/co-po-pso-mappings`
- `PUT    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/co-po-pso-mappings`
- `GET    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/survey`
- `POST   /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/survey/upload`
- `GET    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/attainment-main`
- `GET    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/atr`
- `PUT    /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/atr`
- `POST   /api/v1/academic/programme-batch-courses/{programmeBatchCourseId}/atr/submit`

### JSON Schema
**Request (POST / PUT)**
```json
{
  "masterCourseId": "course-9101",
  "programmeBatchId": "batch-5678",
  "semester": 1,
  "courseCoordinatorEmail": "faculty@dypiu.edu.in",
  "assignedFaculty": "faculty@dypiu.edu.in"
}
```

**Response (GET)**
```json
{
  "programmeBatchCourseId": "pbc-1111",
  "masterCourseId": "course-9101",
  "programmeBatchId": "batch-5678",
  "semester": 1,
  "courseCode": "CS101",
  "courseName": "Intro to Programming",
  "coordinatorEmail": "faculty@dypiu.edu.in",
  "assignedFaculty": "faculty@dypiu.edu.in",
  "status": "ACTIVE"
}
```

---

## 5. Attainment & Examination

### Endpoints
- `GET    /api/v1/attainment/configurations/{programmeBatchCourseId}`
- `POST   /api/v1/attainment/configurations/save`
- `PUT    /api/v1/attainment/configurations/{configId}`
- `POST   /api/v1/attainment/configurations/submit`
- `GET    /api/v1/attainment/examination/{programmeBatchCourseId}`
- `POST   /api/v1/attainment/examination/{programmeBatchCourseId}`
- `POST   /api/v1/attainment/programme-batch-courses/{programmeBatchCourseId}/examination/upload`
- `POST   /api/v1/attainment/survey/{programmeBatchCourseId}`
- `GET    /api/v1/attainment/programme-batch-courses/{programmeBatchCourseId}`
- `GET    /api/v1/attainment/master-programmes/{masterProgrammeId}/programme-batches/{programmeBatchId}`
- `GET    /api/v1/attainment/master-programmes/{masterProgrammeId}/programme-batches/{programmeBatchId}/dataset`

---

## 6. Outcomes and Targets

### Endpoints
- `GET    /api/v1/academic/outcomes`
- `POST   /api/v1/academic/outcomes`
- `PUT    /api/v1/academic/outcomes`
- `GET    /api/v1/outcomes/master-programmes/{masterProgrammeId}/pos`
- `POST   /api/v1/outcomes/master-programmes/{masterProgrammeId}/pos`
- `GET    /api/v1/outcomes/master-programmes/{masterProgrammeId}/psos`
- `POST   /api/v1/outcomes/master-programmes/{masterProgrammeId}/psos`
- `GET    /api/v1/outcomes/master-programmes/{masterProgrammeId}/peos`
- `POST   /api/v1/outcomes/master-programmes/{masterProgrammeId}/peos`
- `POST   /api/v1/outcomes/master-programmes/{masterProgrammeId}/targets`

---

## 7. Dashboards & Setup Progress

*Query parameters have been updated to strictly expect canonical identifiers.*

### Endpoints
- `GET    /api/v1/dashboard/director`
- `GET    /api/v1/dashboard/hod`
- `GET    /api/v1/dashboard/programme-coordinator?masterProgrammeId=xxx`
- `GET    /api/v1/dashboard/course-coordinator?masterCourseId=xxx&programmeBatchId=xxx`
- `GET    /api/v1/academic/course-coordinator/summary?programmeBatchCourseId=xxx`
- `GET    /api/v1/academic/director/setup-progress`
- `GET    /api/v1/academic/hod/setup-progress`
- `GET    /api/v1/academic/coordinator/setup-progress?masterProgrammeId=xxx`
- `GET    /api/v1/academic/course-coordinator/setup-progress?programmeBatchCourseId=xxx`

---

## 8. Reports & ATR

### Endpoints
- `GET    /api/v1/reports/filters`
- `GET    /api/v1/reports/summary`
- `GET    /api/v1/reports/course-atrs`
- `GET    /api/v1/reports/programme-atrs`
- `GET    /api/v1/reports/master-programmes/{masterProgrammeId}/programme-batches/{programmeBatchId}/programme-atr`
- `POST   /api/v1/reports/master-programmes/programme-atr`
- `POST   /api/v1/reports/master-programmes/{masterProgrammeId}/programme-batches/{programmeBatchId}/programme-atr/submit`
- `GET    /api/v1/atr/programme-batches/previous-year/{programmeBatchId}`
- `GET    /api/v1/reports/attainment-main`
- `GET    /api/v1/reports/master-programmes/{masterProgrammeId}/batch-comparison`
- `GET    /api/v1/reports/programme-batches/{programmeBatchId}/summary`

---

## 9. Users and Authentication (Unchanged Base Paths, Updated Payloads)

### JSON Schema (Users)
**Response (GET `/api/v1/users/me`)**
```json
{
  "id": 1,
  "username": "faculty",
  "name": "Dr. Faculty",
  "email": "faculty@dypiu.edu.in",
  "role": "FACULTY",
  "schoolId": "school-123",
  "departmentId": "dept-123",
  "masterProgrammeId": "prog-1234",
  "department": "Computer Science",
  "programme": "B.Tech Computer Science"
}
```

---

## 10. Programme Coordinator Approval Workflow

Strictly scoped to `programmeBatchId` and `programmeBatchCourseId`.

### Endpoints
- `GET    /api/v1/approvals/pending?programmeBatchId={programmeBatchId}`
- `GET    /api/v1/approvals/reviewed?programmeBatchId={programmeBatchId}`
- `GET    /api/v1/approvals/programme-batch-courses/{programmeBatchCourseId}`
- `POST   /api/v1/approvals/{approvalRequestId}/approve`
- `POST   /api/v1/approvals/{approvalRequestId}/request-revision`

### Canonical Enums
- **Approval Types**: `ATTAINMENT_SETTINGS`, `COURSE_OUTCOMES_TARGETS`, `COURSE_ATR`
- **Approval Statuses**: `PENDING`, `APPROVED`, `REVISION_REQUESTED`

---

### 1. Pending Approvals Inbox
**`GET /api/v1/approvals/pending?programmeBatchId={programmeBatchId}`**
- **Purpose**: Returns all pending approval cards grouped by Programme-Batch-Course.
- **Authorization**: Scoped to the authenticated user's authorized Programme Batch.
- **Request Body**: `None`
- **Response Format**:
```json
{
  "success": true,
  "message": "Pending approvals fetched successfully",
  "data": {
    "programmeBatchId": "batch-6585c71a",
    "programmeBatchName": "2027-2031 Batch",
    "totalPendingItems": 3,
    "totalProgrammeBatchCourses": 1,
    "courses": [
      {
        "programmeBatchCourseId": "pbc-8867ab03",
        "masterCourseId": "crs-38eeee4f",
        "courseCode": "C321",
        "courseName": "Compiler Design",
        "semester": 1,
        "pendingApprovalCount": 3,
        "approvalItems": [
          {
            "approvalRequestId": "app-9001",
            "type": "ATTAINMENT_SETTINGS",
            "status": "PENDING"
          },
          {
            "approvalRequestId": "app-9002",
            "type": "COURSE_OUTCOMES_TARGETS",
            "status": "PENDING"
          },
          {
            "approvalRequestId": "app-9003",
            "type": "COURSE_ATR",
            "status": "PENDING"
          }
        ],
        "submittedBy": {
          "userId": 101,
          "name": "Dr. Alan Turing",
          "email": "cc1@dypiu.ac.in"
        },
        "latestSubmittedAt": "2026-08-28T10:30:00Z"
      }
    ]
  }
}
```

---

### 2. Reviewed Approvals Inbox
**`GET /api/v1/approvals/reviewed?programmeBatchId={programmeBatchId}`**
- **Purpose**: Returns reviewed approval history (Approved & Revision Requested) grouped by course.
- **Authorization**: Scoped to the authenticated user's authorized Programme Batch.
- **Request Body**: `None`
- **Response Format**:
```json
{
  "success": true,
  "message": "Reviewed approvals fetched successfully",
  "data": {
    "programmeBatchId": "batch-6585c71a",
    "programmeBatchName": "2027-2031 Batch",
    "totalReviewedItems": 1,
    "totalProgrammeBatchCourses": 1,
    "courses": [
      {
        "programmeBatchCourseId": "pbc-8867ab03",
        "masterCourseId": "crs-38eeee4f",
        "courseCode": "C321",
        "courseName": "Compiler Design",
        "semester": 1,
        "reviewedApprovalCount": 1,
        "approvalItems": [
          {
            "approvalRequestId": "app-9001",
            "type": "ATTAINMENT_SETTINGS",
            "status": "APPROVED",
            "reviewedAt": "2026-08-28T12:10:00Z",
            "reviewedBy": {
              "userId": 12,
              "name": "Programme Coordinator",
              "email": "pc1@dypiu.ac.in"
            }
          }
        ]
      }
    ]
  }
}
```

---

### 3. Course Approval Workspace Summary
**`GET /api/v1/approvals/programme-batch-courses/{programmeBatchCourseId}`**
- **Purpose**: Identifies course header identity and which approval items/tabs exist for this course offering.
- **Authorization**: Scoped to the authenticated user's authorized Programme Batch.
- **Request Body**: `None`
- **Response Format**:
```json
{
  "success": true,
  "message": "Programme-Batch-Course approval details fetched successfully",
  "data": {
    "programmeBatchCourse": {
      "programmeBatchCourseId": "pbc-8867ab03",
      "programmeBatchId": "batch-6585c71a",
      "masterCourseId": "crs-38eeee4f",
      "courseCode": "C321",
      "courseName": "Compiler Design",
      "semester": 1,
      "programmeBatchName": "2027-2031 Batch"
    },
    "approvalItems": [
      {
        "approvalRequestId": "app-9001",
        "type": "ATTAINMENT_SETTINGS",
        "status": "PENDING"
      },
      {
        "approvalRequestId": "app-9002",
        "type": "COURSE_OUTCOMES_TARGETS",
        "status": "PENDING"
      },
      {
        "approvalRequestId": "app-9003",
        "type": "COURSE_ATR",
        "status": "PENDING"
      }
    ]
  }
}
```

---

### 4. Approve Action
**`POST /api/v1/approvals/{approvalRequestId}/approve`**
- **Purpose**: Approves a specific approval item. Derives actor identity from JWT / Security Context.
- **Request Body JSON**:
```json
{}
```
- **Response Format**:
```json
{
  "success": true,
  "message": "Approval completed successfully",
  "data": {
    "approvalRequestId": "app-9002",
    "type": "COURSE_OUTCOMES_TARGETS",
    "status": "APPROVED",
    "reviewedBy": {
      "userId": 12,
      "name": "Programme Coordinator",
      "email": "pc1@dypiu.ac.in"
    },
    "reviewedAt": "2026-08-28T12:35:00Z"
  }
}
```

---

### 5. Request Revision Action
**`POST /api/v1/approvals/{approvalRequestId}/request-revision`**
- **Category**: Approval Actions
- **Purpose**: Requests revision for a specific approval item with mandatory reason.
- **Authentication**: Bearer JWT (Programme Coordinator)
- **Authorization**: Scoped to the authenticated user's authorized Programme Batch.
- **Path Parameters**: `approvalRequestId` (string, e.g. `app-9002`)
- **Query Parameters**: `None`
- **Request Body JSON**:
```json
{
  "reason": "Please revise the CO2 target."
}
```
- **Response Format**:
```json
{
  "success": true,
  "message": "Revision requested successfully",
  "data": {
    "approvalRequestId": "app-9002",
    "type": "COURSE_OUTCOMES_TARGETS",
    "status": "REVISION_REQUESTED",
    "reviewedBy": {
      "userId": 12,
      "name": "Programme Coordinator",
      "email": "pc1@dypiu.ac.in"
    },
    "reviewedAt": "2026-08-28T12:30:00Z",
    "revisionReason": "Please revise the CO2 target."
  }
}
```

---

### 6. Attainment Settings Content (Read-Only Review)
**`GET /api/v1/attainment/configurations/programme-batch-courses/{programmeBatchCourseId}`**
- **Category**: Attainment Configuration
- **Purpose**: Returns the existing Course Coordinator Attainment Settings data for the approval review tab.
- **Authentication**: Bearer JWT
- **Authorization**: Scoped to the authenticated user's authorized Programme Batch.
- **Path Parameters**: `programmeBatchCourseId` (string, e.g. `pbc-8867ab03`)
- **Query Parameters**: `None`
- **Request Body JSON**: `None`
- **Response Format**:
```json
{
  "success": true,
  "data": {
    "programmeBatchCourseId": "pbc-8867ab03",
    "directAttainmentWeightage": 80.0,
    "indirectAttainmentWeightage": 20.0,
    "cieWeightage": 40.0,
    "seeWeightage": 60.0,
    "targetAttainmentPercentage": 70.0,
    "status": "PENDING_APPROVAL"
  }
}
```

---

### 7. Course Outcomes & Targets Content with Embedded Mappings (Read-Only Review)
**`GET /api/v1/academic/course-outcomes?programmeBatchCourseId={programmeBatchCourseId}`**
- **Category**: Course Outcomes & Targets
- **Purpose**: Returns the existing CO statements, targets, Bloom levels, and embedded PO/PSO matrix mappings for the approval review tab.
- **Authentication**: Bearer JWT
- **Authorization**: Scoped to the authenticated user's authorized Programme Batch.
- **Path Parameters**: `None`
- **Query Parameters**: `programmeBatchCourseId` (string, required)
- **Request Body JSON**: `None`
- **Response Format**:
```json
{
  "success": true,
  "data": [
    {
      "id": "co-101",
      "code": "CO1",
      "statement": "Understand lexical analysis and finite automata",
      "bloomLevel": "UNDERSTAND",
      "targetPercentage": 70.0,
      "poMappings": {
        "PO1": 3,
        "PO2": 2,
        "PO3": 1
      },
      "psoMappings": {
        "PSO1": 2
      },
      "status": "PENDING_APPROVAL"
    },
    {
      "id": "co-102",
      "code": "CO2",
      "statement": "Design context-free grammars and syntax analyzers",
      "bloomLevel": "APPLY",
      "targetPercentage": 65.0,
      "poMappings": {
        "PO1": 2,
        "PO2": 3,
        "PO3": 2
      },
      "psoMappings": {
        "PSO1": 2
      },
      "status": "PENDING_APPROVAL"
    }
  ]
}
```

---

### 8. Course ATR Content (Read-Only Review)
**`GET /api/v1/atr/course/{programmeBatchCourseId}`**
- **Category**: Course Action Taken Report
- **Purpose**: Returns the Course ATR data (targets, actual attainments, observations, action plans) for the approval review tab.
- **Authentication**: Bearer JWT
- **Authorization**: Scoped to the authenticated user's authorized Programme Batch.
- **Path Parameters**: `programmeBatchCourseId` (string, e.g. `pbc-8867ab03`)
- **Query Parameters**: `None`
- **Request Body JSON**: `None`
- **Response Format**:
```json
{
  "success": true,
  "data": {
    "id": "catr-33e1f4a9",
    "programmeBatchCourseId": "pbc-8867ab03",
    "status": "SUBMITTED_FOR_VERIFICATION",
    "coOutcomes": [
      {
        "coCode": "CO1",
        "targetLevel": 2.5,
        "attainmentLevel": 2.1,
        "observation": "Students struggled with parsing tables",
        "action": "Conduct remedial tutorial sessions"
      }
    ]
  }
}
```


