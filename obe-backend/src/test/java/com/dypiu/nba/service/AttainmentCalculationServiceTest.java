package com.dypiu.nba.service;

import com.dypiu.nba.entity.AttainmentConfiguration;
import com.dypiu.nba.entity.CourseOutcome;
import com.dypiu.nba.entity.StudentCoMark;
import com.dypiu.nba.repository.AttainmentConfigurationRepository;
import com.dypiu.nba.repository.CourseOutcomeRepository;
import com.dypiu.nba.repository.StudentCoMarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttainmentCalculationServiceTest {

    @Mock
    private AttainmentConfigurationRepository configRepository;

    @Mock
    private StudentCoMarkRepository studentCoMarkRepository;

    @Mock
    private CourseOutcomeRepository courseOutcomeRepository;

    @InjectMocks
    private AttainmentCalculationService calculationService;

    private AttainmentConfiguration config;
    private CourseOutcome co1;

    @BeforeEach
    void setUp() {
        config = AttainmentConfiguration.builder()
                .id("cfg-crs-1")
                .courseId("crs-1")
                .courseCode("310244")
                .courseName("Computer Network and Security")
                .directWeight(new BigDecimal("80.00"))
                .indirectWeight(new BigDecimal("20.00"))
                .directThreshold(new BigDecimal("60.00"))
                .indirectThreshold(new BigDecimal("60.00"))
                .build();

        co1 = CourseOutcome.builder()
                .id("co-1-1")
                .courseId("crs-1")
                .code("C321.1")
                .statement("Interpret fundamental concepts")
                .build();
    }

    @Test
    void testCalculateCourseCoAttainment_FormulaVerification() {
        when(configRepository.findByCourseId("crs-1")).thenReturn(Optional.of(config));
        when(courseOutcomeRepository.findByCourseId("crs-1")).thenReturn(List.of(co1));

        StudentCoMark m1 = StudentCoMark.builder().coCode("C321.1").marksObtained(new BigDecimal("75")).maxMarks(new BigDecimal("100")).build();
        StudentCoMark m2 = StudentCoMark.builder().coCode("C321.1").marksObtained(new BigDecimal("80")).maxMarks(new BigDecimal("100")).build();
        StudentCoMark m3 = StudentCoMark.builder().coCode("C321.1").marksObtained(new BigDecimal("50")).maxMarks(new BigDecimal("100")).build();

        when(studentCoMarkRepository.findByCourseId("crs-1")).thenReturn(List.of(m1, m2, m3));

        Map<String, Object> result = calculationService.calculateCourseCoAttainment("crs-1");

        assertNotNull(result);
        assertTrue(result.containsKey("coAttainments"));
        List<Map<String, Object>> coAttainments = (List<Map<String, Object>>) result.get("coAttainments");
        assertEquals(1, coAttainments.size());

        Map<String, Object> co1Res = coAttainments.get(0);
        assertEquals("C321.1", co1Res.get("coCode"));
        assertNotNull(co1Res.get("overallAttainment"));
    }
}
