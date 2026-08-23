import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/CourseAttainmentReportDto.java', 'r') as f:
    text = f.read()

# Remove the dangling fields
text = re.sub(r'public static class Table2PsoRow \{.*?\}.*?private BigDecimal directContribution; // avg \* overall / 3\n    \}', 
r'''public static class Table2PsoRow {
        private String psoCode;
        private BigDecimal averageMapping;
        private BigDecimal directContribution;
    }''', text, flags=re.DOTALL)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/CourseAttainmentReportDto.java', 'w') as f:
    f.write(text)

