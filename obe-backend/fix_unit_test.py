import re

with open('src/test/java/com/dypiu/nba/service/SchoolDirectorMappingTest.java', 'r') as f:
    content = f.read()

mock_setup = """
        Department mockDept = Department.builder().id("dept-cs").schoolId("school-1").build();
        when(departmentRepository.findById("dept-cs")).thenReturn(Optional.of(mockDept));
"""

# Insert mock before "when(masterProgrammeRepository.findById"
content = content.replace('when(masterProgrammeRepository.findById("prog-1a1b6c2e")).thenReturn(Optional.of(inputProg));', 
                          mock_setup + '        when(masterProgrammeRepository.findById("prog-1a1b6c2e")).thenReturn(Optional.of(inputProg));')

with open('src/test/java/com/dypiu/nba/service/SchoolDirectorMappingTest.java', 'w') as f:
    f.write(content)

