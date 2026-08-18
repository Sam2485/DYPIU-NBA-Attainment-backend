package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.UserDto;
import com.dypiu.nba.entity.Department;
import com.dypiu.nba.entity.Programme;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.DepartmentRepository;
import com.dypiu.nba.repository.ProgrammeRepository;
import com.dypiu.nba.repository.SchoolRepository;
import com.dypiu.nba.repository.UserRepository;
import com.dypiu.nba.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AcademicService academicService;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final PasswordEncoder passwordEncoder;

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

        User user = User.builder()
                .email(email)
                .username(username)
                .name(name)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .schoolId(scope.schoolId)
                .departmentId(scope.departmentId)
                .programmeId(scope.programmeId)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);

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

        user.setEmail(email);
        user.setName(name);
        user.setUsername(username);
        user.setSchoolId(scope.schoolId);
        user.setDepartmentId(scope.departmentId);
        user.setProgrammeId(scope.programmeId);

        User saved = userRepository.save(user);

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
                throw new BadRequestException("Invalid School: School with ID '" + schoolId + "' does not exist.");
            }
        }

        // 2. Validate Department if provided
        if (rawDeptId != null) {
            Department dept = departmentRepository.findById(rawDeptId)
                    .orElseThrow(() -> new BadRequestException("Invalid Department: Department with ID '" + rawDeptId + "' does not exist."));

            // Check School-Department relationship integrity
            if (dept.getSchoolId() != null) {
                if (schoolId != null && !dept.getSchoolId().equals(schoolId)) {
                    throw new BadRequestException("Department '" + dept.getName() + "' does not belong to the selected School.");
                }
                // If schoolId was not set, automatically resolve it from the department
                schoolId = dept.getSchoolId();
            }
        }

        // 3. Validate Programme if provided
        if (rawProgId != null) {
            Programme prog = programmeRepository.findById(rawProgId)
                    .orElseThrow(() -> new BadRequestException("Invalid Programme: Programme with ID '" + rawProgId + "' does not exist."));

            // Check Department-Programme relationship integrity
            if (prog.getDepartmentId() != null) {
                if (departmentId != null && !prog.getDepartmentId().equals(departmentId)) {
                    throw new BadRequestException("Programme '" + prog.getName() + "' does not belong to the selected Department.");
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
            progName = programmeRepository.findById(user.getProgrammeId())
                    .map(Programme::getName)
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
                .programmeId(user.getProgrammeId())
                .department(deptName)
                .programme(progName)
                .build();
    }
}

