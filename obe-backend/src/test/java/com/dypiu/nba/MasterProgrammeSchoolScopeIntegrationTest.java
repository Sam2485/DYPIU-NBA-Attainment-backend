package com.dypiu.nba;

import com.dypiu.nba.entity.Department;
import com.dypiu.nba.entity.MasterProgramme;
import com.dypiu.nba.entity.School;
import com.dypiu.nba.repository.DepartmentRepository;
import com.dypiu.nba.repository.MasterProgrammeRepository;
import com.dypiu.nba.repository.SchoolRepository;
import com.dypiu.nba.service.AcademicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class MasterProgrammeSchoolScopeIntegrationTest {

    @Autowired
    private AcademicService academicService;

    @Autowired
    private MasterProgrammeRepository masterProgrammeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    private String sch1Id;
    private String sch2Id;
    private String d1Id;
    private String d2Id;
    private String d3Id;

    @BeforeEach
    void setUp() {
        masterProgrammeRepository.deleteAll();
        departmentRepository.deleteAll();
        schoolRepository.deleteAll();

        School s1 = new School();
        s1.setId("sch-s1");
        s1.setCode("S1");
        s1.setName("School 1");
        schoolRepository.save(s1);
        sch1Id = s1.getId();

        School s2 = new School();
        s2.setId("sch-s2");
        s2.setCode("S2");
        s2.setName("School 2");
        schoolRepository.save(s2);
        sch2Id = s2.getId();

        Department d1 = new Department();
        d1.setId("dept-1");
        d1.setSchoolId(sch1Id);
        d1.setCode("D1");
        d1.setName("Dept 1");
        d1.setStatus("ACTIVE");
        departmentRepository.save(d1);
        d1Id = d1.getId();

        Department d2 = new Department();
        d2.setId("dept-2");
        d2.setSchoolId(sch1Id);
        d2.setCode("D2");
        d2.setName("Dept 2");
        d2.setStatus("ACTIVE");
        departmentRepository.save(d2);
        d2Id = d2.getId();

        Department d3 = new Department();
        d3.setId("dept-3");
        d3.setSchoolId(sch2Id);
        d3.setCode("D3");
        d3.setName("Dept 3");
        d3.setStatus("ACTIVE");
        departmentRepository.save(d3);
        d3Id = d3.getId();
    }

    @Test
    void testSameCodeNameDifferentSchoolsAllowed() {
        MasterProgramme p1 = new MasterProgramme();
        p1.setDepartmentId(d1Id);
        p1.setCode("BTECH");
        p1.setName("Bachelor of Tech");
        academicService.saveProgramme(p1);

        MasterProgramme p2 = new MasterProgramme();
        p2.setDepartmentId(d3Id);
        p2.setCode("btech"); // case-insensitive duplicate in code
        p2.setName("bachelor OF tech"); // case-insensitive duplicate in name
        MasterProgramme saved2 = academicService.saveProgramme(p2);

        assertThat(saved2.getId()).isNotNull();
    }

    @Test
    void testSameDegreeAwardedSameSchoolAllowed() {
        MasterProgramme p1 = new MasterProgramme();
        p1.setDepartmentId(d1Id);
        p1.setDegreeAwarded("B.Tech");
        p1.setName("Bachelor of Tech in Computer Science");
        MasterProgramme saved1 = academicService.saveProgramme(p1);

        MasterProgramme p2 = new MasterProgramme();
        p2.setDepartmentId(d2Id); // different department, same school
        p2.setDegreeAwarded("B.Tech"); // multiple programmes in same school award B.Tech
        p2.setName("Bachelor of Tech in Mechanical");
        MasterProgramme saved2 = academicService.saveProgramme(p2);

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getDegreeAwarded()).isEqualTo("B.Tech");
        assertThat(saved2.getDegreeAwarded()).isEqualTo("B.Tech");
    }

    @Test
    void testSameNameSameSchoolRejected() {
        MasterProgramme p1 = new MasterProgramme();
        p1.setDepartmentId(d1Id);
        p1.setCode("BTECH1");
        p1.setName("Bachelor of Tech");
        academicService.saveProgramme(p1);

        MasterProgramme p2 = new MasterProgramme();
        p2.setDepartmentId(d2Id);
        p2.setCode("BTECH2");
        p2.setName("bachelor OF tech");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            academicService.saveProgramme(p2);
        });
        assertThat(ex.getReason()).contains("Programme name already exists in this school");
    }

    @Test
    void testUpdateWithoutChangingCodeNameAllowed() {
        MasterProgramme p1 = new MasterProgramme();
        p1.setDepartmentId(d1Id);
        p1.setCode("BTECH");
        p1.setName("Bachelor of Tech");
        MasterProgramme saved = academicService.saveProgramme(p1);

        // Update it
        saved.setDurationYears(5);
        MasterProgramme updated = academicService.saveProgramme(saved);
        assertThat(updated.getDurationYears()).isEqualTo(5);
    }

    @Test
    void testReusingSoftDeletedProgrammeAllowed() {
        MasterProgramme p1 = new MasterProgramme();
        p1.setDepartmentId(d1Id);
        p1.setCode("BTECH");
        p1.setName("Bachelor of Tech");
        MasterProgramme saved = academicService.saveProgramme(p1);

        // Soft delete it
        academicService.deleteProgramme(saved.getId());

        // Create new with same code and name
        MasterProgramme p2 = new MasterProgramme();
        p2.setDepartmentId(d1Id);
        p2.setCode("BTECH");
        p2.setName("Bachelor of Tech");
        MasterProgramme saved2 = academicService.saveProgramme(p2);

        assertThat(saved2.getId()).isNotEqualTo(saved.getId());
    }
}
