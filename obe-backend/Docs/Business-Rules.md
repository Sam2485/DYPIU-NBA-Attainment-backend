# Business Rules & Attainment Calculation Logic

## 1. Domain Hierarchy & Scope Rules

- **Academic Year**: Primary UI filtering context (e.g. `2025-26`).
- **Batch**: Central historical scope for all student marks, surveys, attainment calculations, and ATRs (e.g. `Batch 2025-29 (BE-COMP)`).
- **Completed Batches**: Remain queryable as historical records and are never overwritten by later academic year runs.

## 2. CO Attainment Calculation Formulas

### Direct CO Attainment
$$\text{Direct CO \%} = \frac{\text{Number of Students Scoring } \ge \text{Target Threshold \%}}{\text{Total Number of Students Attempted Assessment}} \times 100$$

Direct Attainment Level Thresholds:
- `< 50%` = Level 1 (Low)
- `50% to 70%` = Level 2 (Medium)
- `> 70%` = Level 3 (High)

### Indirect CO Attainment
Indirect score is calculated from Course End Survey responses on a scale of 1 to 3:
- Level 1 (1.0 to 1.99)
- Level 2 (2.0 to 2.49)
- Level 3 (2.5 to 3.0)

### Overall CO Attainment Formula
$$\text{Overall CO Attainment} = (\text{Direct Level} \times 0.80) + (\text{Indirect Level} \times 0.20)$$
(Default configured weightages: Direct 80%, Indirect 20%).

## 3. ATR & Approval Workflow Rules

- **Course ATR**: Prepared by Course Coordinator comparing Overall CO Attainment with set CO Target. Submitted to HOD / Programme Coordinator.
- **Programme ATR**: Prepared by Programme Coordinator for the completing Batch after aggregating course-level PO/PSO results. Submitted to HOD for verification.
- **Approval Snapshots**: Once an ATR or PO/PSO framework is approved, it becomes an immutable historical snapshot.
