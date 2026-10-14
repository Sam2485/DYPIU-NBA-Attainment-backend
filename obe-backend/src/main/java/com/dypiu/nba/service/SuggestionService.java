package com.dypiu.nba.service;

import com.dypiu.nba.dto.CreateSuggestionRequestDto;
import com.dypiu.nba.dto.SuggestionDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final CurrentUserScopeService currentUserScopeService;

    private User getAuthenticatedUser() {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope == null || scope.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findById(scope.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /**
     * Submit an academic suggestion from an Analytics context.
     * Recipient is resolved automatically based on context and role hierarchy.
     */
    @Transactional
    public SuggestionDto createSuggestion(CreateSuggestionRequestDto req) {
        User sender = getAuthenticatedUser();
        String senderRoleStr = sender.getRole() != null ? sender.getRole().name() : "FACULTY";

        if ("COURSE_COORDINATOR".equalsIgnoreCase(senderRoleStr) || "FACULTY".equalsIgnoreCase(senderRoleStr)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course Coordinators cannot initiate hierarchical suggestions");
        }

        // Contextual entity lookups & metadata
        String schoolId = req.getSchoolId();
        String departmentId = req.getDepartmentId();
        String masterProgId = req.getMasterProgrammeId();
        String progName = null;
        String batchName = null;
        String courseName = null;

        User recipient = null;
        String recipientRole = null;

        // Routing Rule 1: Course Context -> Route to Course Coordinator
        if (req.getProgrammeBatchCourseId() != null && !req.getProgrammeBatchCourseId().isBlank()) {
            ProgrammeBatchCourse course = programmeBatchCourseRepository.findById(req.getProgrammeBatchCourseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + req.getProgrammeBatchCourseId()));

            courseName = course.getName() != null ? course.getName() : course.getCode();

            // Find coordinator user
            if (course.getCourseCoordinatorId() != null) {
                recipient = userRepository.findById(course.getCourseCoordinatorId()).orElse(null);
            }
            if (recipient == null && course.getCoordinatorEmail() != null) {
                recipient = userRepository.findByEmailIgnoreCase(course.getCoordinatorEmail().trim()).orElse(null);
            }
            if (recipient == null && course.getCourseCoordinatorName() != null) {
                recipient = userRepository.findByUsernameIgnoreCase(course.getCourseCoordinatorName().trim()).orElse(null);
            }

            recipientRole = "COURSE_COORDINATOR";

            // Populate parent context
            if (req.getProgrammeBatchId() == null || req.getProgrammeBatchId().isBlank()) {
                req.setProgrammeBatchId(course.getProgrammeBatchId());
            }
        }

        // Routing Rule 2: Programme Batch Context -> Route to Programme Coordinator
        if (recipient == null && req.getProgrammeBatchId() != null && !req.getProgrammeBatchId().isBlank()) {
            ProgrammeBatch batch = programmeBatchRepository.findById(req.getProgrammeBatchId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme batch not found: " + req.getProgrammeBatchId()));

            batchName = batch.getName() != null ? batch.getName() : "Batch " + batch.getStartYear();
            masterProgId = batch.getMasterProgrammeId();

            if (batch.getCoordinatorId() != null) {
                recipient = userRepository.findById(batch.getCoordinatorId()).orElse(null);
            }
            if (recipient == null && batch.getCoordinatorEmail() != null) {
                recipient = userRepository.findByEmailIgnoreCase(batch.getCoordinatorEmail().trim()).orElse(null);
            }

            recipientRole = "PROGRAMME_COORDINATOR";
        }

        // Routing Rule 3: Programme-level fallback -> Route to Programme Coordinator or HOD
        if (recipient == null && masterProgId != null && !masterProgId.isBlank()) {
            MasterProgramme prog = masterProgrammeRepository.findById(masterProgId).orElse(null);
            if (prog != null) {
                progName = prog.getName();
                departmentId = prog.getDepartmentId();
                if (prog.getCoordinatorEmail() != null) {
                    recipient = userRepository.findByEmailIgnoreCase(prog.getCoordinatorEmail().trim()).orElse(null);
                    recipientRole = "PROGRAMME_COORDINATOR";
                }
            }
        }

        // Routing Rule 4: Department-level fallback -> Route to HOD
        if (recipient == null && departmentId != null && !departmentId.isBlank()) {
            Department dept = departmentRepository.findById(departmentId).orElse(null);
            if (dept != null) {
                schoolId = dept.getSchoolId();
                if (dept.getHodEmail() != null) {
                    recipient = userRepository.findByEmailIgnoreCase(dept.getHodEmail().trim()).orElse(null);
                    recipientRole = "HOD";
                }
            }
        }

        // Routing Rule 5: School-level fallback -> Route to Director
        if (recipient == null && schoolId != null && !schoolId.isBlank()) {
            School school = schoolRepository.findById(schoolId).orElse(null);
            if (school != null && school.getDirectorEmail() != null) {
                recipient = userRepository.findByEmailIgnoreCase(school.getDirectorEmail().trim()).orElse(null);
                recipientRole = "DIRECTOR";
            }
        }

        if (recipient == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not resolve an assigned coordinator/head for this academic context.");
        }

        // Validate hierarchy: sender must be above recipient in the hierarchy
        validateRoleHierarchy(senderRoleStr, recipientRole);

        // Fetch display names if missing
        if (progName == null && masterProgId != null) {
            progName = masterProgrammeRepository.findById(masterProgId).map(MasterProgramme::getName).orElse(null);
        }
        if (batchName == null && req.getProgrammeBatchId() != null) {
            batchName = programmeBatchRepository.findById(req.getProgrammeBatchId()).map(ProgrammeBatch::getName).orElse(null);
        }

        Suggestion suggestion = Suggestion.builder()
                .senderId(sender.getId())
                .senderRole(senderRoleStr)
                .senderName(sender.getName())
                .recipientId(recipient.getId())
                .recipientRole(recipientRole)
                .recipientName(recipient.getName())
                .schoolId(schoolId)
                .departmentId(departmentId)
                .masterProgrammeId(masterProgId)
                .programmeName(progName)
                .programmeBatchId(req.getProgrammeBatchId())
                .batchName(batchName)
                .programmeBatchCourseId(req.getProgrammeBatchCourseId())
                .courseName(courseName)
                .contextType(req.getContextType() != null ? req.getContextType() : "PROGRAMME_BATCH")
                .category(req.getCategory() != null ? req.getCategory() : "GENERAL")
                .content(req.getContent())
                .status("DELIVERED")
                .build();

        Suggestion saved = suggestionRepository.save(suggestion);
        return toDto(saved);
    }

    /**
     * Get Received Suggestions (Inbox)
     */
    @Transactional(readOnly = true)
    public Page<SuggestionDto> getReceivedSuggestions(int page, int size) {
        User currentUser = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        return suggestionRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::toDto);
    }

    /**
     * Get Sent Suggestions (Outbox)
     */
    @Transactional(readOnly = true)
    public Page<SuggestionDto> getSentSuggestions(int page, int size) {
        User currentUser = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        return suggestionRepository.findBySenderIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::toDto);
    }

    /**
     * Mark suggestion as read
     */
    @Transactional
    public void markAsRead(String id) {
        User currentUser = getAuthenticatedUser();
        Suggestion s = suggestionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggestion not found"));

        if (!s.getRecipientId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to mark this suggestion");
        }

        if (s.getReadAt() == null) {
            s.setReadAt(ZonedDateTime.now());
            suggestionRepository.save(s);
        }
    }

    private void validateRoleHierarchy(String senderRole, String recipientRole) {
        String s = senderRole.toUpperCase();
        String r = recipientRole.toUpperCase();

        boolean allowed = false;

        switch (s) {
            case "SUPER_ADMIN":
            case "ADMIN":
            case "TRUSTEE":
            case "IQAC":
                allowed = Set.of("DIRECTOR", "SCHOOL_PROGRAMME_COORDINATOR", "HOD", "PROGRAMME_COORDINATOR", "COURSE_COORDINATOR", "FACULTY").contains(r);
                break;
            case "DIRECTOR":
            case "SCHOOL_PROGRAMME_COORDINATOR":
                allowed = Set.of("HOD", "PROGRAMME_COORDINATOR", "COURSE_COORDINATOR", "FACULTY").contains(r);
                break;
            case "HOD":
                allowed = Set.of("PROGRAMME_COORDINATOR", "COURSE_COORDINATOR", "FACULTY").contains(r);
                break;
            case "PROGRAMME_COORDINATOR":
                allowed = Set.of("COURSE_COORDINATOR", "FACULTY").contains(r);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Role " + senderRole + " is not permitted to send suggestions to " + recipientRole);
        }
    }

    private SuggestionDto toDto(Suggestion s) {
        return SuggestionDto.builder()
                .id(s.getId())
                .senderId(s.getSenderId())
                .senderRole(s.getSenderRole())
                .senderName(s.getSenderName())
                .recipientId(s.getRecipientId())
                .recipientRole(s.getRecipientRole())
                .recipientName(s.getRecipientName())
                .schoolId(s.getSchoolId())
                .departmentId(s.getDepartmentId())
                .masterProgrammeId(s.getMasterProgrammeId())
                .programmeName(s.getProgrammeName())
                .programmeBatchId(s.getProgrammeBatchId())
                .batchName(s.getBatchName())
                .programmeBatchCourseId(s.getProgrammeBatchCourseId())
                .courseName(s.getCourseName())
                .contextType(s.getContextType())
                .category(s.getCategory())
                .content(s.getContent())
                .status(s.getStatus())
                .readAt(s.getReadAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
