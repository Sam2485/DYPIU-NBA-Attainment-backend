import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

text = re.sub(r'List<CourseAttainmentReportDto\.Table2Row> table2 = new ArrayList\<\>\(\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> poAtt = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("poAttainment", Collections\.emptyMap\(\)\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> psoAtt = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("psoAttainment", Collections\.emptyMap\(\)\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> poAvg = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("poAverages", Collections\.emptyMap\(\)\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> psoAvg = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("psoAverages", Collections\.emptyMap\(\)\);\n\n\s*for \(Map\.Entry<String, BigDecimal> e : poAtt\.entrySet\(\)\) \{\n\s*table2\.add\(CourseAttainmentReportDto\.Table2Row\.builder\(\)\n\s*\.poCode\(e\.getKey\(\)\)\n\s*\.averageMapping\(poAvg\.getOrDefault\(e\.getKey\(\), BigDecimal\.ZERO\)\)\n\s*\.directContribution\(e\.getValue\(\)\)\n\s*\.build\(\)\);\n\s*\}\n\s*for \(Map\.Entry<String, BigDecimal> e : psoAtt\.entrySet\(\)\) \{\n\s*table2\.add\(CourseAttainmentReportDto\.Table2Row\.builder\(\)\n\s*\.psoCode\(e\.getKey\(\)\)\n\s*\.averageMapping\(psoAvg\.getOrDefault\(e\.getKey\(\), BigDecimal\.ZERO\)\)\n\s*\.directContribution\(e\.getValue\(\)\)\n\s*\.build\(\)\);\n\s*\}',
r'''List<CourseAttainmentReportDto.Table2PoRow> table2PO = new ArrayList<>();
        List<CourseAttainmentReportDto.Table2PsoRow> table2PSO = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> poAtt = (Map<String, BigDecimal>) calcResult.getOrDefault("poAttainment", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> psoAtt = (Map<String, BigDecimal>) calcResult.getOrDefault("psoAttainment", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> poAvg = (Map<String, BigDecimal>) calcResult.getOrDefault("poAverages", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> psoAvg = (Map<String, BigDecimal>) calcResult.getOrDefault("psoAverages", Collections.emptyMap());

        for (Map.Entry<String, BigDecimal> e : poAtt.entrySet()) {
            table2PO.add(CourseAttainmentReportDto.Table2PoRow.builder()
                    .poCode(e.getKey())
                    .averageMapping(poAvg.getOrDefault(e.getKey(), BigDecimal.ZERO))
                    .directContribution(e.getValue())
                    .build());
        }
        for (Map.Entry<String, BigDecimal> e : psoAtt.entrySet()) {
            table2PSO.add(CourseAttainmentReportDto.Table2PsoRow.builder()
                    .psoCode(e.getKey())
                    .averageMapping(psoAvg.getOrDefault(e.getKey(), BigDecimal.ZERO))
                    .directContribution(e.getValue())
                    .build());
        }''', text)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)

