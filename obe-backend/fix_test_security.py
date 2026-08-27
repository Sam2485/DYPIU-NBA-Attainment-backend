import os
import re

files_to_fix = [
    "src/test/java/com/dypiu/nba/service/FrontendContractHardeningIntegrationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase5RuntimeFalsificationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase6ProgrammeCoordinatorIntegrationTest.java",
    "src/test/java/com/dypiu/nba/service/SchoolDirectorMappingTest.java"
]

for filepath in files_to_fix:
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Add @WithMockUser(roles = "ADMIN") to the class if not present
    if '@WithMockUser' not in content:
        content = content.replace('import org.junit.jupiter.api.Test;', 'import org.junit.jupiter.api.Test;\nimport org.springframework.security.test.context.support.WithMockUser;')
        
        # Find the class declaration and add @WithMockUser above it
        content = re.sub(r'(public class [A-Za-z0-9_]+ \{)', r'@WithMockUser(roles = "ADMIN")\n\1', content)
        
        with open(filepath, 'w') as f:
            f.write(content)
            print(f"Fixed {filepath}")

