import re

files = [
    '/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/test/java/com/dypiu/nba/service/Phase102CoPoMappingArchitectureIntegrationTest.java',
    '/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/test/java/com/dypiu/nba/service/Phase10AttainmentReportPersistenceIntegrationTest.java'
]

for file in files:
    with open(file, 'r') as f:
        text = f.read()

    # Course Table 2
    text = re.sub(r'courseReport\.getTable2Direct\(\)', 'courseReport.getTable2DirectPO()', text)
    text = re.sub(r'report\.getTable2Direct\(\)', 'report.getTable2DirectPO()', text)

    # Programme Report 1
    text = re.sub(r'progReport\.getReport1AverageMapping\(\)', 'progReport.getReport1AverageMappingPO()', text)
    text = re.sub(r'report\.getReport1AverageMapping\(\)', 'report.getReport1AverageMappingPO()', text)

    # Programme Report 2
    text = re.sub(r'progReport\.getReport2DirectAttainment\(\)', 'progReport.getReport2DirectAttainmentPO()', text)
    text = re.sub(r'report\.getReport2DirectAttainment\(\)', 'report.getReport2DirectAttainmentPO()', text)

    # Programme Report 3
    text = re.sub(r'progReport\.getReport3IndirectAttainment\(\)', 'progReport.getReport3IndirectAttainmentPO()', text)
    text = re.sub(r'report\.getReport3IndirectAttainment\(\)', 'report.getReport3IndirectAttainmentPO()', text)

    # Programme Report 4
    text = re.sub(r'progReport\.getReport4OverallAttainment\(\)', 'progReport.getReport4OverallAttainmentPO()', text)
    text = re.sub(r'report\.getReport4OverallAttainment\(\)', 'report.getReport4OverallAttainmentPO()', text)

    with open(file, 'w') as f:
        f.write(text)

