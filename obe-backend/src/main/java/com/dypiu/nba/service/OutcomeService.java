package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutcomeService {

    private final ProgrammeOutcomeRepository poRepository;
    private final ProgrammeSpecificOutcomeRepository psoRepository;
    private final PeoOutcomeRepository peoRepository;
    private final CourseOutcomeRepository coRepository;
    private final PoCompetencyRepository poCompetencyRepository;
    private final PsoCompetencyRepository psoCompetencyRepository;

    @Transactional(readOnly = true)
    public List<ProgrammeOutcome> getPOsByProgramme(String programmeId) {
        System.out.println("[OutcomeService] getPOsByProgramme called | programmeId: " + programmeId);
        List<ProgrammeOutcome> list = poRepository.findByProgrammeId(programmeId);
        for (ProgrammeOutcome po : list) {
            po.setCompetencies(poCompetencyRepository.findByPoId(po.getId()));
        }
        System.out.println("[OutcomeService] Fetched POs (" + list.size() + " items) with competencies for programmeId: " + programmeId);
        return list;
    }

    @Transactional
    public List<ProgrammeOutcome> savePOs(String programmeId, List<ProgrammeOutcome> pos) {
        System.out.println("[OutcomeService] savePOs called | programmeId: " + programmeId + " | count: " + (pos != null ? pos.size() : 0));
        
        List<ProgrammeOutcome> existing = poRepository.findByProgrammeId(programmeId);
        Map<String, ProgrammeOutcome> existingByCodeAndYear = existing.stream()
                .collect(Collectors.toMap(
                        p -> (p.getCode() + "_" + (p.getAcademicYear() != null ? p.getAcademicYear() : "2025-26")).toLowerCase(),
                        p -> p,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<ProgrammeOutcome> toSave = new ArrayList<>();
        Map<String, List<PoCompetency>> competenciesMap = new HashMap<>();

        if (pos != null) {
            for (ProgrammeOutcome po : pos) {
                po.setProgrammeId(programmeId);
                if (po.getAcademicYear() == null || po.getAcademicYear().isBlank()) {
                    po.setAcademicYear("2025-26");
                }

                String key = (po.getCode() + "_" + po.getAcademicYear()).toLowerCase();
                ProgrammeOutcome targetPo;
                if (existingByCodeAndYear.containsKey(key)) {
                    targetPo = existingByCodeAndYear.get(key);
                    targetPo.setStatement(po.getStatement());
                } else {
                    targetPo = po;
                    if (targetPo.getId() == null || targetPo.getId().isBlank()) {
                        targetPo.setId("po-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                }

                competenciesMap.put(targetPo.getId(), po.getCompetencies());
                processedIds.add(targetPo.getId());
                toSave.add(targetPo);
            }
        }

        // Delete obsolete POs & their competencies
        List<ProgrammeOutcome> toDelete = existing.stream()
                .filter(p -> !processedIds.contains(p.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            System.out.println("[OutcomeService] Deleting " + toDelete.size() + " obsolete POs for programmeId: " + programmeId);
            for (ProgrammeOutcome delPo : toDelete) {
                poCompetencyRepository.deleteByPoId(delPo.getId());
            }
            poRepository.deleteAll(toDelete);
        }

        List<ProgrammeOutcome> saved = poRepository.saveAll(toSave);

        // Sync competencies for each saved PO
        for (ProgrammeOutcome po : saved) {
            poCompetencyRepository.deleteByPoId(po.getId());
            List<PoCompetency> rawComps = competenciesMap.get(po.getId());
            List<PoCompetency> compsToSave = new ArrayList<>();
            if (rawComps != null) {
                int cIdx = 1;
                for (PoCompetency c : rawComps) {
                    if (c.getStatement() == null || c.getStatement().isBlank()) continue;
                    String cCode = c.getCode();
                    if (cCode == null || cCode.isBlank()) {
                        cCode = po.getCode() + "." + cIdx;
                    }
                    String cId = c.getId();
                    if (cId == null || cId.isBlank() || cId.startsWith("comp-")) {
                        cId = "pocomp-" + UUID.randomUUID().toString().substring(0, 8);
                    }
                    cIdx++;
                    compsToSave.add(PoCompetency.builder()
                            .id(cId)
                            .poId(po.getId())
                            .code(cCode)
                            .statement(c.getStatement())
                            .build());
                }
            }
            if (!compsToSave.isEmpty()) {
                poCompetencyRepository.saveAll(compsToSave);
            }
            po.setCompetencies(compsToSave);
        }

        System.out.println("[OutcomeService] Saved POs (" + saved.size() + " items) with competencies for programmeId: " + programmeId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeSpecificOutcome> getPSOsByProgramme(String programmeId) {
        System.out.println("[OutcomeService] getPSOsByProgramme called | programmeId: " + programmeId);
        List<ProgrammeSpecificOutcome> list = psoRepository.findByProgrammeId(programmeId);
        for (ProgrammeSpecificOutcome pso : list) {
            pso.setCompetencies(psoCompetencyRepository.findByPsoId(pso.getId()));
        }
        System.out.println("[OutcomeService] Fetched PSOs (" + list.size() + " items) with competencies for programmeId: " + programmeId);
        return list;
    }

    @Transactional
    public List<ProgrammeSpecificOutcome> savePSOs(String programmeId, List<ProgrammeSpecificOutcome> psos) {
        System.out.println("[OutcomeService] savePSOs called | programmeId: " + programmeId + " | count: " + (psos != null ? psos.size() : 0));

        List<ProgrammeSpecificOutcome> existing = psoRepository.findByProgrammeId(programmeId);
        Map<String, ProgrammeSpecificOutcome> existingByCodeAndYear = existing.stream()
                .collect(Collectors.toMap(
                        p -> (p.getCode() + "_" + (p.getAcademicYear() != null ? p.getAcademicYear() : "2025-26")).toLowerCase(),
                        p -> p,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<ProgrammeSpecificOutcome> toSave = new ArrayList<>();
        Map<String, List<PsoCompetency>> competenciesMap = new HashMap<>();

        if (psos != null) {
            for (ProgrammeSpecificOutcome pso : psos) {
                pso.setProgrammeId(programmeId);
                if (pso.getAcademicYear() == null || pso.getAcademicYear().isBlank()) {
                    pso.setAcademicYear("2025-26");
                }

                String key = (pso.getCode() + "_" + pso.getAcademicYear()).toLowerCase();
                ProgrammeSpecificOutcome targetPso;
                if (existingByCodeAndYear.containsKey(key)) {
                    targetPso = existingByCodeAndYear.get(key);
                    targetPso.setStatement(pso.getStatement());
                } else {
                    targetPso = pso;
                    if (targetPso.getId() == null || targetPso.getId().isBlank()) {
                        targetPso.setId("pso-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                }

                competenciesMap.put(targetPso.getId(), pso.getCompetencies());
                processedIds.add(targetPso.getId());
                toSave.add(targetPso);
            }
        }

        // Delete obsolete PSOs & their competencies
        List<ProgrammeSpecificOutcome> toDelete = existing.stream()
                .filter(p -> !processedIds.contains(p.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            System.out.println("[OutcomeService] Deleting " + toDelete.size() + " obsolete PSOs for programmeId: " + programmeId);
            for (ProgrammeSpecificOutcome delPso : toDelete) {
                psoCompetencyRepository.deleteByPsoId(delPso.getId());
            }
            psoRepository.deleteAll(toDelete);
        }

        List<ProgrammeSpecificOutcome> saved = psoRepository.saveAll(toSave);

        // Sync competencies for each saved PSO
        for (ProgrammeSpecificOutcome pso : saved) {
            psoCompetencyRepository.deleteByPsoId(pso.getId());
            List<PsoCompetency> rawComps = competenciesMap.get(pso.getId());
            List<PsoCompetency> compsToSave = new ArrayList<>();
            if (rawComps != null) {
                int cIdx = 1;
                for (PsoCompetency c : rawComps) {
                    if (c.getStatement() == null || c.getStatement().isBlank()) continue;
                    String cCode = c.getCode();
                    if (cCode == null || cCode.isBlank()) {
                        cCode = pso.getCode() + "." + cIdx;
                    }
                    String cId = c.getId();
                    if (cId == null || cId.isBlank() || cId.startsWith("comp-") || cId.startsWith("psocomp-")) {
                        cId = "psocomp-" + UUID.randomUUID().toString().substring(0, 8);
                    }
                    cIdx++;
                    compsToSave.add(PsoCompetency.builder()
                            .id(cId)
                            .psoId(pso.getId())
                            .code(cCode)
                            .statement(c.getStatement())
                            .build());
                }
            }
            if (!compsToSave.isEmpty()) {
                psoCompetencyRepository.saveAll(compsToSave);
            }
            pso.setCompetencies(compsToSave);
        }

        System.out.println("[OutcomeService] Saved PSOs (" + saved.size() + " items) with competencies for programmeId: " + programmeId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PeoOutcome> getPEOsByProgramme(String programmeId) {
        System.out.println("[OutcomeService] getPEOsByProgramme called | programmeId: " + programmeId);
        List<PeoOutcome> list = peoRepository.findByProgrammeId(programmeId);
        System.out.println("[OutcomeService] Fetched PEOs (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional
    public List<PeoOutcome> savePEOs(String programmeId, List<PeoOutcome> peos) {
        System.out.println("[OutcomeService] savePEOs called | programmeId: " + programmeId + " | count: " + (peos != null ? peos.size() : 0));

        List<PeoOutcome> existing = peoRepository.findByProgrammeId(programmeId);
        Map<String, PeoOutcome> existingByCodeAndYear = existing.stream()
                .collect(Collectors.toMap(
                        p -> (p.getCode() + "_" + (p.getAcademicYear() != null ? p.getAcademicYear() : "2025-26")).toLowerCase(),
                        p -> p,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<PeoOutcome> toSave = new ArrayList<>();

        if (peos != null) {
            for (PeoOutcome peo : peos) {
                peo.setProgrammeId(programmeId);
                if (peo.getAcademicYear() == null || peo.getAcademicYear().isBlank()) {
                    peo.setAcademicYear("2025-26");
                }

                String key = (peo.getCode() + "_" + peo.getAcademicYear()).toLowerCase();
                PeoOutcome targetPeo;
                if (existingByCodeAndYear.containsKey(key)) {
                    targetPeo = existingByCodeAndYear.get(key);
                    targetPeo.setStatement(peo.getStatement());
                } else {
                    targetPeo = peo;
                    if (targetPeo.getId() == null || targetPeo.getId().isBlank()) {
                        targetPeo.setId("peo-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                }

                processedIds.add(targetPeo.getId());
                toSave.add(targetPeo);
            }
        }

        List<PeoOutcome> toDelete = existing.stream()
                .filter(p -> !processedIds.contains(p.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            System.out.println("[OutcomeService] Deleting " + toDelete.size() + " obsolete PEOs for programmeId: " + programmeId);
            peoRepository.deleteAll(toDelete);
        }

        List<PeoOutcome> saved = peoRepository.saveAll(toSave);
        System.out.println("[OutcomeService] Saved PEOs (" + saved.size() + " items) for programmeId: " + programmeId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CourseOutcome> getCOsByCourse(String courseId) {
        System.out.println("[OutcomeService] getCOsByCourse called | courseId: " + courseId);
        List<CourseOutcome> list = coRepository.findByCourseId(courseId);
        System.out.println("[OutcomeService] Fetched COs (" + list.size() + " items) for courseId: " + courseId);
        return list;
    }

    @Transactional
    public List<CourseOutcome> saveCOs(String courseId, List<CourseOutcome> cos) {
        System.out.println("[OutcomeService] saveCOs called | courseId: " + courseId + " | count: " + (cos != null ? cos.size() : 0));

        List<CourseOutcome> existing = coRepository.findByCourseId(courseId);
        Map<String, CourseOutcome> existingByCode = existing.stream()
                .collect(Collectors.toMap(
                        c -> c.getCode().toLowerCase(),
                        c -> c,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<CourseOutcome> toSave = new ArrayList<>();

        if (cos != null) {
            for (CourseOutcome co : cos) {
                co.setCourseId(courseId);

                String key = co.getCode().toLowerCase();
                CourseOutcome targetCo;
                if (existingByCode.containsKey(key)) {
                    targetCo = existingByCode.get(key);
                    targetCo.setStatement(co.getStatement());
                } else {
                    targetCo = co;
                    if (targetCo.getId() == null || targetCo.getId().isBlank()) {
                        targetCo.setId("co-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                }

                processedIds.add(targetCo.getId());
                toSave.add(targetCo);
            }
        }

        List<CourseOutcome> toDelete = existing.stream()
                .filter(c -> !processedIds.contains(c.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            System.out.println("[OutcomeService] Deleting " + toDelete.size() + " obsolete COs for courseId: " + courseId);
            coRepository.deleteAll(toDelete);
        }

        List<CourseOutcome> saved = coRepository.saveAll(toSave);
        System.out.println("[OutcomeService] Saved COs (" + saved.size() + " items) for courseId: " + courseId);
        return saved;
    }
}
