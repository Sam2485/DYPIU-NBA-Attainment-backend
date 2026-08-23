import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

# Fix the PSO loop
text = re.sub(r'for \(Map\.Entry<String, BigDecimal> e : psoAtt\.entrySet\(\)\) \{\n            table2PO\.add\(CourseAttainmentReportDto\.Table2PoRow\.builder\(\)\n                    \.psoCode\(e\.getKey\(\)\)',
r'''for (Map.Entry<String, BigDecimal> e : psoAtt.entrySet()) {
            table2PSO.add(CourseAttainmentReportDto.Table2PsoRow.builder()
                    .psoCode(e.getKey())''', text)

# Set the dto fields
text = re.sub(r'\.table2Direct\(table2\)', r'.table2DirectPO(table2PO)\n                .table2DirectPSO(table2PSO)', text)

# Fix deserialization in load report
text = re.sub(r'List<CourseAttainmentReportDto\.Table2Row> table2 = fromJson\(report\.getTable2DirectJson\(\), new TypeReference<>\(\) \{\}\);',
r'''List<CourseAttainmentReportDto.Table2PoRow> table2PO = fromJson(report.getTable2DirectJson(), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table2PsoRow> table2PSO = fromJson(report.getTable2DirectPsoJson(), new TypeReference<>() {});''', text)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)
