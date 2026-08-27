import re

files_to_fix = [
    "src/test/java/com/dypiu/nba/service/FrontendContractHardeningIntegrationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase5RuntimeFalsificationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase6ProgrammeCoordinatorIntegrationTest.java"
]

for filepath in files_to_fix:
    with open(filepath, 'r') as f:
        content = f.read()
    
    user_creation = """
        if (userRepository.findByUsername("user").isEmpty()) {
            userRepository.save(com.dypiu.nba.entity.User.builder()
                .username("user")
                .email("user@dypiu.ac.in")
                .name("Test User")
                .role(com.dypiu.nba.entity.UserRole.ADMIN)
                .build());
        }
"""
    
    content = re.sub(r'(@BeforeEach\s*public void setup\(\)\s*\{)', r'\1' + user_creation, content)
    
    with open(filepath, 'w') as f:
        f.write(content)
        print(f"Fixed {filepath}")

