import re

content = open('/Users/rajshaikh/Desktop/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md', 'r').read()

# 1. Report 1 (average-mapping)
r1_old = r'''### GET /programme-batches/{programmeBatchId}/reports/average-mapping
- \*\*CATEGORY:\*\* Programme Batch Reports
- \*\*METHOD:\*\* GET
- \*\*PATH:\*\* `/programme-batches/{programmeBatchId}/reports/average-mapping`
- \*\*PURPOSE:\*\* Report 1 \(Semester Average Mapping Matrix\)\. Independent data product\.
- \*\*PATH PARAMETERS:\*\* `programmeBatchId` \(string\)
- \*\*RESPONSE BODY:\*\*
```json
\{
  "success": "boolean",
  "data": \[
    \{
      "poCode": "string",
        "psoCode": "string",
      "semesterAverages": \[
        \{
          "semester": "number",
          "average": "number"
        \}
      \],
      "programmeAverage": "number"
    \}
  \]
\}
```'''

r1_new = '''### GET /programme-batches/{programmeBatchId}/reports/average-mapping
- **CATEGORY:** Programme Batch Reports
- **METHOD:** GET
- **PATH:** `/programme-batches/{programmeBatchId}/reports/average-mapping`
- **PURPOSE:** Report 1 (Semester Average Mapping Matrix). Independent data product.
- **PATH PARAMETERS:** `programmeBatchId` (string)
- **RESPONSE BODY:**
```json
{
  "success": "boolean",
  "data": {
    "poMappings": [
      {
        "poCode": "string",
        "semesterAverages": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeAverageMapping": "number"
      }
    ],
    "psoMappings": [
      {
        "psoCode": "string",
        "semesterAverages": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeAverageMapping": "number"
      }
    ]
  }
}
```'''

content = re.sub(r1_old, r1_new, content)

# 2. Report 2 (direct-attainment)
r2_old = r'''### GET /programme-batches/{programmeBatchId}/reports/direct-attainment
- \*\*CATEGORY:\*\* Programme Batch Reports
- \*\*METHOD:\*\* GET
- \*\*PATH:\*\* `/programme-batches/{programmeBatchId}/reports/direct-attainment`
- \*\*PURPOSE:\*\* Report 2 \(Programme Direct PO/PSO Attainment aggregated from Course Table 2\)\.
- \*\*PATH PARAMETERS:\*\* `programmeBatchId` \(string\)
- \*\*RESPONSE BODY:\*\*
```json
\{
  "success": "boolean",
  "data": \[
    \{
      "poCode": "string",
        "psoCode": "string",
      "semesterAverages": \[
        \{
          "semester": "number",
          "average": "number"
        \}
      \],
      "programmeAverage": "number"
    \}
  \]
\}
```'''

r2_new = '''### GET /programme-batches/{programmeBatchId}/reports/direct-attainment
- **CATEGORY:** Programme Batch Reports
- **METHOD:** GET
- **PATH:** `/programme-batches/{programmeBatchId}/reports/direct-attainment`
- **PURPOSE:** Report 2 (Programme Direct PO/PSO Attainment aggregated from Course Table 2).
- **PATH PARAMETERS:** `programmeBatchId` (string)
- **RESPONSE BODY:**
```json
{
  "success": "boolean",
  "data": {
    "poDirectAttainment": [
      {
        "poCode": "string",
        "semesterDirectAttainments": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeDirectAttainment": "number"
      }
    ],
    "psoDirectAttainment": [
      {
        "psoCode": "string",
        "semesterDirectAttainments": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeDirectAttainment": "number"
      }
    ]
  }
}
```'''

content = re.sub(r2_old, r2_new, content)

# 3. Report 3 (indirect-attainment)
r3_old = r'''### GET /programme-batches/{programmeBatchId}/reports/indirect-attainment
- \*\*CATEGORY:\*\* Programme Batch Reports
- \*\*METHOD:\*\* GET
- \*\*PATH:\*\* `/programme-batches/{programmeBatchId}/reports/indirect-attainment`
- \*\*PURPOSE:\*\* Report 3 \(Exit Survey Indirect Attainment\)\.
- \*\*PATH PARAMETERS:\*\* `programmeBatchId` \(string\)
- \*\*RESPONSE BODY:\*\*
```json
\{
  "success": "boolean",
  "data": \[
    \{
      "poCode": "string",
        "psoCode": "string",
      "substantialPercentage": "number",
      "moderatePercentage": "number",
      "slightPercentage": "number",
      "weightedScore": "number",
      "indirectLevel": "number"
    \}
  \]
\}
```'''

r3_new = '''### GET /programme-batches/{programmeBatchId}/reports/indirect-attainment
- **CATEGORY:** Programme Batch Reports
- **METHOD:** GET
- **PATH:** `/programme-batches/{programmeBatchId}/reports/indirect-attainment`
- **PURPOSE:** Report 3 (Exit Survey Indirect Attainment).
- **PATH PARAMETERS:** `programmeBatchId` (string)
- **RESPONSE BODY:**
```json
{
  "success": "boolean",
  "data": {
    "poIndirectAttainment": [
      {
        "poCode": "string",
        "percentageSubstantial": "number",
        "percentageModerate": "number",
        "percentageSlight": "number",
        "weightedScore": "number",
        "indirectPercentage": "number",
        "indirectAttainmentLevel": "number"
      }
    ],
    "psoIndirectAttainment": [
      {
        "psoCode": "string",
        "percentageSubstantial": "number",
        "percentageModerate": "number",
        "percentageSlight": "number",
        "weightedScore": "number",
        "indirectPercentage": "number",
        "indirectAttainmentLevel": "number"
      }
    ]
  }
}
```'''

content = re.sub(r3_old, r3_new, content)

# 4. Report 4 (overall-attainment)
r4_old = r'''### GET /programme-batches/{programmeBatchId}/reports/overall-attainment
- \*\*CATEGORY:\*\* Programme Batch Reports
- \*\*METHOD:\*\* GET
- \*\*PATH:\*\* `/programme-batches/{programmeBatchId}/reports/overall-attainment`
- \*\*PURPOSE:\*\* Report 4 \(Overall PO/PSO Attainment: 80% Direct \+ 20% Indirect per PO/PSO\)\.
- \*\*PATH PARAMETERS:\*\* `programmeBatchId` \(string\)
- \*\*RESPONSE BODY:\*\*
```json
\{
  "success": "boolean",
  "data": \[
    \{
      "poCode": "string",
        "psoCode": "string",
      "directAttainment": "number",
      "indirectAttainment": "number",
      "overallAttainment": "number",
      "targetLevel": "number",
      "targetMet": "boolean",
      "observation": "string"
    \}
  \]
\}
```'''

r4_new = '''### GET /programme-batches/{programmeBatchId}/reports/overall-attainment
- **CATEGORY:** Programme Batch Reports
- **METHOD:** GET
- **PATH:** `/programme-batches/{programmeBatchId}/reports/overall-attainment`
- **PURPOSE:** Report 4 (Overall PO/PSO Attainment: 80% Direct + 20% Indirect per PO/PSO).
- **PATH PARAMETERS:** `programmeBatchId` (string)
- **RESPONSE BODY:**
```json
{
  "success": "boolean",
  "data": {
    "poOverallAttainment": [
      {
        "poCode": "string",
        "statement": "string",
        "targetLevel": "number",
        "directAttainment": "number",
        "indirectAttainment": "number",
        "finalAttainment": "number",
        "targetMet": "boolean",
        "observation": "string"
      }
    ],
    "psoOverallAttainment": [
      {
        "psoCode": "string",
        "statement": "string",
        "targetLevel": "number",
        "directAttainment": "number",
        "indirectAttainment": "number",
        "finalAttainment": "number",
        "targetMet": "boolean",
        "observation": "string"
      }
    ]
  }
}
```'''

content = re.sub(r4_old, r4_new, content)

# 5. Programme Batch Attainment Main
r_main_old = r'''### GET /programme-batches/{programmeBatchId}/reports/attainment-main
- \*\*CATEGORY:\*\* Programme Batch Reports
- \*\*METHOD:\*\* GET
- \*\*PATH:\*\* `/programme-batches/{programmeBatchId}/reports/attainment-main`
- \*\*PURPOSE:\*\* Consolidated snapshot containing Reports 1, 2, 3, and 4\.
- \*\*PATH PARAMETERS:\*\* `programmeBatchId` \(string\)
- \*\*RESPONSE BODY:\*\*
```json
\{
  "success": "boolean",
  "data": \{
    "programmeBatchAttainmentReportId": "string",
    "programmeBatchId": "string",
    "batchName": "string",
    "masterProgrammeId": "string",
    "programmeName": "string",
    "programmeCode": "string",
    "status": "string",
    "overallProgrammeAttainment": "number",
    "report1AverageMapping": "array",
    "report2DirectAttainment": "array",
    "report3IndirectAttainment": "array",
    "report4OverallAttainment": "array",
    "submittedBy": "string",
    "submittedAt": "string",
    "approvedBy": "string",
    "approvedAt": "string"
  \}
\}
```'''

r_main_new = '''### GET /programme-batches/{programmeBatchId}/reports/attainment-main
- **CATEGORY:** Programme Batch Reports
- **METHOD:** GET
- **PATH:** `/programme-batches/{programmeBatchId}/reports/attainment-main`
- **PURPOSE:** Consolidated snapshot containing Reports 1, 2, 3, and 4.
- **PATH PARAMETERS:** `programmeBatchId` (string)
- **RESPONSE BODY:**
```json
{
  "success": "boolean",
  "data": {
    "programmeBatchAttainmentReportId": "string",
    "programmeBatchId": "string",
    "batchName": "string",
    "masterProgrammeId": "string",
    "programmeName": "string",
    "programmeCode": "string",
    "status": "string",
    "overallProgrammeAttainment": "number",
    "report1AverageMappingPO": [
      {
        "poCode": "string",
        "semesterAverages": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeAverageMapping": "number"
      }
    ],
    "report1AverageMappingPSO": [
      {
        "psoCode": "string",
        "semesterAverages": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeAverageMapping": "number"
      }
    ],
    "report2DirectAttainmentPO": [
      {
        "poCode": "string",
        "semesterDirectAttainments": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeDirectAttainment": "number"
      }
    ],
    "report2DirectAttainmentPSO": [
      {
        "psoCode": "string",
        "semesterDirectAttainments": [
          {
            "semester": "number",
            "value": "number"
          }
        ],
        "programmeDirectAttainment": "number"
      }
    ],
    "report3IndirectAttainmentPO": [
      {
        "poCode": "string",
        "percentageSubstantial": "number",
        "percentageModerate": "number",
        "percentageSlight": "number",
        "weightedScore": "number",
        "indirectPercentage": "number",
        "indirectAttainmentLevel": "number"
      }
    ],
    "report3IndirectAttainmentPSO": [
      {
        "psoCode": "string",
        "percentageSubstantial": "number",
        "percentageModerate": "number",
        "percentageSlight": "number",
        "weightedScore": "number",
        "indirectPercentage": "number",
        "indirectAttainmentLevel": "number"
      }
    ],
    "report4OverallAttainmentPO": [
      {
        "poCode": "string",
        "statement": "string",
        "targetLevel": "number",
        "directAttainment": "number",
        "indirectAttainment": "number",
        "finalAttainment": "number",
        "targetMet": "boolean",
        "observation": "string"
      }
    ],
    "report4OverallAttainmentPSO": [
      {
        "psoCode": "string",
        "statement": "string",
        "targetLevel": "number",
        "directAttainment": "number",
        "indirectAttainment": "number",
        "finalAttainment": "number",
        "targetMet": "boolean",
        "observation": "string"
      }
    ],
    "submittedBy": "string",
    "submittedAt": "string",
    "approvedBy": "string",
    "approvedAt": "string"
  }
}
```'''

content = re.sub(r_main_old, r_main_new, content)

# 6. Course Attainment Main (change programmeBatchAttainmentReportId -> courseAttainmentReportId)
c_main_old = r'''### GET /programme-batch-courses/{programmeBatchCourseId}/attainment-main
- \*\*CATEGORY:\*\* Course Attainment
- \*\*METHOD:\*\* GET
- \*\*PATH:\*\* `/programme-batch-courses/{programmeBatchCourseId}/attainment-main`
- \*\*PURPOSE:\*\* Computes or retrieves persisted Course Attainment Tables 1, 2, and 3\.
- \*\*PATH PARAMETERS:\*\* `programmeBatchCourseId` \(string\)
- \*\*RESPONSE BODY:\*\*
```json
\{
  "success": "boolean",
  "data": \{
    "programmeBatchAttainmentReportId": "string",'''

c_main_new = '''### GET /programme-batch-courses/{programmeBatchCourseId}/attainment-main
- **CATEGORY:** Course Attainment
- **METHOD:** GET
- **PATH:** `/programme-batch-courses/{programmeBatchCourseId}/attainment-main`
- **PURPOSE:** Computes or retrieves persisted Course Attainment Tables 1, 2, and 3.
- **PATH PARAMETERS:** `programmeBatchCourseId` (string)
- **RESPONSE BODY:**
```json
{
  "success": "boolean",
  "data": {
    "courseAttainmentReportId": "string",'''

content = re.sub(c_main_old, c_main_new, content)

paths = [
    '/Users/rajshaikh/Desktop/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md',
    '/Users/rajshaikh/Desktop/PHASE_10_16_FINAL_FRONTEND_API_CONTRACT.md',
    '/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/PHASE_10_16_FINAL_FRONTEND_API_CONTRACT.md',
    '/Users/rajshaikh/.gemini/antigravity-cli/brain/6c94e0a5-e10b-4215-a2f8-3316107ff5ea/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md',
    '/Users/rajshaikh/.gemini/antigravity-cli/brain/6c94e0a5-e10b-4215-a2f8-3316107ff5ea/PHASE_10_16_FINAL_FRONTEND_API_CONTRACT.md',
]

for p in paths:
    with open(p, 'w') as f:
        f.write(content)

print("Updated all contracts successfully!")
