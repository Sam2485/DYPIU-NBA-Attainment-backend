import re

files = [
    "src/test/java/com/dypiu/nba/service/FrontendContractHardeningIntegrationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase5RuntimeFalsificationTest.java",
    "src/test/java/com/dypiu/nba/service/Phase6ProgrammeCoordinatorIntegrationTest.java"
]

for filepath in files:
    with open(filepath, 'r') as f:
        content = f.read()
    
    if '@Autowired\n    private UserRepository userRepository;' not in content:
        # find the last @Autowired
        content = re.sub(
            r'(@Autowired\n\s*private \w+Repository \w+Repository;)', 
            r'\1\n    @Autowired\n    private com.dypiu.nba.repository.UserRepository userRepository;', 
            content, 
            count=1
        )
        
        with open(filepath, 'w') as f:
            f.write(content)
            print(f"Fixed {filepath}")

