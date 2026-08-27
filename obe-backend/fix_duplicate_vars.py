import re
import glob

def remove_duplicates(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Pattern for duplicate RequestParams:
    # @RequestParam(required = false) String masterProgrammeId, @RequestParam(required = false) String masterProgrammeId
    content = re.sub(r'(@RequestParam\([^)]*\)\s*String\s*masterProgrammeId\s*),\s*@RequestParam\([^)]*\)\s*String\s*masterProgrammeId(Param)?', r'\1', content)
    content = re.sub(r',?\s*@RequestParam\([^)]*\)\s*String\s*masterProgrammeId(Param)?\s*,?\s*(@RequestParam\([^)]*\)\s*String\s*masterProgrammeId)', r', \2', content)

    # Same for programmeBatchId
    content = re.sub(r'(@RequestParam\([^)]*\)\s*String\s*programmeBatchId\s*),\s*@RequestParam\([^)]*\)\s*String\s*programmeBatchId(Param)?', r'\1', content)
    content = re.sub(r',?\s*@RequestParam\([^)]*\)\s*String\s*programmeBatchId(Param)?\s*,?\s*(@RequestParam\([^)]*\)\s*String\s*programmeBatchId)', r', \2', content)

    # Same for programmeBatchCourseId
    content = re.sub(r'(@RequestParam\([^)]*\)\s*String\s*programmeBatchCourseId\s*),\s*@RequestParam\([^)]*\)\s*String\s*programmeBatchCourseId(Param)?', r'\1', content)
    content = re.sub(r',?\s*@RequestParam\([^)]*\)\s*String\s*programmeBatchCourseId(Param)?\s*,?\s*(@RequestParam\([^)]*\)\s*String\s*programmeBatchCourseId)', r', \2', content)

    # Same for masterCourseId
    content = re.sub(r'(@RequestParam\([^)]*\)\s*String\s*masterCourseId\s*),\s*@RequestParam\([^)]*\)\s*String\s*masterCourseId(Param)?', r'\1', content)
    content = re.sub(r',?\s*@RequestParam\([^)]*\)\s*String\s*masterCourseId(Param)?\s*,?\s*(@RequestParam\([^)]*\)\s*String\s*masterCourseId)', r', \2', content)
    
    # Fix variables without @RequestParam (e.g. @PathVariable or just general duplicates in signatures)
    content = re.sub(r'(String\s*masterProgrammeId)\s*,\s*String\s*masterProgrammeId(Param)?\b', r'\1', content)
    content = re.sub(r'(String\s*programmeBatchId)\s*,\s*String\s*programmeBatchId(Param)?\b', r'\1', content)
    content = re.sub(r'(String\s*programmeBatchCourseId)\s*,\s*String\s*programmeBatchCourseId(Param)?\b', r'\1', content)
    content = re.sub(r'(String\s*masterCourseId)\s*,\s*String\s*masterCourseId(Param)?\b', r'\1', content)
    
    # Let's handle the specific DTO duplications:
    # private String programmeBatchCourseId;
    content = re.sub(r'(private\s+String\s+programmeBatchCourseId\s*;[^\n]*\n)(\s*private\s+String\s+programmeBatchCourseId\s*;)', r'\1', content)
    content = re.sub(r'(private\s+String\s+masterProgrammeId\s*;[^\n]*\n)(\s*private\s+String\s+masterProgrammeId\s*;)', r'\1', content)
    
    with open(filepath, 'w') as f:
        f.write(content)

files_to_fix = [
    'src/main/java/com/dypiu/nba/controller/AcademicController.java',
    'src/main/java/com/dypiu/nba/controller/ApprovalController.java',
    'src/main/java/com/dypiu/nba/controller/DashboardController.java',
    'src/main/java/com/dypiu/nba/dto/CourseCoordinatorSummaryDto.java',
    'src/main/java/com/dypiu/nba/dto/ProgrammeCoordinatorSummaryDto.java'
]

for f in files_to_fix:
    remove_duplicates(f)

