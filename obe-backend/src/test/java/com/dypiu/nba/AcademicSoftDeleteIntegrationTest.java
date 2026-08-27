package com.dypiu.nba;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AcademicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class AcademicSoftDeleteIntegrationTest {

    @Autowired
    private AcademicService academicService;

    @Autowired
    private MasterProgrammeRepository masterProgrammeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    private String deptId;

    @BeforeEach
    void setUp() {
        if (schoolRepository.count() == 0) {
            School school = new School();
            school.setId("sch-1");
            school.setCode("S1");
            school.setName("School 1");
            schoolRepository.save(school);
        }
        if (departmentRepository.count() == 0) {
            Department dept = new Department();
            dept.setId("dept-1");
            dept.setSchoolId("sch-1");
            dept.setCode("D1");
            dept.setName("Dept 1");
            dept.setStatus("ACTIVE");
            departmentRepository.save(dept);
            deptId = dept.getId();
        } else {
            deptId = departmentRepository.findAll().get(0).getId();
        }
    }

    @Test
    void testSoftDeleteAndRecreateProgramme() {
        // 1. Create a MasterProgramme
        MasterProgramme p1 = new MasterProgramme();
        p1.setDepartmentId(deptId);
        p1.setCode("B.TECH-TEST");
        p1.setName("Test Prog 1");
        MasterProgramme saved1 = academicService.saveProgramme(p1);
        String oldId = saved1.getId();

        // 2. Delete it (should soft-delete)
        academicService.deleteProgramme(oldId);

        // Verify it is soft-deleted
        Optional<MasterProgramme> deletedP1 = masterProgrammeRepository.findById(oldId);
        assertThat(deletedP1).isPresent();
        assertThat(deletedP1.get().getDeletedAt()).isNotNull();

        // Verify it is excluded from active listings
        List<MasterProgramme> activeProgs = masterProgrammeRepository.findByDeletedAtIsNull();
        assertThat(activeProgs).extracting(MasterProgramme::getId).doesNotContain(oldId);

        // 3. Create a NEW MasterProgramme with the exact same code
        MasterProgramme p2 = new MasterProgramme();
        p2.setDepartmentId(deptId);
        p2.setCode("B.TECH-TEST");
        p2.setName("Test Prog 2");
        
        MasterProgramme saved2 = academicService.saveProgramme(p2);
        String newId = saved2.getId();

        // Verify NEW ID is created
        assertThat(newId).isNotNull().isNotEqualTo(oldId);

        // Verify both exist in the database with the SAME code
        Optional<MasterProgramme> fromDbNew = masterProgrammeRepository.findById(newId);
        assertThat(fromDbNew).isPresent();
        assertThat(fromDbNew.get().getCode()).isEqualTo("B.TECH-TEST");
        assertThat(fromDbNew.get().getDeletedAt()).isNull();

        Optional<MasterProgramme> fromDbOld = masterProgrammeRepository.findById(oldId);
        assertThat(fromDbOld).isPresent();
        assertThat(fromDbOld.get().getCode()).isEqualTo("B.TECH-TEST");
        assertThat(fromDbOld.get().getDeletedAt()).isNotNull();
    }
}
