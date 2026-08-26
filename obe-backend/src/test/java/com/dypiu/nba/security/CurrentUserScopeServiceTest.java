package com.dypiu.nba.security;

import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
import com.dypiu.nba.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CurrentUserScopeServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserScopeService currentUserScopeService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContextUser(String usernameOrEmail, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                usernameOrEmail,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("1. Successfully resolve Director user scope with schoolId")
    void testResolveDirectorScope_Success() {
        String email = "director1@gmail.com";
        setSecurityContextUser(email, "DIRECTOR");

        User user = User.builder()
                .id(1L)
                .username("director1")
                .email(email)
                .name("Dr. Raj Shaikh")
                .role(UserRole.DIRECTOR)
                .schoolId("sch-soe-01")
                .departmentId(null)
                .masterProgrammeId(null)
                .isActive(true)
                .build();

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(email, email))
                .thenReturn(Optional.of(user));

        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();

        assertNotNull(scope);
        assertEquals(1L, scope.getUserId());
        assertEquals("director1", scope.getUsername());
        assertEquals(email, scope.getEmail());
        assertEquals("Dr. Raj Shaikh", scope.getName());
        assertEquals(UserRole.DIRECTOR, scope.getRole());
        assertTrue(scope.isDirector());
        assertFalse(scope.isHod());
        assertTrue(scope.hasSchoolScope());
        assertEquals("sch-soe-01", scope.getSchoolId());
        assertEquals("sch-soe-01", scope.getRequiredSchoolId());
        assertFalse(scope.hasDepartmentScope());
        assertFalse(scope.hasProgrammeScope());
    }

    @Test
    @DisplayName("2. Successfully resolve HOD user scope with schoolId and departmentId")
    void testResolveHodScope_Success() {
        String email = "hod1@gmail.com";
        setSecurityContextUser(email, "HOD");

        User user = User.builder()
                .id(3L)
                .username("hod1")
                .email(email)
                .name("Prof. Sam")
                .role(UserRole.HOD)
                .schoolId("sch-soe-01")
                .departmentId("dept-cse-01")
                .masterProgrammeId(null)
                .isActive(true)
                .build();

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(email, email))
                .thenReturn(Optional.of(user));

        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();

        assertNotNull(scope);
        assertEquals(3L, scope.getUserId());
        assertEquals(UserRole.HOD, scope.getRole());
        assertTrue(scope.isHod());
        assertEquals("sch-soe-01", scope.getSchoolId());
        assertEquals("dept-cse-01", scope.getDepartmentId());
        assertTrue(scope.hasDepartmentScope());
        assertEquals("dept-cse-01", scope.getRequiredDepartmentId());
        assertFalse(scope.hasProgrammeScope());
    }

    @Test
    @DisplayName("3. Successfully resolve MasterProgramme Coordinator scope with schoolId, departmentId, masterProgrammeId")
    void testResolveProgrammeCoordinatorScope_Success() {
        String email = "pc1@gmail.com";
        setSecurityContextUser(email, "PROGRAMME_COORDINATOR");

        User user = User.builder()
                .id(5L)
                .username("pc1")
                .email(email)
                .name("Prof. Prasad")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-soe-01")
                .departmentId("dept-cse-01")
                .masterProgrammeId("prog-btech-cse-01")
                .isActive(true)
                .build();

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(email, email))
                .thenReturn(Optional.of(user));

        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();

        assertNotNull(scope);
        assertEquals(5L, scope.getUserId());
        assertEquals(UserRole.PROGRAMME_COORDINATOR, scope.getRole());
        assertTrue(scope.isProgrammeCoordinator());
        assertEquals("sch-soe-01", scope.getSchoolId());
        assertEquals("dept-cse-01", scope.getDepartmentId());
        assertEquals("prog-btech-cse-01", scope.getMasterProgrammeId());
        assertTrue(scope.hasProgrammeScope());
        assertEquals("prog-btech-cse-01", scope.getRequiredMasterProgrammeId());
    }

    @Test
    @DisplayName("4. Successfully resolve Admin user scope (global access)")
    void testResolveAdminScope_Success() {
        String username = "admin";
        setSecurityContextUser(username, "ADMIN");

        User user = User.builder()
                .id(99L)
                .username(username)
                .email("admin@dypiu.ac.in")
                .name("Administrator")
                .role(UserRole.ADMIN)
                .schoolId(null)
                .departmentId(null)
                .masterProgrammeId(null)
                .isActive(true)
                .build();

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username))
                .thenReturn(Optional.of(user));

        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();

        assertNotNull(scope);
        assertEquals(99L, scope.getUserId());
        assertTrue(scope.isAdmin());
        assertFalse(scope.hasSchoolScope());
    }

    @Test
    @DisplayName("5. Unauthenticated request throws 401 Unauthorized")
    void testGetCurrentUserScope_Unauthenticated_ThrowsUnauthorized() {
        SecurityContextHolder.clearContext();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            currentUserScopeService.getCurrentUserScope();
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No authenticated user found"));
    }

    @Test
    @DisplayName("6. User not found in database throws 401 Unauthorized (No fallback)")
    void testGetCurrentUserScope_UserNotFound_ThrowsUnauthorized() {
        String email = "unknown@dypiu.ac.in";
        setSecurityContextUser(email, "FACULTY");

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(email, email))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsernameOrEmail(email, email))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            currentUserScopeService.getCurrentUserScope();
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Authenticated user not found in database"));
        verify(userRepository, never()).findAll(); // Verifies NO fallback to first user
    }

    @Test
    @DisplayName("7. Deactivated user throws 403 Forbidden")
    void testGetCurrentUserScope_DeactivatedUser_ThrowsForbidden() {
        String email = "deactivated@dypiu.ac.in";
        setSecurityContextUser(email, "FACULTY");

        User user = User.builder()
                .id(12L)
                .username("deact")
                .email(email)
                .name("Deactivated User")
                .role(UserRole.FACULTY)
                .isActive(false)
                .build();

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(email, email))
                .thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            currentUserScopeService.getCurrentUserScope();
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("User account is deactivated"));
    }

    @Test
    @DisplayName("8. Missing schoolId throws 403 Forbidden when getRequiredSchoolId() is called (NO fallback)")
    void testGetRequiredSchoolId_MissingSchool_ThrowsForbidden_NoFallback() {
        String email = "director_noschool@gmail.com";
        setSecurityContextUser(email, "DIRECTOR");

        User user = User.builder()
                .id(20L)
                .username("dir_noschool")
                .email(email)
                .name("No School Director")
                .role(UserRole.DIRECTOR)
                .schoolId(null) // missing schoolId
                .isActive(true)
                .build();

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(email, email))
                .thenReturn(Optional.of(user));

        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();

        assertNotNull(scope);
        assertFalse(scope.hasSchoolScope());
        assertNull(scope.getSchoolId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            scope.getRequiredSchoolId();
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("has no assigned school scope"));
    }

    @Test
    @DisplayName("9. Missing departmentId throws 403 Forbidden when getRequiredDepartmentId() is called")
    void testGetRequiredDepartmentId_MissingDept_ThrowsForbidden() {
        CurrentUserScope scope = CurrentUserScope.builder()
                .userId(1L)
                .email("test@dypiu.ac.in")
                .role(UserRole.HOD)
                .schoolId("sch-1")
                .departmentId(null)
                .build();

        assertFalse(scope.hasDepartmentScope());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, scope::getRequiredDepartmentId);
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("10. Missing masterProgrammeId throws 403 Forbidden when getRequiredMasterProgrammeId() is called")
    void testGetRequiredMasterProgrammeId_MissingProg_ThrowsForbidden() {
        CurrentUserScope scope = CurrentUserScope.builder()
                .userId(1L)
                .email("test@dypiu.ac.in")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-1")
                .departmentId("dept-1")
                .masterProgrammeId(null)
                .build();

        assertFalse(scope.hasProgrammeScope());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, scope::getRequiredMasterProgrammeId);
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("11. Resolve scope with Principal parameter")
    void testResolveScopeWithPrincipal_Success() {
        Principal principal = () -> "director1@gmail.com";

        User user = User.builder()
                .id(1L)
                .username("director1")
                .email("director1@gmail.com")
                .name("Dr. Raj Shaikh")
                .role(UserRole.DIRECTOR)
                .schoolId("sch-soe-01")
                .isActive(true)
                .build();

        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("director1@gmail.com", "director1@gmail.com"))
                .thenReturn(Optional.of(user));

        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope(principal);

        assertNotNull(scope);
        assertEquals(1L, scope.getUserId());
        assertEquals("sch-soe-01", scope.getSchoolId());
    }
}
