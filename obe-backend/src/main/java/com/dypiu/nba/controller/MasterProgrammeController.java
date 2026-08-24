package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.entity.MasterProgramme;
import com.dypiu.nba.entity.ProgrammeBatch;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.AtrService;
import com.dypiu.nba.service.AttainmentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/master-programmes")
@RequiredArgsConstructor
public class MasterProgrammeController {

    private final AcademicService academicService;
    private final AttainmentReportService attainmentReportService;
    private final AtrService atrService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MasterProgramme>>> getAllMasterProgrammes(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String schoolId) {
        List<MasterProgramme> programmes = (departmentId != null && !departmentId.isBlank())
                ? academicService.getProgrammesByDepartment(departmentId)
                : academicService.getAllProgrammes();
        return ResponseEntity.ok(ApiResponse.<List<MasterProgramme>>builder()
                .success(true)
                .data(programmes)
                .build());
    }

    @GetMapping("/coordinator")
    public ResponseEntity<ApiResponse<List<MasterProgramme>>> getMasterProgrammesForCoordinator(
            @RequestParam(required = false) String coordinatorEmail,
            java.security.Principal principal) {
        String effectiveEmail = (coordinatorEmail != null && !coordinatorEmail.isBlank())
                ? coordinatorEmail
                : (principal != null ? principal.getName() : null);
        List<MasterProgramme> programmes = academicService.getProgrammesByCoordinatorEmail(effectiveEmail);
        return ResponseEntity.ok(ApiResponse.<List<MasterProgramme>>builder()
                .success(true)
                .data(programmes)
                .build());
    }

    @GetMapping("/{masterProgrammeId}")
    public ResponseEntity<ApiResponse<MasterProgramme>> getMasterProgrammeById(
            @PathVariable String masterProgrammeId) {
        return ResponseEntity.ok(ApiResponse.<MasterProgramme>builder()
                .success(true)
                .data(academicService.getProgrammeById(masterProgrammeId))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MasterProgramme>> createMasterProgramme(
            @RequestBody MasterProgramme programme) {
        return ResponseEntity.ok(ApiResponse.<MasterProgramme>builder()
                .success(true)
                .message("MasterProgramme created successfully")
                .data(academicService.saveProgramme(programme))
                .build());
    }

    @PutMapping("/{masterProgrammeId}")
    public ResponseEntity<ApiResponse<MasterProgramme>> updateMasterProgramme(
            @PathVariable String masterProgrammeId,
            @RequestBody MasterProgramme programme) {
        programme.setId(masterProgrammeId);
        return ResponseEntity.ok(ApiResponse.<MasterProgramme>builder()
                .success(true)
                .message("MasterProgramme updated successfully")
                .data(academicService.saveProgramme(programme))
                .build());
    }

    @DeleteMapping("/{masterProgrammeId}")
    public ResponseEntity<ApiResponse<Void>> deleteMasterProgramme(
            @PathVariable String masterProgrammeId) {
        academicService.deleteProgramme(masterProgrammeId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("MasterProgramme deleted successfully")
                .build());
    }

    @GetMapping("/{masterProgrammeId}/programme-batches")
    public ResponseEntity<ApiResponse<List<ProgrammeBatch>>> getProgrammeBatches(
            @PathVariable String masterProgrammeId) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeBatch>>builder()
                .success(true)
                .data(academicService.getBatchesByProgramme(masterProgrammeId))
                .build());
    }

    @GetMapping("/{masterProgrammeId}/historical/attainment-reports")
    public ResponseEntity<ApiResponse<List<ProgrammeBatchAttainmentReportDto>>> getHistoricalAttainmentReports(
            @PathVariable String masterProgrammeId) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeBatchAttainmentReportDto>>builder()
                .success(true)
                .data(attainmentReportService.getHistoricalProgrammeAttainmentReports(masterProgrammeId))
                .build());
    }

    @GetMapping("/{masterProgrammeId}/historical/atrs")
    public ResponseEntity<ApiResponse<List<ProgrammeAtrReportDto>>> getHistoricalProgrammeAtrs(
            @PathVariable String masterProgrammeId) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeAtrReportDto>>builder()
                .success(true)
                .data(atrService.getHistoricalProgrammeAtrs(masterProgrammeId))
                .build());
    }
}
