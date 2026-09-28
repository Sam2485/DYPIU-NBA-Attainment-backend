package com.dypiu.nba.service;

import com.dypiu.nba.entity.School;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
import com.dypiu.nba.repository.SchoolRepository;
import com.dypiu.nba.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dypiu.nba.entity.MasterProgramme;
import com.dypiu.nba.entity.Department;
import com.dypiu.nba.entity.UserOrganizationalAssignment;
import com.dypiu.nba.repository.DepartmentRepository;
import com.dypiu.nba.repository.MasterProgrammeRepository;
import com.dypiu.nba.repository.UserOrganizationalAssignmentRepository;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@WithMockUser(roles = "IQAC")
public class SchoolDirectorMappingTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MasterProgrammeRepository masterProgrammeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AcademicLookupCacheService academicLookupCacheService;

    @Mock
    private CurrentUserScopeService currentUserScopeService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserOrganizationalAssignmentRepository userOrganizationalAssignmentRepository;

    @InjectMocks
    private AcademicService academicService;

    @Test
    @DisplayName("Successfully create school when director is not already mapped")
    void testSaveSchool_Success() {
        School newSchool = School.builder()
                .code("SOE")
                .name("School of Engineering")
                .directorId(10L)
                .directorEmail("director.soe@dypiu.ac.in")
                .directorName("Dr. Director SOE")
                .build();

        User directorUser = User.builder()
                .id(10L)
                .name("Dr. Director SOE")
                .email("director.soe@dypiu.ac.in")
                .role(UserRole.DIRECTOR)
                .build();

        when(schoolRepository.findByDirectorId(10L)).thenReturn(Optional.empty());
        when(schoolRepository.findByDirectorEmailIgnoreCase("director.soe@dypiu.ac.in")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("director.soe@dypiu.ac.in")).thenReturn(Optional.of(directorUser));
        when(schoolRepository.save(any(School.class))).thenAnswer(inv -> inv.getArgument(0));

        School saved = academicService.saveSchool(newSchool);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("SOE", saved.getCode());
        assertEquals(10L, saved.getDirectorId());
        assertEquals("director.soe@dypiu.ac.in", saved.getDirectorEmail());
        verify(userRepository, atLeastOnce()).save(directorUser);
        assertEquals(saved.getId(), directorUser.getSchoolId());
    }

    @Test
    @DisplayName("Fail to create school when director email is already mapped to another school")
    void testSaveSchool_DuplicateDirectorEmail_ThrowsException() {
        School existingSchool = School.builder()
                .id("sch-soe")
                .code("SOE")
                .name("School of Engineering")
                .directorId(10L)
                .directorEmail("director@dypiu.ac.in")
                .build();

        School duplicateSchool = School.builder()
                .code("SOM")
                .name("School of Management")
                .directorEmail("director@dypiu.ac.in")
                .build();

        when(schoolRepository.findByDirectorEmailIgnoreCase("director@dypiu.ac.in"))
                .thenReturn(Optional.of(existingSchool));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            academicService.saveSchool(duplicateSchool);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already assigned to School: School of Engineering"));
        verify(schoolRepository, never()).save(duplicateSchool);
    }

    @Test
    @DisplayName("Fail to update school when assigning a director already mapped to a different school")
    void testUpdateSchool_DuplicateDirector_ThrowsException() {
        School currentSchool = School.builder()
                .id("sch-som")
                .code("SOM")
                .name("School of Management")
                .build();

        School otherSchool = School.builder()
                .id("sch-soe")
                .code("SOE")
                .name("School of Engineering")
                .directorId(10L)
                .directorEmail("director.soe@dypiu.ac.in")
                .build();

        School updateDetails = School.builder()
                .directorId(10L)
                .directorEmail("director.soe@dypiu.ac.in")
                .build();

        when(schoolRepository.findById("sch-som")).thenReturn(Optional.of(currentSchool));
        when(schoolRepository.findByDirectorId(10L)).thenReturn(Optional.of(otherSchool));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            academicService.updateSchool("sch-som", updateDetails);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Director is already assigned to School: School of Engineering"));
    }

    @Test
    @DisplayName("Successfully save programme preserving coordinator and coordinatorEmail")
    void testSaveProgramme_PreservesCoordinatorAndEmail() {
        MasterProgramme inputProg = MasterProgramme.builder()
                .id("prog-1a1b6c2e")
                .name("B.Tech Computer science")
                .code("BTCS")
                .departmentId("dept-cs")
                .coordinator("prag")
                .coordinatorEmail("pc@gmail.com")
                .build();

        User pcUser = User.builder()
                .id(25L)
                .name("Prag PC")
                .email("pc@gmail.com")
                .role(UserRole.FACULTY)
                .build();

        
        Department mockDept = Department.builder().id("dept-cs").schoolId("school-1").build();
        when(departmentRepository.findById("dept-cs")).thenReturn(Optional.of(mockDept));
        when(masterProgrammeRepository.findByIdAndDeletedAtIsNull("prog-1a1b6c2e")).thenReturn(Optional.of(inputProg));
        when(userRepository.findByEmail("pc@gmail.com")).thenReturn(Optional.of(pcUser));
        when(masterProgrammeRepository.save(any(MasterProgramme.class))).thenAnswer(inv -> inv.getArgument(0));

        MasterProgramme saved = academicService.saveProgramme(inputProg);

        assertNotNull(saved);
        assertEquals("prog-1a1b6c2e", saved.getId());
        assertEquals("Prag PC", saved.getCoordinator());
        assertEquals("pc@gmail.com", saved.getCoordinatorEmail());
        verify(userRepository).save(pcUser);
        assertEquals(UserRole.PROGRAMME_COORDINATOR, pcUser.getRole());
        assertEquals("prog-1a1b6c2e", pcUser.getMasterProgrammeId());
    }

    @Test
    @DisplayName("Successfully create school when only directorId is provided (IQAC Assign Director)")
    void testSaveSchool_AssignDirectorByIdOnly_Success() {
        School newSchool = School.builder()
                .code("SBL")
                .name("School of Biosciences and Bioengineering")
                .directorId(15L)
                .build();

        User directorUser = User.builder()
                .id(15L)
                .name("Dr. Bio Director")
                .email("bio.director@dypiu.ac.in")
                .role(UserRole.DIRECTOR)
                .build();

        when(userRepository.findById(15L)).thenReturn(Optional.of(directorUser));
        when(schoolRepository.findByDirectorId(15L)).thenReturn(Optional.empty());
        when(schoolRepository.findByDirectorEmailIgnoreCase("bio.director@dypiu.ac.in")).thenReturn(Optional.empty());
        when(schoolRepository.save(any(School.class))).thenAnswer(inv -> inv.getArgument(0));

        School saved = academicService.saveSchool(newSchool);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("SBL", saved.getCode());
        assertEquals(15L, saved.getDirectorId());
        assertEquals("Dr. Bio Director", saved.getDirectorName());
        assertEquals("bio.director@dypiu.ac.in", saved.getDirectorEmail());
        verify(userRepository, atLeastOnce()).save(directorUser);
        assertEquals(saved.getId(), directorUser.getSchoolId());
    }

    @Test
    @DisplayName("Successfully update school assigning director by directorId only")
    void testUpdateSchool_AssignDirectorByIdOnly_Success() {
        School existingSchool = School.builder()
                .id("sch-sbl")
                .code("SBL")
                .name("School of Biosciences")
                .build();

        School updatePayload = School.builder()
                .code("SBL")
                .name("School of Biosciences and Bioengineering")
                .directorId(20L)
                .build();

        User newDirector = User.builder()
                .id(20L)
                .name("Dr. New Director")
                .email("new.director@dypiu.ac.in")
                .role(UserRole.DIRECTOR)
                .build();

        when(schoolRepository.findById("sch-sbl")).thenReturn(Optional.of(existingSchool));
        when(userRepository.findById(20L)).thenReturn(Optional.of(newDirector));
        when(schoolRepository.findByDirectorId(20L)).thenReturn(Optional.empty());
        when(schoolRepository.findByDirectorEmailIgnoreCase("new.director@dypiu.ac.in")).thenReturn(Optional.empty());
        when(schoolRepository.save(any(School.class))).thenAnswer(inv -> inv.getArgument(0));

        School updated = academicService.updateSchool("sch-sbl", updatePayload);

        assertNotNull(updated);
        assertEquals(20L, updated.getDirectorId());
        assertEquals("Dr. New Director", updated.getDirectorName());
        assertEquals("new.director@dypiu.ac.in", updated.getDirectorEmail());
        verify(userRepository, atLeastOnce()).save(newDirector);
        assertEquals("sch-sbl", newDirector.getSchoolId());
    }

    @Test
    @DisplayName("Successfully soft delete school as IQAC and deactivate assignments")
    void testDeleteSchool_Success() {
        School school = School.builder()
                .id("sch-soe")
                .code("SOE")
                .name("School of Engineering")
                .build();

        CurrentUserScope iqacScope = CurrentUserScope.builder()
                .userId(1L)
                .email("iqac@dypiu.ac.in")
                .role(UserRole.IQAC)
                .build();

        UserOrganizationalAssignment assignment = UserOrganizationalAssignment.builder()
                .id(100L)
                .schoolId("sch-soe")
                .role("DIRECTOR")
                .isActive(true)
                .build();

        when(currentUserScopeService.getCurrentUserScope()).thenReturn(iqacScope);
        when(schoolRepository.findById("sch-soe")).thenReturn(Optional.of(school));
        when(userOrganizationalAssignmentRepository.findBySchoolIdAndIsActiveTrue("sch-soe")).thenReturn(List.of(assignment));

        academicService.deleteSchool("sch-soe");

        assertNotNull(school.getDeletedAt());
        assertEquals("iqac@dypiu.ac.in", school.getDeletedBy());
        verify(schoolRepository).save(school);
        assertFalse(assignment.getIsActive());
        verify(userOrganizationalAssignmentRepository).saveAll(any());
        verify(academicLookupCacheService).evictDepartmentCache();
        verify(auditLogService).recordSuccess(any(), any(), eq("sch-soe"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Fail to delete school if user is not IQAC")
    void testDeleteSchool_ForbiddenWhenNotIqac() {
        CurrentUserScope directorScope = CurrentUserScope.builder()
                .userId(2L)
                .email("director@dypiu.ac.in")
                .role(UserRole.DIRECTOR)
                .build();

        when(currentUserScopeService.getCurrentUserScope()).thenReturn(directorScope);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                academicService.deleteSchool("sch-soe")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(schoolRepository, never()).save(any());
    }
}
