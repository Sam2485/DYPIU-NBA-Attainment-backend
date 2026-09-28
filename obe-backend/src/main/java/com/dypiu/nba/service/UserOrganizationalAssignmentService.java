package com.dypiu.nba.service;

import com.dypiu.nba.dto.AssignmentRequestDto;
import com.dypiu.nba.dto.UserOrganizationalAssignmentDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserOrganizationalAssignmentService {

    private final UserOrganizationalAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;

    @Transactional(readOnly = true)
    public List<UserOrganizationalAssignmentDto> getAssignmentsForUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(userId);
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, School> schoolMap = schoolRepository.findAll().stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(School::getId, s -> s, (a, b) -> a));

        Map<String, Department> deptMap = departmentRepository.findAll().stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));

        Map<String, MasterProgramme> progMap = masterProgrammeRepository.findAll().stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(MasterProgramme::getId, p -> p, (a, b) -> a));

        return assignments.stream()
                .map(a -> toDto(a, schoolMap, deptMap, progMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<UserOrganizationalAssignmentDto>> getAssignmentsForUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdInAndIsActiveTrue(userIds);
        if (assignments.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, School> schoolMap = schoolRepository.findAll().stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(School::getId, s -> s, (a, b) -> a));

        Map<String, Department> deptMap = departmentRepository.findAll().stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));

        Map<String, MasterProgramme> progMap = masterProgrammeRepository.findAll().stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(MasterProgramme::getId, p -> p, (a, b) -> a));

        Map<Long, List<UserOrganizationalAssignmentDto>> result = new HashMap<>();
        for (UserOrganizationalAssignment a : assignments) {
            result.computeIfAbsent(a.getUserId(), k -> new ArrayList<>())
                    .add(toDto(a, schoolMap, deptMap, progMap));
        }
        return result;
    }

    @Transactional
    public UserOrganizationalAssignmentDto addAssignment(Long userId, AssignmentRequestDto request) {
        if (userId == null) {
            throw new BadRequestException("User ID is required.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        ValidatedScope scope = validateAndResolveScope(request);

        // Check for duplicate assignment (Requirement 11: Duplicate assignments are idempotent)
        List<UserOrganizationalAssignment> existing = assignmentRepository.findMatchingAssignments(
                userId,
                scope.role,
                scope.schoolId,
                scope.departmentId,
                scope.masterProgrammeId
        );

        UserOrganizationalAssignment assignmentToReturn;
        if (!existing.isEmpty()) {
            UserOrganizationalAssignment match = existing.get(0);
            if (!Boolean.TRUE.equals(match.getIsActive())) {
                match.setIsActive(true);
                assignmentToReturn = assignmentRepository.save(match);
            } else {
                assignmentToReturn = match;
            }
            log.info("Duplicate assignment requested for user {} - returning existing assignment ID {}", userId, assignmentToReturn.getId());
        } else {
            UserOrganizationalAssignment newAssignment = UserOrganizationalAssignment.builder()
                    .userId(userId)
                    .role(scope.role)
                    .schoolId(scope.schoolId)
                    .departmentId(scope.departmentId)
                    .masterProgrammeId(scope.masterProgrammeId)
                    .isActive(true)
                    .build();
            assignmentToReturn = assignmentRepository.save(newAssignment);
            log.info("Added new assignment ID {} for user {}", assignmentToReturn.getId(), userId);
        }

        syncUserIdentityColumns(user);

        return enrichSingleDto(assignmentToReturn);
    }

    @Transactional
    public UserOrganizationalAssignmentDto updateAssignment(Long assignmentId, AssignmentRequestDto request) {
        if (assignmentId == null) {
            throw new BadRequestException("Assignment ID is required.");
        }
        UserOrganizationalAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        ValidatedScope scope = validateAndResolveScope(request);

        // Check if another assignment already matches these parameters for this user
        List<UserOrganizationalAssignment> existing = assignmentRepository.findMatchingAssignments(
                assignment.getUserId(),
                scope.role,
                scope.schoolId,
                scope.departmentId,
                scope.masterProgrammeId
        );

        Optional<UserOrganizationalAssignment> otherMatch = existing.stream()
                .filter(a -> !a.getId().equals(assignmentId))
                .findFirst();

        if (otherMatch.isPresent()) {
            // Merge into other match
            assignmentRepository.delete(assignment);
            User user = userRepository.findById(assignment.getUserId()).orElse(null);
            if (user != null) {
                syncUserIdentityColumns(user);
            }
            return enrichSingleDto(otherMatch.get());
        }

        assignment.setRole(scope.role);
        assignment.setSchoolId(scope.schoolId);
        assignment.setDepartmentId(scope.departmentId);
        assignment.setMasterProgrammeId(scope.masterProgrammeId);
        assignment.setIsActive(true);

        UserOrganizationalAssignment saved = assignmentRepository.save(assignment);

        User user = userRepository.findById(saved.getUserId()).orElse(null);
        if (user != null) {
            syncUserIdentityColumns(user);
        }

        return enrichSingleDto(saved);
    }

    @Transactional
    public void removeAssignment(Long assignmentId) {
        if (assignmentId == null) {
            throw new BadRequestException("Assignment ID is required.");
        }
        UserOrganizationalAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        Long userId = assignment.getUserId();
        assignmentRepository.delete(assignment);
        log.info("Removed organizational assignment ID {} for user ID {}", assignmentId, userId);

        userRepository.findById(userId).ifPresent(this::syncUserIdentityColumns);
    }

    @Transactional
    public void deactivateAssignmentsForUser(Long userId) {
        if (userId == null) {
            return;
        }
        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(userId);
        if (assignments != null && !assignments.isEmpty()) {
            for (UserOrganizationalAssignment a : assignments) {
                a.setIsActive(false);
            }
            assignmentRepository.saveAll(assignments);
            log.info("Deactivated {} assignments for user ID {}", assignments.size(), userId);
        }
    }

    @Transactional
    public void syncUserIdentityColumns(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(user.getId());
        Set<String> rolesSet = new LinkedHashSet<>();

        // If user already had explicit IQAC role, preserve it
        for (String r : user.getRoleList()) {
            if ("IQAC".equalsIgnoreCase(r)) {
                rolesSet.add("IQAC");
            }
        }

        for (UserOrganizationalAssignment a : assignments) {
            if (a.getRole() != null && !a.getRole().isBlank()) {
                rolesSet.add(a.getRole().toUpperCase());
            }
        }

        if (rolesSet.isEmpty()) {
            if (user.getRole() != null) {
                rolesSet.add(user.getRole().name());
            } else {
                rolesSet.add("FACULTY");
            }
        }

        user.setRoleList(new ArrayList<>(rolesSet));

        // Update primary role if needed
        if (!rolesSet.isEmpty()) {
            String primary = rolesSet.iterator().next();
            try {
                user.setRole(UserRole.valueOf(primary));
            } catch (Exception ignored) {}
        }

        // Keep primary school / dept / prog aligned with first relevant assignment if null or missing
        if (!assignments.isEmpty()) {
            UserOrganizationalAssignment first = assignments.get(0);
            if (user.getSchoolId() == null || assignments.stream().noneMatch(a -> Objects.equals(a.getSchoolId(), user.getSchoolId()))) {
                user.setSchoolId(first.getSchoolId());
            }
            if (user.getDepartmentId() == null || assignments.stream().noneMatch(a -> Objects.equals(a.getDepartmentId(), user.getDepartmentId()))) {
                user.setDepartmentId(first.getDepartmentId());
            }
            if (user.getMasterProgrammeId() == null || assignments.stream().noneMatch(a -> Objects.equals(a.getMasterProgrammeId(), user.getMasterProgrammeId()))) {
                user.setMasterProgrammeId(first.getMasterProgrammeId());
            }
        } else if (user.getRole() != UserRole.IQAC && !rolesSet.contains("IQAC")) {
            user.setSchoolId(null);
            user.setDepartmentId(null);
            user.setMasterProgrammeId(null);
        }

        userRepository.save(user);
    }

    private UserOrganizationalAssignmentDto enrichSingleDto(UserOrganizationalAssignment a) {
        String schoolName = null;
        if (a.getSchoolId() != null) {
            schoolName = schoolRepository.findById(a.getSchoolId()).map(School::getName).orElse(null);
        }
        String deptName = null;
        if (a.getDepartmentId() != null) {
            deptName = departmentRepository.findById(a.getDepartmentId()).map(Department::getName).orElse(null);
        }
        String progName = null;
        if (a.getMasterProgrammeId() != null) {
            progName = masterProgrammeRepository.findById(a.getMasterProgrammeId()).map(MasterProgramme::getName).orElse(null);
        }

        return UserOrganizationalAssignmentDto.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .role(a.getRole())
                .schoolId(a.getSchoolId())
                .schoolName(schoolName)
                .departmentId(a.getDepartmentId())
                .departmentName(deptName)
                .masterProgrammeId(a.getMasterProgrammeId())
                .masterProgrammeName(progName)
                .isActive(a.getIsActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    public UserOrganizationalAssignmentDto toDto(
            UserOrganizationalAssignment a,
            Map<String, School> schoolMap,
            Map<String, Department> deptMap,
            Map<String, MasterProgramme> progMap) {

        String schoolName = null;
        if (a.getSchoolId() != null && schoolMap.containsKey(a.getSchoolId())) {
            schoolName = schoolMap.get(a.getSchoolId()).getName();
        }

        String deptName = null;
        if (a.getDepartmentId() != null && deptMap.containsKey(a.getDepartmentId())) {
            deptName = deptMap.get(a.getDepartmentId()).getName();
        }

        String progName = null;
        if (a.getMasterProgrammeId() != null && progMap.containsKey(a.getMasterProgrammeId())) {
            progName = progMap.get(a.getMasterProgrammeId()).getName();
        }

        return UserOrganizationalAssignmentDto.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .role(a.getRole())
                .schoolId(a.getSchoolId())
                .schoolName(schoolName)
                .departmentId(a.getDepartmentId())
                .departmentName(deptName)
                .masterProgrammeId(a.getMasterProgrammeId())
                .masterProgrammeName(progName)
                .isActive(a.getIsActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private record ValidatedScope(String role, String schoolId, String departmentId, String masterProgrammeId) {}

    private ValidatedScope validateAndResolveScope(AssignmentRequestDto req) {
        if (req == null || req.getRole() == null || req.getRole().isBlank()) {
            throw new BadRequestException("Role is required for organizational assignment.");
        }

        String rawRole = req.getRole().trim().toUpperCase().replace("-", "_");
        String role;
        if (rawRole.equals("COURSE_COORDINATOR") || rawRole.equals("CC") || rawRole.equals("FACULTY")) {
            role = "FACULTY";
        } else if (rawRole.equals("PROGRAMME_COORDINATOR") || rawRole.equals("PC") || rawRole.equals("COORDINATOR")) {
            role = "PROGRAMME_COORDINATOR";
        } else if (rawRole.equals("HOD") || rawRole.equals("HEAD_OF_DEPARTMENT")) {
            role = "HOD";
        } else if (rawRole.equals("DIRECTOR") || rawRole.equals("DEAN")) {
            role = "DIRECTOR";
        } else if (rawRole.equals("IQAC")) {
            role = "IQAC";
        } else {
            role = rawRole;
        }

        String schoolId = req.getSchoolId() != null && !req.getSchoolId().isBlank() ? req.getSchoolId().trim() : null;
        if (schoolId == null && req.getSchoolName() != null && !req.getSchoolName().isBlank()) {
            schoolId = req.getSchoolName().trim();
        }

        if (schoolId != null && !schoolRepository.existsById(schoolId)) {
            final String targetSchoolId = schoolId;
            Optional<School> matched = schoolRepository.findAll().stream()
                    .filter(s -> (s.getId() != null && s.getId().equalsIgnoreCase(targetSchoolId))
                            || (s.getCode() != null && s.getCode().equalsIgnoreCase(targetSchoolId))
                            || (s.getName() != null && s.getName().equalsIgnoreCase(targetSchoolId)))
                    .findFirst();
            if (matched.isPresent()) {
                schoolId = matched.get().getId();
            } else {
                throw new BadRequestException("Invalid School ID: " + schoolId);
            }
        }

        String deptId = req.getDepartmentId() != null && !req.getDepartmentId().isBlank() ? req.getDepartmentId().trim() : null;
        String progId = req.getMasterProgrammeId() != null && !req.getMasterProgrammeId().isBlank() ? req.getMasterProgrammeId().trim() : null;

        // Role-specific scoping rules (Requirements 2, 7, 8, 14, 17)
        if ("IQAC".equals(role)) {
            // IQAC is institution-wide, schoolId is optional
            return new ValidatedScope("IQAC", schoolId, null, null);
        }

        if ("DIRECTOR".equals(role)) {
            if (schoolId == null) {
                throw new BadRequestException("School is required for Director assignment.");
            }
            return new ValidatedScope("DIRECTOR", schoolId, null, null);
        }

        if ("HOD".equals(role)) {
            if (deptId != null) {
                final String targetDeptId = deptId;
                Department dept = departmentRepository.findById(targetDeptId)
                        .orElseThrow(() -> new BadRequestException("Invalid Department ID: " + targetDeptId));

                if (dept.getSchoolId() != null) {
                    if (schoolId != null && !dept.getSchoolId().equalsIgnoreCase(schoolId)) {
                        throw new BadRequestException("Department '" + dept.getName() + "' does not belong to the selected School.");
                    }
                    schoolId = dept.getSchoolId();
                }
            }
            if (schoolId == null) {
                throw new BadRequestException("School is required for HOD assignment.");
            }
            return new ValidatedScope("HOD", schoolId, deptId, null);
        }

        if ("PROGRAMME_COORDINATOR".equals(role)) {
            if (progId != null) {
                final String targetProgId = progId;
                MasterProgramme prog = masterProgrammeRepository.findById(targetProgId)
                        .orElseThrow(() -> new BadRequestException("Invalid MasterProgramme ID: " + targetProgId));

                if (prog.getDepartmentId() != null) {
                    if (deptId != null && !prog.getDepartmentId().equalsIgnoreCase(deptId)) {
                        throw new BadRequestException("Programme '" + prog.getName() + "' does not belong to the selected Department.");
                    }
                    deptId = prog.getDepartmentId();
                }

                if (deptId != null) {
                    final String targetDeptId = deptId;
                    Department dept = departmentRepository.findById(targetDeptId)
                            .orElseThrow(() -> new BadRequestException("Invalid Department ID: " + targetDeptId));
                    if (dept.getSchoolId() != null) {
                        if (schoolId != null && !dept.getSchoolId().equalsIgnoreCase(schoolId)) {
                            throw new BadRequestException("Department '" + dept.getName() + "' does not belong to the selected School.");
                        }
                        schoolId = dept.getSchoolId();
                    }
                }
            }

            if (schoolId == null) {
                throw new BadRequestException("School is required for Programme Coordinator assignment.");
            }

            return new ValidatedScope("PROGRAMME_COORDINATOR", schoolId, deptId, progId);
        }

        if ("FACULTY".equals(role)) {
            if (deptId != null) {
                final String targetDeptId = deptId;
                Department dept = departmentRepository.findById(targetDeptId)
                        .orElseThrow(() -> new BadRequestException("Invalid Department ID: " + targetDeptId));
                if (dept.getSchoolId() != null) {
                    if (schoolId != null && !dept.getSchoolId().equalsIgnoreCase(schoolId)) {
                        throw new BadRequestException("Department '" + dept.getName() + "' does not belong to the selected School.");
                    }
                    schoolId = dept.getSchoolId();
                }
            }

            if (progId != null) {
                final String targetProgId = progId;
                MasterProgramme prog = masterProgrammeRepository.findById(targetProgId)
                        .orElseThrow(() -> new BadRequestException("Invalid MasterProgramme ID: " + targetProgId));
                if (deptId != null && prog.getDepartmentId() != null && !prog.getDepartmentId().equalsIgnoreCase(deptId)) {
                    throw new BadRequestException("Programme '" + prog.getName() + "' does not belong to the selected Department.");
                }
            }

            if (schoolId == null) {
                throw new BadRequestException("School is required for Faculty assignment.");
            }

            return new ValidatedScope("FACULTY", schoolId, deptId, progId);
        }

        return new ValidatedScope(role, schoolId, deptId, progId);
    }
}
