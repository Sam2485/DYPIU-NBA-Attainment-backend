package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutcomeService {

    private final ProgrammeOutcomeRepository poRepository;
    private final ProgrammeSpecificOutcomeRepository psoRepository;
    private final PeoOutcomeRepository peoRepository;
    private final CourseOutcomeRepository coRepository;

    @Transactional(readOnly = true)
    public List<ProgrammeOutcome> getPOsByProgramme(String programmeId) {
        return poRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public List<ProgrammeOutcome> savePOs(String programmeId, List<ProgrammeOutcome> pos) {
        pos.forEach(po -> {
            po.setProgrammeId(programmeId);
            if (po.getId() == null) po.setId("po-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return poRepository.saveAll(pos);
    }

    @Transactional(readOnly = true)
    public List<ProgrammeSpecificOutcome> getPSOsByProgramme(String programmeId) {
        return psoRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public List<ProgrammeSpecificOutcome> savePSOs(String programmeId, List<ProgrammeSpecificOutcome> psos) {
        psos.forEach(pso -> {
            pso.setProgrammeId(programmeId);
            if (pso.getId() == null) pso.setId("pso-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return psoRepository.saveAll(psos);
    }

    @Transactional(readOnly = true)
    public List<PeoOutcome> getPEOsByProgramme(String programmeId) {
        return peoRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public List<PeoOutcome> savePEOs(String programmeId, List<PeoOutcome> peos) {
        peos.forEach(peo -> {
            peo.setProgrammeId(programmeId);
            if (peo.getId() == null) peo.setId("peo-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return peoRepository.saveAll(peos);
    }

    @Transactional(readOnly = true)
    public List<CourseOutcome> getCOsByCourse(String courseId) {
        return coRepository.findByCourseId(courseId);
    }

    @Transactional
    public List<CourseOutcome> saveCOs(String courseId, List<CourseOutcome> cos) {
        cos.forEach(co -> {
            co.setCourseId(courseId);
            if (co.getId() == null) co.setId("co-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return coRepository.saveAll(cos);
    }
}
