import re
with open('src/main/java/com/dypiu/nba/controller/AttainmentController.java', 'r') as f:
    content = f.read()

content = re.sub(r'@GetMapping\(\{"/survey/\{programmeBatchCourseId\}", "/course-offerings/\{programmeBatchCourseId\}/survey"\}\)', r'@GetMapping("/programme-batch-courses/{programmeBatchCourseId}/survey")', content)
content = re.sub(r'@PostMapping\(\{"/survey/\{programmeBatchCourseId\}", "/course-offerings/\{programmeBatchCourseId\}/survey"\}\)', r'@PostMapping("/programme-batch-courses/{programmeBatchCourseId}/survey")', content)
content = re.sub(r'@PostMapping\(value = \{"/survey/\{programmeBatchCourseId\}/upload", "/course-offerings/\{programmeBatchCourseId\}/survey/upload"\}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE\)', r'@PostMapping(value = "/programme-batch-courses/{programmeBatchCourseId}/survey/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)', content)

with open('src/main/java/com/dypiu/nba/controller/AttainmentController.java', 'w') as f:
    f.write(content)
