import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

text = re.sub(r'report\.setTable2DirectJson\(toJson\(table2\)\);', 
r'''report.setTable2DirectJson(toJson(java.util.Map.of("po", table2PO, "pso", table2PSO)));''', text)

text = re.sub(r'List<CourseAttainmentReportDto\.Table2PoRow> table2PO = fromJson\(report\.getTable2DirectJson\(\), new TypeReference<>\(\) \{\}\);\n        List<CourseAttainmentReportDto\.Table2PsoRow> table2PSO = fromJson\(report\.getTable2DirectPsoJson\(\), new TypeReference<>\(\) \{\}\);',
r'''java.util.Map<String, Object> table2Map = fromJson(report.getTable2DirectJson(), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table2PoRow> table2PO = objectMapper.convertValue(table2Map.get("po"), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table2PsoRow> table2PSO = objectMapper.convertValue(table2Map.get("pso"), new TypeReference<>() {});''', text)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)
