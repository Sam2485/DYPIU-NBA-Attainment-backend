package com.dypiu.nba.service;

import com.dypiu.nba.entity.ApprovalHistory;
import com.dypiu.nba.entity.ApprovalRequest;
import com.dypiu.nba.entity.ApprovalStatus;
import com.dypiu.nba.entity.ApprovalType;
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
    private final com.dypiu.nba.repository.AttainmentConfigurationRepository configRepository;
    private final com.dypiu.nba.repository.CourseAtrRepository courseAtrRepository;
    private final com.dypiu.nba.repository.ProgrammeAtrRepository programmeAtrRepository;
    private final com.dypiu.nba.repository.CourseOfferingRepository courseOfferingRepository;
    private final com.dypiu.nba.repository.CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getApprovals(String role, String status, String type, String schoolId, String programmeId) {
        System.out.println("[ApprovalService] getApprovals called | role: " + role + " | status: " + status + " | type: " + type + " | schoolId: " + schoolId + " | programmeId: " + programmeId);
        List<ApprovalRequest> list = approvalRequestRepository.findAll();

        if (schoolId != null && !schoolId.isBlank()) {
            list = list.stream().filter(a -> schoolId.equalsIgnoreCase(a.getSchoolId())).toList();
        }
        if (programmeId != null && !programmeId.isBlank()) {
            list = list.stream().filter(a -> programmeId.equalsIgnoreCase(a.getProgrammeId())).toList();
        }
        if (status != null && !status.isBlank()) {
            try {
                ApprovalStatus targetStatus = ApprovalStatus.valueOf(status.trim().toUpperCase());
                list = list.stream().filter(a -> a.getStatus() == targetStatus).toList();
            } catch (IllegalArgumentException ignored) {}
        }
        if (type != null && !type.isBlank()) {
            try {
                ApprovalType targetType = ApprovalType.valueOf(type.trim().toUpperCase());
                list = list.stream().filter(a -> a.getType() == targetType).toList();
            } catch (IllegalArgumentException ignored) {}
        }
        if (role != null && !role.isBlank()) {
            if ("DIRECTOR".equalsIgnoreCase(role)) {
                list = list.stream().filter(a -> a.getType() == ApprovalType.OUTCOME_FRAMEWORK || a.getType() == ApprovalType.PROGRAMME_ATR).toList();
            } else if ("HOD".equalsIgnoreCase(role)) {
                list = list.stream().filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION || a.getType() == ApprovalType.PO_PSO_TARGETS).toList();
            }
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getDirectorApprovals(String schoolId) {
        System.out.println("[ApprovalService] getDirectorApprovals called | schoolId: " + schoolId);
        if (schoolId != null) {
            return approvalRequestRepository.findBySchoolId(schoolId);
        }
        return approvalRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getHodApprovals(String programmeId) {
        System.out.println("[ApprovalService] getHodApprovals called | programmeId: " + programmeId);
        if (programmeId != null) {
            return approvalRequestRepository.findByProgrammeId(programmeId);
        }
        return approvalRequestRepository.findAll();
    }

    @Transactional
    public ApprovalRequest actionRequest(String id, String action, String comments, String actorName, String actorRole) {
        System.out.println("[ApprovalService] actionRequest called | id: " + id + " | action: " + action);
        if ("APPROVE".equalsIgnoreCase(action) || "APPROVED".equalsIgnoreCase(action)) {
            return approveRequest(id, actorName != null ? actorName : "Approver", actorRole != null ? actorRole : "APPROVER");
        } else if ("REJECT".equalsIgnoreCase(action) || "REJECTED".equalsIgnoreCase(action) || "REQUEST_REVISION".equalsIgnoreCase(action) || "NEEDS_REVISION".equalsIgnoreCase(action)) {
            return rejectRequest(id, comments != null ? comments : "Revision requested.", actorName != null ? actorName : "Reviewer", actorRole != null ? actorRole : "REVIEWER");
        } else {
            throw new com.dypiu.nba.exception.BadRequestException("Invalid approval action: " + action + ". Allowed: APPROVE, REJECT, REQUEST_REVISION");
        }
    }

    @Transactional
    public ApprovalRequest submitApprovalRequest(ApprovalRequest request) {
        System.out.println("[ApprovalService] submitApprovalRequest called | type: " + (request != null ? request.getType() : "null") + " | resourceId: " + (request != null ? request.getResourceId() : "null"));
        if (request.getId() == null) request.setId("app-" + UUID.randomUUID().toString().substring(0, 8));
        if (request.getSubmittedBy() == null || request.getSubmittedBy().isBlank()) {
            request.setSubmittedBy("Director");
        }
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
        System.out.println("[ApprovalService] approveRequest called | id: " + id + " | approver: " + approverName);
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
        System.out.println("[ApprovalService] rejectRequest called | id: " + id + " | actorName: " + actorName);
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
        System.out.println("[ApprovalService] getApprovalById called | id: " + id);
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistory> getApprovalHistory(String approvalRequestId) {
        System.out.println("[ApprovalService] getApprovalHistory called | approvalRequestId: " + approvalRequestId);
        return approvalHistoryRepository.findByApprovalRequestId(approvalRequestId);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPendingApprovals(String role, String schoolId, String programmeId) {
        System.out.println("[ApprovalService] getPendingApprovals called | role: " + role + " | schoolId: " + schoolId + " | programmeId: " + programmeId);
        if ("DIRECTOR".equalsIgnoreCase(role)) {
            return getDirectorApprovals(schoolId);
        } else if ("HOD".equalsIgnoreCase(role) || "PROGRAMME_COORDINATOR".equalsIgnoreCase(role)) {
            return getHodApprovals(programmeId);
        }
        return approvalRequestRepository.findByStatus(ApprovalStatus.PENDING);
    }

    private static final java.util.Comparator<ApprovalRequest> LATEST_APPROVAL_COMPARATOR = (a, b) -> {
        ZonedDateTime ta = a.getUpdatedAt() != null ? a.getUpdatedAt() : (a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getCreatedAt());
        ZonedDateTime tb = b.getUpdatedAt() != null ? b.getUpdatedAt() : (b.getSubmittedAt() != null ? b.getSubmittedAt() : b.getCreatedAt());
        if (ta == null && tb == null) return 0;
        if (ta == null) return -1;
        if (tb == null) return 1;
        return ta.compareTo(tb);
    };

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getVerificationStatus(String key) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (key == null || key.isBlank()) return result;

        String lowerKey = key.trim().toLowerCase();
        if (lowerKey.startsWith("allocation")) {
            String progId = key.replace("allocation-", "").replace("allocation_", "").replace("allocation", "");
            ApprovalRequest req = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION && (progId.equalsIgnoreCase(a.getProgrammeId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);
            result.put("allocationStatus", req != null && req.getStatus() != null ? req.getStatus().name() : "DRAFT");
            result.put("allocationRemarks", req != null && req.getRemarks() != null ? req.getRemarks() : "");
            result.put("verifiedBy", req != null && req.getApprovedBy() != null ? req.getApprovedBy() : "");
        } else if (lowerKey.startsWith("targets") || lowerKey.startsWith("po-pso-targets") || lowerKey.startsWith("po_pso_targets")) {
            String progId = key.replace("po-pso-targets-", "").replace("po_pso_targets-", "").replace("po-pso-targets_", "").replace("po_pso_targets_", "").replace("targets-", "").replace("targets_", "").replace("targets", "");
            ApprovalRequest req = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.PO_PSO_TARGETS && (progId.equalsIgnoreCase(a.getProgrammeId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);
            result.put("poPsoTargetsStatus", req != null && req.getStatus() != null ? req.getStatus().name() : "DRAFT");
            result.put("poPsoTargetsRemarks", req != null && req.getRemarks() != null ? req.getRemarks() : "");
            result.put("verifiedBy", req != null && req.getApprovedBy() != null ? req.getApprovedBy() : "");
        } else if (lowerKey.startsWith("prog-atr") || lowerKey.startsWith("programme-atr") || lowerKey.startsWith("prog_atr")) {
            String progId = key.replace("prog-atr-", "").replace("programme-atr-", "").replace("prog_atr-", "").replace("prog_atr_", "").replace("prog-atr", "").replace("programme-atr", "");
            com.dypiu.nba.entity.ProgrammeAtr patr = programmeAtrRepository.findAll().stream()
                    .filter(p -> progId.equalsIgnoreCase(p.getProgrammeId()) || key.equalsIgnoreCase(p.getId()))
                    .max(java.util.Comparator.comparing(com.dypiu.nba.entity.ProgrammeAtr::getUpdatedAt, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                    .orElse(null);

            ApprovalRequest patrReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.PROGRAMME_ATR && (progId.equalsIgnoreCase(a.getProgrammeId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);

            String status = patrReq != null && patrReq.getStatus() != null ? patrReq.getStatus().name()
                    : (patr != null && patr.getStatus() != null ? patr.getStatus().name() : "DRAFT");
            String remarks = patrReq != null && patrReq.getRemarks() != null ? patrReq.getRemarks()
                    : (patr != null && patr.getVerificationComments() != null ? patr.getVerificationComments() : "");
            String verifier = patrReq != null && patrReq.getApprovedBy() != null ? patrReq.getApprovedBy()
                    : (patr != null && patr.getVerifiedBy() != null ? patr.getVerifiedBy() : "");

            result.put("programmeAtrStatus", status);
            result.put("programmeAtrRemarks", remarks);
            result.put("verifiedBy", verifier);
        } else {
            String offeringId = key;
            List<com.dypiu.nba.entity.CourseOffering> offerings = courseOfferingRepository.findByCourseId(key);
            if (!offerings.isEmpty()) {
                offeringId = offerings.get(0).getId();
            }

            com.dypiu.nba.entity.AttainmentConfiguration config = configRepository.findByCourseOfferingId(offeringId).orElse(null);
            String finalOfferingId = offeringId;
            ApprovalRequest configReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.ATTAINMENT_CONFIGURATION && (finalOfferingId.equalsIgnoreCase(a.getCourseOfferingId()) || key.equalsIgnoreCase(a.getCourseId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);

            ApprovalRequest coReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> (a.getType() == ApprovalType.CO_DEFINITION || a.getType() == ApprovalType.CO_TARGETS) && (finalOfferingId.equalsIgnoreCase(a.getCourseOfferingId()) || key.equalsIgnoreCase(a.getCourseId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);

            ApprovalRequest atrReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.COURSE_ATR && (finalOfferingId.equalsIgnoreCase(a.getCourseOfferingId()) || key.equalsIgnoreCase(a.getCourseId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);

            List<com.dypiu.nba.entity.CourseAtr> atrs = courseAtrRepository.findByCourseOfferingId(offeringId);
            com.dypiu.nba.entity.CourseAtr sampleAtr = atrs.isEmpty() ? null : atrs.get(0);

            String configStatus = configReq != null && configReq.getStatus() != null ? configReq.getStatus().name() : (config != null && config.getStatus() != null ? config.getStatus().name() : "DRAFT");
            String coStatus = coReq != null && coReq.getStatus() != null ? coReq.getStatus().name() : "APPROVED";
            String atrStatus = atrReq != null && atrReq.getStatus() != null ? atrReq.getStatus().name() : (sampleAtr != null && sampleAtr.getStatus() != null ? sampleAtr.getStatus().name() : "DRAFT");

            String configRemarks = configReq != null && configReq.getRemarks() != null ? configReq.getRemarks() : "";
            String coRemarks = coReq != null && coReq.getRemarks() != null ? coReq.getRemarks() : "";
            String atrRemarks = atrReq != null && atrReq.getRemarks() != null ? atrReq.getRemarks() : (sampleAtr != null && sampleAtr.getVerificationComments() != null ? sampleAtr.getVerificationComments() : "");

            String verifier = atrReq != null && atrReq.getApprovedBy() != null ? atrReq.getApprovedBy()
                    : (sampleAtr != null && sampleAtr.getVerifiedBy() != null ? sampleAtr.getVerifiedBy()
                    : (configReq != null && configReq.getApprovedBy() != null ? configReq.getApprovedBy()
                    : (coReq != null && coReq.getApprovedBy() != null ? coReq.getApprovedBy() : "")));

            result.put("configStatus", configStatus);
            result.put("coStatus", coStatus);
            result.put("atrStatus", atrStatus);
            result.put("configRemarks", configRemarks);
            result.put("coRemarks", coRemarks);
            result.put("atrRemarks", atrRemarks);
            result.put("verifiedBy", verifier);
        }
        return result;
    }

    @Transactional
    public java.util.Map<String, Object> verifyStatus(String key, String statusType, String statusValue, String remarksValue, String verifierName) {
        System.out.println("[ApprovalService] verifyStatus called | key: " + key + " | statusType: " + statusType + " | statusValue: " + statusValue);
        ApprovalStatus status = "APPROVED".equalsIgnoreCase(statusValue) || "VERIFIED".equalsIgnoreCase(statusValue) ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;

        ApprovalType type = ApprovalType.OTHER;
        String programmeId = null;
        String courseId = null;
        String courseOfferingId = null;

        if ("allocationStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ALLOCATION;
            programmeId = key != null ? key.replace("allocation-", "").replace("allocation_", "").replace("allocation", "") : null;
        } else if ("poPsoTargetsStatus".equalsIgnoreCase(statusType) || "targetsStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PO_PSO_TARGETS;
            programmeId = key != null ? key.replace("po-pso-targets-", "").replace("po_pso_targets-", "").replace("targets-", "").replace("targets_", "").replace("targets", "") : null;
        } else if ("configStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.ATTAINMENT_CONFIGURATION;
            courseOfferingId = key;
            com.dypiu.nba.entity.AttainmentConfiguration cfg = configRepository.findByCourseOfferingId(key).orElse(null);
            if (cfg != null) {
                cfg.setStatus(com.dypiu.nba.entity.AttainmentConfigStatus.APPROVED);
                configRepository.save(cfg);
            }
        } else if ("coStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.CO_DEFINITION;
            courseOfferingId = key;
        } else if ("atrStatus".equalsIgnoreCase(statusType) || "courseAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ATR;
            courseOfferingId = key;
            List<com.dypiu.nba.entity.CourseAtr> atrs = courseAtrRepository.findByCourseOfferingId(key);
            for (com.dypiu.nba.entity.CourseAtr a : atrs) {
                a.setStatus(com.dypiu.nba.entity.CourseAtrStatus.APPROVED);
                a.setVerifiedBy(verifierName);
                a.setVerificationComments(remarksValue);
                courseAtrRepository.save(a);
            }
        } else if ("programmeAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PROGRAMME_ATR;
            programmeId = key != null ? key.replace("prog-atr-", "").replace("programme-atr-", "").replace("prog_atr-", "").replace("prog_atr_", "").replace("prog-atr", "").replace("programme-atr", "") : null;
            String targetPId = programmeId;
            com.dypiu.nba.entity.ProgrammeAtr patr = programmeAtrRepository.findAll().stream()
                    .filter(p -> targetPId != null && (targetPId.equalsIgnoreCase(p.getProgrammeId()) || key.equalsIgnoreCase(p.getId())))
                    .findFirst().orElse(null);
            if (patr != null) {
                patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.APPROVED);
                patr.setVerifiedBy(verifierName);
                patr.setVerificationComments(remarksValue);
                programmeAtrRepository.save(patr);
            }
        }

        ApprovalRequest req = ApprovalRequest.builder()
                .id("app-" + UUID.randomUUID().toString().substring(0, 8))
                .type(type)
                .title(statusType + " for " + key)
                .resourceId(key)
                .programmeId(programmeId)
                .courseId(courseId)
                .courseOfferingId(courseOfferingId)
                .status(status)
                .submittedBy(verifierName != null ? verifierName : "Verifier")
                .submittedAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .approvedBy(verifierName)
                .approvedAt(ZonedDateTime.now())
                .remarks(remarksValue)
                .build();
        approvalRequestRepository.save(req);

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(req.getId())
                .actorName(verifierName)
                .actorRole("VERIFIER")
                .action("VERIFIED")
                .comments(remarksValue != null ? remarksValue : "Verified successfully")
                .build());

        return getVerificationStatus(key);
    }

    @Transactional
    public java.util.Map<String, Object> requestRevisionStatus(String key, String statusType, String statusValue, String remarksValue, String verifierName) {
        System.out.println("[ApprovalService] requestRevisionStatus called | key: " + key + " | statusType: " + statusType + " | remarks: " + remarksValue);
        ApprovalType type = ApprovalType.OTHER;
        String programmeId = null;
        String courseId = null;
        String courseOfferingId = null;

        if ("allocationStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ALLOCATION;
            programmeId = key != null ? key.replace("allocation-", "").replace("allocation_", "").replace("allocation", "") : null;
        } else if ("poPsoTargetsStatus".equalsIgnoreCase(statusType) || "targetsStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PO_PSO_TARGETS;
            programmeId = key != null ? key.replace("po-pso-targets-", "").replace("po_pso_targets-", "").replace("targets-", "").replace("targets_", "").replace("targets", "") : null;
        } else if ("configStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.ATTAINMENT_CONFIGURATION;
            courseOfferingId = key;
            com.dypiu.nba.entity.AttainmentConfiguration cfg = configRepository.findByCourseOfferingId(key).orElse(null);
            if (cfg != null) {
                cfg.setStatus(com.dypiu.nba.entity.AttainmentConfigStatus.NEEDS_REVISION);
                configRepository.save(cfg);
            }
        } else if ("coStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.CO_DEFINITION;
            courseOfferingId = key;
        } else if ("atrStatus".equalsIgnoreCase(statusType) || "courseAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ATR;
            courseOfferingId = key;
            List<com.dypiu.nba.entity.CourseAtr> atrs = courseAtrRepository.findByCourseOfferingId(key);
            for (com.dypiu.nba.entity.CourseAtr a : atrs) {
                a.setStatus(com.dypiu.nba.entity.CourseAtrStatus.NEEDS_REVISION);
                a.setVerificationComments(remarksValue);
                courseAtrRepository.save(a);
            }
        } else if ("programmeAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PROGRAMME_ATR;
            programmeId = key != null ? key.replace("prog-atr-", "").replace("programme-atr-", "").replace("prog_atr-", "").replace("prog_atr_", "").replace("prog-atr", "").replace("programme-atr", "") : null;
            String targetPId = programmeId;
            com.dypiu.nba.entity.ProgrammeAtr patr = programmeAtrRepository.findAll().stream()
                    .filter(p -> targetPId != null && (targetPId.equalsIgnoreCase(p.getProgrammeId()) || key.equalsIgnoreCase(p.getId())))
                    .findFirst().orElse(null);
            if (patr != null) {
                patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.NEEDS_REVISION);
                patr.setVerificationComments(remarksValue);
                programmeAtrRepository.save(patr);
            }
        }

        ApprovalRequest req = ApprovalRequest.builder()
                .id("app-" + UUID.randomUUID().toString().substring(0, 8))
                .type(type)
                .title("Revision requested for " + statusType + " on " + key)
                .resourceId(key)
                .programmeId(programmeId)
                .courseId(courseId)
                .courseOfferingId(courseOfferingId)
                .status(ApprovalStatus.NEEDS_REVISION)
                .submittedBy(verifierName != null ? verifierName : "Reviewer")
                .submittedAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .remarks(remarksValue)
                .approvedBy(verifierName)
                .build();
        approvalRequestRepository.save(req);

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(req.getId())
                .actorName(verifierName)
                .actorRole("REVIEWER")
                .action("REVISION_REQUESTED")
                .comments(remarksValue)
                .build());

        return getVerificationStatus(key);
    }
}
