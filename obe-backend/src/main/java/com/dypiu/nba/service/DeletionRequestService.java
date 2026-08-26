package com.dypiu.nba.service;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.deletion.DeletionRequestStatus;
import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeletionRequestService {

    private final DeletionRequestRepository deletionRequestRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserScopeService currentUserScopeService;
    private final AuditLogService auditLogService;

    private User resolveAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        String principal = auth.getName();
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(principal, principal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user account not found."));
    }

    private CurrentUserScope getScope() {
        try {
            return currentUserScopeService.getCurrentUserScope();
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public DeletionRequestResponseDto requestDeletion(DeletionRequestCreateDto dto) {
        User user = resolveAuthenticatedUser();
        CurrentUserScope scope = getScope();

        if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: ADMIN and IQAC roles do not have academic deletion authority. Deletion must follow the academic hierarchy.");
        }

        if (dto.getResourceType() == null || dto.getResourceId() == null || dto.getResourceId().isBlank()) {
            throw new BadRequestException("resourceType and resourceId are required.");
        }

        // Duplicate pending request check
        if (deletionRequestRepository.existsByResourceTypeAndResourceIdAndStatus(dto.getResourceType(), dto.getResourceId(), DeletionRequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending deletion request already exists for this " + dto.getResourceType());
        }

        String programmeBatchId = null;
        String batchCourseId = null;
        String masterProgrammeId = null;
        String departmentId = null;
        String schoolId = null;

        if (dto.getResourceType() == ResourceType.PROGRAMME_BATCH_COURSE) {
            // Role verification: ONLY Programme Coordinator
            if (scope == null || !scope.isProgrammeCoordinator()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only Programme Coordinators can initiate ProgrammeBatchCourse deletion requests.");
            }

            ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(dto.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatchCourse not found with id: " + dto.getResourceId()));

            if (offering.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "ProgrammeBatchCourse is already deleted.");
            }

            batchCourseId = offering.getId();
            programmeBatchId = offering.getProgrammeBatchId();

            if (programmeBatchId != null) {
                ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId).orElse(null);
                if (batch != null) {
                    masterProgrammeId = batch.getMasterProgrammeId();
                    if (masterProgrammeId != null) {
                        MasterProgramme prog = masterProgrammeRepository.findById(masterProgrammeId).orElse(null);
                        if (prog != null) {
                            departmentId = prog.getDepartmentId();
                            if (departmentId != null) {
                                Department dept = departmentRepository.findById(departmentId).orElse(null);
                                if (dept != null) {
                                    schoolId = dept.getSchoolId();
                                }
                            }
                        }
                    }
                }
            }

            // Enforce PC scope
            if (masterProgrammeId != null && !masterProgrammeId.equalsIgnoreCase(scope.getRequiredMasterProgrammeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: ProgrammeBatchCourse belongs to a different programme outside your scope.");
            }

        } else if (dto.getResourceType() == ResourceType.PROGRAMME_BATCH) {
            // Role verification: ONLY HOD
            if (scope == null || !scope.isHod()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only HOD can initiate ProgrammeBatch deletion requests.");
            }

            ProgrammeBatch batch = programmeBatchRepository.findById(dto.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatch not found with id: " + dto.getResourceId()));

            if (batch.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "ProgrammeBatch is already deleted.");
            }

            programmeBatchId = batch.getId();
            masterProgrammeId = batch.getMasterProgrammeId();
            if (masterProgrammeId != null) {
                MasterProgramme prog = masterProgrammeRepository.findById(masterProgrammeId).orElse(null);
                if (prog != null) {
                    departmentId = prog.getDepartmentId();
                    if (departmentId != null) {
                        Department dept = departmentRepository.findById(departmentId).orElse(null);
                        if (dept != null) {
                            schoolId = dept.getSchoolId();
                        }
                    }
                }
            }

            // Enforce HOD scope
            if (departmentId != null && !departmentId.equalsIgnoreCase(scope.getRequiredDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: ProgrammeBatch belongs to a different department outside your scope.");
            }

        } else {
            throw new BadRequestException("Unsupported deletion resourceType: " + dto.getResourceType() + ". Deletion requests only support PROGRAMME_BATCH and PROGRAMME_BATCH_COURSE.");
        }

        DeletionRequest req = DeletionRequest.builder()
                .resourceType(dto.getResourceType())
                .resourceId(dto.getResourceId())
                .programmeBatchId(programmeBatchId)
                .programmeBatchCourseId(batchCourseId)
                .masterProgrammeId(masterProgrammeId)
                .departmentId(departmentId)
                .schoolId(schoolId)
                .status(DeletionRequestStatus.PENDING)
                .requestedBy(user.getName())
                .requestedById(String.valueOf(user.getId()))
                .requestedByRole(user.getRole().name())
                .requestedAt(ZonedDateTime.now())
                .remarks(dto.getRemarks())
                .build();

        DeletionRequest saved = deletionRequestRepository.save(req);

        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    AuditAction.DELETE_REQUESTED,
                    dto.getResourceType(),
                    dto.getResourceId(),
                    null,
                    "PENDING",
                    dto.getRemarks() != null ? dto.getRemarks() : "Deletion requested",
                    Map.of("deletionRequestId", saved.getId(), "requestedByRole", user.getRole().name())
            );
        }

        return mapToDto(saved);
    }

    @Transactional
    public DeletionRequestResponseDto rejectDeletion(Long id, DeletionRejectDto dto) {
        User reviewer = resolveAuthenticatedUser();
        CurrentUserScope scope = getScope();

        if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: ADMIN and IQAC roles cannot review or reject deletion requests.");
        }

        DeletionRequest req = deletionRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request not found with id: " + id));

        if (req.getStatus() != DeletionRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deletion request has already been processed with status: " + req.getStatus());
        }

        // Self-approval prevention
        if (Objects.equals(req.getRequestedById(), String.valueOf(reviewer.getId())) || Objects.equals(req.getRequestedBy(), reviewer.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Requesters cannot review or reject their own deletion requests.");
        }

        // Authorization checks according to strict hierarchy
        if (req.getResourceType() == ResourceType.PROGRAMME_BATCH_COURSE) {
            if (scope == null || !scope.isHod()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only HOD can review ProgrammeBatchCourse deletion requests.");
            }
            if (req.getDepartmentId() != null && !req.getDepartmentId().equalsIgnoreCase(scope.getRequiredDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Deletion request is outside your department scope.");
            }
        } else if (req.getResourceType() == ResourceType.PROGRAMME_BATCH) {
            if (scope == null || !scope.isDirector()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only Director can review ProgrammeBatch deletion requests.");
            }
            if (req.getSchoolId() != null && !req.getSchoolId().equalsIgnoreCase(scope.getRequiredSchoolId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Deletion request is outside your school scope.");
            }
        }

        req.setStatus(DeletionRequestStatus.REJECTED);
        req.setReviewedBy(reviewer.getName());
        req.setReviewedById(String.valueOf(reviewer.getId()));
        req.setReviewedByRole(reviewer.getRole().name());
        req.setReviewedAt(ZonedDateTime.now());
        req.setRejectionReason(dto != null ? dto.getRemarks() : "Rejected by reviewer");

        DeletionRequest updated = deletionRequestRepository.save(req);

        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    AuditAction.DELETE_REJECTED,
                    req.getResourceType(),
                    req.getResourceId(),
                    "PENDING",
                    "REJECTED",
                    req.getRejectionReason(),
                    Map.of("deletionRequestId", updated.getId(), "reviewedByRole", reviewer.getRole().name())
            );
        }

        return mapToDto(updated);
    }

    @Transactional
    public DeletionRequestResponseDto executeDeletion(Long id, DeletionExecuteDto dto) {
        User reviewer = resolveAuthenticatedUser();
        CurrentUserScope scope = getScope();

        if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: ADMIN and IQAC roles cannot execute academic deletion requests.");
        }

        if (dto == null || dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password confirmation is required to execute deletion.");
        }

        DeletionRequest req = deletionRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request not found with id: " + id));

        if (req.getStatus() != DeletionRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deletion request has already been processed with status: " + req.getStatus());
        }

        // Self-approval prevention
        if (Objects.equals(req.getRequestedById(), String.valueOf(reviewer.getId())) || Objects.equals(req.getRequestedBy(), reviewer.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Requesters cannot execute their own deletion requests.");
        }

        // Authorization checks according to strict hierarchy
        if (req.getResourceType() == ResourceType.PROGRAMME_BATCH_COURSE) {
            if (scope == null || !scope.isHod()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only HOD can execute ProgrammeBatchCourse deletion.");
            }
            if (req.getDepartmentId() != null && !req.getDepartmentId().equalsIgnoreCase(scope.getRequiredDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Deletion request is outside your department scope.");
            }
        } else if (req.getResourceType() == ResourceType.PROGRAMME_BATCH) {
            if (scope == null || !scope.isDirector()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only Director can execute ProgrammeBatch deletion.");
            }
            if (req.getSchoolId() != null && !req.getSchoolId().equalsIgnoreCase(scope.getRequiredSchoolId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Deletion request is outside your school scope.");
            }
        }

        // Password verification against the authenticated user's credentials
        if (!passwordEncoder.matches(dto.getPassword(), reviewer.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid password confirmation. Deletion aborted.");
        }

        // Execute Soft-Deletion on the underlying resource
        ZonedDateTime now = ZonedDateTime.now();
        if (req.getResourceType() == ResourceType.PROGRAMME_BATCH_COURSE) {
            ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(req.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatchCourse not found: " + req.getResourceId()));
            if (offering.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "ProgrammeBatchCourse is already soft-deleted.");
            }
            offering.setDeletedAt(now);
            offering.setDeletedBy(reviewer.getEmail());
            offering.setStatus("DELETED");
            programmeBatchCourseRepository.save(offering);
        } else if (req.getResourceType() == ResourceType.PROGRAMME_BATCH) {
            ProgrammeBatch batch = programmeBatchRepository.findById(req.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatch not found: " + req.getResourceId()));
            if (batch.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "ProgrammeBatch is already soft-deleted.");
            }
            batch.setDeletedAt(now);
            batch.setDeletedBy(reviewer.getEmail());
            batch.setStatus("DELETED");
            programmeBatchRepository.save(batch);
        }

        req.setStatus(DeletionRequestStatus.EXECUTED);
        req.setReviewedBy(reviewer.getName());
        req.setReviewedById(String.valueOf(reviewer.getId()));
        req.setReviewedByRole(reviewer.getRole().name());
        req.setReviewedAt(now);
        req.setExecutedAt(now);

        DeletionRequest updated = deletionRequestRepository.save(req);

        // Audit Logging (Audit logs are permanently recorded; passwords/tokens are NEVER logged)
        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    AuditAction.DELETE_APPROVED,
                    req.getResourceType(),
                    req.getResourceId(),
                    "PENDING",
                    "APPROVED",
                    "Deletion approved after password verification",
                    Map.of("deletionRequestId", updated.getId(), "reviewedByRole", reviewer.getRole().name())
            );
            auditLogService.recordSuccess(
                    AuditAction.DELETE_EXECUTED,
                    req.getResourceType(),
                    req.getResourceId(),
                    "APPROVED",
                    "EXECUTED",
                    "Soft-deletion executed successfully",
                    Map.of("deletionRequestId", updated.getId(), "executedByRole", reviewer.getRole().name())
            );
        }

        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public List<DeletionRequestResponseDto> getDeletionRequests(DeletionRequestStatus status, ResourceType resourceType) {
        User user = resolveAuthenticatedUser();
        CurrentUserScope scope = getScope();

        List<DeletionRequest> list;
        if (scope == null || scope.isAdmin() || scope.isIqac()) {
            list = deletionRequestRepository.findAll();
        } else if (scope.isDirector()) {
            list = deletionRequestRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(scope.getRequiredSchoolId(), status != null ? status : DeletionRequestStatus.PENDING);
        } else if (scope.isHod()) {
            list = deletionRequestRepository.findByDepartmentIdAndStatusOrderByCreatedAtDesc(scope.getRequiredDepartmentId(), status != null ? status : DeletionRequestStatus.PENDING);
        } else {
            list = deletionRequestRepository.findByRequestedByIdOrderByCreatedAtDesc(String.valueOf(user.getId()));
        }

        return list.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> resourceType == null || r.getResourceType() == resourceType)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeletionRequestResponseDto getDeletionRequestById(Long id) {
        DeletionRequest req = deletionRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request not found with id: " + id));
        return mapToDto(req);
    }

    private DeletionRequestResponseDto mapToDto(DeletionRequest r) {
        return DeletionRequestResponseDto.builder()
                .id(r.getId())
                .resourceType(r.getResourceType())
                .resourceId(r.getResourceId())
                .programmeBatchId(r.getProgrammeBatchId())
                .programmeBatchCourseId(r.getProgrammeBatchCourseId())
                .masterProgrammeId(r.getMasterProgrammeId())
                .departmentId(r.getDepartmentId())
                .schoolId(r.getSchoolId())
                .status(r.getStatus())
                .requestedBy(r.getRequestedBy())
                .requestedById(r.getRequestedById())
                .requestedByRole(r.getRequestedByRole())
                .requestedAt(r.getRequestedAt())
                .reviewedBy(r.getReviewedBy())
                .reviewedById(r.getReviewedById())
                .reviewedByRole(r.getReviewedByRole())
                .reviewedAt(r.getReviewedAt())
                .executedAt(r.getExecutedAt())
                .remarks(r.getRemarks())
                .rejectionReason(r.getRejectionReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
