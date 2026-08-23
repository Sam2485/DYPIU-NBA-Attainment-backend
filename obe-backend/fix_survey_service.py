import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/ProgrammeSurveyService.java', 'r') as f:
    text = f.read()

# Replace Outcomes setup
text = re.sub(
    r'List<ProgrammeSurveyResultDto\.OutcomeIndirectItem> poItems = new ArrayList\<\>\(\);\n        List<ProgrammeSurveyResultDto\.OutcomeIndirectItem> psoItems = new ArrayList\<\>\(\);\n\n        for \(Map\.Entry<String, BigDecimal> entry : poScore\.entrySet\(\)\) \{\n            poItems\.add\(ProgrammeSurveyResultDto\.OutcomeIndirectItem\.builder\(\)\n                    \.poCode\(entry\.getKey\(\)\)\n                    \.indirectAttainment\(entry\.getValue\(\)\)\n                    \.build\(\)\);\n        \}\n        for \(Map\.Entry<String, BigDecimal> entry : psoScore\.entrySet\(\)\) \{\n            psoItems\.add\(ProgrammeSurveyResultDto\.OutcomeIndirectItem\.builder\(\)\n                    \.psoCode\(entry\.getKey\(\)\)\n                    \.indirectAttainment\(entry\.getValue\(\)\)\n                    \.build\(\)\);\n        \}',
r'''List<ProgrammeSurveyResultDto.PoIndirectItem> poItems = new ArrayList<>();
        List<ProgrammeSurveyResultDto.PsoIndirectItem> psoItems = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : poScore.entrySet()) {
            poItems.add(ProgrammeSurveyResultDto.PoIndirectItem.builder()
                    .poCode(entry.getKey())
                    .indirectAttainment(entry.getValue())
                    .build());
        }
        for (Map.Entry<String, BigDecimal> entry : psoScore.entrySet()) {
            psoItems.add(ProgrammeSurveyResultDto.PsoIndirectItem.builder()
                    .psoCode(entry.getKey())
                    .indirectAttainment(entry.getValue())
                    .build());
        }''', text
)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/ProgrammeSurveyService.java', 'w') as f:
    f.write(text)

