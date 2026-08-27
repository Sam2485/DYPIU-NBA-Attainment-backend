import re
import os

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # We will strip arrays from @Mapping, e.g. {"/programmes/{id}", "/master-programmes/{id}"} -> "/master-programmes/{id}"
    
    # Simple regex for stripping arrays where the new name is also present:
    # Usually it's {"/legacy", "/new"} or {"/legacy/{id}", "/new/{id}"}
    
    # 1. AcademicController
    content = re.sub(r'@GetMapping\(\{"/programmes/coordinator", "/master-programmes/coordinator"\}\)', r'@GetMapping("/master-programmes/coordinator")', content)
    content = re.sub(r'@GetMapping\(\{"/programmes/\{id\}", "/master-programmes/\{id\}"\}\)', r'@GetMapping("/master-programmes/{id}")', content)
    content = re.sub(r'@GetMapping\(\{"/batches", "/programme-batches"\}\)', r'@GetMapping("/programme-batches")', content)
    content = re.sub(r'@GetMapping\(\{"/batches/course-coordinator", "/programme-batches/course-coordinator"\}\)', r'@GetMapping("/programme-batches/course-coordinator")', content)
    content = re.sub(r'@GetMapping\(\{"/master-courses", "/courses"\}\)', r'@GetMapping("/master-courses")', content)
    content = re.sub(r'@GetMapping\(\{"/master-courses/\{id\}", "/courses/\{id\}"\}\)', r'@GetMapping("/master-courses/{id}")', content)
    content = re.sub(r'@PostMapping\(\{"/master-courses", "/courses"\}\)', r'@PostMapping("/master-courses")', content)
    content = re.sub(r'@PutMapping\(\{"/master-courses/\{id\}", "/courses/\{id\}"\}\)', r'@PutMapping("/master-courses/{id}")', content)
    content = re.sub(r'@DeleteMapping\(\{"/master-courses/\{id\}", "/courses/\{id\}"\}\)', r'@DeleteMapping("/master-courses/{id}")', content)
    content = re.sub(r'@GetMapping\(\{"/coordinator/setup-progress", "/programme-coordinator/setup-progress", "/programmes/\{masterProgrammeId\}/setup-progress", "/master-programmes/\{masterProgrammeId\}/setup-progress"\}\)', r'@GetMapping("/master-programmes/{masterProgrammeId}/setup-progress")', content)
    
    # Fix paths that just use the legacy names outright:
    content = re.sub(r'@PostMapping\("/programmes"\)', r'@PostMapping("/master-programmes")', content)
    content = re.sub(r'@GetMapping\("/programmes"\)', r'@GetMapping("/master-programmes")', content)
    content = re.sub(r'@PutMapping\("/programmes/\{id\}"\)', r'@PutMapping("/master-programmes/{id}")', content)
    content = re.sub(r'@PutMapping\("/programmes/\{id\}/coordinator"\)', r'@PutMapping("/master-programmes/{id}/coordinator")', content)
    content = re.sub(r'@DeleteMapping\("/programmes/\{id\}"\)', r'@DeleteMapping("/master-programmes/{id}")', content)
    
    content = re.sub(r'@GetMapping\("/batches/\{id\}"\)', r'@GetMapping("/programme-batches/{id}")', content)
    content = re.sub(r'@GetMapping\("/batches/\{programmeBatchId\}/context"\)', r'@GetMapping("/programme-batches/{programmeBatchId}/context")', content)
    content = re.sub(r'@PostMapping\("/batches"\)', r'@PostMapping("/programme-batches")', content)
    content = re.sub(r'@PutMapping\("/batches/\{id\}"\)', r'@PutMapping("/programme-batches/{id}")', content)
    content = re.sub(r'@PostMapping\("/batches/\{id\}/status"\)', r'@PostMapping("/programme-batches/{id}/status")', content)
    content = re.sub(r'@PostMapping\("/batches/\{id\}/reopen"\)', r'@PostMapping("/programme-batches/{id}/reopen")', content)
    content = re.sub(r'@PostMapping\("/batches/\{id\}/close-reopening"\)', r'@PostMapping("/programme-batches/{id}/close-reopening")', content)
    content = re.sub(r'@DeleteMapping\("/batches/\{id\}"\)', r'@DeleteMapping("/programme-batches/{id}")', content)
    content = re.sub(r'@GetMapping\("/batches/\{programmeBatchId\}/students"\)', r'@GetMapping("/programme-batches/{programmeBatchId}/students")', content)
    content = re.sub(r'@PostMapping\("/batches/\{programmeBatchId\}/students"\)', r'@PostMapping("/programme-batches/{programmeBatchId}/students")', content)
    
    content = re.sub(r'@PostMapping\("/courses/allocate"\)', r'@PostMapping("/master-courses/allocate")', content)
    
    content = re.sub(r'@GetMapping\("/course-offerings"\)', r'@GetMapping("/programme-batch-courses")', content)
    content = re.sub(r'@GetMapping\("/course-offerings/\{offeringId\}"\)', r'@GetMapping("/programme-batch-courses/{offeringId}")', content)
    content = re.sub(r'@PostMapping\("/course-offerings"\)', r'@PostMapping("/programme-batch-courses")', content)
    content = re.sub(r'@PutMapping\("/course-offerings/\{offeringId\}"\)', r'@PutMapping("/programme-batch-courses/{offeringId}")', content)
    content = re.sub(r'@DeleteMapping\("/course-offerings/\{id\}"\)', r'@DeleteMapping("/programme-batch-courses/{id}")', content)
    content = re.sub(r'@GetMapping\("/course-offerings/\{offeringId\}/outcomes"\)', r'@GetMapping("/programme-batch-courses/{offeringId}/outcomes")', content)
    content = re.sub(r'@PostMapping\("/course-offerings/\{offeringId\}/outcomes"\)', r'@PostMapping("/programme-batch-courses/{offeringId}/outcomes")', content)
    content = re.sub(r'@GetMapping\("/course-offerings/\{offeringId\}/mappings"\)', r'@GetMapping("/programme-batch-courses/{offeringId}/mappings")', content)
    content = re.sub(r'@PutMapping\("/course-offerings/\{offeringId\}/mappings"\)', r'@PutMapping("/programme-batch-courses/{offeringId}/mappings")', content)
    
    content = re.sub(r'@GetMapping\("/courses/\{masterCourseId\}/co-targets"\)', r'@GetMapping("/master-courses/{masterCourseId}/co-targets")', content)
    content = re.sub(r'@GetMapping\("/courses/\{masterCourseId\}/outcomes"\)', r'@GetMapping("/master-courses/{masterCourseId}/outcomes")', content)
    content = re.sub(r'@GetMapping\("/courses/\{masterCourseId\}/mapping"\)', r'@GetMapping("/master-courses/{masterCourseId}/mapping")', content)
    content = re.sub(r'@GetMapping\("/programmes/\{masterProgrammeId\}/targets"\)', r'@GetMapping("/master-programmes/{masterProgrammeId}/targets")', content)
    
    # 2. OutcomeController
    content = re.sub(r'@GetMapping\("/programmes/\{masterProgrammeId\}/pos"\)', r'@GetMapping("/master-programmes/{masterProgrammeId}/pos")', content)
    content = re.sub(r'@PostMapping\("/programmes/\{masterProgrammeId\}/pos"\)', r'@PostMapping("/master-programmes/{masterProgrammeId}/pos")', content)
    content = re.sub(r'@GetMapping\("/programmes/\{masterProgrammeId\}/psos"\)', r'@GetMapping("/master-programmes/{masterProgrammeId}/psos")', content)
    content = re.sub(r'@PostMapping\("/programmes/\{masterProgrammeId\}/psos"\)', r'@PostMapping("/master-programmes/{masterProgrammeId}/psos")', content)
    content = re.sub(r'@GetMapping\("/programmes/\{masterProgrammeId\}/peos"\)', r'@GetMapping("/master-programmes/{masterProgrammeId}/peos")', content)
    content = re.sub(r'@PostMapping\("/programmes/\{masterProgrammeId\}/peos"\)', r'@PostMapping("/master-programmes/{masterProgrammeId}/peos")', content)
    content = re.sub(r'@GetMapping\("/courses/\{masterCourseId\}/cos"\)', r'@GetMapping("/master-courses/{masterCourseId}/cos")', content)
    content = re.sub(r'@PostMapping\("/courses/\{masterCourseId\}/cos"\)', r'@PostMapping("/master-courses/{masterCourseId}/cos")', content)
    content = re.sub(r'@GetMapping\("/programmes/\{masterProgrammeId\}/targets"\)', r'@GetMapping("/master-programmes/{masterProgrammeId}/targets")', content)
    content = re.sub(r'@PostMapping\("/programmes/\{masterProgrammeId\}/targets"\)', r'@PostMapping("/master-programmes/{masterProgrammeId}/targets")', content)
    content = re.sub(r'@GetMapping\("/courses/\{masterCourseId\}/mappings"\)', r'@GetMapping("/master-courses/{masterCourseId}/mappings")', content)
    content = re.sub(r'@PostMapping\("/courses/\{masterCourseId\}/mappings"\)', r'@PostMapping("/master-courses/{masterCourseId}/mappings")', content)
    
    # 3. AtrController
    content = re.sub(r'@GetMapping\(\{"/course/\{masterCourseId\}", "/courses/\{masterCourseId\}"\}\)', r'@GetMapping("/master-courses/{masterCourseId}")', content)
    content = re.sub(r'@GetMapping\(\{"/programme/\{masterProgrammeId\}", "/programmes/\{masterProgrammeId\}"\}\)', r'@GetMapping("/master-programmes/{masterProgrammeId}")', content)
    content = re.sub(r'@GetMapping\(\{"/programme/previous-batch/\{programmeBatchId\}", "/programmes/previous-batch/\{programmeBatchId\}"\}\)', r'@GetMapping("/master-programmes/previous-batch/{programmeBatchId}")', content)
    content = re.sub(r'@GetMapping\(\{"/programme/previous-year/\{programmeBatchId\}", "/programmes/previous-year/\{programmeBatchId\}"\}\)', r'@GetMapping("/master-programmes/previous-year/{programmeBatchId}")', content)
    
    # 4. ReportController
    content = re.sub(r'@GetMapping\("/programmes/\{masterProgrammeId\}/batch-comparison"\)', r'@GetMapping("/master-programmes/{masterProgrammeId}/batch-comparison")', content)
    
    with open(filepath, 'w') as f:
        f.write(content)

process_file('src/main/java/com/dypiu/nba/controller/AcademicController.java')
process_file('src/main/java/com/dypiu/nba/controller/OutcomeController.java')
process_file('src/main/java/com/dypiu/nba/controller/AtrController.java')
process_file('src/main/java/com/dypiu/nba/controller/ReportController.java')

