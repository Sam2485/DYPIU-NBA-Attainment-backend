package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.UserDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private CurrentUserScope getScope() {
        try {
            return currentUserScopeService.getCurrentUserScope();
        } catch (Exception e) {
            return null;
        }
    }

    private void enforceUserScope(User targetUser) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.<List<UserDto>>builder()
                .success(true)
                .data(academicService.getUsersByRole(role))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody Map<String, Object> body) {
        String email = body.get("email") != null ? body.get("email").toString().trim() : "";
        if (email.isBlank()) {
            throw new BadRequestException("Email address is required.");
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

        // Check uniqueness
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email address '" + email + "' is already registered to another user.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken.");
        }

        // Validate and resolve organizational scope
        ResolvedScope scope = validateAndResolveScope(body, role);

        CurrentUserScope currentScope = getScope();
        String finalSchoolId = scope.schoolId;
        String finalDeptId = scope.departmentId;
        String finalProgId = scope.programmeId;

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
                .programmeId(finalProgId)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.CREATE, com.dypiu.nba.audit.ResourceType.USER, String.valueOf(saved.getId()), null, "ACTIVE", "Created User " + saved.getName(), java.util.Map.of("username", saved.getUsername(), "role", saved.getRole() != null ? saved.getRole().name() : ""));
        }

        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .message("Academic member registered successfully.")
                .data(toDto(saved))
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

        // Validate and resolve organizational scope
        ResolvedScope scope = validateAndResolveScope(body, user.getRole());

        CurrentUserScope currentScope = getScope();
        String finalSchoolId = scope.schoolId != null
                ? scope.schoolId
                : (body.containsKey("schoolId") && body.get("schoolId") == null ? null : user.getSchoolId());
        String finalDeptId = scope.departmentId != null
                ? scope.departmentId
                : (body.containsKey("departmentId") && body.get("departmentId") == null ? null : user.getDepartmentId());
        String finalProgId = scope.programmeId != null
                ? scope.programmeId
                : (body.containsKey("programmeId") && body.get("programmeId") == null ? null : user.getProgrammeId());

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
        user.setProgrammeId(finalProgId);

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        enforceUserScope(user);

        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .data(toDto(user))
                .build());
    }

    private record ResolvedScope(String schoolId, String departmentId, String programmeId) {}

    private ResolvedScope validateAndResolveScope(Map<String, Object> body, UserRole role) {
        String rawSchoolId = (body.get("schoolId") != null && !body.get("schoolId").toString().isBlank())
                ? body.get("schoolId").toString().trim()
                : null;
        String rawDeptId = (body.get("departmentId") != null && !body.get("departmentId").toString().isBlank())
                ? body.get("departmentId").toString().trim()
                : null;
        String rawProgId = (body.get("programmeId") != null && !body.get("programmeId").toString().isBlank())
                ? body.get("programmeId").toString().trim()
                : null;

        String schoolId = rawSchoolId;
        String departmentId = rawDeptId;
        String programmeId = rawProgId;

        // 1. Validate School if provided
        if (schoolId != null) {
            if (!schoolRepository.existsById(schoolId)) {
                // Check if schoolId matches code or name
                Optional<School> matched = schoolRepository.findAll().stream()
                        .filter(s -> (s.getCode() != null && s.getCode().equalsIgnoreCase(rawSchoolId))
                                || (s.getName() != null && s.getName().equalsIgnoreCase(rawSchoolId))
                                || (s.getId() != null && s.getId().equalsIgnoreCase(rawSchoolId)))
                        .findFirst();
                if (matched.isPresent()) {
                    schoolId = matched.get().getId();
                } else {
                    throw new BadRequestException("Invalid School: School with ID '" + rawSchoolId + "' does not exist.");
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

            // Check School-Department relationship integrity
            if (dept.getSchoolId() != null) {
                if (schoolId != null && !dept.getSchoolId().equals(schoolId)) {
                    throw new BadRequestException("Department '" + dept.getName() + "' does not belong to the selected School.");
                }
                // If schoolId was not set, automatically resolve it from the department
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

            programmeId = prog.getId();

            // Check Department-MasterProgramme relationship integrity
            if (prog.getDepartmentId() != null) {
                if (departmentId != null && !prog.getDepartmentId().equals(departmentId)) {
                    throw new BadRequestException("MasterProgramme '" + prog.getName() + "' does not belong to the selected Department.");
                }
                // If departmentId was not set, automatically resolve it from the programme
                departmentId = prog.getDepartmentId();

                // If schoolId was not set, resolve from department
                final String finalDeptId = departmentId;
                if (schoolId == null) {
                    schoolId = departmentRepository.findById(finalDeptId)
                            .map(Department::getSchoolId)
                            .orElse(null);
                }
            }
        }

        return new ResolvedScope(schoolId, departmentId, programmeId);
    }

    private UserDto toDto(User user) {
        String deptName = null;
        if (user.getDepartmentId() != null) {
            deptName = departmentRepository.findById(user.getDepartmentId())
                    .map(Department::getName)
                    .orElse(user.getDepartmentId());
        }

        String progName = null;
        if (user.getProgrammeId() != null) {
            progName = masterProgrammeRepository.findById(user.getProgrammeId())
                    .map(MasterProgramme::getName)
                    .orElse(user.getProgrammeId());
        }

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : "FACULTY")
                .schoolId(user.getSchoolId())
                .departmentId(user.getDepartmentId())
                .masterProgrammeId(user.getProgrammeId())
                .department(deptName)
                .programme(progName)
                .build();
    }
}

