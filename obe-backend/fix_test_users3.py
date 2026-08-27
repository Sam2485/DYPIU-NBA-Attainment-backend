import re

files_to_fix = [
    "src/test/java/com/dypiu/nba/service/FrontendContractHardeningIntegrationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase5RuntimeFalsificationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase6ProgrammeCoordinatorIntegrationTest.java"
]

for filepath in files_to_fix:
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Add passwordHash to the user builder
    content = content.replace('.role(com.dypiu.nba.entity.UserRole.ADMIN)', '.role(com.dypiu.nba.entity.UserRole.ADMIN)\n                .passwordHash("dummy")')
    
    with open(filepath, 'w') as f:
        f.write(content)
        print(f"Fixed {filepath}")

