package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.ReportSection;
import com.dypiu.nba.reports.model.snapshot.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class ExcelReportRenderer {

    public byte[] renderProgrammeAttainmentMaster(ProgrammeAttainmentSnapshot snapshot) {
        try (Workbook wb = new XSSFWorkbook()) {
            AverageMappingSheetBuilder.build(wb, "Average Mapping", snapshot);
            AverageDirectAttainmentSheetBuilder.build(wb, "Average Direct Attainment", snapshot);
            AverageIndirectAttainmentSheetBuilder.build(wb, "Average Indirect Attainment", snapshot);
            OverallAttainmentSheetBuilder.build(wb, "Overall Attainment", snapshot);
            return writeToBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render Programme Attainment Master Excel", e);
        }
    }

    public byte[] renderProgrammeAttainmentSection(ProgrammeAttainmentSnapshot snapshot, ReportSection section) {
        try (Workbook wb = new XSSFWorkbook()) {
            switch (section) {
                case AVERAGE_MAPPING -> AverageMappingSheetBuilder.build(wb, "Average Mapping", snapshot);
                case AVERAGE_DIRECT -> AverageDirectAttainmentSheetBuilder.build(wb, "Average Direct Attainment", snapshot);
                case AVERAGE_INDIRECT -> AverageIndirectAttainmentSheetBuilder.build(wb, "Average Indirect Attainment", snapshot);
                case OVERALL -> OverallAttainmentSheetBuilder.build(wb, "Overall Attainment", snapshot);
                case ALL -> {
                    AverageMappingSheetBuilder.build(wb, "Average Mapping", snapshot);
                    AverageDirectAttainmentSheetBuilder.build(wb, "Average Direct Attainment", snapshot);
                    AverageIndirectAttainmentSheetBuilder.build(wb, "Average Indirect Attainment", snapshot);
                    OverallAttainmentSheetBuilder.build(wb, "Overall Attainment", snapshot);
                }
            }
            return writeToBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render Programme Attainment Section Excel: " + section, e);
        }
    }

    public byte[] renderCourseAttainment(CourseAttainmentSnapshot snapshot, byte[] leftLogo, byte[] rightLogo) {
        try (Workbook wb = new XSSFWorkbook()) {
            CourseAttainmentSheetBuilder.build(wb, "Attainment-main", snapshot, leftLogo, rightLogo);
            PoMappingSheetBuilder.build(wb, "PO mapping", snapshot, leftLogo, rightLogo);
            PsoMappingSheetBuilder.build(wb, "PSO mapping", snapshot, leftLogo, rightLogo);
            ExaminationSheetBuilder.build(wb, "Examination", snapshot, leftLogo, rightLogo);
            CourseEndSurveySheetBuilder.build(wb, "Course End Survey", snapshot, leftLogo, rightLogo);
            return writeToBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render Course Attainment Excel", e);
        }
    }

    public byte[] renderCourseAttainment(CourseAttainmentSnapshot snapshot) {
        return renderCourseAttainment(snapshot, null, null);
    }

    public byte[] renderProgrammeAtr(ProgrammeAtrSnapshot snapshot) {
        try (Workbook wb = new XSSFWorkbook()) {
            ProgrammeAtrSheetBuilder.build(wb, "Programme ATR", snapshot);
            return writeToBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render Programme ATR Excel", e);
        }
    }

    public byte[] renderCourseAtr(CourseAtrSnapshot snapshot, byte[] leftLogo, byte[] rightLogo) {
        try (Workbook wb = new XSSFWorkbook()) {
            CourseAtrSheetBuilder.build(wb, "Course ATR", snapshot, leftLogo, rightLogo);
            return writeToBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render Course ATR Excel", e);
        }
    }

    public byte[] renderCourseAtr(CourseAtrSnapshot snapshot) {
        return renderCourseAtr(snapshot, null, null);
    }

    private byte[] writeToBytes(Workbook wb) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            wb.write(baos);
            return baos.toByteArray();
        }
    }
}
