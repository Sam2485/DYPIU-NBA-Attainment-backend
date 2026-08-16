package com.dypiu.nba.service;

import com.dypiu.nba.entity.ApprovalHistory;
import com.dypiu.nba.entity.ApprovalRequest;
import com.dypiu.nba.entity.ApprovalStatus;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.ApprovalHistoryRepository;
import com.dypiu.nba.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getDirectorApprovals(String schoolId) {
        if (schoolId != null) {
            return approvalRequestRepository.findBySchoolId(schoolId);
        }
        return approvalRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getHodApprovals(String programmeId) {
        if (programmeId != null) {
            return approvalRequestRepository.findByProgrammeId(programmeId);
        }
        return approvalRequestRepository.findAll();
    }

    @Transactional
    public ApprovalRequest submitApprovalRequest(ApprovalRequest request) {
        if (request.getId() == null) request.setId("app-" + UUID.randomUUID().toString().substring(0, 8));
        request.setStatus(ApprovalStatus.PENDING);
        request.setSubmittedAt(ZonedDateTime.now());
        ApprovalRequest saved = approvalRequestRepository.save(request);

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(saved.getId())
                .actorName(request.getSubmittedBy())
                .actorRole("SUBMITTER")
                .action("SUBMITTED")
                .comments("Submitted for review")
                .build());

        return saved;
    }

    @Transactional
    public ApprovalRequest approveRequest(String id, String approverName, String approverRole) {
        ApprovalRequest req = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));

        req.setStatus(ApprovalStatus.APPROVED);
        req.setApprovedBy(approverName);
        req.setApprovedAt(ZonedDateTime.now());
        ApprovalRequest updated = approvalRequestRepository.save(req);

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(updated.getId())
                .actorName(approverName)
                .actorRole(approverRole)
                .action("APPROVED")
                .comments("Approved successfully")
                .build());

        return updated;
    }

    @Transactional
    public ApprovalRequest rejectRequest(String id, String remarks, String actorName, String actorRole) {
        ApprovalRequest req = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));

        req.setStatus(ApprovalStatus.NEEDS_REVISION);
        req.setRemarks(remarks);
        ApprovalRequest updated = approvalRequestRepository.save(req);

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(updated.getId())
                .actorName(actorName)
                .actorRole(actorRole)
                .action("REVISION_REQUESTED")
                .comments(remarks)
                .build());

        return updated;
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getApprovalById(String id) {
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistory> getApprovalHistory(String approvalRequestId) {
        return approvalHistoryRepository.findByApprovalRequestId(approvalRequestId);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPendingApprovals(String role, String schoolId, String programmeId) {
        if ("DIRECTOR".equalsIgnoreCase(role)) {
            return getDirectorApprovals(schoolId);
        } else if ("HOD".equalsIgnoreCase(role) || "PROGRAMME_COORDINATOR".equalsIgnoreCase(role)) {
            return getHodApprovals(programmeId);
        }
        return approvalRequestRepository.findByStatus(ApprovalStatus.PENDING);
    }
}
