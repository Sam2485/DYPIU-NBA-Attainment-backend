import re

with open('/Users/rajshaikh/Desktop/PHASE_10_10_FINAL_FRONTEND_API_CONTRACT.md', 'r') as f:
    text = f.read()

# Users API
text = re.sub(r'("user": \{\s*)"id"', r'\1"userId"', text)
text = re.sub(r'(\s*)"id": "number",(\s*"username":)', r'\1"userId": "number",\2', text)

# Dashboards
text = re.sub(r'("school": \{\s*)"id"', r'\1"schoolId"', text)
text = re.sub(r'("department": \{\s*)"id"', r'\1"departmentId"', text)
text = re.sub(r'("programme": \{\s*)"id"', r'\1"masterProgrammeId"', text)
text = re.sub(r'("batches": \[\s*\{\s*)"id"', r'\1"programmeBatchId"', text)
text = re.sub(r'("course": \{\s*)"id"', r'\1"masterCourseId"', text)

# Master APIs
text = re.sub(r'("data": \[\s*\{\s*)"id": "string",(\s*"name": "string",\s*"code": "string",\s*"departmentId")', r'\1"masterProgrammeId": "string",\2', text)
text = re.sub(r'("data": \[\s*\{\s*)"id": "string",(\s*"programmeBatchId": "string",\s*"batchName")', r'\1"programmeBatchAttainmentReportId": "string",\2', text)
text = re.sub(r'("data": \[\s*\{\s*)"id": "string",(\s*"programmeBatchCourseId": "string",\s*"masterCourseId")', r'\1"courseAttainmentReportId": "string",\2', text)

# Approvals & Governance
text = re.sub(r'("data": \[\s*\{\s*)"id": "string",(\s*"type": "string",\s*"title")', r'\1"approvalRequestId": "string",\2', text)
text = re.sub(r'("data": \{\s*)"id": "number",(\s*"resourceType": "string",\s*"resourceId")', r'\1"deletionRequestId": "number",\2', text)
text = re.sub(r'("data": \{\s*)"id": "number",(\s*"status": "string"\s*\}\s*\})', r'\1"deletionRequestId": "number",\2', text)
text = re.sub(r'("content": \[\s*\{\s*)"id": "number",(\s*"actorId")', r'\1"auditLogId": "number",\2', text)

with open('/Users/rajshaikh/Desktop/PHASE_10_14_FINAL_FRONTEND_API_CONTRACT.md', 'w') as f:
    f.write(text)

