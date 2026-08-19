# REST API Catalog & Integration Contracts

Base API URL: `/api/v1`

## 1. Authentication Endpoints

| Endpoint | Method | Role Allowed | Description |
| :--- | :--- | :--- | :--- |
| `/auth/login` | POST | Public | User authentication with username/email & password. Returns JWT token & user profile. |
| `/auth/register` | POST | Public | User registration. Returns JWT token & created user profile. |
| `/auth/forgot-password` | POST | Public | Triggers password reset email link. |
| `/auth/reset-password` | POST | Public | Resets user password using reset token. |
| `/auth/verify-otp` | POST | Public | MFA code verification. Returns elevated session token. |

## 2. Academic Management Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/academic/schools` | GET / POST | Fetch / create school organization structure. |
| `/academic/departments` | GET / POST / DELETE | Fetch / manage department list & HOD assignments. |
| `/academic/programmes` | GET / POST / DELETE | Fetch / manage programme degree duration and coordinators. |
| `/academic/batches` | GET / POST / DELETE | Fetch / create academic batch years. |
| `/academic/courses` | GET / POST / DELETE | Fetch / allocate course coordinator & faculty roster. |
| `/academic/batches/{batchId}/students` | GET / POST | Fetch / enroll students by PRN into batch scope. |

## 3. Outcome Management & Attainment Engine Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/outcomes/programmes/{progId}/pos` | GET / POST | Fetch / save Program Outcomes (PO1-PO12). |
| `/outcomes/programmes/{progId}/psos` | GET / POST | Fetch / save Program Specific Outcomes (PSO1-PSO3). |
| `/outcomes/courses/{courseId}/cos` | GET / POST | Fetch / save Course Outcomes (C321.1-C321.6). |
| `/mappings/cos/{coId}/po` | GET / POST | Fetch / save CO-PO mapping matrix levels (0, 1, 2, 3). |
| `/attainment/config/{courseId}` | GET / POST | Fetch / configure Direct (80%) vs Indirect (20%) weights. |
| `/attainment/calculate/course/{courseId}` | GET | Execute Direct + Indirect CO Attainment Engine. |
| `/atr/course/{courseId}` | GET / POST | Fetch / save Course Action Taken Reports (ATR). |
| `/atr/programme/{progId}` | GET / POST | Fetch / save Programme Action Taken Reports (ATR). |
| `/approvals/director` | GET | Fetch Director pending outcome & ATR approval requests. |
| `/approvals/hod` | GET | Fetch HOD pending course weightage & allocation requests. |
| `/approvals/{id}/approve` | POST | Approve pending submission. |
| `/approvals/{id}/reject` | POST | Reject / request revision on submission. |
