# Authorization & Role-Based Access Control (RBAC)

## User Roles & Hierarchy

```text
IQAC (Institutional Quality Assurance Cell)
  └─► Director (School Dean / Director)
        └─► HOD (Head of Department)
              └─► Programme Coordinator
                    └─► Course Coordinator / Faculty
```

## Role Permissions Matrix

| Role | Scope | Privileges |
| :--- | :--- | :--- |
| **IQAC** | Institutional Level | Reads all institutional reports, historical attainment runs, school-wide ATR audits. |
| **DIRECTOR** | School Level | Creates Departments under School, assigns HODs, oversees Programmes, approves PO/PSO outcome frameworks & Programme ATRs. |
| **HOD** | Department Level | Manages Programmes, initializes Batches, creates PO/PSO/PEO outcomes, assigns Programme Coordinators, approves Course CO weightages, allocation rosters & Course ATRs. |
| **PROGRAMME_COORDINATOR** | Programme Level | Sets Programme PO/PSO target levels (1.0 - 3.0 scale), aggregates course attainment across semesters, prepares Programme ATR for completing batch. |
| **FACULTY (Course Coordinator)** | Course Level | Defines Course Outcomes (COs), sets CO target levels, maps CO-PO/PSO matrices, uploads End Sem marks, runs CO attainment engine, prepares Course ATR. |

## Spring Security Enforcement

Spring Security enforces role restrictions using `@PreAuthorize("hasRole('HOD')")` annotations and filter chain definitions. Backend authorization is mandatory and independent of frontend UI state.
