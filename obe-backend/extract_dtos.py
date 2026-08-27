import os
import re

files_to_check = [
    "src/main/java/com/dypiu/nba/entity/MasterProgramme.java",
    "src/main/java/com/dypiu/nba/entity/ProgrammeBatch.java",
    "src/main/java/com/dypiu/nba/entity/MasterCourse.java",
    "src/main/java/com/dypiu/nba/entity/ProgrammeBatchCourse.java",
    "src/main/java/com/dypiu/nba/dto/UserDto.java"
]

for filepath in files_to_check:
    if not os.path.exists(filepath): continue
    print(f"--- {os.path.basename(filepath)} ---")
    with open(filepath, 'r') as f:
        for line in f:
            if 'private ' in line and not 'static' in line:
                print(line.strip())

