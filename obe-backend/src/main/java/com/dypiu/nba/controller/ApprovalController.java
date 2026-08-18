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
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getPendingApprovals(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String programmeId) {
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .data(approvalService.getPendingApprovals(role, schoolId, programmeId))
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
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getHodApprovals(@RequestParam(required = false) String programmeId) {
        return ResponseEntity.ok(ApiResponse.<List<ApprovalRequest>>builder()
                .success(true)
                .data(approvalService.getHodApprovals(programmeId))
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

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ApprovalRequest>> approveRequest(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String actorName = body != null && body.containsKey("actorName") ? body.get("actorName") : "Approver";
        String actorRole = body != null && body.containsKey("actorRole") ? body.get("actorRole") : "APPROVER";
        return ResponseEntity.ok(ApiResponse.<ApprovalRequest>builder()
                .success(true)
                .message("Request approved successfully")
                .data(approvalService.approveRequest(id, actorName, actorRole))
                .build());
    }

    @PostMapping({"/{id}/reject", "/{id}/request-revision"})
    public ResponseEntity<ApiResponse<ApprovalRequest>> rejectRequest(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String remarks = body != null && body.containsKey("remarks") ? body.get("remarks") : (body != null && body.containsKey("comments") ? body.get("comments") : "Revision requested.");
        String actorName = body != null && body.containsKey("actorName") ? body.get("actorName") : "Reviewer";
        String actorRole = body != null && body.containsKey("actorRole") ? body.get("actorRole") : "REVIEWER";
        return ResponseEntity.ok(ApiResponse.<ApprovalRequest>builder()
                .success(true)
                .message("Revision requested")
                .data(approvalService.rejectRequest(id, remarks, actorName, actorRole))
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
