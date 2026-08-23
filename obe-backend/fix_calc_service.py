import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java', 'r') as f:
    text = f.read()

text = re.sub(r'List<ProgrammeSurveyResultDto\.OutcomeIndirectItem> poItems = new ArrayList\<\>\(\);',
r'''List<ProgrammeSurveyResultDto.PoIndirectItem> poItems = new ArrayList<>();''', text)

text = re.sub(r'List<ProgrammeSurveyResultDto\.OutcomeIndirectItem> psoItems = new ArrayList\<\>\(\);',
r'''List<ProgrammeSurveyResultDto.PsoIndirectItem> psoItems = new ArrayList<>();''', text)

text = re.sub(r'poItems\.add\(ProgrammeSurveyResultDto\.OutcomeIndirectItem\.builder\(\)\n                                \.poCode\(poCode\)',
r'''poItems.add(ProgrammeSurveyResultDto.PoIndirectItem.builder()
                                .poCode(poCode)''', text)

text = re.sub(r'psoItems\.add\(ProgrammeSurveyResultDto\.OutcomeIndirectItem\.builder\(\)\n                                \.psoCode\(psoCode\)',
r'''psoItems.add(ProgrammeSurveyResultDto.PsoIndirectItem.builder()
                                .psoCode(psoCode)''', text)

text = re.sub(r'poItems\.add\(ProgrammeSurveyResultDto\.OutcomeIndirectItem\.builder\(\)\.poCode\(poCode\)\.indirectAttainment\(BigDecimal\.ZERO\)\.build\(\)\);',
r'''poItems.add(ProgrammeSurveyResultDto.PoIndirectItem.builder().poCode(poCode).indirectAttainment(BigDecimal.ZERO).build());''', text)

text = re.sub(r'psoItems\.add\(ProgrammeSurveyResultDto\.OutcomeIndirectItem\.builder\(\)\.psoCode\(psoCode\)\.indirectAttainment\(BigDecimal\.ZERO\)\.build\(\)\);',
r'''psoItems.add(ProgrammeSurveyResultDto.PsoIndirectItem.builder().psoCode(psoCode).indirectAttainment(BigDecimal.ZERO).build());''', text)

text = re.sub(r'for \(ProgrammeSurveyResultDto\.OutcomeIndirectItem it : exitSurvey\.getPoIndirectAttainment\(\)\)',
r'''for (ProgrammeSurveyResultDto.PoIndirectItem it : exitSurvey.getPoIndirectAttainment())''', text)

text = re.sub(r'for \(ProgrammeSurveyResultDto\.OutcomeIndirectItem it : exitSurvey\.getPsoIndirectAttainment\(\)\)',
r'''for (ProgrammeSurveyResultDto.PsoIndirectItem it : exitSurvey.getPsoIndirectAttainment())''', text)


with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentCalculationService.java', 'w') as f:
    f.write(text)

