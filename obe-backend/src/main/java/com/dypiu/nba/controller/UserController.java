package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.AssignmentRequestDto;
import com.dypiu.nba.dto.UserDto;
import com.dypiu.nba.dto.UserOrganizationalAssignmentDto;
import com.dypiu.nba.entity.Department;
import com.dypiu.nba.entity.MasterProgramme;
import com.dypiu.nba.entity.School;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.DepartmentRepository;
import com.dypiu.nba.repository.MasterProgrammeRepository;
import com.dypiu.nba.repository.SchoolRepository;
import com.dypiu.nba.repository.UserRepository;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.AuditLogService;
import com.dypiu.nba.service.AuthService;
import com.dypiu.nba.service.UserOrganizationalAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUserScopeService currentUserScopeService;
    private final AcademicService academicService;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final UserOrganizationalAssignmentService assignmentService;

    private CurrentUserScope getScope() {
        try {
            return currentUserScopeService.getCurrentUserScope();
        } catch (Exception e) {
            return null;
        }
    }

    private void enforceUserScope(User targetUser) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isIqac()) return;
        if (scope.isDirector()) {
            String dirSchoolId = scope.getRequiredSchoolId();
            if (targetUser.getSchoolId() != null && !targetUser.getSchoolId().equals(dirSchoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: User belongs to a different school.");
            }
        }
        if (scope.isProgrammeCoordinator()) {
            if (scope.hasDepartmentScope() && targetUser.getDepartmentId() != null && !targetUser.getDepartmentId().equals(scope.getDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: User belongs to a different department.");
            }
            if (scope.hasSchoolScope() && targetUser.getSchoolId() != null && !targetUser.getSchoolId().equals(scope.getSchoolId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: User belongs to a different school.");
            }
            return;
        }
        if (scope.isHod()) {
            String deptId = scope.getRequiredDepartmentId();
            if (targetUser.getDepartmentId() != null && !targetUser.getDepartmentId().equals(deptId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: User belongs to a different department.");
            }
            String schoolId = scope.getRequiredSchoolId();
            if (targetUser.getSchoolId() != null && !targetUser.getSchoolId().equals(schoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: User belongs to a different school.");
            }
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(java.security.Principal principal) {
        User user = currentUserScopeService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .data(toDto(user))
                .build());
    }

    @GetMapping("/me/roles")
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.UserRolesResponseDto>> getMyRoles(java.security.Principal principal) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.UserRolesResponseDto>builder()
                .success(true)
                .message("User roles and profiles retrieved successfully")
                .data(authService.getAvailableRoles(principal))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(@RequestParam(required = false) String role) {
        List<UserDto> baseDtos = academicService.getUsersByRole(role);
        List<Long> userIds = baseDtos.stream().map(UserDto::getId).filter(Objects::nonNull).toList();
        Map<Long, List<UserOrganizationalAssignmentDto>> assignmentsMap = assignmentService != null
                ? assignmentService.getAssignmentsForUsers(userIds)
                : Collections.emptyMap();

        Map<String, String> schoolCache = schoolRepository.findAll().stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(School::getId, School::getName, (a, b) -> a));

        List<UserDto> enriched = baseDtos.stream().map(dto -> {
            List<UserOrganizationalAssignmentDto> userAssignments = assignmentsMap.getOrDefault(dto.getId(), Collections.emptyList());
            Map<String, String> schoolIdToName = new LinkedHashMap<>();
            for (UserOrganizationalAssignmentDto a : userAssignments) {
                if (a.getSchoolId() != null) {
                    schoolIdToName.put(a.getSchoolId(), a.getSchoolName() != null ? a.getSchoolName() : schoolCache.getOrDefault(a.getSchoolId(), a.getSchoolId()));
                }
            }
            if (dto.getSchoolId() != null && !schoolIdToName.containsKey(dto.getSchoolId())) {
                schoolIdToName.put(dto.getSchoolId(), schoolCache.getOrDefault(dto.getSchoolId(), dto.getSchoolId()));
            }

            Set<String> allRoles = new LinkedHashSet<>();
            if (dto.getRoles() != null) {
                allRoles.addAll(dto.getRoles());
            }
            if (dto.getRole() != null) {
                allRoles.add(dto.getRole());
            }
            for (UserOrganizationalAssignmentDto a : userAssignments) {
                if (a.getRole() != null) {
                    allRoles.add(a.getRole().toUpperCase());
                }
            }

            dto.setAssignments(userAssignments);
            dto.setSchools(schoolIdToName.entrySet().stream()
                    .map(e -> Map.of("id", e.getKey(), "name", e.getValue()))
                    .collect(Collectors.toList()));
            dto.setSchoolIds(new ArrayList<>(schoolIdToName.keySet()));
            dto.setSchoolNames(new ArrayList<>(schoolIdToName.values()));
            dto.setRoles(new ArrayList<>(allRoles));
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<UserDto>>builder()
                .success(true)
                .data(enriched)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody Map<String, Object> body) {
        String email = body.get("email") != null ? body.get("email").toString().trim() : "";
        if (email.isBlank()) {
            throw new BadRequestException("Email address is required.");
        }

        // Check uniqueness or extend existing user (Requirement 10: ADD EXISTING USER for IQAC)
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            CurrentUserScope currentScope = getScope();
            if (currentScope != null && currentScope.isIqac()) {
                User existingUser = existingUserOpt.get();
                attachAssignmentsFromPayload(existingUser, body);
                List<UserOrganizationalAssignmentDto> assignments = assignmentService.getAssignmentsForUser(existingUser.getId());
                return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                        .success(true)
                        .message("Existing user found. Organizational access extended successfully.")
                        .data(toEnrichedDto(existingUser, assignments))
                        .build());
            } else {
                throw new BadRequestException("Email address '" + email + "' is already registered to another user.");
            }
        }

        String name = body.get("name") != null ? body.get("name").toString().trim() : "";
        if (name.isBlank()) {
            throw new BadRequestException("Full Name is required.");
        }

        String username = body.get("username") != null && !body.get("username").toString().isBlank()
                ? body.get("username").toString().trim()
                : (email.contains("@") ? email.split("@")[0] : email);

        String rawPassword = body.get("password") != null && !body.get("password").toString().isBlank()
                ? body.get("password").toString().trim()
                : null;
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BadRequestException("Password is required for creating a new academic user.");
        }

        String roleStr = body.get("role") != null ? body.get("role").toString() : "FACULTY";
        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            role = UserRole.FACULTY;
        }

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken.");
        }

        // Validate and resolve organizational scope
        ResolvedScope scope = validateAndResolveScope(body, role);

        CurrentUserScope currentScope = getScope();
        String finalSchoolId = scope.schoolId;
        String finalDeptId = scope.departmentId;
        String finalProgId = scope.masterProgrammeId;

        if (currentScope != null && currentScope.isDirector()) {
            finalSchoolId = currentScope.getRequiredSchoolId();
        } else if (currentScope != null && currentScope.isHod()) {
            finalSchoolId = currentScope.getRequiredSchoolId();
            finalDeptId = currentScope.getRequiredDepartmentId();
        }

        User user = User.builder()
                .email(email)
                .username(username)
                .name(name)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .schoolId(finalSchoolId)
                .departmentId(finalDeptId)
                .masterProgrammeId(finalProgId)
                .isActive(true)
                .build();

        if (body.containsKey("roles")) {
            List<String> assignedRolesList = parseRoles(body.get("roles"));
            if (assignedRolesList != null && !assignedRolesList.isEmpty()) {
                user.setRoleList(assignedRolesList);
            }
        }

        User saved = userRepository.save(user);

        // Attach assignments (multi-school or multi-assignment support)
        attachAssignmentsFromPayload(saved, body);

        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.CREATE, com.dypiu.nba.audit.ResourceType.USER, String.valueOf(saved.getId()), null, "ACTIVE", "Created User " + saved.getName(), java.util.Map.of("username", saved.getUsername(), "role", saved.getRole() != null ? saved.getRole().name() : ""));
        }

        List<UserOrganizationalAssignmentDto> assignments = assignmentService.getAssignmentsForUser(saved.getId());

        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .message("Academic member registered successfully.")
                .data(toEnrichedDto(saved, assignments))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        enforceUserScope(user);

        String email = body.get("email") != null ? body.get("email").toString().trim() : user.getEmail();
        if (email.isBlank()) {
            throw new BadRequestException("Email address cannot be empty.");
        }

        String name = body.get("name") != null ? body.get("name").toString().trim() : user.getName();
        if (name.isBlank()) {
            throw new BadRequestException("Full Name cannot be empty.");
        }

        String username = body.get("username") != null && !body.get("username").toString().isBlank()
                ? body.get("username").toString().trim()
                : user.getUsername();

        // Check if new email/username is already taken by a different user
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new BadRequestException("Email address '" + email + "' is already in use by another user.");
            }
        });
        userRepository.findByUsername(username).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new BadRequestException("Username '" + username + "' is already taken by another user.");
            }
        });

        // Update password if provided
        String rawPassword = body.get("password") != null && !body.get("password").toString().isBlank()
                ? body.get("password").toString().trim()
                : null;
        if (rawPassword != null) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        if (body.get("role") != null) {
            try {
                user.setRole(UserRole.valueOf(body.get("role").toString().toUpperCase()));
            } catch (Exception ignored) {}
        }

        if (body.containsKey("roles")) {
            List<String> assignedRolesList = parseRoles(body.get("roles"));
            user.setRoleList(assignedRolesList);
        }

        // Validate and resolve organizational scope
        ResolvedScope scope = validateAndResolveScope(body, user.getRole());

        CurrentUserScope currentScope = getScope();
        String finalSchoolId = scope.schoolId != null
                ? scope.schoolId
                : (body.containsKey("schoolId") && body.get("schoolId") == null ? null : user.getSchoolId());
        String finalDeptId = scope.departmentId != null
                ? scope.departmentId
                : (body.containsKey("departmentId") && body.get("departmentId") == null ? null : user.getDepartmentId());
        String finalProgId = scope.masterProgrammeId != null
                ? scope.masterProgrammeId
                : (body.containsKey("masterProgrammeId") && body.get("masterProgrammeId") == null ? null : user.getMasterProgrammeId());

        if (currentScope != null && currentScope.isDirector()) {
            finalSchoolId = currentScope.getRequiredSchoolId();
        } else if (currentScope != null && currentScope.isHod()) {
            finalSchoolId = currentScope.getRequiredSchoolId();
            finalDeptId = currentScope.getRequiredDepartmentId();
        }

        user.setEmail(email);
        user.setName(name);
        user.setUsername(username);
        user.setSchoolId(finalSchoolId);
        user.setDepartmentId(finalDeptId);
        user.setMasterProgrammeId(finalProgId);

        User saved = userRepository.save(user);
        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.CREATE, com.dypiu.nba.audit.ResourceType.USER, String.valueOf(saved.getId()), null, "ACTIVE", "Created User " + saved.getName(), java.util.Map.of("username", saved.getUsername(), "role", saved.getRole() != null ? saved.getRole().name() : ""));
        }

        // Sync leadership mapping if Director
        if (saved.getRole() == UserRole.DIRECTOR && finalSchoolId != null) {
            schoolRepository.findById(finalSchoolId).ifPresent(s -> {
                s.setDirectorId(saved.getId());
                s.setDirectorName(saved.getName());
                s.setDirectorEmail(saved.getEmail());
                s.setDean(saved.getName());
                s.setDeanEmail(saved.getEmail());
                schoolRepository.save(s);
            });
        }

        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .message("Academic member updated successfully.")
                .data(toDto(saved))
                .build());
    }

    @RequestMapping(value = "/{id}/roles", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<UserDto>> updateUserRoles(@PathVariable Long id, @RequestBody Object body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        enforceUserScope(user);

        List<String> assignedRolesList = parseRoles(body);
        if (assignedRolesList != null) {
            user.setRoleList(assignedRolesList);
            if (!assignedRolesList.isEmpty()) {
                try {
                    user.setRole(UserRole.valueOf(assignedRolesList.get(0).toUpperCase()));
                } catch (Exception ignored) {}
            }
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .message("User roles updated successfully.")
                .data(toDto(saved))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        enforceUserScope(user);
        List<UserOrganizationalAssignmentDto> assignments = assignmentService != null
                ? assignmentService.getAssignmentsForUser(id)
                : Collections.emptyList();

        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .data(toEnrichedDto(user, assignments))
                .build());
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<List<UserOrganizationalAssignmentDto>>> getUserAssignments(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        enforceUserScope(user);
        return ResponseEntity.ok(ApiResponse.<List<UserOrganizationalAssignmentDto>>builder()
                .success(true)
                .data(assignmentService != null ? assignmentService.getAssignmentsForUser(id) : Collections.emptyList())
                .build());
    }

    @PostMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<UserOrganizationalAssignmentDto>> addAssignment(
            @PathVariable Long id,
            @RequestBody AssignmentRequestDto request) {
        CurrentUserScope scope = getScope();
        if (scope == null || !scope.isIqac()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: IQAC authority required to manage organizational assignments.");
        }
        UserOrganizationalAssignmentDto created = assignmentService.addAssignment(id, request);
        return ResponseEntity.ok(ApiResponse.<UserOrganizationalAssignmentDto>builder()
                .success(true)
                .message("Organizational assignment saved successfully.")
                .data(created)
                .build());
    }

    @PutMapping(value = {"/assignments/{assignmentId}", "/{userId}/assignments/{assignmentId}"})
    public ResponseEntity<ApiResponse<UserOrganizationalAssignmentDto>> updateAssignment(
            @PathVariable(required = false) Long userId,
            @PathVariable Long assignmentId,
            @RequestBody AssignmentRequestDto request) {
        CurrentUserScope scope = getScope();
        if (scope == null || !scope.isIqac()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: IQAC authority required to manage organizational assignments.");
        }
        UserOrganizationalAssignmentDto updated = assignmentService.updateAssignment(assignmentId, request);
        return ResponseEntity.ok(ApiResponse.<UserOrganizationalAssignmentDto>builder()
                .success(true)
                .message("Organizational assignment updated successfully.")
                .data(updated)
                .build());
    }

    @DeleteMapping(value = {"/assignments/{assignmentId}", "/{userId}/assignments/{assignmentId}"})
    public ResponseEntity<ApiResponse<Void>> removeAssignment(
            @PathVariable(required = false) Long userId,
            @PathVariable Long assignmentId) {
        CurrentUserScope scope = getScope();
        if (scope == null || !scope.isIqac()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: IQAC authority required to manage organizational assignments.");
        }
        assignmentService.removeAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Organizational assignment removed successfully.")
                .build());
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<UserDto>> checkEmail(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(ApiResponse.<UserDto>builder().success(true).data(null).build());
        }
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.<UserDto>builder().success(true).data(null).build());
        }
        User user = userOpt.get();
        List<UserOrganizationalAssignmentDto> assignments = assignmentService != null
                ? assignmentService.getAssignmentsForUser(user.getId())
                : Collections.emptyList();
        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .data(toEnrichedDto(user, assignments))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        enforceUserScope(user);

        user.setIsActive(false);
        userRepository.save(user);

        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.DELETE, com.dypiu.nba.audit.ResourceType.USER, String.valueOf(user.getId()), "ACTIVE", "INACTIVE", "Deactivated/Deleted User " + user.getName(), java.util.Map.of("username", user.getUsername(), "role", user.getRole() != null ? user.getRole().name() : ""));
        }

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User deactivated/deleted successfully.")
                .build());
    }

    @SuppressWarnings("unchecked")
    private List<String> parseRoles(Object rolesObj) {
        if (rolesObj == null) return null;
        if (rolesObj instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
        }
        if (rolesObj instanceof Map<?, ?> map) {
            if (map.containsKey("roles")) {
                return parseRoles(map.get("roles"));
            }
        }
        if (rolesObj instanceof String str && !str.isBlank()) {
            if (str.trim().startsWith("[")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    return mapper.readValue(str, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                } catch (Exception ignored) {}
            }
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private void attachAssignmentsFromPayload(User user, Map<String, Object> body) {
        if (user == null || body == null) return;

        // 1. Explicit assignments array (Requirement 9)
        if (body.containsKey("assignments") && body.get("assignments") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String role = map.get("role") != null ? map.get("role").toString() : (user.getRole() != null ? user.getRole().name() : "FACULTY");
                    String schoolId = map.get("schoolId") != null ? map.get("schoolId").toString() : null;
                    String departmentId = map.get("departmentId") != null ? map.get("departmentId").toString() : null;
                    String masterProgrammeId = map.get("masterProgrammeId") != null ? map.get("masterProgrammeId").toString() : null;

                    assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                            .role(role)
                            .schoolId(schoolId)
                            .departmentId(departmentId)
                            .masterProgrammeId(masterProgrammeId)
                            .build());
                }
            }
            return;
        }

        // 2. Multi-school selection (Requirement 8)
        List<String> schoolIdsList = new ArrayList<>();
        if (body.containsKey("schools") && body.get("schools") instanceof List<?> list) {
            for (Object s : list) {
                if (s != null && !s.toString().isBlank()) schoolIdsList.add(s.toString().trim());
            }
        } else if (body.containsKey("schoolIds") && body.get("schoolIds") instanceof List<?> list) {
            for (Object s : list) {
                if (s != null && !s.toString().isBlank()) schoolIdsList.add(s.toString().trim());
            }
        } else if (body.get("schoolId") != null && !body.get("schoolId").toString().isBlank()) {
            schoolIdsList.add(body.get("schoolId").toString().trim());
        }

        String roleStr = body.get("role") != null ? body.get("role").toString() : (user.getRole() != null ? user.getRole().name() : "FACULTY");
        String deptId = body.get("departmentId") != null && !body.get("departmentId").toString().isBlank() ? body.get("departmentId").toString().trim() : null;
        String progId = body.get("masterProgrammeId") != null && !body.get("masterProgrammeId").toString().isBlank() ? body.get("masterProgrammeId").toString().trim() : null;

        if (!schoolIdsList.isEmpty()) {
            for (String sId : schoolIdsList) {
                assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                        .role(roleStr)
                        .schoolId(sId)
                        .departmentId(deptId)
                        .masterProgrammeId(progId)
                        .build());
            }
        } else if ("IQAC".equalsIgnoreCase(roleStr)) {
            assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                    .role("IQAC")
                    .schoolId(null)
                    .departmentId(null)
                    .masterProgrammeId(null)
                    .build());
        } else if (deptId != null || progId != null) {
            assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                    .role(roleStr)
                    .schoolId(null)
                    .departmentId(deptId)
                    .masterProgrammeId(progId)
                    .build());
        }
    }

    private record ResolvedScope(String schoolId, String departmentId, String masterProgrammeId) {}

    private ResolvedScope validateAndResolveScope(Map<String, Object> body, UserRole role) {
        String rawSchoolId = null;
        if (body.get("schoolId") != null && !body.get("schoolId").toString().isBlank()) {
            rawSchoolId = body.get("schoolId").toString().trim();
        } else if (body.get("schools") instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            rawSchoolId = list.get(0).toString().trim();
        } else if (body.get("schoolIds") instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            rawSchoolId = list.get(0).toString().trim();
        } else if (body.get("assignments") instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map && map.get("schoolId") != null) {
            rawSchoolId = map.get("schoolId").toString().trim();
        }
        String rawDeptId = (body.get("departmentId") != null && !body.get("departmentId").toString().isBlank())
                ? body.get("departmentId").toString().trim()
                : null;
        String rawProgId = (body.get("masterProgrammeId") != null && !body.get("masterProgrammeId").toString().isBlank())
                ? body.get("masterProgrammeId").toString().trim()
                : null;

        String schoolId = rawSchoolId;
        String departmentId = rawDeptId;
        String masterProgrammeId = rawProgId;

        // 1. Validate School if provided
        if (schoolId != null) {
            final String targetSchoolId = rawSchoolId;
            if (!schoolRepository.existsById(schoolId)) {
                Optional<School> matched = schoolRepository.findAll().stream()
                        .filter(s -> (s.getCode() != null && s.getCode().equalsIgnoreCase(targetSchoolId))
                                || (s.getName() != null && s.getName().equalsIgnoreCase(targetSchoolId))
                                || (s.getId() != null && s.getId().equalsIgnoreCase(targetSchoolId)))
                        .findFirst();
                if (matched.isPresent()) {
                    schoolId = matched.get().getId();
                } else {
                    throw new BadRequestException("Invalid School: School with ID '" + targetSchoolId + "' does not exist.");
                }
            }
        }

        // 2. Validate Department if provided
        if (rawDeptId != null) {
            Department dept = departmentRepository.findById(rawDeptId)
                    .orElseGet(() -> departmentRepository.findAll().stream()
                            .filter(d -> (d.getName() != null && d.getName().equalsIgnoreCase(rawDeptId))
                                    || (d.getCode() != null && d.getCode().equalsIgnoreCase(rawDeptId)))
                            .findFirst()
                            .orElseThrow(() -> new BadRequestException("Invalid Department: Department with ID '" + rawDeptId + "' does not exist.")));

            departmentId = dept.getId();

            if (dept.getSchoolId() != null) {
                if (schoolId != null && !dept.getSchoolId().equals(schoolId)) {
                    throw new BadRequestException("Department '" + dept.getName() + "' does not belong to the selected School.");
                }
                schoolId = dept.getSchoolId();
            }
        }

        // 3. Validate MasterProgramme if provided
        if (rawProgId != null) {
            MasterProgramme prog = masterProgrammeRepository.findById(rawProgId)
                    .orElseGet(() -> masterProgrammeRepository.findAll().stream()
                            .filter(p -> (p.getName() != null && p.getName().equalsIgnoreCase(rawProgId))
                                    || (p.getCode() != null && p.getCode().equalsIgnoreCase(rawProgId)))
                            .findFirst()
                            .orElseThrow(() -> new BadRequestException("Invalid Programme: MasterProgramme with ID '" + rawProgId + "' does not exist.")));

            masterProgrammeId = prog.getId();

            if (prog.getDepartmentId() != null) {
                if (departmentId != null && !prog.getDepartmentId().equals(departmentId)) {
                    throw new BadRequestException("MasterProgramme '" + prog.getName() + "' does not belong to the selected Department.");
                }
                departmentId = prog.getDepartmentId();

                final String finalDeptId = departmentId;
                if (schoolId == null) {
                    schoolId = departmentRepository.findById(finalDeptId)
                            .map(Department::getSchoolId)
                            .orElse(null);
                }
            }
        }

        return new ResolvedScope(schoolId, departmentId, masterProgrammeId);
    }

    private UserDto toDto(User user) {
        if (user == null) return null;
        List<UserOrganizationalAssignmentDto> assignments = assignmentService != null
                ? assignmentService.getAssignmentsForUser(user.getId())
                : Collections.emptyList();
        return toEnrichedDto(user, assignments);
    }

    private UserDto toEnrichedDto(User user, List<UserOrganizationalAssignmentDto> assignments) {
        String deptName = null;
        if (user.getDepartmentId() != null) {
            deptName = departmentRepository.findById(user.getDepartmentId())
                    .map(Department::getName)
                    .orElse(user.getDepartmentId());
        }

        String progName = null;
        if (user.getMasterProgrammeId() != null) {
            progName = masterProgrammeRepository.findById(user.getMasterProgrammeId())
                    .map(MasterProgramme::getName)
                    .orElse(user.getMasterProgrammeId());
        }

        List<UserOrganizationalAssignmentDto> safeAssignments = assignments != null ? assignments : Collections.emptyList();

        Map<String, String> schoolIdToName = new LinkedHashMap<>();
        for (UserOrganizationalAssignmentDto a : safeAssignments) {
            if (a.getSchoolId() != null) {
                schoolIdToName.put(a.getSchoolId(), a.getSchoolName() != null ? a.getSchoolName() : a.getSchoolId());
            }
        }
        if (schoolIdToName.isEmpty() && user.getSchoolId() != null) {
            String sName = schoolRepository.findById(user.getSchoolId()).map(School::getName).orElse(user.getSchoolId());
            schoolIdToName.put(user.getSchoolId(), sName);
        }

        Set<String> allRoles = new LinkedHashSet<>();
        allRoles.addAll(user.getRoleList());
        for (UserOrganizationalAssignmentDto a : safeAssignments) {
            if (a.getRole() != null) {
                allRoles.add(a.getRole().toUpperCase());
            }
        }
        if (allRoles.isEmpty()) {
            allRoles.add(user.getRole() != null ? user.getRole().name() : "FACULTY");
        }

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : "FACULTY")
                .roles(new ArrayList<>(allRoles))
                .schoolId(user.getSchoolId())
                .departmentId(user.getDepartmentId())
                .masterProgrammeId(user.getMasterProgrammeId())
                .department(deptName)
                .programme(progName)
                .assignments(safeAssignments)
                .schools(schoolIdToName.entrySet().stream()
                        .map(e -> Map.of("id", e.getKey(), "name", e.getValue()))
                        .collect(Collectors.toList()))
                .schoolIds(new ArrayList<>(schoolIdToName.keySet()))
                .schoolNames(new ArrayList<>(schoolIdToName.values()))
                .isActive(user.getIsActive())
                .build();
    }
}

