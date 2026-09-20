package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Authoritative Excel Sheet Builder for Sheet #4: 'Examination'.
 * <p>
 * Strictly replicates sheet4.xml from the DYPIU NBA Course Attainment reference workbook
 * (Template-CO-PO-PSO-Attainment-TH-v2-Sample.xlsx).
 * <p>
 * Strict Invariant: HEADER LAST COLUMN == CONTENT LAST COLUMN (Col 4 + numCO).
 */
@Slf4j
public class ExaminationSheetBuilder {

    // Authoritative reference colors matching styles.xml
    public static final Color COLOR_BRIGHT_BLUE = new Color(0, 176, 240);       // #00B0F0 (bright electric blue)
    public static final Color COLOR_CYAN_TINT_LIGHT = new Color(180, 220, 235); // theme 8 tint 0.6 (#B4DCEB)
    public static final Color COLOR_CYAN_TINT_DEEP = new Color(142, 201, 218);  // theme 8 tint 0.4 (#8EC9DA)
    public static final Color COLOR_LABEL_TINT = new Color(225, 240, 245);      // soft cyan/grey tint
    public static final Color COLOR_OUT_OF_BG = new Color(217, 217, 217);       // #D9D9D9 (light gray)
    public static final Color COLOR_CREAM_TINT = new Color(245, 245, 240);      // theme 2 tint -0.05
    public static final Color COLOR_PEACH_TINT = new Color(250, 238, 230);      // theme 6 tint 0.8
    public static final Color COLOR_STUDENT_COUNT_BG = new Color(230, 245, 235); // soft greenish/cyan tint

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot) {
        return build(wb, sheetName, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot, byte[] leftLogo, byte[] rightLogo) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Examination");

        // 1. Resolve Dynamic COs in natural ascending order with acronyms
        CourseOutcomeOrderHelper.CourseOutcomeRegistry registry = CourseOutcomeOrderHelper.resolveRegistry(snapshot);
        List<CourseOutcomeOrderHelper.CourseOutcomeItem> coItems = registry.getItems();
        int numCO = coItems.size();
        if (numCO < 1) numCO = 6;

        // Content columns: Col A (0), Col B (1), Col C (2), Col D (3), Col E (4) + CO columns (5 .. 4 + numCO)
        int totalCols = 5 + numCO;
        int endCol = totalCols - 1;

        // 2. Set authoritative column widths (+10% increased)
        sheet.setColumnWidth(0, (int) (8.525 * 256));  // Col A: Sr No
        sheet.setColumnWidth(1, (int) (7.975 * 256));  // Col B: PRN No
        sheet.setColumnWidth(2, (int) (37.125 * 256)); // Col C: Name
        sheet.setColumnWidth(3, (int) (1.65 * 256));   // Col D: blank separator
        sheet.setColumnWidth(4, (int) (1.19 * 256));   // Col E: blank separator
        for (int c = 5; c <= endCol; c++) {
            sheet.setColumnWidth(c, (int) (8.34 * 256)); // Cols F..endCol: CO columns
        }

        // 3. Create Styles
        StyleBundle s = createStyles(wb);

        // 4. Render Standard Course Header (Rows 0-3 in 0-indexed, no bottom spacer row)
        // Returns 4, so next content row is Row 4 (Excel Row 5)
        int rowIdx = CourseExcelHeaderRenderer.renderHeader(
                wb, sheet,
                snapshot.getInstitutionName(),
                snapshot.getSchoolName(),
                "Course Outcome attainment Calculations through Direct Method",
                totalCols,
                leftLogo,
                rightLogo,
                true,
                false);

        // 5. Extract Data from Snapshot
        CourseAttainmentSnapshot.ExaminationSection examData = snapshot.getExaminationData();
        String courseName = (examData != null && examData.getCourseName() != null && !examData.getCourseName().isBlank())
                ? examData.getCourseName()
                : (snapshot.getCourseName() != null ? snapshot.getCourseName() : "");
        String className = (examData != null && examData.getClassName() != null && !examData.getClassName().isBlank())
                ? examData.getClassName()
                : (snapshot.getSemester() != null ? "Semester " + snapshot.getSemester() : "TY B.Tech");
        String academicYear = (examData != null && examData.getAcademicYear() != null && !examData.getAcademicYear().isBlank())
                ? examData.getAcademicYear()
                : (snapshot.getAcademicYear() != null ? snapshot.getAcademicYear() : "");

        BigDecimal thresholdPct = (examData != null && examData.getThresholdPercentage() != null)
                ? examData.getThresholdPercentage()
                : new BigDecimal("60.00");

        List<CourseAttainmentSnapshot.StudentMarksRow> students = (examData != null && examData.getStudents() != null)
                ? examData.getStudents()
                : Collections.emptyList();

        int studentCount = (examData != null && examData.getTotalStudents() != null && examData.getTotalStudents() > 0)
                ? examData.getTotalStudents()
                : students.size();

        // Max marks per CO (Out Of)
        Map<String, BigDecimal> coMaxMarks = (examData != null && examData.getCoMaxMarks() != null)
                ? new LinkedHashMap<>(examData.getCoMaxMarks())
                : new LinkedHashMap<>();

        // Fractions per CO (maxMarks * threshold / 100)
        Map<String, BigDecimal> coThresholdMarks = (examData != null && examData.getCoThresholdMarks() != null)
                ? new LinkedHashMap<>(examData.getCoThresholdMarks())
                : new LinkedHashMap<>();

        // Counts above threshold
        Map<String, Integer> studentsAboveThreshold = (examData != null && examData.getStudentsAboveThreshold() != null)
                ? new LinkedHashMap<>(examData.getStudentsAboveThreshold())
                : new LinkedHashMap<>();

        // Percentages above threshold
        Map<String, BigDecimal> percentageAboveThreshold = (examData != null && examData.getPercentageAboveThreshold() != null)
                ? new LinkedHashMap<>(examData.getPercentageAboveThreshold())
                : new LinkedHashMap<>();

        // Self-heal any missing values defensively
        for (CourseOutcomeOrderHelper.CourseOutcomeItem item : coItems) {
            String actual = item.getActualCode();
            String acronym = item.getAcronym();

            BigDecimal max = registry.lookupValue(coMaxMarks, item);
            if (max == null) max = new BigDecimal("15.00");
            coMaxMarks.put(actual, max);
            coMaxMarks.put(acronym, max);

            BigDecimal threshMark = registry.lookupValue(coThresholdMarks, item);
            if (threshMark == null) {
                threshMark = max.multiply(thresholdPct).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
            }
            coThresholdMarks.put(actual, threshMark);
            coThresholdMarks.put(acronym, threshMark);

            Integer count = registry.lookupValue(studentsAboveThreshold, item);
            if (count == null) {
                count = 0;
                for (CourseAttainmentSnapshot.StudentMarksRow st : students) {
                    BigDecimal mark = registry.lookupValue(st.getCoMarks(), item);
                    if (mark != null && mark.compareTo(threshMark) >= 0) {
                        count++;
                    }
                }
            }
            studentsAboveThreshold.put(actual, count);
            studentsAboveThreshold.put(acronym, count);

            BigDecimal pct = registry.lookupValue(percentageAboveThreshold, item);
            if (pct == null) {
                pct = (studentCount > 0)
                        ? BigDecimal.valueOf(count).multiply(new BigDecimal("100.00")).divide(BigDecimal.valueOf(studentCount), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            }
            percentageAboveThreshold.put(actual, pct);
            percentageAboveThreshold.put(acronym, pct);
        }

        // =========================================================================
        // ROW 4 (Excel Row 5): SUBJECT NAME (ht = 19.15 pt)
        // =========================================================================
        Row r5 = getOrCreateRow(sheet, rowIdx++);
        r5.setHeightInPoints(19.15f);

        // A5:C5 merged - "Subject Name"
        mergeAndStyle(sheet, r5.getRowNum(), r5.getRowNum(), 0, 2, s.thinCenterLabel);
        getOrCreateCell(r5, 0).setCellValue("Subject Name");

        // F5:endCol merged - Course Name value
        mergeAndStyle(sheet, r5.getRowNum(), r5.getRowNum(), 5, endCol, s.mediumPeachBox);
        getOrCreateCell(r5, 5).setCellValue(courseName);

        // =========================================================================
        // ROW 5 (Excel Row 6): SPACER (ht = 14.25 pt)
        // =========================================================================
        Row r6 = getOrCreateRow(sheet, rowIdx++);
        r6.setHeightInPoints(14.25f);

        // =========================================================================
        // ROW 6 (Excel Row 7): CLASS & ACADEMIC YEAR (ht = 14.25 pt)
        // =========================================================================
        Row r7 = getOrCreateRow(sheet, rowIdx++);
        r7.setHeightInPoints(14.25f);

        // A7:B7 merged - "Class"
        mergeAndStyle(sheet, r7.getRowNum(), r7.getRowNum(), 0, 1, s.thinCenterLabel);
        getOrCreateCell(r7, 0).setCellValue("Class");

        // C7 - Class value box
        Cell cellC7 = getOrCreateCell(r7, 2);
        cellC7.setCellStyle(s.mediumPeachBox);
        cellC7.setCellValue(className);

        // F7:H7 (or F7 to mid) - "Academic Year"
        int ayMid = Math.min(7, endCol - 1);
        if (ayMid < 5) ayMid = 5;
        mergeAndStyle(sheet, r7.getRowNum(), r7.getRowNum(), 5, ayMid, s.noBorderCenter);
        getOrCreateCell(r7, 5).setCellValue("Academic Year");

        // (ayMid + 1):endCol - Academic Year value box
        int ayValStart = Math.min(ayMid + 1, endCol);
        mergeAndStyle(sheet, r7.getRowNum(), r7.getRowNum(), ayValStart, endCol, s.mediumBorderBox);
        getOrCreateCell(r7, ayValStart).setCellValue(academicYear);

        // =========================================================================
        // ROW 7 (Excel Row 8): SPACER (ht = 14.25 pt)
        // =========================================================================
        Row r8 = getOrCreateRow(sheet, rowIdx++);
        r8.setHeightInPoints(14.25f);

        // =========================================================================
        // ROW 8 (Excel Row 9): TOTAL NUMBER OF STUDENTS (ht = 14.25 pt)
        // =========================================================================
        Row r9 = getOrCreateRow(sheet, rowIdx++);
        r9.setHeightInPoints(14.25f);

        // A9:C9 merged - "Total Number of Students"
        mergeAndStyle(sheet, r9.getRowNum(), r9.getRowNum(), 0, 2, s.thinCenterLabel);
        getOrCreateCell(r9, 0).setCellValue("Total Number of Students");

        // F9:endCol merged - student count value
        mergeAndStyle(sheet, r9.getRowNum(), r9.getRowNum(), 5, endCol, s.mediumStudentCountBox);
        getOrCreateCell(r9, 5).setCellValue(studentCount);

        // =========================================================================
        // ROW 9 (Excel Row 10): SPACER (ht = 14.25 pt)
        // =========================================================================
        Row r10 = getOrCreateRow(sheet, rowIdx++);
        r10.setHeightInPoints(14.25f);

        // =========================================================================
        // ROW 10 (Excel Row 11): THRESHOLD FOR ATTAINMENT LEVEL (ht = 17.5 pt)
        // =========================================================================
        Row r11 = getOrCreateRow(sheet, rowIdx++);
        r11.setHeightInPoints(17.5f);

        // A11:endCol-2 merged - "Threshhold for attainment level "
        int threshLabelEnd = Math.max(0, endCol - 2);
        mergeAndStyle(sheet, r11.getRowNum(), r11.getRowNum(), 0, threshLabelEnd, s.creamCenterBold);
        getOrCreateCell(r11, 0).setCellValue("Threshhold for attainment level ");

        if (endCol - 1 > threshLabelEnd) {
            for (int c = threshLabelEnd + 1; c < endCol; c++) {
                Cell cell = getOrCreateCell(r11, c);
                cell.setCellStyle(s.creamCenterBold);
            }
        }

        // Col endCol: Threshold percentage numeric value (bright cyan background)
        Cell threshCell = getOrCreateCell(r11, endCol);
        threshCell.setCellStyle(s.thresholdBrightBlueBox);
        threshCell.setCellValue(thresholdPct.stripTrailingZeros().toPlainString());

        // =========================================================================
        // ROW 11 (Excel Row 12): SPACER (ht = 14.25 pt)
        // =========================================================================
        Row r12 = getOrCreateRow(sheet, rowIdx++);
        r12.setHeightInPoints(14.25f);

        // =========================================================================
        // ROW 12 (Excel Row 13): REFERENCE : NUMBER OF STUDENTS ABOVE THRESHOLD
        // =========================================================================
        Row r13 = getOrCreateRow(sheet, rowIdx++);
        r13.setHeightInPoints(14.25f);
        mergeAndStyle(sheet, r13.getRowNum(), r13.getRowNum(), 2, endCol, s.leftRefLabel);
        getOrCreateCell(r13, 2).setCellValue("Reference : Number of students above threshold");

        // =========================================================================
        // ROW 13 (Excel Row 14): # OF STUDENT >= OF OUT OF MARKS (ht = 14.25 pt)
        // =========================================================================
        Row r14 = getOrCreateRow(sheet, rowIdx++);
        r14.setHeightInPoints(14.25f);

        Cell c14 = getOrCreateCell(r14, 2);
        c14.setCellStyle(s.softLabelCenterThin);
        c14.setCellValue("# of student >= of out of marks");

        for (int i = 0; i < numCO; i++) {
            CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
            int col = 5 + i;
            Cell valCell = getOrCreateCell(r14, col);
            valCell.setCellStyle(s.cyanLightCountCell);
            Integer count = registry.lookupValue(studentsAboveThreshold, item);
            valCell.setCellValue(count != null ? count : 0);
        }

        // =========================================================================
        // ROW 14 (Excel Row 15): SPACER (ht = 14.25 pt)
        // =========================================================================
        Row r15 = getOrCreateRow(sheet, rowIdx++);
        r15.setHeightInPoints(14.25f);

        // =========================================================================
        // ROW 15 (Excel Row 16): REFERENCE : % OF NUMBER OF STUDENTS ABOVE THRESHOLD
        // =========================================================================
        Row r16 = getOrCreateRow(sheet, rowIdx++);
        r16.setHeightInPoints(14.25f);
        mergeAndStyle(sheet, r16.getRowNum(), r16.getRowNum(), 2, endCol, s.leftRefLabel);
        getOrCreateCell(r16, 2).setCellValue("Reference : % of Number of students above threshold");

        // =========================================================================
        // ROW 16 (Excel Row 17): % OF STUDENTS ABOVE THRESHHOLD (ht = 14.25 pt)
        // =========================================================================
        Row r17 = getOrCreateRow(sheet, rowIdx++);
        r17.setHeightInPoints(14.25f);

        Cell c17 = getOrCreateCell(r17, 2);
        c17.setCellStyle(s.softLabelCenterThin);
        c17.setCellValue("% of students above threshhold");

        for (int i = 0; i < numCO; i++) {
            CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
            int col = 5 + i;
            Cell valCell = getOrCreateCell(r17, col);
            valCell.setCellStyle(s.cyanDeepPercentCell);
            BigDecimal pct = registry.lookupValue(percentageAboveThreshold, item);
            valCell.setCellValue(pct != null ? pct.doubleValue() : 0.0);
        }

        // =========================================================================
        // ROW 17 (Excel Row 18): SPACER (ht = 14.25 pt)
        // =========================================================================
        Row r18 = getOrCreateRow(sheet, rowIdx++);
        r18.setHeightInPoints(14.25f);

        // =========================================================================
        // ROW 18 (Excel Row 19): OUT OF (ht = 14.25 pt)
        // =========================================================================
        Row r19 = getOrCreateRow(sheet, rowIdx++);
        r19.setHeightInPoints(14.25f);

        // A19:C19 merged - "Out of"
        mergeAndStyle(sheet, r19.getRowNum(), r19.getRowNum(), 0, 2, s.outOfLabel);
        getOrCreateCell(r19, 0).setCellValue("Out of");

        for (int i = 0; i < numCO; i++) {
            CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
            int col = 5 + i;
            Cell valCell = getOrCreateCell(r19, col);
            valCell.setCellStyle(s.outOfValueCell);
            BigDecimal max = registry.lookupValue(coMaxMarks, item);
            if (max == null) max = new BigDecimal("15");
            valCell.setCellValue(max.stripTrailingZeros().toPlainString());
        }

        // =========================================================================
        // ROW 19 (Excel Row 20): STUDENT TABLE HEADER 1 (ht = 15.0 pt)
        // =========================================================================
        Row r20 = getOrCreateRow(sheet, rowIdx++);
        r20.setHeightInPoints(15.0f);

        Cell srNoHeader = getOrCreateCell(r20, 0);
        srNoHeader.setCellStyle(s.boldCenterThin);
        srNoHeader.setCellValue("Sr No");

        Cell prnHeader = getOrCreateCell(r20, 1);
        prnHeader.setCellStyle(s.boldCenterThin);
        prnHeader.setCellValue("PRN No");

        Cell nameHeader = getOrCreateCell(r20, 2);
        nameHeader.setCellStyle(s.boldCenterThin);
        nameHeader.setCellValue("Name");

        // F20:endCol merged - "CO Assessment "
        mergeAndStyle(sheet, r20.getRowNum(), r20.getRowNum(), 5, endCol, s.boldCenterThin);
        getOrCreateCell(r20, 5).setCellValue("CO Assessment ");

        // =========================================================================
        // ROW 20 (Excel Row 21): FRACTION OF OUT OF MARKS (ht = 15.0 pt)
        // =========================================================================
        Row r21 = getOrCreateRow(sheet, rowIdx++);
        r21.setHeightInPoints(15.0f);

        // A21:C21 merged - "Fraction of Out of marks with respect to Threshold "
        mergeAndStyle(sheet, r21.getRowNum(), r21.getRowNum(), 0, 2, s.fractionLabel);
        getOrCreateCell(r21, 0).setCellValue("Fraction of Out of marks with respect to Threshold ");

        for (int i = 0; i < numCO; i++) {
            CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
            int col = 5 + i;
            Cell valCell = getOrCreateCell(r21, col);
            valCell.setCellStyle(s.boldCenterThin);
            BigDecimal frac = registry.lookupValue(coThresholdMarks, item);
            if (frac == null) frac = BigDecimal.ZERO;
            valCell.setCellValue(frac.stripTrailingZeros().toPlainString());
        }

        // =========================================================================
        // ROW 21 (Excel Row 22): CO HEADERS (ht = 23.25 pt)
        // =========================================================================
        Row r22 = getOrCreateRow(sheet, rowIdx++);
        r22.setHeightInPoints(23.25f);

        for (int i = 0; i < numCO; i++) {
            CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
            int col = 5 + i;
            Cell coCell = getOrCreateCell(r22, col);
            coCell.setCellStyle(s.coHeaderBrightBlue);
            coCell.setCellValue(item.getAcronym());
        }

        // =========================================================================
        // ROWS 22+ (Excel Row 23+): STUDENT MARKS DATA ROWS (ht = 14.25 pt)
        // =========================================================================
        int studentIdx = 1;
        for (CourseAttainmentSnapshot.StudentMarksRow st : students) {
            Row rStudent = getOrCreateRow(sheet, rowIdx++);
            rStudent.setHeightInPoints(14.25f);

            // Col 0: Sr No
            Cell cSr = getOrCreateCell(rStudent, 0);
            cSr.setCellStyle(s.regularCenterThin);
            cSr.setCellValue(st.getSrNo() != null ? st.getSrNo() : studentIdx);

            // Col 1: PRN No
            Cell cPrn = getOrCreateCell(rStudent, 1);
            cPrn.setCellStyle(s.studentPrnThin);
            cPrn.setCellValue(st.getPrn() != null ? st.getPrn() : "");

            // Col 2: Name
            Cell cName = getOrCreateCell(rStudent, 2);
            cName.setCellStyle(s.studentNameThin);
            cName.setCellValue(st.getStudentName() != null ? st.getStudentName() : "");

            // Cols 5..endCol: CO Marks
            for (int i = 0; i < numCO; i++) {
                CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
                int col = 5 + i;
                Cell cMark = getOrCreateCell(rStudent, col);
                cMark.setCellStyle(s.regularCenterThin);

                BigDecimal mark = registry.lookupValue(st.getCoMarks(), item);
                if (mark != null) {
                    cMark.setCellValue(mark.stripTrailingZeros().toPlainString());
                } else {
                    cMark.setCellValue("");
                }
            }
            studentIdx++;
        }

        return sheet;
    }

    private static List<String> resolveCoCodes(CourseAttainmentSnapshot snapshot) {
        return CourseOutcomeOrderHelper.resolveRegistry(snapshot).getAcronyms();
    }

    private static void mergeAndStyle(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol, CellStyle style) {
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = getOrCreateRow(sheet, r);
            for (int c = firstCol; c <= lastCol; c++) {
                Cell cell = getOrCreateCell(row, c);
                cell.setCellStyle(style);
            }
        }
        if (firstRow != lastRow || firstCol != lastCol) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
        }
    }

    private static Row getOrCreateRow(Sheet sheet, int rowIdx) {
        Row r = sheet.getRow(rowIdx);
        return r != null ? r : sheet.createRow(rowIdx);
    }

    private static Cell getOrCreateCell(Row row, int colIdx) {
        Cell c = row.getCell(colIdx);
        return c != null ? c : row.createCell(colIdx);
    }

    // =========================================================================
    // STYLE BUNDLE
    // =========================================================================

    private static class StyleBundle {
        CellStyle thinCenterLabel;
        CellStyle mediumPeachBox;
        CellStyle noBorderCenter;
        CellStyle mediumBorderBox;
        CellStyle mediumStudentCountBox;
        CellStyle creamCenterBold;
        CellStyle thresholdBrightBlueBox;
        CellStyle leftRefLabel;
        CellStyle softLabelCenterThin;
        CellStyle cyanLightCountCell;
        CellStyle cyanDeepPercentCell;
        CellStyle outOfLabel;
        CellStyle outOfValueCell;
        CellStyle boldCenterThin;
        CellStyle fractionLabel;
        CellStyle coHeaderBrightBlue;
        CellStyle regularCenterThin;
        CellStyle studentPrnThin;
        CellStyle studentNameThin;
    }

    private static StyleBundle createStyles(Workbook wb) {
        StyleBundle s = new StyleBundle();

        DataFormat df = wb.createDataFormat();
        short pctFormat = df.getFormat("0.00");

        s.thinCenterLabel = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, true, false);
        s.mediumPeachBox = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_PEACH_TINT, false, true);
        s.noBorderCenter = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, false, false);
        s.mediumBorderBox = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, false, true);
        s.mediumStudentCountBox = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_STUDENT_COUNT_BG, false, true);
        s.creamCenterBold = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_CREAM_TINT, false, false);
        s.thresholdBrightBlueBox = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, false, true);

        s.leftRefLabel = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, null, false, false);
        s.softLabelCenterThin = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_LABEL_TINT, true, false);
        s.cyanLightCountCell = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_CYAN_TINT_LIGHT, true, false);

        s.cyanDeepPercentCell = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_CYAN_TINT_DEEP, true, false);
        s.cyanDeepPercentCell.setDataFormat(pctFormat);

        s.outOfLabel = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, false, COLOR_PEACH_TINT, true, false);
        s.outOfValueCell = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_OUT_OF_BG, true, false);

        s.boldCenterThin = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, true, false);
        s.fractionLabel = createStyle(wb, "Verdana", (short) 10, false, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, false, COLOR_PEACH_TINT, true, false);
        s.coHeaderBrightBlue = createStyle(wb, "Verdana", (short) 10, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, true, false);

        s.regularCenterThin = createStyle(wb, "Arial", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, true, false);
        s.studentPrnThin = createStyle(wb, "Cambria", (short) 10, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, true, false);
        s.studentNameThin = createStyle(wb, "Times New Roman", (short) 9, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, null, true, false);

        return s;
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
            boolean thinBorder,
            boolean mediumBorder) {

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

        if (thinBorder) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.BLACK.getIndex());
            style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        } else if (mediumBorder) {
            style.setBorderTop(BorderStyle.MEDIUM);
            style.setBorderBottom(BorderStyle.MEDIUM);
            style.setBorderLeft(BorderStyle.MEDIUM);
            style.setBorderRight(BorderStyle.MEDIUM);
            style.setTopBorderColor(IndexedColors.BLACK.getIndex());
            style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        }

        return style;
    }
}
