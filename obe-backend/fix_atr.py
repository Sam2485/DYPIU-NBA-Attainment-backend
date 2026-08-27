import re

with open('src/main/java/com/dypiu/nba/controller/AtrController.java', 'r') as f:
    content = f.read()

content = re.sub(
    r'@RequestMapping\(value = \{"/course/\{masterCourseId\}", "/courses/\{masterCourseId\}"\}, method = \{RequestMethod\.POST, RequestMethod\.PUT\}\)', 
    r'@RequestMapping(value = "/master-courses/{masterCourseId}", method = {RequestMethod.POST, RequestMethod.PUT})', 
    content
)

content = re.sub(
    r'@RequestMapping\(value = \{"/programme/\{masterProgrammeId\}", "/programmes/\{masterProgrammeId\}"\}, method = \{RequestMethod\.POST, RequestMethod\.PUT\}\)', 
    r'@RequestMapping(value = "/master-programmes/{masterProgrammeId}", method = {RequestMethod.POST, RequestMethod.PUT})', 
    content
)

with open('src/main/java/com/dypiu/nba/controller/AtrController.java', 'w') as f:
    f.write(content)

