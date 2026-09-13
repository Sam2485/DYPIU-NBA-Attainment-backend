package com.dypiu.nba;

import com.dypiu.nba.dto.DeletedItemDto;
import com.dypiu.nba.dto.RecoverySummaryDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.RecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RecoveryServiceTest {

    @Autowired
    private AcademicService academicService;

    @Autowired
    private RecoveryService recoveryService;

    @Autowired
    private MasterProgrammeRepository masterProgrammeRepository;

    @Autowired
    private ProgrammeBatchRepository programmeBatchRepository;

    @Autowired
    private ProgrammeBatchCourseRepository programmeBatchCourseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    private String deptId;

    @BeforeEach
    void setUp() {
        if (schoolRepository.count() == 0) {
            School school = new School();
            school.setId("sch-rec-1");
            school.setCode("SREC1");
            school.setName("School Recovery 1");
            schoolRepository.save(school);
        }
        if (departmentRepository.count() == 0) {
            Department dept = new Department();
            dept.setId("dept-rec-1");
            dept.setSchoolId("sch-rec-1");
            dept.setCode("DREC1");
            dept.setName("Dept Recovery 1");
            dept.setStatus("ACTIVE");
            departmentRepository.save(dept);
            deptId = dept.getId();
        } else {
            deptId = departmentRepository.findAll().get(0).getId();
        }
    }

    @Test
    void testRecoveryFlowForProgrammesBatchesAndCourses() {
        // 1. Create a MasterProgramme
        MasterProgramme prog = MasterProgramme.builder()
                .departmentId(deptId)
                .code("REC-PROG-1")
                .name("Recovery Programme 1")
                .build();
        MasterProgramme savedProg = academicService.saveProgramme(prog);

        // 2. Create a Batch
        ProgrammeBatch batch = ProgrammeBatch.builder()
                .masterProgrammeId(savedProg.getId())
                .name("Batch Rec 2024-2028")
                .startYear(2024)
                .endYear(2028)
                .durationYears(4)
                .build();
        ProgrammeBatch savedBatch = academicService.saveBatch(batch);

        // 3. Create a Course
        com.dypiu.nba.dto.CourseOfferingRequestDto courseReq = com.dypiu.nba.dto.CourseOfferingRequestDto.builder()
                .programmeBatchId(savedBatch.getId())
                .code("REC101")
                .name("Recovery Course 101")
                .credits(3)
                .courseType("THEORY")
                .semester(1)
                .build();
        ProgrammeBatchCourse savedCourse = academicService.createCourseOffering(courseReq);

        // 4. Soft Delete the Course
        academicService.deleteProgrammeBatchCourse(savedCourse.getId());

        // Check getDeletedItems for COURSE
        List<DeletedItemDto> deletedCourses = recoveryService.getDeletedItems("COURSE", null, null, null);
        assertThat(deletedCourses).extracting(DeletedItemDto::getId).contains(savedCourse.getId());

        // 5. Soft Delete the Batch
        academicService.deleteBatch(savedBatch.getId());

        // Check getDeletedItems for BATCH
        List<DeletedItemDto> deletedBatches = recoveryService.getDeletedItems("BATCH", null, null, null);
        assertThat(deletedBatches).extracting(DeletedItemDto::getId).contains(savedBatch.getId());

        // 6. Soft Delete the Programme
        academicService.deleteProgramme(savedProg.getId());

        // Check getDeletedItems for ALL
        List<DeletedItemDto> allDeleted = recoveryService.getDeletedItems("ALL", null, null, null);
        assertThat(allDeleted).extracting(DeletedItemDto::getId).contains(savedProg.getId());

        // Check Summary
        RecoverySummaryDto summary = recoveryService.getSummary();
        assertThat(summary.getProgrammesCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getBatchesCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getCoursesCount()).isGreaterThanOrEqualTo(1);
    }
}
