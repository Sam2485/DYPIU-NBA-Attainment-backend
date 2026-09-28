package com.dypiu.nba.service;

import com.dypiu.nba.controller.UserController;
import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.AssignmentRequestDto;
import com.dypiu.nba.dto.UserDto;
import com.dypiu.nba.dto.UserOrganizationalAssignmentDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IqacUserManagementIntegrationTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserOrganizationalAssignmentService assignmentService;

    @Autowired
    private UserOrganizationalAssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MasterProgrammeRepository masterProgrammeRepository;

    private School schoolA;
    private School schoolB;
    private Department deptA;
    private Department deptB;
    private MasterProgramme progA;
    private MasterProgramme progB;
    private User iqacAdmin;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
        masterProgrammeRepository.deleteAll();
        departmentRepository.deleteAll();
        schoolRepository.deleteAll();

        schoolA = schoolRepository.save(School.builder()
                .id("sch-eng-001")
                .name("School of Engineering")
                .code("SOE")
                .build());

        schoolB = schoolRepository.save(School.builder()
                .id("sch-mgmt-002")
                .name("School of Management")
                .code("SOM")
                .build());

        deptA = departmentRepository.save(Department.builder()
                .id("dept-cse-001")
                .name("Computer Science and Engineering")
                .code("CSE")
                .schoolId(schoolA.getId())
                .build());

        deptB = departmentRepository.save(Department.builder()
                .id("dept-fin-002")
                .name("Finance Management")
                .code("FIN")
                .schoolId(schoolB.getId())
                .build());

        progA = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-btech-cse")
                .name("B.Tech Computer Science")
                .code("BT-CSE")
                .departmentId(deptA.getId())
                .build());

        progB = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-mba-fin")
                .name("MBA Finance")
                .code("MBA-FIN")
                .departmentId(deptB.getId())
                .build());

        iqacAdmin = userRepository.save(User.builder()
                .username("iqac_admin")
                .email("iqac@dypiu.ac.in")
                .name("IQAC Administrator")
                .passwordHash("hashed_pwd")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        authenticateAs(iqacAdmin);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    // -------------------------------------------------------------------------
    // Scenario 1: Create new user with one school
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 1: Create new user with one school creates 1 user identity and 1 assignment")
    void testCreateNewUserWithOneSchool() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Dr. Alice");
        body.put("email", "alice@dypiu.ac.in");
        body.put("password", "Pass@1234");
        body.put("role", "DIRECTOR");
        body.put("schoolId", schoolA.getId());

        ResponseEntity<ApiResponse<UserDto>> response = userController.createUser(body);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());

        UserDto created = response.getBody().getData();
        assertNotNull(created.getId());
        assertEquals("Dr. Alice", created.getName());
        assertEquals("alice@dypiu.ac.in", created.getEmail());

        // Verify database: exactly 1 User record
        long userCount = userRepository.count();
        // iqacAdmin + Dr. Alice = 2
        assertEquals(2, userCount);

        // Verify assignments: exactly 1 organizational assignment
        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(created.getId());
        assertEquals(1, assignments.size());
        assertEquals("DIRECTOR", assignments.get(0).getRole());
        assertEquals(schoolA.getId(), assignments.get(0).getSchoolId());
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Create new user with multiple schools
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 2: Create new user with multiple schools creates 1 user identity and 2 assignments")
    void testCreateNewUserWithMultipleSchools() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Dr. Bob MultiSchool");
        body.put("email", "bob@dypiu.ac.in");
        body.put("password", "Pass@1234");
        body.put("role", "DIRECTOR");
        body.put("schools", List.of(schoolA.getId(), schoolB.getId()));

        ResponseEntity<ApiResponse<UserDto>> response = userController.createUser(body);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());

        UserDto created = response.getBody().getData();
        assertEquals(1, userRepository.findByEmail("bob@dypiu.ac.in").stream().count());

        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(created.getId());
        assertEquals(2, assignments.size());
        Set<String> assignedSchoolIds = Set.of(assignments.get(0).getSchoolId(), assignments.get(1).getSchoolId());
        assertTrue(assignedSchoolIds.contains(schoolA.getId()));
        assertTrue(assignedSchoolIds.contains(schoolB.getId()));
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Create one user with multiple roles
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 3: Create one user with multiple roles creates 1 user identity and multiple assignments")
    void testCreateOneUserWithMultipleRoles() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Dr. Charlie MultiRole");
        body.put("email", "charlie@dypiu.ac.in");
        body.put("password", "Pass@1234");
        body.put("assignments", List.of(
                Map.of("role", "DIRECTOR", "schoolId", schoolA.getId()),
                Map.of("role", "FACULTY", "schoolId", schoolB.getId(), "departmentId", deptB.getId(), "masterProgrammeId", progB.getId())
        ));

        ResponseEntity<ApiResponse<UserDto>> response = userController.createUser(body);
        assertNotNull(response.getBody());
        UserDto created = response.getBody().getData();

        // 1 user identity
        assertEquals(1, userRepository.findByEmail("charlie@dypiu.ac.in").stream().count());

        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(created.getId());
        assertEquals(2, assignments.size());
        Set<String> roles = Set.of(assignments.get(0).getRole(), assignments.get(1).getRole());
        assertTrue(roles.contains("DIRECTOR"));
        assertTrue(roles.contains("FACULTY"));
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Same school with multiple roles
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 4: Same school with multiple roles creates separate assignments without collapsing")
    void testSameSchoolWithMultipleRoles() {
        User user = userRepository.save(User.builder()
                .name("Dr. Dave")
                .email("dave@dypiu.ac.in")
                .username("dave")
                .passwordHash("pwd")
                .role(UserRole.DIRECTOR)
                .schoolId(schoolA.getId())
                .isActive(true)
                .build());

        // Assignment 1: Director in School A
        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        // Assignment 2: HOD in School A (Dept A)
        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("HOD")
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .build());

        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertEquals(2, assignments.size());

        boolean hasDirector = assignments.stream().anyMatch(a -> "DIRECTOR".equals(a.getRole()) && schoolA.getId().equals(a.getSchoolId()));
        boolean hasHod = assignments.stream().anyMatch(a -> "HOD".equals(a.getRole()) && schoolA.getId().equals(a.getSchoolId()) && deptA.getId().equals(a.getDepartmentId()));
        assertTrue(hasDirector);
        assertTrue(hasHod);
    }

    // -------------------------------------------------------------------------
    // Scenario 5: HOD with department scope
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 5: HOD requires department scope and validates school belonging")
    void testHodWithDepartmentScope() {
        User user = userRepository.save(User.builder()
                .name("Dr. Eve")
                .email("eve@dypiu.ac.in")
                .username("eve")
                .passwordHash("pwd")
                .role(UserRole.HOD)
                .isActive(true)
                .build());

        // Valid HOD assignment
        UserOrganizationalAssignmentDto dto = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("HOD")
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .build());
        assertNotNull(dto);
        assertEquals(deptA.getId(), dto.getDepartmentId());

        // Invalid: missing department
        assertThrows(BadRequestException.class, () -> {
            assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                    .role("HOD")
                    .schoolId(schoolA.getId())
                    .build());
        });

        // Invalid: department belongs to School B but schoolId specified is School A
        assertThrows(BadRequestException.class, () -> {
            assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                    .role("HOD")
                    .schoolId(schoolA.getId())
                    .departmentId(deptB.getId()) // deptB belongs to schoolB!
                    .build());
        });
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Programme Coordinator with programme scope
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 6: Programme Coordinator requires programme scope")
    void testProgrammeCoordinatorWithProgrammeScope() {
        User user = userRepository.save(User.builder()
                .name("Dr. Frank")
                .email("frank@dypiu.ac.in")
                .username("frank")
                .passwordHash("pwd")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .isActive(true)
                .build());

        // Valid PC assignment
        UserOrganizationalAssignmentDto dto = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("PROGRAMME_COORDINATOR")
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .masterProgrammeId(progA.getId())
                .build());
        assertNotNull(dto);
        assertEquals(progA.getId(), dto.getMasterProgrammeId());

        // Missing programme throws BadRequestException
        assertThrows(BadRequestException.class, () -> {
            assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                    .role("PROGRAMME_COORDINATOR")
                    .schoolId(schoolA.getId())
                    .departmentId(deptA.getId())
                    .build());
        });
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Faculty with programme scope
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 7: Faculty assignment with school and optional department/programme")
    void testFacultyWithProgrammeScope() {
        User user = userRepository.save(User.builder()
                .name("Dr. Grace")
                .email("grace@dypiu.ac.in")
                .username("grace")
                .passwordHash("pwd")
                .role(UserRole.FACULTY)
                .isActive(true)
                .build());

        UserOrganizationalAssignmentDto dto = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("FACULTY")
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .masterProgrammeId(progA.getId())
                .build());
        assertNotNull(dto);
        assertEquals("FACULTY", dto.getRole());
        assertEquals(schoolA.getId(), dto.getSchoolId());
        assertEquals(deptA.getId(), dto.getDepartmentId());
        assertEquals(progA.getId(), dto.getMasterProgrammeId());
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Add existing user without creating duplicate identity
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 8: Add existing user extends access without duplicate user creation")
    void testAddExistingUserWithoutCreatingDuplicateIdentity() {
        // Pre-existing user with School A Director
        User existing = userRepository.save(User.builder()
                .name("Dr. Heidi")
                .email("heidi@dypiu.ac.in")
                .username("heidi")
                .passwordHash("pwd")
                .role(UserRole.DIRECTOR)
                .schoolId(schoolA.getId())
                .isActive(true)
                .build());

        assignmentService.addAssignment(existing.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        // IQAC calls createUser with the exact same email to extend access to School B
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Dr. Heidi");
        body.put("email", "heidi@dypiu.ac.in");
        body.put("role", "DIRECTOR");
        body.put("schoolId", schoolB.getId());

        ResponseEntity<ApiResponse<UserDto>> response = userController.createUser(body);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertThat(response.getBody().getMessage()).contains("Existing user found");

        // Identity must remain ONE
        List<User> matchedUsers = userRepository.findAll().stream()
                .filter(u -> "heidi@dypiu.ac.in".equalsIgnoreCase(u.getEmail()))
                .toList();
        assertEquals(1, matchedUsers.size());
        assertEquals(existing.getId(), matchedUsers.get(0).getId());

        // Now has 2 assignments
        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(existing.getId());
        assertEquals(2, assignments.size());
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Add new assignment to existing user
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 9: Add new assignment to existing user via assignment service")
    void testAddNewAssignmentToExistingUser() {
        User user = userRepository.save(User.builder()
                .name("Dr. Ivan")
                .email("ivan@dypiu.ac.in")
                .username("ivan")
                .passwordHash("pwd")
                .role(UserRole.DIRECTOR)
                .schoolId(schoolA.getId())
                .isActive(true)
                .build());

        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        // Add second assignment
        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("HOD")
                .schoolId(schoolB.getId())
                .departmentId(deptB.getId())
                .build());

        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertEquals(2, assignments.size());
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Duplicate assignment is rejected/idempotent
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 10: Duplicate assignment is idempotent and does not create extra records")
    void testDuplicateAssignmentIsIdempotent() {
        User user = userRepository.save(User.builder()
                .name("Dr. Jack")
                .email("jack@dypiu.ac.in")
                .username("jack")
                .passwordHash("pwd")
                .role(UserRole.DIRECTOR)
                .schoolId(schoolA.getId())
                .isActive(true)
                .build());

        // 1st call
        UserOrganizationalAssignmentDto first = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        // 2nd identical call
        UserOrganizationalAssignmentDto second = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        assertEquals(first.getId(), second.getId(), "Must return existing assignment ID");
        List<UserOrganizationalAssignment> assignments = assignmentRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertEquals(1, assignments.size(), "Must not create duplicate assignment row");
    }

    // -------------------------------------------------------------------------
    // Scenario 11: Remove assignment without deleting user
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 11: Remove assignment deletes assignment only; user identity remains active")
    void testRemoveAssignmentWithoutDeletingUser() {
        User user = userRepository.save(User.builder()
                .name("Dr. Karen")
                .email("karen@dypiu.ac.in")
                .username("karen")
                .passwordHash("pwd")
                .role(UserRole.DIRECTOR)
                .schoolId(schoolA.getId())
                .isActive(true)
                .build());

        UserOrganizationalAssignmentDto a1 = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        UserOrganizationalAssignmentDto a2 = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolB.getId())
                .build());

        assertEquals(2, assignmentRepository.findByUserIdAndIsActiveTrue(user.getId()).size());

        // Remove School A assignment
        assignmentService.removeAssignment(a1.getId());

        // Verify assignment deleted
        List<UserOrganizationalAssignment> remaining = assignmentRepository.findByUserIdAndIsActiveTrue(user.getId());
        assertEquals(1, remaining.size());
        assertEquals(schoolB.getId(), remaining.get(0).getSchoolId());

        // Verify User identity is STILL active and untouched
        User foundUser = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(foundUser);
        assertTrue(foundUser.getIsActive());
        assertEquals("karen@dypiu.ac.in", foundUser.getEmail());
    }

    // -------------------------------------------------------------------------
    // Scenario 12: Search by secondary school assignment
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 12: User with primary School A and secondary School B can be found via School B")
    void testSearchBySecondarySchoolAssignment() {
        User user = userRepository.save(User.builder()
                .name("Dr. Leo")
                .email("leo@dypiu.ac.in")
                .username("leo")
                .passwordHash("pwd")
                .role(UserRole.DIRECTOR)
                .schoolId(schoolA.getId())
                .isActive(true)
                .build());

        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolB.getId())
                .build());

        ResponseEntity<ApiResponse<List<UserDto>>> res = userController.getUsers(null);
        assertNotNull(res.getBody());
        List<UserDto> users = res.getBody().getData();

        UserDto leoDto = users.stream().filter(u -> "leo@dypiu.ac.in".equals(u.getEmail())).findFirst().orElse(null);
        assertNotNull(leoDto);
        assertTrue(leoDto.getSchoolIds().contains(schoolB.getId()));
        assertTrue(leoDto.getSchoolNames().contains(schoolB.getName()));
    }

    // -------------------------------------------------------------------------
    // Scenario 13: Search by secondary role assignment
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 13: User with primary Director and secondary HOD has both roles in roles list")
    void testSearchBySecondaryRoleAssignment() {
        User user = userRepository.save(User.builder()
                .name("Dr. Mona")
                .email("mona@dypiu.ac.in")
                .username("mona")
                .passwordHash("pwd")
                .role(UserRole.DIRECTOR)
                .schoolId(schoolA.getId())
                .isActive(true)
                .build());

        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());

        assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("HOD")
                .schoolId(schoolB.getId())
                .departmentId(deptB.getId())
                .build());

        ResponseEntity<ApiResponse<List<UserDto>>> res = userController.getUsers(null);
        List<UserDto> users = res.getBody().getData();

        UserDto monaDto = users.stream().filter(u -> "mona@dypiu.ac.in".equals(u.getEmail())).findFirst().orElse(null);
        assertNotNull(monaDto);
        assertTrue(monaDto.getRoles().contains("DIRECTOR"));
        assertTrue(monaDto.getRoles().contains("HOD"));
    }

    // -------------------------------------------------------------------------
    // Scenario 14: IQAC can manage users across all schools
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 14: IQAC can add and modify assignments in any school without school restriction")
    void testIqacCanManageUsersAcrossAllSchools() {
        User user = userRepository.save(User.builder()
                .name("Dr. Nathan")
                .email("nathan@dypiu.ac.in")
                .username("nathan")
                .passwordHash("pwd")
                .role(UserRole.FACULTY)
                .isActive(true)
                .build());

        // IQAC adds assignment in School A
        ResponseEntity<ApiResponse<UserOrganizationalAssignmentDto>> r1 = userController.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolA.getId())
                .build());
        assertTrue(r1.getBody().isSuccess());

        // IQAC adds assignment in School B
        ResponseEntity<ApiResponse<UserOrganizationalAssignmentDto>> r2 = userController.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("DIRECTOR")
                .schoolId(schoolB.getId())
                .build());
        assertTrue(r2.getBody().isSuccess());
        assertEquals(2, assignmentRepository.findByUserIdAndIsActiveTrue(user.getId()).size());
    }

    // -------------------------------------------------------------------------
    // Scenario 15: IQAC does not require school scope
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 15: IQAC assignments are institution-wide and do not require school scope")
    void testIqacDoesNotRequireSchoolScope() {
        User user = userRepository.save(User.builder()
                .name("Dr. Olivia IQAC")
                .email("olivia@dypiu.ac.in")
                .username("olivia")
                .passwordHash("pwd")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        UserOrganizationalAssignmentDto dto = assignmentService.addAssignment(user.getId(), AssignmentRequestDto.builder()
                .role("IQAC")
                .schoolId(null)
                .build());

        assertNotNull(dto);
        assertEquals("IQAC", dto.getRole());
        assertNull(dto.getSchoolId());
    }

    // -------------------------------------------------------------------------
    // Scenario 16: Existing IQAC user remains valid
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 16: Existing IQAC user without schoolId functions cleanly and displays institution-wide")
    void testExistingIqacUserRemainsValid() {
        ResponseEntity<ApiResponse<UserDto>> response = userController.getUserById(iqacAdmin.getId());
        assertNotNull(response.getBody());
        UserDto dto = response.getBody().getData();

        assertTrue(dto.getRoles().contains("IQAC"));
        assertTrue(dto.getSchoolIds().isEmpty() || dto.getSchoolId() == null);
        assertTrue(dto.getIsActive());
    }

    // -------------------------------------------------------------------------
    // Scenario 17: Existing single-school users continue working
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 17: Legacy single-school users without assignment rows are seamlessly enriched")
    void testExistingSingleSchoolUsersContinueWorking() {
        User legacyUser = userRepository.save(User.builder()
                .name("Dr. Peter Legacy")
                .email("peter@dypiu.ac.in")
                .username("peter")
                .passwordHash("pwd")
                .role(UserRole.FACULTY)
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .masterProgrammeId(progA.getId())
                .isActive(true)
                .build());

        // Zero rows in user_organizational_assignments
        assertEquals(0, assignmentRepository.findByUserIdAndIsActiveTrue(legacyUser.getId()).size());

        ResponseEntity<ApiResponse<UserDto>> response = userController.getUserById(legacyUser.getId());
        assertNotNull(response.getBody());
        UserDto dto = response.getBody().getData();

        assertEquals(schoolA.getId(), dto.getSchoolId());
        assertTrue(dto.getSchoolIds().contains(schoolA.getId()));
        assertTrue(dto.getSchoolNames().contains(schoolA.getName()));
        assertTrue(dto.getRoles().contains("FACULTY"));
    }
}
