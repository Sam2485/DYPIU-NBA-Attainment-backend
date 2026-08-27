import re

filepath = "src/test/java/com/dypiu/nba/service/FrontendContractHardeningIntegrationTest.java"
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace('"Dr. Rahul Verma (MasterProgramme Coordinator)"', '"Test User"')
content = content.replace('"Dr. Ananya Joshi (HOD)"', '"Test User"')
content = content.replace('assertEquals("Test HOD",', 'assertEquals("Test User",')

with open(filepath, 'w') as f:
    f.write(content)

