import re

with open('src/main/java/com/dypiu/nba/service/OutcomeService.java', 'r') as f:
    content = f.read()

replacement = """
    private void enforceProgrammeCoordinatorMutation(String programmeOrProgrammeBatchId) {
        CurrentUserScope scope = getScope();
        if (scope.isAdmin() || scope.isIqac()) return;
        String programmeBatchId = resolveProgrammeBatchId(programmeOrProgrammeBatchId);
        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + programmeBatchId));
        
        boolean isAssignedBatchCoordinator = Objects.equals(batch.getCoordinatorId(), scope.getUserId());
        boolean isAssignedProgrammeCoordinator = scope.isProgrammeCoordinator() && 
                                               Objects.equals(batch.getMasterProgrammeId(), scope.getMasterProgrammeId());
        
        if (!isAssignedBatchCoordinator && !isAssignedProgrammeCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: only the coordinator assigned to this programme batch may modify its targets.");
        }
    }
"""
content = re.sub(
    r'\s*private void enforceProgrammeCoordinatorMutation\(String [a-zA-Z]+\) \{.*?(?=\s*private void enforceSchoolScope)', 
    replacement, 
    content, 
    flags=re.DOTALL
)

with open('src/main/java/com/dypiu/nba/service/OutcomeService.java', 'w') as f:
    f.write(content)

