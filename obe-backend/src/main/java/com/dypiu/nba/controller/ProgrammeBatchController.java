package com.dypiu.nba.controller;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.ProgrammeAtr;
import com.dypiu.nba.entity.ProgrammeBatch;
import com.dypiu.nba.entity.ProgrammeBatchCourse;
import com.dypiu.nba.entity.Student;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.AtrService;
import com.dypiu.nba.service.AttainmentCalculationService;
import com.dypiu.nba.service.AttainmentReportService;
import com.dypiu.nba.service.OutcomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/programme-batches")
@RequiredArgsConstructor
public class ProgrammeBatchController {

    private final AcademicService academicService;
    private final AttainmentCalculationService calculationService;
    private final AttainmentReportService attainmentReportService;
    private final AtrService atrService;
    private final OutcomeService outcomeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProgrammeBatch>>> getAllProgrammeBatches(
            @RequestParam(required = false) String masterProgrammeId) {
        List<ProgrammeBatch> batches = (masterProgrammeId != null && !masterProgrammeId.isBlank())
                ? academicService.getBatchesByProgramme(masterProgrammeId)
                : academicService.getAllBatches();
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeBatch>>builder()
                .success(true)
                .data(batches)
                .build());
    }

    @GetMapping("/{programmeBatchId}")
    public ResponseEntity<ApiResponse<ProgrammeBatch>> getProgrammeBatchById(
            @PathVariable String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeBatch>builder()
                .success(true)
                .data(academicService.getBatchById(programmeBatchId))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProgrammeBatch>> createProgrammeBatch(
            @RequestBody ProgrammeBatch batch) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeBatch>builder()
                .success(true)
                .message("ProgrammeBatch created successfully")
                .data(academicService.saveBatch(batch))
                .build());
    }

    @PutMapping("/{programmeBatchId}")
    public ResponseEntity<ApiResponse<ProgrammeBatch>> updateProgrammeBatch(
            @PathVariable String programmeBatchId,
            @RequestBody ProgrammeBatch batch) {
        batch.setId(programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<ProgrammeBatch>builder()
                .success(true)
                .message("ProgrammeBatch updated successfully")
                .data(academicService.saveBatch(batch))
                .build());
    }

    @DeleteMapping("/{programmeBatchId}")
    public ResponseEntity<ApiResponse<Void>> deleteProgrammeBatch(
            @PathVariable String programmeBatchId) {
        academicService.deleteBatch(programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("ProgrammeBatch deleted successfully")
                .build());
    }

    @GetMapping("/{programmeBatchId}/programme-batch-courses")
    public ResponseEntity<ApiResponse<List<ProgrammeBatchCourse>>> getProgrammeBatchCourses(
            @PathVariable String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeBatchCourse>>builder()
                .success(true)
                .data(academicService.getProgrammeBatchCoursesByBatch(programmeBatchId))
                .build());
    }

    @GetMapping("/{programmeBatchId}/students")
    public ResponseEntity<ApiResponse<List<Student>>> getStudentsByBatch(
            @PathVariable String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<List<Student>>builder()
                .success(true)
                .data(academicService.getStudentsByBatch(programmeBatchId))
                .build());
    }

    @PostMapping("/{programmeBatchId}/students")
    public ResponseEntity<ApiResponse<Student>> addStudentToBatch(
            @PathVariable String programmeBatchId,
            @RequestBody Student student) {
        student.setBatchId(programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<Student>builder()
                .success(true)
                .message("Student added to ProgrammeBatch successfully")
                .data(academicService.saveStudent(student))
                .build());
    }

    @GetMapping("/{programmeBatchId}/targets")
    public ResponseEntity<ApiResponse<ProgrammeTargetDto>> getProgrammeTargets(
            @PathVariable String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeTargetDto>builder()
                .success(true)
                .data(outcomeService.getProgrammeTargets(programmeBatchId))
                .build());
    }

    @PostMapping("/{programmeBatchId}/targets")
    public ResponseEntity<ApiResponse<ProgrammeTargetDto>> saveProgrammeTargets(
            @PathVariable String programmeBatchId,
            @RequestBody ProgrammeTargetDto dto) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeTargetDto>builder()
                .success(true)
                .message("Programme targets saved successfully")
                .data(outcomeService.saveProgrammeTargets(programmeBatchId, dto))
                .build());
    }

    // --- Report 1: Average Mapping Report ---
    @GetMapping("/{programmeBatchId}/reports/average-mapping")
    public ResponseEntity<ApiResponse<Object>> getAverageMappingReport(
            @PathVariable String programmeBatchId) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(batch.getMasterProgrammeId(), programmeBatchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poMappings", report.getReport1AverageMappingPO() != null ? report.getReport1AverageMappingPO() : java.util.Collections.emptyList(),
                        "psoMappings", report.getReport1AverageMappingPSO() != null ? report.getReport1AverageMappingPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    // --- Report 2: Direct Attainment Report ---
    @GetMapping("/{programmeBatchId}/reports/direct-attainment")
    public ResponseEntity<ApiResponse<Object>> getDirectAttainmentReport(
            @PathVariable String programmeBatchId) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(batch.getMasterProgrammeId(), programmeBatchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poDirectAttainment", report.getReport2DirectAttainmentPO() != null ? report.getReport2DirectAttainmentPO() : java.util.Collections.emptyList(),
                        "psoDirectAttainment", report.getReport2DirectAttainmentPSO() != null ? report.getReport2DirectAttainmentPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    // --- Report 3: Indirect Attainment Report ---
    @GetMapping("/{programmeBatchId}/reports/indirect-attainment")
    public ResponseEntity<ApiResponse<Object>> getIndirectAttainmentReport(
            @PathVariable String programmeBatchId) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(batch.getMasterProgrammeId(), programmeBatchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poIndirectAttainment", report.getReport3IndirectAttainmentPO() != null ? report.getReport3IndirectAttainmentPO() : java.util.Collections.emptyList(),
                        "psoIndirectAttainment", report.getReport3IndirectAttainmentPSO() != null ? report.getReport3IndirectAttainmentPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    // --- Report 4: Overall Attainment Report ---
    @GetMapping("/{programmeBatchId}/reports/overall-attainment")
    public ResponseEntity<ApiResponse<Object>> getOverallAttainmentReport(
            @PathVariable String programmeBatchId) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(batch.getMasterProgrammeId(), programmeBatchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poOverallAttainment", report.getReport4OverallAttainmentPO() != null ? report.getReport4OverallAttainmentPO() : java.util.Collections.emptyList(),
                        "psoOverallAttainment", report.getReport4OverallAttainmentPSO() != null ? report.getReport4OverallAttainmentPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    // --- Programme Batch Main Attainment Report ---
    @GetMapping("/{programmeBatchId}/reports/attainment-main")
    public ResponseEntity<ApiResponse<ProgrammeBatchAttainmentReportDto>> getProgrammeBatchAttainmentMainReport(
            @PathVariable String programmeBatchId) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<ProgrammeBatchAttainmentReportDto>builder()
                .success(true)
                .data(attainmentReportService.getOrCreateProgrammeAttainmentReport(batch.getMasterProgrammeId(), programmeBatchId))
                .build());
    }

    @PostMapping("/{programmeBatchId}/reports/finalize")
    public ResponseEntity<ApiResponse<ProgrammeBatchAttainmentReportDto>> finalizeProgrammeBatchAttainmentReport(
            @PathVariable String programmeBatchId,
            Principal principal) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        String actorName = principal != null ? principal.getName() : "Programme Coordinator";
        return ResponseEntity.ok(ApiResponse.<ProgrammeBatchAttainmentReportDto>builder()
                .success(true)
                .message("Programme Batch Attainment Report finalized successfully")
                .data(attainmentReportService.finalizeProgrammeReport(batch.getMasterProgrammeId(), programmeBatchId, actorName))
                .build());
    }

    // --- Programme-Batch ATR ---
    @GetMapping("/{programmeBatchId}/atr")
    public ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getProgrammeBatchAtr(
            @PathVariable String programmeBatchId) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtrReportDto>builder()
                .success(true)
                .data(atrService.getProgrammeAtrReport(batch.getMasterProgrammeId(), programmeBatchId))
                .build());
    }

    @PostMapping("/{programmeBatchId}/atr")
    public ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> saveProgrammeBatchAtr(
            @PathVariable String programmeBatchId,
            @RequestBody ProgrammeAtrReportDto dto) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtrReportDto>builder()
                .success(true)
                .message("Programme Batch ATR saved successfully")
                .data(atrService.saveProgrammeAtrReport(dto))
                .build());
    }

    @PostMapping("/{programmeBatchId}/atr/submit")
    public ResponseEntity<ApiResponse<ProgrammeAtr>> submitProgrammeBatchAtr(
            @PathVariable String programmeBatchId,
            Principal principal) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        String submitter = principal != null ? principal.getName() : "Programme Coordinator";
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .message("Programme Batch ATR submitted for verification")
                .data(atrService.submitProgrammeAtr(batch.getMasterProgrammeId(), programmeBatchId, submitter))
                .build());
    }

    // --- Programme End Survey Excel Upload ---
    @PostMapping(value = "/{programmeBatchId}/survey/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgrammeSurveyResultDto>> uploadProgrammeSurvey(
            @PathVariable String programmeBatchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            Principal principal) {
        ProgrammeBatch batch = academicService.getBatchById(programmeBatchId);
        String uploader = (uploadedBy != null && !uploadedBy.isBlank()) ? uploadedBy : (principal != null ? principal.getName() : "Programme Coordinator");
        return ResponseEntity.ok(ApiResponse.<ProgrammeSurveyResultDto>builder()
                .success(true)
                .message("Programme exit survey processed successfully")
                .data(calculationService.processAndSaveProgrammeSurveyFile(batch.getMasterProgrammeId(), programmeBatchId, file, uploader))
                .build());
    }
}
