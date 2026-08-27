import re

with open('src/main/java/com/dypiu/nba/service/OutcomeService.java', 'r') as f:
    content = f.read()

replacement = """
    private void enforceCourseCoordinatorMutation(String courseIdOrOfferingId) {
        CurrentUserScope scope = getScope();
        if (scope.isAdmin() || scope.isIqac()) return;
        String offeringId = resolveOfferingId(courseIdOrOfferingId);
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + offeringId));
        
        boolean isAssignedCourseCoordinator = Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId());
        
        boolean isAssignedProgrammeCoordinator = false;
        if (scope.isProgrammeCoordinator()) {
            ProgrammeBatch batch = programmeBatchRepository.findById(offering.getProgrammeBatchId()).orElse(null);
            if (batch != null && Objects.equals(batch.getMasterProgrammeId(), scope.getMasterProgrammeId())) {
                isAssignedProgrammeCoordinator = true;
            }
        }
        
        if (!isAssignedCourseCoordinator && !isAssignedProgrammeCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: only the coordinator assigned to this course offering may modify it.");
        }
    }
"""
content = re.sub(
    r'\s*private void enforceCourseCoordinatorMutation\(String [a-zA-Z]+\) \{.*?(?=\s*private void enforceProgrammeCoordinatorMutation)', 
    replacement, 
    content, 
    flags=re.DOTALL
)

with open('src/main/java/com/dypiu/nba/service/OutcomeService.java', 'w') as f:
    f.write(content)

