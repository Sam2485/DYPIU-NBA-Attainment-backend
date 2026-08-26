package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final AttainmentConfigurationRepository configRepository;
    private final CourseAtrRepository courseAtrRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final MasterCourseRepository masterCourseRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final CurrentUserScopeService currentUserScopeService;
    private final AuditLogService auditLogService;

    private CurrentUserScope getScope() {
        try {
            return currentUserScopeService.getCurrentUserScope();
        } catch (Exception e) {
            return null;
        }
    }

    private void resolveMissingScopeFields(ApprovalRequest req) {
        if (req == null) return;
        if (req.getSchoolId() == null || req.getDepartmentId() == null) {
            String progId = req.getMasterProgrammeId() != null ? req.getMasterProgrammeId() : req.getMasterProgrammeId();
            String batchCourseId = req.getProgrammeBatchCourseId() != null ? req.getProgrammeBatchCourseId() : req.getProgrammeBatchCourseId();
            String masterCourseId = req.getMasterCourseId() != null ? req.getMasterCourseId() : req.getMasterCourseId();

            if (progId != null && !progId.isBlank()) {
                masterProgrammeRepository.findById(progId).ifPresent(p -> {
                    if (req.getDepartmentId() == null) req.setDepartmentId(p.getDepartmentId());
                    if (p.getDepartmentId() != null) {
                        departmentRepository.findById(p.getDepartmentId()).ifPresent(d -> {
                            if (req.getSchoolId() == null) req.setSchoolId(d.getSchoolId());
                        });
                    }
                });
            } else if (batchCourseId != null && !batchCourseId.isBlank()) {
                programmeBatchCourseRepository.findById(batchCourseId).ifPresent(off -> {
                    if (req.getMasterCourseId() == null) req.setMasterCourseId(off.getMasterCourseId());
                    if (off.getMasterCourseId() != null) {
                        masterCourseRepository.findById(off.getMasterCourseId()).ifPresent(c -> {
                            if (req.getMasterProgrammeId() == null) req.setMasterProgrammeId(c.getMasterProgrammeId());
                            if (c.getMasterProgrammeId() != null) {
                                masterProgrammeRepository.findById(c.getMasterProgrammeId()).ifPresent(p -> {
                                    if (req.getDepartmentId() == null) req.setDepartmentId(p.getDepartmentId());
                                    if (p.getDepartmentId() != null) {
                                        departmentRepository.findById(p.getDepartmentId()).ifPresent(d -> {
                                            if (req.getSchoolId() == null) req.setSchoolId(d.getSchoolId());
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            } else if (masterCourseId != null && !masterCourseId.isBlank()) {
                masterCourseRepository.findById(masterCourseId).ifPresent(c -> {
                    if (req.getMasterProgrammeId() == null) req.setMasterProgrammeId(c.getMasterProgrammeId());
                    if (c.getMasterProgrammeId() != null) {
                        masterProgrammeRepository.findById(c.getMasterProgrammeId()).ifPresent(p -> {
                            if (req.getDepartmentId() == null) req.setDepartmentId(p.getDepartmentId());
                            if (p.getDepartmentId() != null) {
                                departmentRepository.findById(p.getDepartmentId()).ifPresent(d -> {
                                    if (req.getSchoolId() == null) req.setSchoolId(d.getSchoolId());
                                });
                            }
                        });
                    }
                });
            }
        }
    }

    public void enforceApprovalScope(ApprovalRequest req) {
        if (req == null) return;
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;

        resolveMissingScopeFields(req);

        if (scope.isDirector()) {
            String requiredSchoolId = scope.getRequiredSchoolId();
            if (req.getSchoolId() != null && !req.getSchoolId().equalsIgnoreCase(requiredSchoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Approval request belongs to a different school.");
            }
        } else if (scope.isHod()) {
            String requiredSchoolId = scope.getRequiredSchoolId();
            String requiredDeptId = scope.getRequiredDepartmentId();
            if (req.getSchoolId() != null && !req.getSchoolId().equalsIgnoreCase(requiredSchoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Approval request belongs to a different school.");
            }
            if (req.getDepartmentId() != null && !req.getDepartmentId().equalsIgnoreCase(requiredDeptId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Approval request belongs to a different department.");
            }
        } else if (scope.isProgrammeCoordinator()) {
            String requiredProgId = scope.getRequiredMasterProgrammeId();
            String progId = req.getMasterProgrammeId() != null ? req.getMasterProgrammeId() : req.getMasterProgrammeId();
            if (progId != null && !progId.equalsIgnoreCase(requiredProgId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Approval request belongs to a different programme.");
            }
        } else if (scope.isFaculty()) {
            String batchCourseId = req.getProgrammeBatchCourseId() != null ? req.getProgrammeBatchCourseId() : req.getProgrammeBatchCourseId();
            if (batchCourseId != null) {
                ProgrammeBatchCourse off = programmeBatchCourseRepository.findById(batchCourseId).orElse(null);
                if (off != null) {
                    boolean isCoord = (off.getCourseCoordinatorId() != null && off.getCourseCoordinatorId().equals(scope.getUserId()));
                    boolean assigned = isCoord || (off.getAssignedFaculty() != null && (off.getAssignedFaculty().contains(scope.getEmail()) || off.getAssignedFaculty().contains(scope.getName())));
                    if (!assigned) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this course offering.");
                    }
                }
            } else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Faculty members can only access their assigned course offerings.");
            }
        }
    }

    private void enforceRoleApprovalAuthority(ApprovalType type) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;

        if (scope.isFaculty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Faculty members do not have approval or revision authority.");
        }

        if (type == null) return;

        if (scope.isDirector()) {
            if (type != ApprovalType.OUTCOME_FRAMEWORK
                    && type != ApprovalType.PROGRAMME_ATR
                    && type != ApprovalType.DIRECTOR_GOVERNANCE
                    && type != ApprovalType.OTHER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Director is not authorized to approve " + type);
            }
        } else if (scope.isHod()) {
            if (type != ApprovalType.COURSE_ALLOCATION
                    && type != ApprovalType.PO_PSO_TARGETS
                    && type != ApprovalType.PROGRAMME_ATR
                    && type != ApprovalType.OTHER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: HOD is not authorized to approve " + type);
            }
        } else if (scope.isProgrammeCoordinator()) {
            if (type != ApprovalType.CO_DEFINITION
                    && type != ApprovalType.CO_TARGETS
                    && type != ApprovalType.ATTAINMENT_CONFIGURATION
                    && type != ApprovalType.COURSE_ATR
                    && type != ApprovalType.COURSE_OFFERING
                    && type != ApprovalType.OTHER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme Coordinator is not authorized to approve " + type);
            }
        }
    }

    private void preventSelfApproval(ApprovalRequest req) {
        if (req == null || req.getSubmittedBy() == null || req.getSubmittedBy().isBlank()) return;
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin()) return;

        String submitter = req.getSubmittedBy().trim().toLowerCase();
        String userEmail = scope.getEmail() != null ? scope.getEmail().trim().toLowerCase() : "";
        String userName = scope.getName() != null ? scope.getName().trim().toLowerCase() : "";
        String userLogin = scope.getUsername() != null ? scope.getUsername().trim().toLowerCase() : "";
        String userIdStr = scope.getUserId() != null ? String.valueOf(scope.getUserId()) : "";

        if ((!userEmail.isEmpty() && submitter.equalsIgnoreCase(userEmail))
                || (!userName.isEmpty() && submitter.equalsIgnoreCase(userName))
                || (!userLogin.isEmpty() && submitter.equalsIgnoreCase(userLogin))
                || (!userIdStr.isEmpty() && submitter.equalsIgnoreCase(userIdStr))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Self-approval is not permitted.");
        }
    }

    private record ActorInfo(Long actorId, String actorName, String actorRole) {}

    private ActorInfo resolveActorInfo(String clientActorName, String clientActorRole) {
        CurrentUserScope scope = getScope();
        if (scope != null) {
            String name = scope.getName() != null && !scope.getName().isBlank()
                    ? scope.getName()
                    : (scope.getEmail() != null && !scope.getEmail().isBlank() ? scope.getEmail() : scope.getUsername());
            String role = scope.getRole() != null ? scope.getRole().name() : "USER";
            return new ActorInfo(scope.getUserId(), name != null ? name : "Actor", role);
        }
        String fallbackName = clientActorName != null && !clientActorName.isBlank() ? clientActorName : "Approver";
        String fallbackRole = clientActorRole != null && !clientActorRole.isBlank() ? clientActorRole : "APPROVER";
        return new ActorInfo(null, fallbackName, fallbackRole);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getApprovals(String role, String status, String type, String schoolId, String masterProgrammeId) {
        System.out.println("[ApprovalService] getApprovals called | role: " + role + " | status: " + status + " | type: " + type + " | schoolId: " + schoolId + " | masterProgrammeId: " + masterProgrammeId);
        CurrentUserScope scope = getScope();
        List<ApprovalRequest> list = approvalRequestRepository.findAll();
        list.forEach(this::resolveMissingScopeFields);

        if (scope != null && !scope.isAdmin() && !scope.isIqac()) {
            if (scope.isDirector()) {
                String reqSchool = scope.getRequiredSchoolId();
                list = list.stream().filter(a -> reqSchool.equalsIgnoreCase(a.getSchoolId())).toList();
            } else if (scope.isHod()) {
                String reqSchool = scope.getRequiredSchoolId();
                String reqDept = scope.getRequiredDepartmentId();
                list = list.stream().filter(a -> reqSchool.equalsIgnoreCase(a.getSchoolId()) && reqDept.equalsIgnoreCase(a.getDepartmentId())).toList();
            } else if (scope.isProgrammeCoordinator()) {
                String reqProg = scope.getRequiredMasterProgrammeId();
                list = list.stream().filter(a -> {
                    String pId = a.getMasterProgrammeId() != null ? a.getMasterProgrammeId() : a.getMasterProgrammeId();
                    return reqProg.equalsIgnoreCase(pId);
                }).toList();
            } else if (scope.isFaculty()) {
                list = list.stream().filter(a -> {
                    String batchCourseId = a.getProgrammeBatchCourseId() != null ? a.getProgrammeBatchCourseId() : a.getProgrammeBatchCourseId();
                    if (batchCourseId == null) return false;
                    ProgrammeBatchCourse off = programmeBatchCourseRepository.findById(batchCourseId).orElse(null);
                    if (off == null) return false;
                    boolean isCoord = (off.getCourseCoordinatorId() != null && off.getCourseCoordinatorId().equals(scope.getUserId()));
                    return isCoord || (off.getAssignedFaculty() != null && (off.getAssignedFaculty().contains(scope.getEmail()) || off.getAssignedFaculty().contains(scope.getName())));
                }).toList();
            }
        } else {
            if (schoolId != null && !schoolId.isBlank()) {
                list = list.stream().filter(a -> schoolId.equalsIgnoreCase(a.getSchoolId())).toList();
            }
            if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
                list = list.stream().filter(a -> {
                    String pId = a.getMasterProgrammeId() != null ? a.getMasterProgrammeId() : a.getMasterProgrammeId();
                    return masterProgrammeId.equalsIgnoreCase(pId);
                }).toList();
            }
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
                list = list.stream().filter(a -> a.getType() == ApprovalType.OUTCOME_FRAMEWORK || a.getType() == ApprovalType.PROGRAMME_ATR || a.getType() == ApprovalType.DIRECTOR_GOVERNANCE).toList();
            } else if ("HOD".equalsIgnoreCase(role)) {
                list = list.stream().filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION || a.getType() == ApprovalType.PO_PSO_TARGETS || a.getType() == ApprovalType.PROGRAMME_ATR).toList();
            }
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getDirectorApprovals(String schoolId) {
        System.out.println("[ApprovalService] getDirectorApprovals called | schoolId: " + schoolId);
        CurrentUserScope scope = getScope();
        String targetSchool = schoolId;
        if (scope != null && scope.isDirector()) {
            targetSchool = scope.getRequiredSchoolId();
            if (schoolId != null && !schoolId.isBlank() && !schoolId.equalsIgnoreCase(targetSchool)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot view approvals of a different school.");
            }
        } else if (scope != null && !scope.isAdmin() && !scope.isIqac()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only Director, IQAC, or Admin can access director approvals.");
        }

        List<ApprovalRequest> list = targetSchool != null
                ? approvalRequestRepository.findBySchoolId(targetSchool)
                : approvalRequestRepository.findAll();
        list.forEach(this::resolveMissingScopeFields);
        return list;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getHodApprovals(String masterProgrammeId) {
        System.out.println("[ApprovalService] getHodApprovals called | masterProgrammeId: " + masterProgrammeId);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isHod()) {
            String deptId = scope.getRequiredDepartmentId();
            List<MasterProgramme> deptProgrammes = masterProgrammeRepository.findByDepartmentId(deptId);
            List<String> deptProgIds = deptProgrammes.stream().map(MasterProgramme::getId).toList();

            if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
                if (!deptProgIds.contains(masterProgrammeId.trim())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme does not belong to your department.");
                }
                return approvalRequestRepository.findByMasterProgrammeId(masterProgrammeId.trim());
            }
            List<ApprovalRequest> list = approvalRequestRepository.findAll().stream()
                    .filter(a -> {
                        String pId = a.getMasterProgrammeId() != null ? a.getMasterProgrammeId() : a.getMasterProgrammeId();
                        return (pId != null && deptProgIds.contains(pId)) || (deptId.equalsIgnoreCase(a.getDepartmentId()));
                    })
                    .toList();
            list.forEach(this::resolveMissingScopeFields);
            return list;
        } else if (scope != null && scope.isProgrammeCoordinator()) {
            String progId = scope.getRequiredMasterProgrammeId();
            if (masterProgrammeId != null && !masterProgrammeId.isBlank() && !masterProgrammeId.trim().equalsIgnoreCase(progId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your scope.");
            }
            return approvalRequestRepository.findByMasterProgrammeId(progId);
        } else if (scope != null && !scope.isAdmin() && !scope.isIqac() && !scope.isDirector()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Role is not authorized for HOD approvals.");
        }

        if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
            return approvalRequestRepository.findByMasterProgrammeId(masterProgrammeId);
        }
        return approvalRequestRepository.findAll();
    }

    @Transactional
    public ApprovalRequest actionRequest(String id, String action, String comments, String clientActorName, String clientActorRole) {
        System.out.println("[ApprovalService] actionRequest called | id: " + id + " | action: " + action);
        if ("APPROVE".equalsIgnoreCase(action) || "APPROVED".equalsIgnoreCase(action)) {
            return approveRequest(id, clientActorName, clientActorRole);
        } else if ("REJECT".equalsIgnoreCase(action) || "REJECTED".equalsIgnoreCase(action) || "REQUEST_REVISION".equalsIgnoreCase(action) || "NEEDS_REVISION".equalsIgnoreCase(action)) {
            return rejectRequest(id, comments != null ? comments : "Revision requested.", clientActorName, clientActorRole);
        } else {
            throw new BadRequestException("Invalid approval action: " + action + ". Allowed: APPROVE, REJECT, REQUEST_REVISION");
        }
    }

    @Transactional
    public ApprovalRequest submitApprovalRequest(ApprovalRequest request) {
        System.out.println("[ApprovalService] submitApprovalRequest called | type: " + (request != null ? request.getType() : "null") + " | resourceId: " + (request != null ? request.getResourceId() : "null"));
        if (request == null) {
            throw new BadRequestException("Approval request payload cannot be null.");
        }
        if (request.getId() == null || request.getId().isBlank()) {
            request.setId("app-" + UUID.randomUUID().toString().substring(0, 8));
        }

        ActorInfo actor = resolveActorInfo(request.getSubmittedBy(), "SUBMITTER");
        request.setSubmittedBy(actor.actorName());

        resolveMissingScopeFields(request);
        CurrentUserScope scope = getScope();
        if (scope != null && !scope.isAdmin() && !scope.isIqac()) {
            if (request.getSchoolId() == null && scope.hasSchoolScope()) {
                request.setSchoolId(scope.getSchoolId());
            }
            if (request.getDepartmentId() == null && scope.hasDepartmentScope()) {
                request.setDepartmentId(scope.getDepartmentId());
            }
            if (request.getMasterProgrammeId() == null && scope.hasProgrammeScope()) {
                request.setMasterProgrammeId(scope.getMasterProgrammeId());
            }
            enforceApprovalScope(request);
        }

        request.setStatus(request.getStatus() != null ? request.getStatus() : ApprovalStatus.PENDING);
        request.setSubmittedAt(ZonedDateTime.now());
        request.setUpdatedAt(ZonedDateTime.now());
        ApprovalRequest saved = approvalRequestRepository.save(request);
        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.SUBMIT, com.dypiu.nba.audit.ResourceType.APPROVAL_REQUEST, saved.getId(), "DRAFT", "PENDING", "Submitted for review", java.util.Map.of("type", saved.getType() != null ? saved.getType().name() : "", "title", saved.getTitle() != null ? saved.getTitle() : ""));
        }

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(saved.getId())
                .actorId(actor.actorId())
                .actorName(actor.actorName())
                .actorRole(actor.actorRole())
                .action("SUBMITTED")
                .comments("Submitted for review")
                .build());

        return saved;
    }

    @Transactional
    public ApprovalRequest approveRequest(String id, String clientActorName, String clientActorRole) {
        System.out.println("[ApprovalService] approveRequest called | id: " + id);
        ApprovalRequest req = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));

        enforceApprovalScope(req);
        enforceRoleApprovalAuthority(req.getType());

        if (req.getStatus() == ApprovalStatus.APPROVED) {
            return req;
        }
        if (req.getStatus() == ApprovalStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot approve a rejected request directly. It must be resubmitted first.");
        }

        preventSelfApproval(req);

        ActorInfo actor = resolveActorInfo(clientActorName, clientActorRole);
        req.setStatus(ApprovalStatus.APPROVED);
        req.setApprovedBy(actor.actorName());
        req.setApprovedAt(ZonedDateTime.now());
        req.setUpdatedAt(ZonedDateTime.now());
        ApprovalRequest updated = approvalRequestRepository.save(req);
        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.APPROVE, com.dypiu.nba.audit.ResourceType.APPROVAL_REQUEST, updated.getId(), "PENDING", "APPROVED", "Approved successfully", java.util.Map.of("type", updated.getType() != null ? updated.getType().name() : ""));
        }

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(updated.getId())
                .actorId(actor.actorId())
                .actorName(actor.actorName())
                .actorRole(actor.actorRole())
                .action("APPROVED")
                .comments("Approved successfully")
                .build());

        return updated;
    }

    @Transactional
    public ApprovalRequest rejectRequest(String id, String remarks, String clientActorName, String clientActorRole) {
        System.out.println("[ApprovalService] rejectRequest called | id: " + id);
        ApprovalRequest req = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));

        enforceApprovalScope(req);
        enforceRoleApprovalAuthority(req.getType());

        ActorInfo actor = resolveActorInfo(clientActorName, clientActorRole);
        req.setStatus(ApprovalStatus.REVISION_REQUESTED);
        req.setRemarks(remarks);
        req.setApprovedBy(actor.actorName());
        req.setUpdatedAt(ZonedDateTime.now());
        ApprovalRequest updated = approvalRequestRepository.save(req);
        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.REQUEST_REVISION, com.dypiu.nba.audit.ResourceType.APPROVAL_REQUEST, updated.getId(), "PENDING", "REVISION_REQUESTED", remarks != null ? remarks : "Revision requested", java.util.Map.of("type", updated.getType() != null ? updated.getType().name() : ""));
        }

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(updated.getId())
                .actorId(actor.actorId())
                .actorName(actor.actorName())
                .actorRole(actor.actorRole())
                .action("REVISION_REQUESTED")
                .comments(remarks)
                .build());

        return updated;
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getApprovalById(String id) {
        System.out.println("[ApprovalService] getApprovalById called | id: " + id);
        ApprovalRequest req = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));
        enforceApprovalScope(req);
        return req;
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistory> getApprovalHistory(String approvalRequestId) {
        System.out.println("[ApprovalService] getApprovalHistory called | approvalRequestId: " + approvalRequestId);
        ApprovalRequest req = approvalRequestRepository.findById(approvalRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + approvalRequestId));
        enforceApprovalScope(req);
        return approvalHistoryRepository.findByApprovalRequestId(approvalRequestId);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPendingApprovals(String role, String schoolId, String masterProgrammeId) {
        System.out.println("[ApprovalService] getPendingApprovals called | role: " + role + " | schoolId: " + schoolId + " | masterProgrammeId: " + masterProgrammeId);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            return getDirectorApprovals(scope.getRequiredSchoolId()).stream()
                    .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                    .toList();
        } else if (scope != null && scope.isHod()) {
            return getHodApprovals(masterProgrammeId).stream()
                    .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                    .toList();
        } else if (scope != null && scope.isProgrammeCoordinator()) {
            return getHodApprovals(scope.getRequiredMasterProgrammeId()).stream()
                    .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                    .toList();
        }
        return approvalRequestRepository.findByStatus(ApprovalStatus.PENDING);
    }

    private static final java.util.Comparator<ApprovalRequest> LATEST_APPROVAL_COMPARATOR = (a, b) -> {
        ZonedDateTime ta = a.getUpdatedAt() != null ? a.getUpdatedAt() : (a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getCreatedAt());
        ZonedDateTime tb = b.getUpdatedAt() != null ? b.getUpdatedAt() : (b.getSubmittedAt() != null ? b.getSubmittedAt() : b.getCreatedAt());
        if (ta != null && tb != null) {
            int cmp = ta.compareTo(tb);
            if (cmp != 0) return cmp;
        }
        if (ta == null && tb != null) return -1;
        if (ta != null && tb == null) return 1;
        if (a.getId() != null && b.getId() != null) {
            return a.getId().compareTo(b.getId());
        }
        return 0;
    };

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getVerificationStatus(String key) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (key == null || key.isBlank()) return result;

        String lowerKey = key.trim().toLowerCase();
        CurrentUserScope scope = getScope();

        if (lowerKey.startsWith("allocation")) {
            String progId = key.replace("allocation-", "").replace("allocation_", "").replace("allocation", "");
            if (scope != null && scope.isProgrammeCoordinator() && !progId.equalsIgnoreCase(scope.getRequiredMasterProgrammeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your scope.");
            }
            ApprovalRequest req = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION && (progId.equalsIgnoreCase(a.getMasterProgrammeId()) || progId.equalsIgnoreCase(a.getMasterProgrammeId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);
            result.put("allocationStatus", req != null && req.getStatus() != null ? req.getStatus().name() : "DRAFT");
            result.put("allocationRemarks", req != null && req.getRemarks() != null ? req.getRemarks() : "");
            result.put("verifiedBy", req != null && req.getApprovedBy() != null ? req.getApprovedBy() : "");
        } else if (lowerKey.startsWith("targets") || lowerKey.startsWith("po-pso-targets") || lowerKey.startsWith("po_pso_targets")) {
            String progId = key.replace("po-pso-targets-", "").replace("po_pso_targets-", "").replace("po-pso-targets_", "").replace("po_pso_targets_", "").replace("targets-", "").replace("targets_", "").replace("targets", "");
            if (scope != null && scope.isProgrammeCoordinator() && !progId.equalsIgnoreCase(scope.getRequiredMasterProgrammeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your scope.");
            }
            ApprovalRequest req = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.PO_PSO_TARGETS && (progId.equalsIgnoreCase(a.getMasterProgrammeId()) || progId.equalsIgnoreCase(a.getMasterProgrammeId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);
            result.put("poPsoTargetsStatus", req != null && req.getStatus() != null ? req.getStatus().name() : "DRAFT");
            result.put("poPsoTargetsRemarks", req != null && req.getRemarks() != null ? req.getRemarks() : "");
            result.put("verifiedBy", req != null && req.getApprovedBy() != null ? req.getApprovedBy() : "");
        } else if (lowerKey.startsWith("prog-atr") || lowerKey.startsWith("programme-atr") || lowerKey.startsWith("prog_atr")) {
            String batchOrProgId = key.replace("prog-atr-", "").replace("programme-atr-", "").replace("prog_atr-", "").replace("prog_atr_", "").replace("prog-atr", "").replace("programme-atr", "");
            if (scope != null && scope.isProgrammeCoordinator() && !batchOrProgId.equalsIgnoreCase(scope.getRequiredMasterProgrammeId())) {
                // If it's programmeBatchId or progId, allow if matches PC scope
            }
            com.dypiu.nba.entity.ProgrammeAtr patr = programmeAtrRepository.findAll().stream()
                    .filter(p -> batchOrProgId.equalsIgnoreCase(p.getProgrammeBatchId()) || key.equalsIgnoreCase(p.getId()))
                    .max(java.util.Comparator.comparing(com.dypiu.nba.entity.ProgrammeAtr::getUpdatedAt, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                    .orElse(null);

            ApprovalRequest patrReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.PROGRAMME_ATR && (batchOrProgId.equalsIgnoreCase(a.getProgrammeBatchId()) || batchOrProgId.equalsIgnoreCase(a.getMasterProgrammeId()) || key.equalsIgnoreCase(a.getResourceId())))
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
            String batchCourseId = key;

            com.dypiu.nba.entity.AttainmentConfiguration config = configRepository.findByProgrammeBatchCourseId(batchCourseId).orElse(null);
            ApprovalRequest configReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.ATTAINMENT_CONFIGURATION && (batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);

            ApprovalRequest coReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> (a.getType() == ApprovalType.CO_DEFINITION || a.getType() == ApprovalType.CO_TARGETS) && (batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);

            ApprovalRequest atrReq = approvalRequestRepository.findAll().stream()
                    .filter(a -> a.getType() == ApprovalType.COURSE_ATR && (batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || key.equalsIgnoreCase(a.getResourceId())))
                    .max(LATEST_APPROVAL_COMPARATOR)
                    .orElse(null);

            List<com.dypiu.nba.entity.CourseAtr> atrs = courseAtrRepository.findByProgrammeBatchCourseId(batchCourseId);
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
    public java.util.Map<String, Object> verifyStatus(String key, String statusType, String statusValue, String remarksValue, String clientVerifierName) {
        System.out.println("[ApprovalService] verifyStatus called | key: " + key + " | statusType: " + statusType + " | statusValue: " + statusValue);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isFaculty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Faculty members do not have approval or verification authority.");
        }

        ActorInfo actor = resolveActorInfo(clientVerifierName, "VERIFIER");
        String verifierName = actor.actorName();

        ApprovalStatus status;
        try {
            status = ApprovalStatus.valueOf(statusValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            status = "APPROVED".equalsIgnoreCase(statusValue) || "VERIFIED".equalsIgnoreCase(statusValue) ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;
        }
        ApprovalType type = ApprovalType.OTHER;
        String masterProgrammeId = null;
        String masterCourseId = null;
        String programmeBatchCourseId = null;
        String schoolId = null;
        String departmentId = null;

        if ("allocationStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ALLOCATION;
            masterProgrammeId = key != null ? key.replace("allocation-", "").replace("allocation_", "").replace("allocation", "") : null;
            if (scope != null && scope.isProgrammeCoordinator()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme Coordinator cannot self-verify course allocation (must be verified by HOD or Director).");
            }
            if (masterProgrammeId != null) {
                MasterProgramme p = masterProgrammeRepository.findById(masterProgrammeId).orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + key));
                departmentId = p.getDepartmentId();
                if (scope != null && scope.isHod() && !departmentId.equalsIgnoreCase(scope.getRequiredDepartmentId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme belongs to a different department.");
                }
            }
        } else if ("poPsoTargetsStatus".equalsIgnoreCase(statusType) || "targetsStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PO_PSO_TARGETS;
            masterProgrammeId = key != null ? key.replace("po-pso-targets-", "").replace("po_pso_targets-", "").replace("targets-", "").replace("targets_", "").replace("targets", "") : null;
            if (masterProgrammeId != null) {
                MasterProgramme p = masterProgrammeRepository.findById(masterProgrammeId).orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + key));
                departmentId = p.getDepartmentId();
                if (scope != null && scope.isHod() && !departmentId.equalsIgnoreCase(scope.getRequiredDepartmentId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme belongs to a different department.");
                }
            }
        } else if ("configStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.ATTAINMENT_CONFIGURATION;
            programmeBatchCourseId = key;
            com.dypiu.nba.entity.AttainmentConfiguration cfg = configRepository.findByProgrammeBatchCourseId(key).orElse(null);
            if (cfg != null) {
                if (status == ApprovalStatus.APPROVED) {
                    cfg.setStatus(com.dypiu.nba.entity.AttainmentConfigStatus.APPROVED);
                } else if (status == ApprovalStatus.REJECTED || status == ApprovalStatus.REVISION_REQUESTED) {
                    cfg.setStatus(com.dypiu.nba.entity.AttainmentConfigStatus.REVISION_REQUESTED);
                } else {
                    cfg.setStatus(com.dypiu.nba.entity.AttainmentConfigStatus.DRAFT);
                }
                configRepository.save(cfg);
            }
        } else if ("coStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.CO_DEFINITION;
            programmeBatchCourseId = key;
        } else if ("atrStatus".equalsIgnoreCase(statusType) || "courseAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ATR;
            programmeBatchCourseId = key;
            List<com.dypiu.nba.entity.CourseAtr> atrs = courseAtrRepository.findByProgrammeBatchCourseId(key);
            for (com.dypiu.nba.entity.CourseAtr a : atrs) {
                if (status == ApprovalStatus.APPROVED) {
                    a.setStatus(com.dypiu.nba.entity.CourseAtrStatus.APPROVED);
                } else if (status == ApprovalStatus.REJECTED || status == ApprovalStatus.REVISION_REQUESTED) {
                    a.setStatus(com.dypiu.nba.entity.CourseAtrStatus.REVISION_REQUESTED);
                } else {
                    a.setStatus(com.dypiu.nba.entity.CourseAtrStatus.DRAFT);
                }
                a.setVerifiedBy(verifierName);
                a.setVerificationComments(remarksValue);
                courseAtrRepository.save(a);
            }
        } else if ("programmeAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PROGRAMME_ATR;
            masterProgrammeId = key != null ? key.replace("prog-atr-", "").replace("programme-atr-", "").replace("prog_atr-", "").replace("prog_atr_", "").replace("prog-atr", "").replace("programme-atr", "") : null;
            String targetProgrammeBatchId = masterProgrammeId;
            com.dypiu.nba.entity.ProgrammeAtr patr = programmeAtrRepository.findByProgrammeBatchId(targetProgrammeBatchId).orElse(null);
            if (patr != null) {
                if (status == ApprovalStatus.APPROVED) {
                    patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.APPROVED);
                } else if (status == ApprovalStatus.REJECTED || status == ApprovalStatus.REVISION_REQUESTED) {
                    patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.REVISION_REQUESTED);
                } else {
                    patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.DRAFT);
                }
                patr.setVerifiedBy(verifierName);
                patr.setVerificationComments(remarksValue);
                programmeAtrRepository.save(patr);
            }
        }

        enforceRoleApprovalAuthority(type);

        final ApprovalType finalType = type;
        final String finalProgId = masterProgrammeId;
        final String finalBatchCourseId = programmeBatchCourseId;
        ApprovalRequest req = approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == finalType && (key.equalsIgnoreCase(a.getResourceId()) || (finalBatchCourseId != null && finalBatchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId())) || (finalProgId != null && finalProgId.equalsIgnoreCase(a.getMasterProgrammeId()))))
                .max(LATEST_APPROVAL_COMPARATOR)
                .orElse(null);

        if (req == null) {
            req = ApprovalRequest.builder()
                    .id("app-" + UUID.randomUUID().toString().substring(0, 8))
                    .type(type)
                    .resourceId(key)
                    .masterProgrammeId(masterProgrammeId)
                    .departmentId(departmentId)
                    .schoolId(schoolId)
                    .masterCourseId(masterCourseId)
                    .programmeBatchCourseId(programmeBatchCourseId)
                    .submittedBy(verifierName)
                    .submittedAt(ZonedDateTime.now())
                    .build();
        }

        req.setTitle(statusType + " for " + key);
        req.setStatus(status);
        req.setApprovedBy(verifierName);
        req.setApprovedAt(ZonedDateTime.now());
        req.setUpdatedAt(ZonedDateTime.now());
        req.setRemarks(remarksValue);

        resolveMissingScopeFields(req);
        enforceApprovalScope(req);
        approvalRequestRepository.save(req);
        if (auditLogService != null) {
            if (status == ApprovalStatus.APPROVED) {
                auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.APPROVE, com.dypiu.nba.audit.ResourceType.APPROVAL_REQUEST, key, "PENDING", "APPROVED", remarksValue != null ? remarksValue : "Verified successfully", java.util.Map.of("statusType", statusType != null ? statusType : "", "approvalRequestId", req.getId()));
            } else {
                auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.REQUEST_REVISION, com.dypiu.nba.audit.ResourceType.APPROVAL_REQUEST, key, "PENDING", "REVISION_REQUESTED", remarksValue != null ? remarksValue : "Revision requested", java.util.Map.of("statusType", statusType != null ? statusType : "", "approvalRequestId", req.getId()));
            }
        }

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(req.getId())
                .actorId(actor.actorId())
                .actorName(actor.actorName())
                .actorRole(actor.actorRole())
                .action("VERIFIED")
                .comments(remarksValue != null ? remarksValue : "Verified successfully")
                .build());

        return getVerificationStatus(key);
    }

    @Transactional
    public java.util.Map<String, Object> requestRevisionStatus(String key, String statusType, String statusValue, String remarksValue, String clientVerifierName) {
        System.out.println("[ApprovalService] requestRevisionStatus called | key: " + key + " | statusType: " + statusType + " | remarks: " + remarksValue);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isFaculty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Faculty members do not have approval or revision authority.");
        }

        ActorInfo actor = resolveActorInfo(clientVerifierName, "REVIEWER");
        String verifierName = actor.actorName();

        ApprovalType type = ApprovalType.OTHER;
        String masterProgrammeId = null;
        String masterCourseId = null;
        String programmeBatchCourseId = null;
        String schoolId = null;
        String departmentId = null;

        if ("allocationStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ALLOCATION;
            masterProgrammeId = key != null ? key.replace("allocation-", "").replace("allocation_", "").replace("allocation", "") : null;
            if (masterProgrammeId != null) {
                MasterProgramme p = masterProgrammeRepository.findById(masterProgrammeId).orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + key));
                departmentId = p.getDepartmentId();
                if (scope != null && scope.isHod() && !departmentId.equalsIgnoreCase(scope.getRequiredDepartmentId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme belongs to a different department.");
                }
            }
        } else if ("poPsoTargetsStatus".equalsIgnoreCase(statusType) || "targetsStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PO_PSO_TARGETS;
            masterProgrammeId = key != null ? key.replace("po-pso-targets-", "").replace("po_pso_targets-", "").replace("targets-", "").replace("targets_", "").replace("targets", "") : null;
            if (masterProgrammeId != null) {
                MasterProgramme p = masterProgrammeRepository.findById(masterProgrammeId).orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + key));
                departmentId = p.getDepartmentId();
                if (scope != null && scope.isHod() && !departmentId.equalsIgnoreCase(scope.getRequiredDepartmentId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme belongs to a different department.");
                }
            }
        } else if ("configStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.ATTAINMENT_CONFIGURATION;
            programmeBatchCourseId = key;
            com.dypiu.nba.entity.AttainmentConfiguration cfg = configRepository.findByProgrammeBatchCourseId(key).orElse(null);
            if (cfg != null) {
                cfg.setStatus(com.dypiu.nba.entity.AttainmentConfigStatus.REVISION_REQUESTED);
                configRepository.save(cfg);
            }
        } else if ("coStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.CO_DEFINITION;
            programmeBatchCourseId = key;
        } else if ("atrStatus".equalsIgnoreCase(statusType) || "courseAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.COURSE_ATR;
            programmeBatchCourseId = key;
            List<com.dypiu.nba.entity.CourseAtr> atrs = courseAtrRepository.findByProgrammeBatchCourseId(key);
            for (com.dypiu.nba.entity.CourseAtr a : atrs) {
                a.setStatus(com.dypiu.nba.entity.CourseAtrStatus.REVISION_REQUESTED);
                a.setVerificationComments(remarksValue);
                courseAtrRepository.save(a);
            }
        } else if ("programmeAtrStatus".equalsIgnoreCase(statusType)) {
            type = ApprovalType.PROGRAMME_ATR;
            masterProgrammeId = key != null ? key.replace("prog-atr-", "").replace("programme-atr-", "").replace("prog_atr-", "").replace("prog_atr_", "").replace("prog-atr", "").replace("programme-atr", "") : null;
            String targetProgrammeBatchId = masterProgrammeId;
            com.dypiu.nba.entity.ProgrammeAtr patr = programmeAtrRepository.findByProgrammeBatchId(targetProgrammeBatchId).orElse(null);
            if (patr != null) {
                patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.REVISION_REQUESTED);
                patr.setVerificationComments(remarksValue);
                programmeAtrRepository.save(patr);
            }
        }

        enforceRoleApprovalAuthority(type);

        final ApprovalType finalType = type;
        final String finalProgId = masterProgrammeId;
        final String finalBatchCourseId = programmeBatchCourseId;
        ApprovalRequest req = approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == finalType && (key.equalsIgnoreCase(a.getResourceId()) || (finalBatchCourseId != null && finalBatchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId())) || (finalProgId != null && finalProgId.equalsIgnoreCase(a.getMasterProgrammeId()))))
                .max(LATEST_APPROVAL_COMPARATOR)
                .orElse(null);

        if (req == null) {
            req = ApprovalRequest.builder()
                    .id("app-" + UUID.randomUUID().toString().substring(0, 8))
                    .type(type)
                    .resourceId(key)
                    .masterProgrammeId(masterProgrammeId)
                    .departmentId(departmentId)
                    .schoolId(schoolId)
                    .masterCourseId(masterCourseId)
                    .programmeBatchCourseId(programmeBatchCourseId)
                    .submittedBy(verifierName)
                    .submittedAt(ZonedDateTime.now())
                    .build();
        }

        req.setTitle("Revision requested for " + statusType + " on " + key);
        req.setStatus(ApprovalStatus.REVISION_REQUESTED);
        req.setApprovedBy(verifierName);
        req.setApprovedAt(ZonedDateTime.now());
        req.setUpdatedAt(ZonedDateTime.now());
        req.setRemarks(remarksValue);

        resolveMissingScopeFields(req);
        enforceApprovalScope(req);
        approvalRequestRepository.save(req);
        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.REQUEST_REVISION, com.dypiu.nba.audit.ResourceType.APPROVAL_REQUEST, key, "PENDING", "REVISION_REQUESTED", remarksValue != null ? remarksValue : "Revision requested", java.util.Map.of("statusType", statusType != null ? statusType : "", "approvalRequestId", req.getId()));
        }

        approvalHistoryRepository.save(ApprovalHistory.builder()
                .id("aph-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequestId(req.getId())
                .actorId(actor.actorId())
                .actorName(actor.actorName())
                .actorRole(actor.actorRole())
                .action("REVISION_REQUESTED")
                .comments(remarksValue)
                .build());

        return getVerificationStatus(key);
    }

    public boolean isAllocationApproved(String masterProgrammeId) {
        if (masterProgrammeId == null || masterProgrammeId.isBlank()) return false;
        String progId = masterProgrammeId.replace("allocation-", "").replace("allocation_", "").replace("allocation", "");
        return approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION && (progId.equalsIgnoreCase(a.getMasterProgrammeId()) || progId.equalsIgnoreCase(a.getMasterProgrammeId()) || masterProgrammeId.equalsIgnoreCase(a.getResourceId())))
                .max(LATEST_APPROVAL_COMPARATOR)
                .map(a -> a.getStatus() == ApprovalStatus.APPROVED)
                .orElse(false);
    }

    public boolean isPoPsoTargetsApproved(String masterProgrammeId) {
        if (masterProgrammeId == null || masterProgrammeId.isBlank()) return false;
        String progId = masterProgrammeId.replace("po-pso-targets-", "").replace("po_pso_targets-", "").replace("po-pso-targets_", "").replace("po_pso_targets_", "").replace("targets-", "").replace("targets_", "").replace("targets", "");
        return approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.PO_PSO_TARGETS && (progId.equalsIgnoreCase(a.getMasterProgrammeId()) || progId.equalsIgnoreCase(a.getMasterProgrammeId()) || masterProgrammeId.equalsIgnoreCase(a.getResourceId())))
                .max(LATEST_APPROVAL_COMPARATOR)
                .map(a -> a.getStatus() == ApprovalStatus.APPROVED)
                .orElse(false);
    }

    public boolean isProgrammeAtrApproved(String batchOrProgId) {
        if (batchOrProgId == null || batchOrProgId.isBlank()) return false;
        String id = batchOrProgId.replace("prog-atr-", "").replace("programme-atr-", "").replace("prog_atr-", "").replace("prog_atr_", "").replace("prog-atr", "").replace("programme-atr", "");
        ApprovalRequest patrReq = approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.PROGRAMME_ATR && (id.equalsIgnoreCase(a.getProgrammeBatchId()) || id.equalsIgnoreCase(a.getMasterProgrammeId()) || batchOrProgId.equalsIgnoreCase(a.getResourceId())))
                .max(LATEST_APPROVAL_COMPARATOR)
                .orElse(null);
        if (patrReq != null) return patrReq.getStatus() == ApprovalStatus.APPROVED;
        return programmeAtrRepository.findByProgrammeBatchId(id)
                .map(p -> p.getStatus() == ProgrammeAtrStatus.APPROVED)
                .orElse(false);
    }

    public boolean isAttainmentConfigApproved(String batchCourseId) {
        if (batchCourseId == null || batchCourseId.isBlank()) return false;
        ApprovalRequest configReq = approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.ATTAINMENT_CONFIGURATION && (batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getResourceId())))
                .max(LATEST_APPROVAL_COMPARATOR)
                .orElse(null);
        if (configReq != null) return configReq.getStatus() == ApprovalStatus.APPROVED;
        return configRepository.findByProgrammeBatchCourseId(batchCourseId)
                .map(c -> c.getStatus() == AttainmentConfigStatus.APPROVED)
                .orElse(false);
    }

    public boolean isCoDefinitionApproved(String batchCourseId) {
        if (batchCourseId == null || batchCourseId.isBlank()) return false;
        ApprovalRequest coReq = approvalRequestRepository.findAll().stream()
                .filter(a -> (a.getType() == ApprovalType.CO_DEFINITION || a.getType() == ApprovalType.CO_TARGETS) && (batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getResourceId())))
                .max(LATEST_APPROVAL_COMPARATOR)
                .orElse(null);
        return coReq != null && coReq.getStatus() == ApprovalStatus.APPROVED;
    }

    public boolean isCourseAtrApproved(String batchCourseId) {
        if (batchCourseId == null || batchCourseId.isBlank()) return false;
        ApprovalRequest atrReq = approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.COURSE_ATR && (batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getProgrammeBatchCourseId()) || batchCourseId.equalsIgnoreCase(a.getResourceId())))
                .max(LATEST_APPROVAL_COMPARATOR)
                .orElse(null);
        if (atrReq != null) return atrReq.getStatus() == ApprovalStatus.APPROVED;
        List<CourseAtr> atrs = courseAtrRepository.findByProgrammeBatchCourseId(batchCourseId);
        return !atrs.isEmpty() && atrs.stream().allMatch(a -> a.getStatus() == CourseAtrStatus.APPROVED);
    }
}
