package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.UserDto;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
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
        String email = body.get("email") != null ? body.get("email").toString() : "";
        String name = body.get("name") != null ? body.get("name").toString() : "";
        String roleStr = body.get("role") != null ? body.get("role").toString() : "FACULTY";
        String deptId = body.get("departmentId") != null ? body.get("departmentId").toString() : null;
        String progId = body.get("programmeId") != null ? body.get("programmeId").toString() : null;
        String schoolId = body.get("schoolId") != null ? body.get("schoolId").toString() : null;

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            role = UserRole.FACULTY;
        }
        final UserRole finalRole = role;

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder()
                        .email(email)
                        .username(email)
                        .name(name)
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .role(finalRole)
                        .schoolId(schoolId)
                        .departmentId(deptId)
                        .programmeId(progId)
                        .build());

        user.setName(name);
        user.setRole(role);
        if (deptId != null) user.setDepartmentId(deptId);
        if (progId != null) user.setProgrammeId(progId);
        if (schoolId != null) user.setSchoolId(schoolId);

        User saved = userRepository.save(user);

        UserDto dto = UserDto.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .name(saved.getName())
                .email(saved.getEmail())
                .role(saved.getRole() != null ? saved.getRole().name() : "FACULTY")
                .schoolId(saved.getSchoolId())
                .departmentId(saved.getDepartmentId())
                .programmeId(saved.getProgrammeId())
                .department(saved.getDepartment())
                .programme(saved.getProgramme())
                .build();

        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .message("User created successfully.")
                .data(dto)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.dypiu.nba.exception.ResourceNotFoundException("User not found: " + id));

        UserDto dto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : "FACULTY")
                .schoolId(user.getSchoolId())
                .departmentId(user.getDepartmentId())
                .programmeId(user.getProgrammeId())
                .department(user.getDepartment())
                .programme(user.getProgramme())
                .build();

        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .data(dto)
                .build());
    }
}
