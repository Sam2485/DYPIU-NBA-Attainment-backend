package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.ApprovalHistory;
import com.dypiu.nba.entity.ApprovalRequest;
import com.dypiu.nba.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/approvals", "/api/v1/approvals"})
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping({"", "/"})
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getApprovals(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String masterProgrammeId) {
        String effectiveProgId = (masterProgrammeId != null && !masterProgrammeId.isBlank()) ? masterProgrammeId : masterProgrammeId;
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .data(approvalService.getApprovals(role, status, type, schoolId, effectiveProgId))
                .build());
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<?>> getPendingApprovals(
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String masterProgrammeId) {
        if (programmeBatchId != null && !programmeBatchId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.ProgrammeBatchApprovalInboxDto>builder()
                    .success(true)
                    .message("Pending approvals fetched successfully")
                    .data(approvalService.getPendingApprovalsByProgrammeBatch(programmeBatchId.trim()))
                    .build());
        }
        String effectiveProgId = (masterProgrammeId != null && !masterProgrammeId.isBlank()) ? masterProgrammeId : masterProgrammeId;
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .message("Pending approvals fetched successfully")
                .data(approvalService.getPendingApprovals(role, schoolId, effectiveProgId))
                .build());
    }

    @GetMapping("/reviewed")
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.ProgrammeBatchApprovalInboxDto>> getReviewedApprovals(
            @RequestParam(required = false) String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.ProgrammeBatchApprovalInboxDto>builder()
                .success(true)
                .message("Reviewed approvals fetched successfully")
                .data(approvalService.getReviewedApprovalsByProgrammeBatch(programmeBatchId))
                .build());
    }

    @GetMapping("/programme-batch-courses/{programmeBatchCourseId}")
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.CourseApprovalWorkspaceDto>> getCourseApprovalWorkspace(
            @PathVariable String programmeBatchCourseId) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.CourseApprovalWorkspaceDto>builder()
                .success(true)
                .message("Programme-Batch-Course approval details fetched successfully")
                .data(approvalService.getCourseApprovalWorkspace(programmeBatchCourseId))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalRequest>> getApprovalById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<ApprovalRequest>builder()
                .success(true)
                .data(approvalService.getApprovalById(id))
                .build());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<ApprovalHistory>>> getApprovalHistory(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<List<ApprovalHistory>>builder()
                .success(true)
                .data(approvalService.getApprovalHistory(id))
                .build());
    }

    @GetMapping("/director")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getDirectorApprovals(@RequestParam(required = false) String schoolId) {
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .data(approvalService.getDirectorApprovals(schoolId))
                .build());
    }

    @GetMapping("/hod")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getHodApprovals(
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String status) {
        String effectiveProgId = (masterProgrammeId != null && !masterProgrammeId.isBlank()) ? masterProgrammeId : masterProgrammeId;
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .data(approvalService.getHodApprovals(effectiveProgId, status))
                .build());
    }

    @GetMapping("/hod/pending")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getHodPendingApprovals(
            @RequestParam(required = false) String masterProgrammeId) {
        String effectiveProgId = (masterProgrammeId != null && !masterProgrammeId.isBlank()) ? masterProgrammeId : masterProgrammeId;
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .message("HOD pending approvals fetched successfully")
                .data(approvalService.getHodPendingApprovals(effectiveProgId))
                .build());
    }

    @GetMapping("/hod/reviewed")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getHodReviewedApprovals(
            @RequestParam(required = false) String masterProgrammeId) {
        String effectiveProgId = (masterProgrammeId != null && !masterProgrammeId.isBlank()) ? masterProgrammeId : masterProgrammeId;
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .message("HOD reviewed approvals fetched successfully")
                .data(approvalService.getHodReviewedApprovals(effectiveProgId))
                .build());
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ApprovalRequest>> submitApprovalRequest(@RequestBody ApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.<ApprovalRequest>builder()
                .success(true)
                .message("Approval request submitted")
                .data(approvalService.submitApprovalRequest(request))
                .build());
    }

    @PostMapping({"/{approvalRequestId}/approve"})
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.ApprovalActionResultDto>> approveRequest(
            @PathVariable String approvalRequestId,
            @RequestBody(required = false) Map<String, String> body) {
        String actorName = body != null && body.containsKey("actorName") ? body.get("actorName") : null;
        String actorRole = body != null && body.containsKey("actorRole") ? body.get("actorRole") : null;
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.ApprovalActionResultDto>builder()
                .success(true)
                .message("Approval completed successfully")
                .data(approvalService.approveRequestDto(approvalRequestId, actorName, actorRole))
                .build());
    }

    @PostMapping({"/{approvalRequestId}/request-revision", "/{approvalRequestId}/reject"})
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.ApprovalActionResultDto>> rejectRequest(
            @PathVariable String approvalRequestId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null && body.containsKey("reason") ? body.get("reason")
                : (body != null && body.containsKey("remarks") ? body.get("remarks")
                : (body != null && body.containsKey("comments") ? body.get("comments") : "Revision requested."));
        String actorName = body != null && body.containsKey("actorName") ? body.get("actorName") : null;
        String actorRole = body != null && body.containsKey("actorRole") ? body.get("actorRole") : null;
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.ApprovalActionResultDto>builder()
                .success(true)
                .message("Revision requested successfully")
                .data(approvalService.requestRevisionDto(approvalRequestId, reason, actorName, actorRole))
                .build());
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<ApiResponse<ApprovalRequest>> actionRequest(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String action = body != null && body.containsKey("action") ? body.get("action") : "APPROVE";
        String comments = body != null && body.containsKey("comments") ? body.get("comments") : (body != null && body.containsKey("remarks") ? body.get("remarks") : "");
        String actorName = body != null && body.containsKey("actorName") ? body.get("actorName") : "Actor";
        String actorRole = body != null && body.containsKey("actorRole") ? body.get("actorRole") : "REVIEWER";
        return ResponseEntity.ok(ApiResponse.<ApprovalRequest>builder()
                .success(true)
                .message("Action " + action + " executed successfully")
                .data(approvalService.actionRequest(id, action, comments, actorName, actorRole))
                .build());
    }

    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVerificationStatus(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "id", required = false) String id) {
        String targetKey = (key != null && !key.isBlank()) ? key : id;
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(approvalService.getVerificationStatus(targetKey))
                .build());
    }

    @RequestMapping(value = "/verify", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyStatus(@RequestBody Map<String, String> body) {
        String key = body != null ? (body.containsKey("key") ? body.get("key") : body.get("id")) : "";
        String statusType = body != null ? body.get("statusType") : "status";
        String statusValue = body != null ? body.get("statusValue") : "APPROVED";
        String remarksValue = body != null ? (body.containsKey("remarksValue") ? body.get("remarksValue") : body.get("remarks")) : "";
        String verifierName = body != null ? body.get("verifierName") : "Verifier";

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Status updated successfully.")
                .data(approvalService.verifyStatus(key, statusType, statusValue, remarksValue, verifierName))
                .build());
    }

    @RequestMapping(value = "/request-revision", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestRevisionDirect(@RequestBody Map<String, String> body) {
        String key = body != null ? (body.containsKey("key") ? body.get("key") : body.get("id")) : "";
        String statusType = body != null ? body.get("statusType") : "status";
        String statusValue = body != null ? body.get("statusValue") : "REVISION_REQUESTED";
        String remarksValue = body != null ? (body.containsKey("remarksValue") ? body.get("remarksValue") : body.get("remarks")) : "Please revise.";
        String verifierName = body != null ? body.get("verifierName") : "Reviewer";

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Revision request recorded. Coordinator notified.")
                .data(approvalService.requestRevisionStatus(key, statusType, statusValue, remarksValue, verifierName))
                .build());
    }
}
