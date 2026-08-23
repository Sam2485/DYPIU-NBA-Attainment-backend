import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

text = re.sub(
    r'\.report1AverageMapping\(report1 != null \? report1 : Collections\.emptyList\(\)\)\n                \.report2DirectAttainment\(report2 != null \? report2 : Collections\.emptyList\(\)\)\n                \.report3IndirectAttainment\(report3 != null \? report3 : Collections\.emptyList\(\)\)\n                \.report4OverallAttainment\(report4 != null \? report4 : Collections\.emptyList\(\)\)',
r'''.report1AverageMappingPO(report1PO != null ? report1PO : Collections.emptyList()).report1AverageMappingPSO(report1PSO != null ? report1PSO : Collections.emptyList())
                .report2DirectAttainmentPO(report2PO != null ? report2PO : Collections.emptyList()).report2DirectAttainmentPSO(report2PSO != null ? report2PSO : Collections.emptyList())
                .report3IndirectAttainmentPO(report3PO != null ? report3PO : Collections.emptyList()).report3IndirectAttainmentPSO(report3PSO != null ? report3PSO : Collections.emptyList())
                .report4OverallAttainmentPO(report4PO != null ? report4PO : Collections.emptyList()).report4OverallAttainmentPSO(report4PSO != null ? report4PSO : Collections.emptyList())''', text
)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)

