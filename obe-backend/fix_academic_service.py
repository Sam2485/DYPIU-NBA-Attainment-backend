import re

with open('src/main/java/com/dypiu/nba/service/AcademicService.java', 'r') as f:
    content = f.read()

# I need to insert the school-scoped checks instead of existsByCodeAndDeletedAtIsNull
# Find the block where `existsByCodeAndDeletedAtIsNull` is used in saveProgramme
target_block = """        } else {
            // Uniqueness must apply only among non-deleted records.
            if (programme.getCode() != null && masterProgrammeRepository.existsByCodeAndDeletedAtIsNull(programme.getCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A MasterProgramme with this code already exists and is active.");
            }
            targetProg.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        }"""

new_block = """        } else {
            targetProg.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        }
        
        // Ensure department is loaded to get schoolId
        String deptId = targetProg.getDepartmentId();
        if (deptId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department ID is required.");
        }
        Department dept = departmentRepository.findById(deptId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Department ID."));
        String schoolId = dept.getSchoolId();
        
        String excludeId = targetProg.getId();
        
        if (targetProg.getCode() != null) {
            boolean codeExists = masterProgrammeRepository.existsByCodeInSchoolExcludeId(schoolId, targetProg.getCode(), excludeId);
            if (codeExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Programme code already exists in this school.");
            }
        }
        
        if (targetProg.getName() != null) {
            boolean nameExists = masterProgrammeRepository.existsByNameInSchoolExcludeId(schoolId, targetProg.getName(), excludeId);
            if (nameExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Programme name already exists in this school.");
            }
        }"""

if target_block in content:
    content = content.replace(target_block, new_block)
else:
    print("Could not find the target block in AcademicService.java")
    
with open('src/main/java/com/dypiu/nba/service/AcademicService.java', 'w') as f:
    f.write(content)

