import re

with open('src/main/java/com/dypiu/nba/controller/DashboardController.java', 'r') as f:
    content = f.read()

# Remove getProgrammeCoordinatorDashboard overload
pattern1 = r'public ResponseEntity<ApiResponse<Map<String, Object>>> getProgrammeCoordinatorDashboard\(\s*String masterProgrammeId,\s*Principal principal\)\s*\{\s*return getProgrammeCoordinatorDashboard[^}]*;\s*\}'
content = re.sub(pattern1, '', content)

# Remove getCourseCoordinatorDashboard overload
pattern2 = r'public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseCoordinatorDashboard\(\s*String masterCourseId,\s*String programmeBatchId,\s*Principal principal\)\s*\{\s*return getCourseCoordinatorDashboard[^}]*;\s*\}'
content = re.sub(pattern2, '', content)

# In case the parameter lists were slightly different, let's just use regex for the whole block of those specific methods that do not have annotations (since they are delegates without @GetMapping).
pattern3 = r'public ResponseEntity<ApiResponse<Map<String, Object>>> getProgrammeCoordinatorDashboard\([^)]*\)\s*\{\s*return getProgrammeCoordinatorDashboard\([^)]*\);\s*\}'
content = re.sub(pattern3, '', content)

pattern4 = r'public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseCoordinatorDashboard\([^)]*\)\s*\{\s*return getCourseCoordinatorDashboard\([^)]*\);\s*\}'
content = re.sub(pattern4, '', content)


with open('src/main/java/com/dypiu/nba/controller/DashboardController.java', 'w') as f:
    f.write(content)
