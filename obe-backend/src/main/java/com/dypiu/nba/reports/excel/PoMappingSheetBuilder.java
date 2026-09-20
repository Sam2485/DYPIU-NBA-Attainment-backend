package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.awt.Color;
import java.util.*;

/**
 * Authoritative Excel Builder for Course Attainment Sheet #2: "PO mapping".
 * <p>
 * Strictly replicates the visual structure, layout, styles, row heights, column widths,
 * borders, fonts, fills, and spacing of Template-CO-PO-PSO-Attainment-TH-v2-Sample.xlsx
 * (Sheet "PO mapping", sheet2.xml).
 * <p>
 * Columns:
 * - Col 0 (Col A, width 29.25): Programme Outcomes (merged vertically across competencies)
 * - Col 1 (Col B, width 47.0): Competency (one row per competency)
 * - Col 2 (Col C, width 1.58): Separator column
 * - Cols 3 to 2 + numCO (Cols D.., width 8.58): Keywords mapping to Competency from respective CO
 * - Col 3 + numCO (width 1.5): Separator column
 * - Cols 4 + numCO to 3 + 2 * numCO (width 7.08): Y or N mapping
 * <p>
 * Invariant: HEADER LAST COLUMN == CONTENT LAST COLUMN (totalCols = 4 + 2 * numCO).
 */
public class PoMappingSheetBuilder {

    // Authoritative reference colors
    public static final Color COLOR_BANNER_CYAN = new Color(218, 238, 243);    // #DAEEF3 (accent5, tint 0.8)
    public static final Color COLOR_ACCENT_CYAN = new Color(146, 205, 220);    // #92CDDC (accent5, tint 0.4)
    public static final Color COLOR_BRIGHT_BLUE = new Color(0, 176, 240);      // #00B0F0 (bright electric blue)
    public static final Color COLOR_HEADER_BG = new Color(182, 221, 232);      // #B6DDE8 (table column headers)
    public static final Color COLOR_KEYWORD_GRAY = new Color(216, 216, 216);   // #D8D8D8 (mapped keyword cell fill)
    public static final Color COLOR_SUM_ROW1 = new Color(194, 214, 155);       // #C2D69B (No of competencies)
    public static final Color COLOR_SUM_ROW2 = new Color(146, 208, 80);        // #92D050 (% of competencies)
    public static final Color COLOR_SUM_ROW3 = new Color(118, 146, 60);        // #76923C (Mapping strength)

    // Authoritative uniform light-grey fill for all PO cells and all Competency cells (#F2F2F2)
    public static final Color COLOR_LIGHT_GREY = new Color(242, 242, 242);

    private static final String[] DEFAULT_PO_STATEMENTS = new String[] {
            "Apply the knowledge of mathematics, science, engineering fundamentals and engineering specialization to the solution of complex engineering problems",
            "Identify, formulate, review research literature, and analyze complex computer engineering problems reaching substantiated conclusions using first principals of mathematic, natural sciences, and engineering sciences",
            "Design solutions for complex computer engineering problems and design system components or process that meet the specialized need with appropriate consideration for the public health and safety and the cultural, societal, and environmental considerations",
            "Use research based knowledge and research methods including design of experiments, analysis and interpretation of data and synthesis of the information to provide valid conclusions.",
            "Create, select and apply appropriate techniques, resources, and modern engineering and IT tools including prediction and modeling to complex computer engineering activities with an understanding of the limitations.",
            "Apply reasoning informed by the contextual knowledge to assess societal, health, safety, legal and cultural issues and the consequent responsibilities relevant to the professional engineering practice.",
            "Understand the impact of the professional engineering solutions in societal and environmental contexts, and demonstrate the knowledge of, and need for sustainable development.",
            "Apply ethical principles and commit to professional ethics and responsibilities and norms of the engineering practice.",
            "Function effectively as an individual, and as a member or leader in diverse teams, and in multidisciplinary settings.",
            "Communicate effectively on complex computer engineering activities with the engineering community and with society at large, such as, being able to comprehend and write effective reports and design documentations, make effective presentations and give and receive clear instructions.",
            "Demonstrate knowledge and understanding of the computer engineering and management principals and apply these to one’s own work, as a member and leader in a team, to manage projects and in multidisciplinary environments.",
            "Recognize the need for and have the preparation and ability to engage in independent and life-long learning in the broadcast context of technological change."
    };

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot) {
        return build(wb, sheetName, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot, byte[] leftLogo, byte[] rightLogo) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "PO mapping");

        // 1. Resolve dynamic CO list in ascending order with acronyms
        CourseOutcomeOrderHelper.CourseOutcomeRegistry registry = CourseOutcomeOrderHelper.resolveRegistry(snapshot);
        List<CourseOutcomeOrderHelper.CourseOutcomeItem> coItems = registry.getItems();
        int numCO = coItems.size();

        // 2. Compute dynamic columns width (HEADER LAST COLUMN == CONTENT LAST COLUMN)
        int totalCols = 4 + 2 * numCO;
        if (totalCols < 4) totalCols = 4;
        int endCol = totalCols - 1;

        // 3. Set authoritative column widths (+10% increased)
        sheet.setColumnWidth(0, (int) (32.175 * 256));
        sheet.setColumnWidth(1, (int) (51.7 * 256));
        sheet.setColumnWidth(2, (int) (1.738 * 256)); // Thin blank separator
        for (int i = 0; i < numCO; i++) {
            sheet.setColumnWidth(3 + i, (int) (9.438 * 256)); // Keywords CO columns
        }
        int sepCol2 = 3 + numCO;
        sheet.setColumnWidth(sepCol2, (int) (1.65 * 256)); // Thin blank separator
        for (int i = 0; i < numCO; i++) {
            sheet.setColumnWidth(4 + numCO + i, (int) (7.788 * 256)); // Y or N CO columns
        }

        // 4. Create CellStyles bundle
        StyleBundle s = createStyles(wb);

        // 5. Render Header (Rows 0-4)
        int rowIdx = CourseExcelHeaderRenderer.renderHeader(
                wb, sheet,
                snapshot.getInstitutionName(),
                snapshot.getSchoolName(),
                "Course Outcome - Programme Outcome (CO - PO) Mapping",
                totalCols,
                leftLogo,
                rightLogo,
                true);

        // =========================================================================
        // BANNER ROW (Row 5, ht = 24.65 pt)
        // =========================================================================
        Row rBanner = getOrCreateRow(sheet, rowIdx);
        rBanner.setHeightInPoints(24.65f);

        // Justification 1 (Cols 0 to 1 merged)
        for (int c = 0; c <= 1; c++) {
            Cell cell = getOrCreateCell(rBanner, c);
            cell.setCellStyle(s.bannerJustification);
        }
        getOrCreateCell(rBanner, 0).setCellValue("Justification 1");
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
        applyMergedBorders(sheet, new CellRangeAddress(rowIdx, rowIdx, 0, 1));

        // Separator Col 2
        getOrCreateCell(rBanner, 2);

        // Keywords mapping banner (Cols 3 to 2 + numCO merged)
        int kwStartCol = 3;
        int kwEndCol = 2 + numCO;
        for (int c = kwStartCol; c <= kwEndCol; c++) {
            Cell cell = getOrCreateCell(rBanner, c);
            cell.setCellStyle(s.bannerKeywords);
        }
        getOrCreateCell(rBanner, kwStartCol).setCellValue("Keywords mapping to Competency from resepctive CO");
        if (kwEndCol > kwStartCol) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, kwStartCol, kwEndCol));
            applyMergedBorders(sheet, new CellRangeAddress(rowIdx, rowIdx, kwStartCol, kwEndCol));
        }

        // Separator Col 3 + numCO
        getOrCreateCell(rBanner, sepCol2);

        // Y or N banner (Cols 4 + numCO to 3 + 2 * numCO merged)
        int ynStartCol = 4 + numCO;
        int ynEndCol = endCol;
        for (int c = ynStartCol; c <= ynEndCol; c++) {
            Cell cell = getOrCreateCell(rBanner, c);
            cell.setCellStyle(s.bannerYn);
        }
        getOrCreateCell(rBanner, ynStartCol).setCellValue("Y or N");
        if (ynEndCol > ynStartCol) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, ynStartCol, ynEndCol));
            applyMergedBorders(sheet, new CellRangeAddress(rowIdx, rowIdx, ynStartCol, ynEndCol));
        }
        rowIdx++;

        // =========================================================================
        // TABLE COLUMN HEADERS ROW (Row 6, ht = 15.0 pt)
        // =========================================================================
        Row rHeaders = getOrCreateRow(sheet, rowIdx);
        rHeaders.setHeightInPoints(15.0f);

        Cell cProg = getOrCreateCell(rHeaders, 0);
        cProg.setCellValue("Programme Outcomes");
        cProg.setCellStyle(s.tableHeaderLeft);

        Cell cComp = getOrCreateCell(rHeaders, 1);
        cComp.setCellValue("Competency");
        cComp.setCellStyle(s.tableHeaderLeft);

        getOrCreateCell(rHeaders, 2); // separator

        for (int i = 0; i < numCO; i++) {
            Cell cKw = getOrCreateCell(rHeaders, 3 + i);
            cKw.setCellValue(coItems.get(i).getAcronym());
            cKw.setCellStyle(s.tableHeaderCenter);
        }

        getOrCreateCell(rHeaders, sepCol2); // separator

        for (int i = 0; i < numCO; i++) {
            Cell cYn = getOrCreateCell(rHeaders, 4 + numCO + i);
            cYn.setCellValue(coItems.get(i).getAcronym());
            cYn.setCellStyle(s.tableHeaderCenter);
        }
        rowIdx++;

        // =========================================================================
        // PO BLOCKS (Competency rows followed by 3 Summary rows per PO)
        // =========================================================================
        List<PoModel> poModels = resolvePoModels(snapshot);

        for (int poIdx = 0; poIdx < poModels.size(); poIdx++) {
            PoModel po = poModels.get(poIdx);
            CellStyle poStatementStyle = s.poStatement;
            CellStyle compStatementStyle = s.compStatement;

            List<CompModel> comps = po.competencies;
            int numComps = Math.max(1, comps.size());
            int startPoRow = rowIdx;

            // Render Competency rows
            for (int k = 0; k < numComps; k++) {
                Row r = getOrCreateRow(sheet, rowIdx);
                r.setHeightInPoints(30.0f);

                // Col 0: PO Statement (set text on first row, merged later)
                Cell c0 = getOrCreateCell(r, 0);
                c0.setCellStyle(poStatementStyle);
                if (k == 0) {
                    c0.setCellValue(formatPoStatement(poIdx + 1, po.code, po.statement));
                }

                // Col 1: Competency Statement
                Cell c1 = getOrCreateCell(r, 1);
                c1.setCellStyle(compStatementStyle);
                CompModel comp = (k < comps.size()) ? comps.get(k) : null;
                c1.setCellValue(comp != null && comp.statement != null ? comp.statement : "");

                // Col 2: Separator
                getOrCreateCell(r, 2);

                // Cols 3 to 2 + numCO: Keywords from respective CO
                for (int j = 0; j < numCO; j++) {
                    CourseOutcomeOrderHelper.CourseOutcomeItem coItem = coItems.get(j);
                    String kw = (comp != null && comp.coKeywords != null) ? registry.lookupValue(comp.coKeywords, coItem) : null;
                    Cell cellKw = getOrCreateCell(r, 3 + j);
                    if (kw != null && !kw.isBlank()) {
                        cellKw.setCellValue(kw.trim());
                        cellKw.setCellStyle(s.keywordMapped);
                    } else {
                        cellKw.setCellValue("");
                        cellKw.setCellStyle(s.keywordEmpty);
                    }
                }

                // Separator col
                getOrCreateCell(r, sepCol2);

                // Cols 4 + numCO to 3 + 2 * numCO: Y or N
                for (int j = 0; j < numCO; j++) {
                    CourseOutcomeOrderHelper.CourseOutcomeItem coItem = coItems.get(j);
                    String yn = (comp != null && comp.coMappings != null) ? registry.lookupValue(comp.coMappings, coItem) : null;
                    if (yn == null && comp != null && comp.coKeywords != null) {
                        String kw = registry.lookupValue(comp.coKeywords, coItem);
                        if (kw != null && !kw.isBlank()) {
                            yn = "Y";
                        }
                    }
                    Cell cellYn = getOrCreateCell(r, 4 + numCO + j);
                    cellYn.setCellStyle(s.ynValue);
                    if ("Y".equalsIgnoreCase(yn)) {
                        cellYn.setCellValue("Y");
                    } else {
                        cellYn.setCellValue("");
                    }
                }

                rowIdx++;
            }

            // Vertical merge of Col A across competency rows of this PO
            if (numComps > 1) {
                CellRangeAddress poMerge = new CellRangeAddress(startPoRow, startPoRow + numComps - 1, 0, 0);
                sheet.addMergedRegion(poMerge);
                applyMergedBorders(sheet, poMerge);
            }

            // ---------------------------------------------------------------------
            // SUMMARY ROW 1: No of competencies from given PO mapped by COs (ht = 30 pt)
            // ---------------------------------------------------------------------
            Row rSum1 = getOrCreateRow(sheet, rowIdx);
            rSum1.setHeightInPoints(30.0f);

            getOrCreateCell(rSum1, 0).setCellStyle(s.blankBordered);
            getOrCreateCell(rSum1, 1).setCellStyle(s.blankBordered);
            getOrCreateCell(rSum1, 2); // separator

            for (int c = kwStartCol; c <= kwEndCol; c++) {
                getOrCreateCell(rSum1, c).setCellStyle(s.summary1Text);
            }
            getOrCreateCell(rSum1, kwStartCol).setCellValue("No of competencies from given " + po.code + " mapped by COs");
            if (kwEndCol > kwStartCol) {
                CellRangeAddress s1Merge = new CellRangeAddress(rowIdx, rowIdx, kwStartCol, kwEndCol);
                sheet.addMergedRegion(s1Merge);
                applyMergedBorders(sheet, s1Merge);
            }

            getOrCreateCell(rSum1, sepCol2); // separator

            int[] mappedCounts = new int[numCO];
            for (int j = 0; j < numCO; j++) {
                CourseOutcomeOrderHelper.CourseOutcomeItem coItem = coItems.get(j);
                int count = 0;
                for (CompModel c : comps) {
                    String yn = (c.coMappings != null) ? registry.lookupValue(c.coMappings, coItem) : null;
                    if ("Y".equalsIgnoreCase(yn) || (c.coKeywords != null && registry.lookupValue(c.coKeywords, coItem) != null && !registry.lookupValue(c.coKeywords, coItem).isBlank())) {
                        count++;
                    }
                }
                // Defensive fallback: if 0 competencies were defined, check table1Mapping strength
                if (count == 0 && comps.isEmpty()) {
                    Integer strength = findStrengthInTable1(snapshot, coItem, po.code);
                    if (strength != null && strength > 0) count = 1;
                }
                mappedCounts[j] = count;
                Cell cVal = getOrCreateCell(rSum1, 4 + numCO + j);
                cVal.setCellValue(String.valueOf(count));
                cVal.setCellStyle(s.summary1Value);
            }
            rowIdx++;

            // ---------------------------------------------------------------------
            // SUMMARY ROW 2: % of competencies from given PO mapped by COs (ht = 30 pt)
            // ---------------------------------------------------------------------
            Row rSum2 = getOrCreateRow(sheet, rowIdx);
            rSum2.setHeightInPoints(30.0f);

            getOrCreateCell(rSum2, 0).setCellStyle(s.blankBordered);
            getOrCreateCell(rSum2, 1).setCellStyle(s.blankBordered);
            getOrCreateCell(rSum2, 2); // separator

            for (int c = kwStartCol; c <= kwEndCol; c++) {
                getOrCreateCell(rSum2, c).setCellStyle(s.summary2Text);
            }
            getOrCreateCell(rSum2, kwStartCol).setCellValue("% of competencies from given " + po.code + " mapped by COs");
            if (kwEndCol > kwStartCol) {
                CellRangeAddress s2Merge = new CellRangeAddress(rowIdx, rowIdx, kwStartCol, kwEndCol);
                sheet.addMergedRegion(s2Merge);
                applyMergedBorders(sheet, s2Merge);
            }

            getOrCreateCell(rSum2, sepCol2); // separator

            int[] pctValues = new int[numCO];
            for (int j = 0; j < numCO; j++) {
                int count = mappedCounts[j];
                int pct = (numComps > 0) ? (count * 100) / numComps : 0;
                pctValues[j] = pct;
                Cell cVal = getOrCreateCell(rSum2, 4 + numCO + j);
                cVal.setCellValue(String.valueOf(pct));
                cVal.setCellStyle(s.summary2Value);
            }
            rowIdx++;

            // ---------------------------------------------------------------------
            // SUMMARY ROW 3: Mapping strength of PO of CO (ht = 30 pt)
            // ---------------------------------------------------------------------
            Row rSum3 = getOrCreateRow(sheet, rowIdx);
            rSum3.setHeightInPoints(30.0f);

            getOrCreateCell(rSum3, 0).setCellStyle(s.blankBordered);
            getOrCreateCell(rSum3, 1).setCellStyle(s.blankBordered);
            getOrCreateCell(rSum3, 2); // separator

            for (int c = kwStartCol; c <= kwEndCol; c++) {
                getOrCreateCell(rSum3, c).setCellStyle(s.summary3Text);
            }
            getOrCreateCell(rSum3, kwStartCol).setCellValue("Mapping strength of " + po.code + " of CO");
            if (kwEndCol > kwStartCol) {
                CellRangeAddress s3Merge = new CellRangeAddress(rowIdx, rowIdx, kwStartCol, kwEndCol);
                sheet.addMergedRegion(s3Merge);
                applyMergedBorders(sheet, s3Merge);
            }

            getOrCreateCell(rSum3, sepCol2); // separator

            for (int j = 0; j < numCO; j++) {
                CourseOutcomeOrderHelper.CourseOutcomeItem coItem = coItems.get(j);
                Integer strength = findStrengthInTable1(snapshot, coItem, po.code);
                if (strength == null || strength <= 0) {
                    int pct = pctValues[j];
                    if (pct >= 75) strength = 3;
                    else if (pct >= 50) strength = 2;
                    else if (pct > 0) strength = 1;
                    else strength = 0;
                }

                String displayStrength = (strength != null && strength > 0) ? String.valueOf(strength) : "-";
                Cell cVal = getOrCreateCell(rSum3, 4 + numCO + j);
                cVal.setCellValue(displayStrength);
                cVal.setCellStyle(s.summary3Value);
            }
            rowIdx++;
        }

        // Configure sheet display
        sheet.setDisplayGridlines(true);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);

        return sheet;
    }

    private static List<String> extractCoCodes(CourseAttainmentSnapshot snapshot) {
        return CourseOutcomeOrderHelper.resolveRegistry(snapshot).getAcronyms();
    }

    private static Integer findStrengthInTable1(CourseAttainmentSnapshot snapshot, CourseOutcomeOrderHelper.CourseOutcomeItem coItem, String poCode) {
        if (snapshot == null || snapshot.getTable1Mapping() == null || coItem == null) return null;
        for (CourseAttainmentSnapshot.CoMappingRow r : snapshot.getTable1Mapping()) {
            if (r.getCoCode() != null && (r.getCoCode().equalsIgnoreCase(coItem.getActualCode()) || r.getCoCode().equalsIgnoreCase(coItem.getAcronym()))) {
                if (r.getPoMappings() != null && r.getPoMappings().containsKey(poCode)) {
                    return r.getPoMappings().get(poCode);
                }
            }
        }
        return null;
    }

    private static Integer findStrengthInTable1(CourseAttainmentSnapshot snapshot, String coCode, String poCode) {
        if (snapshot == null || snapshot.getTable1Mapping() == null || coCode == null) return null;
        for (CourseAttainmentSnapshot.CoMappingRow r : snapshot.getTable1Mapping()) {
            if (coCode.equalsIgnoreCase(r.getCoCode())) {
                if (r.getPoMappings() != null && r.getPoMappings().containsKey(poCode)) {
                    return r.getPoMappings().get(poCode);
                }
            }
        }
        return null;
    }

    private static String formatPoStatement(int index, String poCode, String statement) {
        String numPrefix = index + ". ";
        if (statement == null || statement.isBlank()) {
            int defaultIdx = index - 1;
            if (defaultIdx >= 0 && defaultIdx < DEFAULT_PO_STATEMENTS.length) {
                return numPrefix + DEFAULT_PO_STATEMENTS[defaultIdx];
            }
            return numPrefix + poCode;
        }
        String trimmed = statement.trim();
        if (trimmed.startsWith(String.valueOf(index) + ".") || trimmed.startsWith(poCode)) {
            return trimmed;
        }
        return numPrefix + trimmed;
    }

    private static List<PoModel> resolvePoModels(CourseAttainmentSnapshot snapshot) {
        List<PoModel> list = new ArrayList<>();
        Map<String, CourseAttainmentSnapshot.PoDetailRow> detailMap = new LinkedHashMap<>();
        if (snapshot.getPoDetails() != null) {
            for (CourseAttainmentSnapshot.PoDetailRow d : snapshot.getPoDetails()) {
                if (d.getPoCode() != null) {
                    detailMap.put(d.getPoCode().toUpperCase().trim(), d);
                }
            }
        }

        List<String> poCodes = snapshot.getPoCodes();
        if (poCodes == null || poCodes.isEmpty()) {
            if (!detailMap.isEmpty()) {
                poCodes = new ArrayList<>(detailMap.keySet());
            } else {
                poCodes = new ArrayList<>();
                for (int i = 1; i <= 12; i++) poCodes.add("PO" + i);
            }
        }

        for (int i = 0; i < poCodes.size(); i++) {
            String code = poCodes.get(i).trim();
            CourseAttainmentSnapshot.PoDetailRow detail = detailMap.get(code.toUpperCase());

            String stmt = (detail != null && detail.getStatement() != null && !detail.getStatement().isBlank())
                    ? detail.getStatement()
                    : (i < DEFAULT_PO_STATEMENTS.length ? DEFAULT_PO_STATEMENTS[i] : code);

            List<CompModel> comps = new ArrayList<>();
            if (detail != null && detail.getCompetencies() != null && !detail.getCompetencies().isEmpty()) {
                for (CourseAttainmentSnapshot.CompetencyDetailRow cr : detail.getCompetencies()) {
                    comps.add(new CompModel(
                            cr.getCompetencyCode(),
                            cr.getStatement(),
                            cr.getCoKeywords() != null ? cr.getCoKeywords() : Collections.emptyMap(),
                            cr.getCoMappings() != null ? cr.getCoMappings() : Collections.emptyMap()
                    ));
                }
            } else {
                // Synthesize 1 competency row fallback
                comps.add(new CompModel(
                        code + ".1",
                        "Demonstrate competence in " + code,
                        Collections.emptyMap(),
                        Collections.emptyMap()
                ));
            }

            list.add(new PoModel(code, stmt, comps));
        }

        return list;
    }

    private static class PoModel {
        final String code;
        final String statement;
        final List<CompModel> competencies;

        PoModel(String code, String statement, List<CompModel> competencies) {
            this.code = code;
            this.statement = statement;
            this.competencies = competencies;
        }
    }

    private static class CompModel {
        final String code;
        final String statement;
        final Map<String, String> coKeywords;
        final Map<String, String> coMappings;

        CompModel(String code, String statement, Map<String, String> coKeywords, Map<String, String> coMappings) {
            this.code = code;
            this.statement = statement;
            this.coKeywords = coKeywords;
            this.coMappings = coMappings;
        }
    }

    private static void applyMergedBorders(Sheet sheet, CellRangeAddress region) {
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
    }

    private static Row getOrCreateRow(Sheet sheet, int rowIdx) {
        Row r = sheet.getRow(rowIdx);
        return r != null ? r : sheet.createRow(rowIdx);
    }

    private static Cell getOrCreateCell(Row row, int colIdx) {
        Cell c = row.getCell(colIdx);
        return c != null ? c : row.createCell(colIdx);
    }

    private static StyleBundle createStyles(Workbook wb) {
        StyleBundle b = new StyleBundle(wb);

        // Banner Styles
        b.bannerJustification = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_BANNER_CYAN, true);
        b.bannerKeywords = createStyle(wb, "Verdana", (short) 9, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, true);
        b.bannerYn = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_ACCENT_CYAN, true);

        // Table Header Styles
        b.tableHeaderLeft = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, true, COLOR_HEADER_BG, true);
        b.tableHeaderCenter = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_HEADER_BG, true);

        // Blank Bordered
        b.blankBordered = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, true);

        // Summary Row 1
        b.summary1Text = createStyle(wb, "Times New Roman", (short) 12, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SUM_ROW1, true);
        b.summary1Value = createStyle(wb, "Times New Roman", (short) 12, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SUM_ROW1, true);

        // Summary Row 2
        b.summary2Text = createStyle(wb, "Times New Roman", (short) 12, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SUM_ROW2, true);
        b.summary2Value = createStyle(wb, "Times New Roman", (short) 12, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SUM_ROW2, true);

        // Summary Row 3
        b.summary3Text = createStyle(wb, "Times New Roman", (short) 12, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SUM_ROW3, true);
        b.summary3Value = createStyle(wb, "Times New Roman", (short) 12, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SUM_ROW3, true);

        // Keyword Cells
        b.keywordMapped = createStyle(wb, "Verdana", (short) 8, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_KEYWORD_GRAY, true);
        b.keywordEmpty = createStyle(wb, "Verdana", (short) 8, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, null, true);

        // Y or N Values
        b.ynValue = createStyle(wb, "Times New Roman", (short) 12, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, null, true);

        // Uniform Light-Grey styles for PO statement and Competency statement
        b.poStatement = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, true, COLOR_LIGHT_GREY, true);
        b.compStatement = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, true, COLOR_LIGHT_GREY, true);

        return b;
    }

    private static class StyleBundle {
        final Workbook wb;
        CellStyle bannerJustification;
        CellStyle bannerKeywords;
        CellStyle bannerYn;
        CellStyle tableHeaderLeft;
        CellStyle tableHeaderCenter;
        CellStyle blankBordered;
        CellStyle summary1Text;
        CellStyle summary1Value;
        CellStyle summary2Text;
        CellStyle summary2Value;
        CellStyle summary3Text;
        CellStyle summary3Value;
        CellStyle keywordMapped;
        CellStyle keywordEmpty;
        CellStyle ynValue;
        CellStyle poStatement;
        CellStyle compStatement;

        StyleBundle(Workbook wb) {
            this.wb = wb;
        }
    }

    private static CellStyle createStyle(
            Workbook wb,
            String fontName,
            short fontSize,
            boolean bold,
            HorizontalAlignment hAlign,
            VerticalAlignment vAlign,
            boolean wrapText,
            Color fillColor,
            boolean border) {

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName(fontName);
        font.setFontHeightInPoints(fontSize);
        font.setBold(bold);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        style.setAlignment(hAlign);
        style.setVerticalAlignment(vAlign);
        style.setWrapText(wrapText);

        if (fillColor != null) {
            if (wb instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook) {
                ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(fillColor, null));
            } else {
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        if (border) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.BLACK.getIndex());
            style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        }

        return style;
    }
}
