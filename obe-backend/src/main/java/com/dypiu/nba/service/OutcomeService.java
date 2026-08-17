package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.dto.ProgrammeTargetDto;
import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
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
    private final ProgrammeTargetRepository targetRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CoPoMappingRepository coPoMappingRepository;
    private final CoPsoMappingRepository coPsoMappingRepository;
    private final CourseMappingKeywordRepository courseMappingKeywordRepository;
    private final BatchRepository batchRepository;
    private final ObjectMapper objectMapper;



    private static final Comparator<String> NATURAL_CODE_COMPARATOR = (c1, c2) -> {
        if (c1 == null) return -1;
        if (c2 == null) return 1;
        String p1 = c1.replaceAll("\\D+", "");
        String p2 = c2.replaceAll("\\D+", "");
        if (!p1.isEmpty() && !p2.isEmpty()) {
            try {
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                if (n1 != n2) return Integer.compare(n1, n2);
            } catch (NumberFormatException ignored) {}
        }
        return c1.compareToIgnoreCase(c2);
    };

    private final Map<String, Map<String, Object>> coursePoKeywordsMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> coursePsoKeywordsMap = new java.util.concurrent.ConcurrentHashMap<>();

    @Transactional
    public List<ProgrammeOutcome> getPOsByProgramme(String programmeId) {
        System.out.println("================================================================================");
        System.out.println("[OutcomeService] >>> getPOsByProgramme called | programmeId: " + programmeId);
        List<ProgrammeOutcome> list = poRepository.findByProgrammeIdOrderByCodeAsc(programmeId);
        if (list.isEmpty()) {
            System.out.println("[OutcomeService] No POs found in DB for programmeId: " + programmeId + ". Seeding default POs...");
            list = seedDefaultPOs(programmeId);
        }
        for (ProgrammeOutcome po : list) {
            List<PoCompetency> comps = poCompetencyRepository.findByPoIdOrderByCodeAsc(po.getId());
            comps.sort(Comparator.comparing(PoCompetency::getCode, NATURAL_CODE_COMPARATOR));
            po.setCompetencies(comps);
            System.out.println("  [PO ENTRY] Code: " + po.getCode() + " | Statement: " + po.getStatement());
            for (PoCompetency comp : comps) {
                System.out.println("      -> Competency Code: " + comp.getCode() + " | Statement: " + comp.getStatement());
            }
        }
        list.sort(Comparator.comparing(ProgrammeOutcome::getCode, NATURAL_CODE_COMPARATOR));
        System.out.println("[OutcomeService] <<< OUTGOING POs Payload (" + list.size() + " items) for programmeId: " + programmeId);
        System.out.println("================================================================================");
        return list;
    }

    private List<ProgrammeOutcome> seedDefaultPOs(String programmeId) {
        String pId = (programmeId != null && !programmeId.isBlank()) ? programmeId : "prog-1";
        String[][] poDefs = {
            {"PO1", "Engineering Knowledge: Apply knowledge of mathematics, science, engineering fundamentals, and computer engineering to solve complex problems."},
            {"PO2", "Problem Analysis: Identify, formulate, review research literature, and analyze complex engineering problems reaching substantiated conclusions."},
            {"PO3", "Design/Development of Solutions: Design solutions for complex engineering problems and design system components or processes."},
            {"PO4", "Conduct Investigations of Complex Problems: Use research-based knowledge and research methods including design of experiments, analysis and interpretation of data."},
            {"PO5", "Modern Tool Usage: Create, select, and apply appropriate techniques, resources, and modern engineering and IT tools."},
            {"PO6", "The Engineer and Society: Apply reasoning informed by contextual knowledge to assess societal, health, safety, legal and cultural issues."},
            {"PO7", "Environment and Sustainability: Understand the impact of professional engineering solutions in societal and environmental contexts."},
            {"PO8", "Ethics: Apply ethical principles and commit to professional ethics and responsibilities and norms of engineering practice."},
            {"PO9", "Individual and Team Work: Function effectively as an individual, and as a member or leader in diverse teams, and in multidisciplinary settings."},
            {"PO10", "Communication: Communicate effectively on complex engineering activities with the engineering community and with society at large."},
            {"PO11", "Project Management and Finance: Demonstrate knowledge and understanding of engineering and management principles and apply these to manage projects."},
            {"PO12", "Life-long Learning: Recognize the need for, and have the preparation and ability to engage in independent and life-long learning in the broadest context of technological change."}
        };

        List<ProgrammeOutcome> posToSave = new ArrayList<>();
        for (String[] def : poDefs) {
            String code = def[0];
            String stmt = def[1];
            String poId = "po-" + pId + "-" + code.toLowerCase();

            ProgrammeOutcome po = ProgrammeOutcome.builder()
                    .id(poId)
                    .programmeId(pId)
                    .code(code)
                    .statement(stmt)
                    .build();

            poRepository.save(po);

            List<PoCompetency> comps = new ArrayList<>();
            comps.add(PoCompetency.builder()
                    .id("comp-" + poId + "-1")
                    .poId(poId)
                    .code(code + ".1")
                    .statement("Demonstrate core competency and analytical skills for " + code)
                    .build());
            comps.add(PoCompetency.builder()
                    .id("comp-" + poId + "-2")
                    .poId(poId)
                    .code(code + ".2")
                    .statement("Apply contextual knowledge and modern methods for " + code)
                    .build());
            poCompetencyRepository.saveAll(comps);
            po.setCompetencies(comps);
            posToSave.add(po);
        }
        return posToSave;
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
                compsToSave.sort(Comparator.comparing(PoCompetency::getCode, NATURAL_CODE_COMPARATOR));
                poCompetencyRepository.saveAll(compsToSave);
            }
            po.setCompetencies(compsToSave);
        }

        saved.sort(Comparator.comparing(ProgrammeOutcome::getCode, NATURAL_CODE_COMPARATOR));
        System.out.println("[OutcomeService] Saved POs (" + saved.size() + " items) with competencies for programmeId: " + programmeId);
        return saved;
    }

    @Transactional
    public List<ProgrammeSpecificOutcome> getPSOsByProgramme(String programmeId) {
        System.out.println("================================================================================");
        System.out.println("[OutcomeService] >>> getPSOsByProgramme called | programmeId: " + programmeId);
        List<ProgrammeSpecificOutcome> list = psoRepository.findByProgrammeIdOrderByCodeAsc(programmeId);
        if (list.isEmpty()) {
            System.out.println("[OutcomeService] No PSOs found in DB for programmeId: " + programmeId + ". Seeding default PSOs...");
            list = seedDefaultPSOs(programmeId);
        }
        for (ProgrammeSpecificOutcome pso : list) {
            List<PsoCompetency> comps = psoCompetencyRepository.findByPsoIdOrderByCodeAsc(pso.getId());
            comps.sort(Comparator.comparing(PsoCompetency::getCode, NATURAL_CODE_COMPARATOR));
            pso.setCompetencies(comps);
            System.out.println("  [PSO ENTRY] Code: " + pso.getCode() + " | Statement: " + pso.getStatement());
            for (PsoCompetency comp : comps) {
                System.out.println("      -> Competency Code: " + comp.getCode() + " | Statement: " + comp.getStatement());
            }
        }
        list.sort(Comparator.comparing(ProgrammeSpecificOutcome::getCode, NATURAL_CODE_COMPARATOR));
        System.out.println("[OutcomeService] <<< OUTGOING PSOs Payload (" + list.size() + " items) for programmeId: " + programmeId);
        System.out.println("================================================================================");
        return list;
    }

    private List<ProgrammeSpecificOutcome> seedDefaultPSOs(String programmeId) {
        String pId = (programmeId != null && !programmeId.isBlank()) ? programmeId : "prog-1";
        String[][] psoDefs = {
            {"PSO1", "Software System Design & Development: Ability to design, build, test and maintain scalable software applications using modern frameworks."},
            {"PSO2", "Data Analytics & AI Integration: Ability to apply data structures, machine learning algorithms and statistical models to extract insights."},
            {"PSO3", "Network Architecture & Security: Ability to configure, analyze and secure computer networks, cloud infrastructure and distributed systems."}
        };

        List<ProgrammeSpecificOutcome> psosToSave = new ArrayList<>();
        for (String[] def : psoDefs) {
            String code = def[0];
            String stmt = def[1];
            String psoId = "pso-" + pId + "-" + code.toLowerCase();

            ProgrammeSpecificOutcome pso = ProgrammeSpecificOutcome.builder()
                    .id(psoId)
                    .programmeId(pId)
                    .code(code)
                    .statement(stmt)
                    .academicYear("2025-26")
                    .build();
            psoRepository.save(pso);

            List<PsoCompetency> comps = new ArrayList<>();
            comps.add(PsoCompetency.builder()
                    .id("psocomp-" + psoId + "-1")
                    .psoId(psoId)
                    .code(code + ".1")
                    .statement("Demonstrate specialized domain skill statement 1 for " + code)
                    .build());
            comps.add(PsoCompetency.builder()
                    .id("psocomp-" + psoId + "-2")
                    .psoId(psoId)
                    .code(code + ".2")
                    .statement("Implement practical design and architecture solutions for " + code)
                    .build());
            psoCompetencyRepository.saveAll(comps);
            pso.setCompetencies(comps);
            psosToSave.add(pso);
        }
        return psosToSave;
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
                compsToSave.sort(Comparator.comparing(PsoCompetency::getCode, NATURAL_CODE_COMPARATOR));
                psoCompetencyRepository.saveAll(compsToSave);
            }
            pso.setCompetencies(compsToSave);
        }

        saved.sort(Comparator.comparing(ProgrammeSpecificOutcome::getCode, NATURAL_CODE_COMPARATOR));
        System.out.println("[OutcomeService] Saved PSOs (" + saved.size() + " items) with competencies for programmeId: " + programmeId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PeoOutcome> getPEOsByProgramme(String programmeId) {
        System.out.println("[OutcomeService] getPEOsByProgramme called | programmeId: " + programmeId);
        List<PeoOutcome> list = peoRepository.findByProgrammeIdOrderByCodeAsc(programmeId);
        list.sort(Comparator.comparing(PeoOutcome::getCode, NATURAL_CODE_COMPARATOR));
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
        saved.sort(Comparator.comparing(PeoOutcome::getCode, NATURAL_CODE_COMPARATOR));
        System.out.println("[OutcomeService] Saved PEOs (" + saved.size() + " items) for programmeId: " + programmeId);
        return saved;
    }

    private String resolveOfferingId(String offeringOrCourseId) {
        if (offeringOrCourseId == null || offeringOrCourseId.isBlank()) return null;
        if (courseOfferingRepository.existsById(offeringOrCourseId)) {
            return offeringOrCourseId;
        }
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(offeringOrCourseId);
        if (!offerings.isEmpty()) {
            return offerings.get(0).getId();
        }
        return offeringOrCourseId;
    }

    @Transactional(readOnly = true)
    public List<CourseOutcome> getCOsByCourse(String courseIdOrOfferingId) {
        System.out.println("[OutcomeService] getCOsByCourse called | courseIdOrOfferingId: " + courseIdOrOfferingId);
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        List<CourseOutcome> list = coRepository.findByCourseOfferingId(targetOfferingId);
        list.sort(Comparator.comparing(CourseOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return list;
    }

    @Transactional
    public List<CourseOutcome> saveCOs(String courseIdOrOfferingId, List<CourseOutcome> cos) {
        System.out.println("[OutcomeService] saveCOs called | courseIdOrOfferingId: " + courseIdOrOfferingId + " | count: " + (cos != null ? cos.size() : 0));
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        List<CourseOutcome> existing = coRepository.findByCourseOfferingId(targetOfferingId);
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
                co.setCourseOfferingId(targetOfferingId);

                String key = co.getCode().toLowerCase();
                CourseOutcome targetCo;
                if (existingByCode.containsKey(key)) {
                    targetCo = existingByCode.get(key);
                    targetCo.setStatement(co.getStatement());
                    if (co.getTargetLevel() != null) {
                        targetCo.setTargetLevel(co.getTargetLevel());
                    }
                } else {
                    targetCo = co;
                    if (targetCo.getId() == null || targetCo.getId().isBlank()) {
                        targetCo.setId("co-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                    if (targetCo.getTargetLevel() == null) {
                        targetCo.setTargetLevel(new BigDecimal("2.50"));
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
            coRepository.deleteAll(toDelete);
        }

        List<CourseOutcome> saved = coRepository.saveAll(toSave);
        saved.sort(Comparator.comparing(CourseOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return saved;
    }

    // --- Programme Target Benchmark Levels ---
    @Transactional(readOnly = true)
    public ProgrammeTargetDto getProgrammeTargets(String programmeId) {
        System.out.println("[OutcomeService] getProgrammeTargets called | programmeId: " + programmeId);
        List<Batch> batches = batchRepository.findByProgrammeId(programmeId);
        List<String> batchIds = batches.stream().map(Batch::getId).collect(Collectors.toList());
        List<ProgrammeTarget> list = batchIds.isEmpty() ? Collections.emptyList() : targetRepository.findByBatchIdIn(batchIds);

        Map<String, BigDecimal> poTargets = new LinkedHashMap<>();
        Map<String, BigDecimal> psoTargets = new LinkedHashMap<>();

        for (ProgrammeTarget pt : list) {
            if (pt.getOutcomeCode() != null) {
                if (pt.getOutcomeCode().toUpperCase().startsWith("PSO")) {
                    psoTargets.put(pt.getOutcomeCode(), pt.getTargetValue());
                } else if (pt.getOutcomeCode().toUpperCase().startsWith("PO")) {
                    poTargets.put(pt.getOutcomeCode(), pt.getTargetValue());
                }
            }
        }

        return ProgrammeTargetDto.builder()
                .programmeId(programmeId)
                .poTargets(poTargets)
                .psoTargets(psoTargets)
                .build();
    }

    @Transactional(readOnly = true)
    public ProgrammeTargetDto getBatchProgrammeTargets(String batchId) {
        System.out.println("[OutcomeService] getBatchProgrammeTargets called | batchId: " + batchId);
        Batch batch = batchRepository.findById(batchId).orElse(null);
        String progId = batch != null ? batch.getProgrammeId() : null;
        List<ProgrammeTarget> list = targetRepository.findByBatchId(batchId);

        Map<String, BigDecimal> poTargets = new LinkedHashMap<>();
        Map<String, BigDecimal> psoTargets = new LinkedHashMap<>();

        for (ProgrammeTarget pt : list) {
            if (pt.getOutcomeCode() != null) {
                if (pt.getOutcomeCode().toUpperCase().startsWith("PSO")) {
                    psoTargets.put(pt.getOutcomeCode(), pt.getTargetValue());
                } else if (pt.getOutcomeCode().toUpperCase().startsWith("PO")) {
                    poTargets.put(pt.getOutcomeCode(), pt.getTargetValue());
                }
            }
        }

        return ProgrammeTargetDto.builder()
                .programmeId(progId)
                .batchId(batchId)
                .poTargets(poTargets)
                .psoTargets(psoTargets)
                .build();
    }

    @Transactional
    public ProgrammeTargetDto saveProgrammeTargets(String programmeId, ProgrammeTargetDto dto) {
        System.out.println("[OutcomeService] saveProgrammeTargets called | programmeId: " + programmeId);
        if (dto == null) return getProgrammeTargets(programmeId);

        List<Batch> batches = batchRepository.findByProgrammeId(programmeId);
        String targetBatchId = (dto.getBatchId() != null && !dto.getBatchId().isBlank())
                ? dto.getBatchId()
                : (!batches.isEmpty() ? batches.get(0).getId() : "batch-" + programmeId);

        Map<String, BigDecimal> combined = new LinkedHashMap<>();
        if (dto.getPoTargets() != null) combined.putAll(dto.getPoTargets());
        if (dto.getPsoTargets() != null) combined.putAll(dto.getPsoTargets());

        for (Map.Entry<String, BigDecimal> entry : combined.entrySet()) {
            String code = entry.getKey();
            BigDecimal val = entry.getValue() != null ? entry.getValue() : new BigDecimal("2.00");
            OutcomeType oType = code.toUpperCase().startsWith("PSO") ? OutcomeType.PSO : (code.toUpperCase().startsWith("PEO") ? OutcomeType.PEO : OutcomeType.PO);

            ProgrammeTarget target = targetRepository.findByBatchIdAndOutcomeCode(targetBatchId, code)
                    .orElseGet(() -> ProgrammeTarget.builder()
                            .id("target-" + UUID.randomUUID().toString().substring(0, 8))
                            .batchId(targetBatchId)
                            .outcomeType(oType)
                            .outcomeCode(code)
                            .build());

            target.setTargetValue(val);
            target.setOutcomeType(oType);
            target.setUpdatedAt(ZonedDateTime.now());
            targetRepository.save(target);
        }

        return getProgrammeTargets(programmeId);
    }

    @Transactional
    public CourseMappingMatrixDto getCourseMappings(String courseIdOrOfferingId) {
        System.out.println("[OutcomeService] getCourseMappings called | courseIdOrOfferingId: " + courseIdOrOfferingId);
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        CourseOffering offering = courseOfferingRepository.findById(targetOfferingId).orElse(null);
        String courseId = offering != null ? offering.getCourseId() : targetOfferingId;

        Course course = courseRepository.findById(courseId).orElse(null);
        String progId = course != null ? course.getProgrammeId() : null;

        List<CourseOutcome> cos = getCOsByCourse(targetOfferingId);
        List<ProgrammeOutcome> pos = (progId != null) ? getPOsByProgramme(progId) : Collections.emptyList();
        List<ProgrammeSpecificOutcome> psos = (progId != null) ? getPSOsByProgramme(progId) : Collections.emptyList();

        List<String> coIds = cos.stream().map(CourseOutcome::getId).collect(Collectors.toList());

        List<CoPoMapping> poMappings = coIds.isEmpty() ? Collections.emptyList() : coPoMappingRepository.findByCourseOutcomeIdIn(coIds);
        List<CoPsoMapping> psoMappings = coIds.isEmpty() ? Collections.emptyList() : coPsoMappingRepository.findByCourseOutcomeIdIn(coIds);

        Map<String, Object> poKw = Collections.emptyMap();
        Map<String, Object> psoKw = Collections.emptyMap();

        Optional<CourseMappingKeyword> poKwOpt = courseMappingKeywordRepository.findByCourseOfferingIdAndKeywordType(targetOfferingId, "PO");
        if (poKwOpt.isPresent()) {
            try {
                poKw = objectMapper.readValue(poKwOpt.get().getKeywordsJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        } else if (coursePoKeywordsMap.containsKey(targetOfferingId)) {
            poKw = coursePoKeywordsMap.get(targetOfferingId);
        }

        Optional<CourseMappingKeyword> psoKwOpt = courseMappingKeywordRepository.findByCourseOfferingIdAndKeywordType(targetOfferingId, "PSO");
        if (psoKwOpt.isPresent()) {
            try {
                psoKw = objectMapper.readValue(psoKwOpt.get().getKeywordsJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        } else if (coursePsoKeywordsMap.containsKey(targetOfferingId)) {
            psoKw = coursePsoKeywordsMap.get(targetOfferingId);
        }

        return CourseMappingMatrixDto.builder()
                .courseId(courseId)
                .programmeId(progId)
                .cos(cos)
                .pos(pos)
                .psos(psos)
                .poMappings(poMappings)
                .psoMappings(psoMappings)
                .poKeywordsStore(poKw)
                .psoKeywordsStore(psoKw)
                .build();
    }


    @Transactional
    public CourseMappingMatrixDto saveCourseMappings(String courseIdOrOfferingId, CourseMappingMatrixDto dto) {
        System.out.println("[OutcomeService] saveCourseMappings called | courseIdOrOfferingId: " + courseIdOrOfferingId);
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        CourseOffering offering = courseOfferingRepository.findById(targetOfferingId).orElse(null);
        String courseId = offering != null ? offering.getCourseId() : targetOfferingId;

        Course course = courseRepository.findById(courseId).orElse(null);
        String progId = course != null ? course.getProgrammeId() : (dto != null && dto.getProgrammeId() != null ? dto.getProgrammeId() : null);

        Map<String, Object> poKwToReturn = Collections.emptyMap();
        Map<String, Object> psoKwToReturn = Collections.emptyMap();

        if (dto != null && dto.getPoKeywordsStore() != null) {
            poKwToReturn = dto.getPoKeywordsStore();
            coursePoKeywordsMap.put(targetOfferingId, poKwToReturn);
            try {
                String poJson = objectMapper.writeValueAsString(poKwToReturn);
                CourseMappingKeyword entity = courseMappingKeywordRepository.findByCourseOfferingIdAndKeywordType(targetOfferingId, "PO")
                        .orElse(CourseMappingKeyword.builder()
                                .id("kw-po-" + UUID.randomUUID().toString().substring(0, 8))
                                .courseOfferingId(targetOfferingId)
                                .keywordType("PO")
                                .build());
                entity.setKeywordsJson(poJson);
                courseMappingKeywordRepository.save(entity);
            } catch (Exception ignored) {}
        }

        if (dto != null && dto.getPsoKeywordsStore() != null) {
            psoKwToReturn = dto.getPsoKeywordsStore();
            coursePsoKeywordsMap.put(targetOfferingId, psoKwToReturn);
            try {
                String psoJson = objectMapper.writeValueAsString(psoKwToReturn);
                CourseMappingKeyword entity = courseMappingKeywordRepository.findByCourseOfferingIdAndKeywordType(targetOfferingId, "PSO")
                        .orElse(CourseMappingKeyword.builder()
                                .id("kw-pso-" + UUID.randomUUID().toString().substring(0, 8))
                                .courseOfferingId(targetOfferingId)
                                .keywordType("PSO")
                                .build());
                entity.setKeywordsJson(psoJson);
                courseMappingKeywordRepository.save(entity);
            } catch (Exception ignored) {}
        }

        List<CourseOutcome> cos = getCOsByCourse(targetOfferingId);

        List<String> coIds = cos.stream().map(CourseOutcome::getId).collect(Collectors.toList());

        if (!coIds.isEmpty()) {
            coPoMappingRepository.deleteByCourseOutcomeIdIn(coIds);
            coPsoMappingRepository.deleteByCourseOutcomeIdIn(coIds);
            coPoMappingRepository.flush();
            coPsoMappingRepository.flush();
        }

        List<CoPoMapping> savedPo = Collections.emptyList();
        if (dto != null && dto.getPoMappings() != null && !dto.getPoMappings().isEmpty()) {
            Map<String, CoPoMapping> uniquePoMap = new LinkedHashMap<>();
            for (CoPoMapping m : dto.getPoMappings()) {
                if (m.getCourseOutcomeId() == null || m.getPoCode() == null) continue;
                String key = m.getCourseOutcomeId() + "::" + m.getPoCode();
                if (m.getId() == null || m.getId().isBlank()) {
                    m.setId("copomap-" + UUID.randomUUID().toString().substring(0, 8));
                }
                uniquePoMap.put(key, m);
            }
            savedPo = coPoMappingRepository.saveAll(uniquePoMap.values());
            coPoMappingRepository.flush();
        }
        System.out.println("  [SAVED PO MAPPINGS COUNT]: " + savedPo.size());

        List<CoPsoMapping> savedPso = Collections.emptyList();
        if (dto != null && dto.getPsoMappings() != null && !dto.getPsoMappings().isEmpty()) {
            Map<String, CoPsoMapping> uniquePsoMap = new LinkedHashMap<>();
            for (CoPsoMapping m : dto.getPsoMappings()) {
                if (m.getCourseOutcomeId() == null || m.getPsoCode() == null) continue;
                String key = m.getCourseOutcomeId() + "::" + m.getPsoCode();
                if (m.getId() == null || m.getId().isBlank()) {
                    m.setId("copsomap-" + UUID.randomUUID().toString().substring(0, 8));
                }
                uniquePsoMap.put(key, m);
            }
            savedPso = coPsoMappingRepository.saveAll(uniquePsoMap.values());
            coPsoMappingRepository.flush();
        }
        System.out.println("  [SAVED PSO MAPPINGS COUNT]: " + savedPso.size());
        System.out.println("================================================================================");

        List<ProgrammeOutcome> pos = (progId != null) ? getPOsByProgramme(progId) : Collections.emptyList();
        List<ProgrammeSpecificOutcome> psos = (progId != null) ? getPSOsByProgramme(progId) : Collections.emptyList();

        return CourseMappingMatrixDto.builder()
                .courseId(courseIdOrOfferingId)
                .programmeId(progId)
                .cos(cos)
                .pos(pos)
                .psos(psos)
                .poMappings(savedPo)
                .psoMappings(savedPso)
                .poKeywordsStore(poKwToReturn)
                .psoKeywordsStore(psoKwToReturn)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CourseOutcome> getOutcomesByOffering(String offeringId) {
        System.out.println("[OutcomeService] getOutcomesByOffering called | offeringId: " + offeringId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new com.dypiu.nba.exception.ResourceNotFoundException("Course Offering not found: " + offeringId));
        return getCOsByCourse(offering.getId());
    }


    @Transactional
    public List<CourseOutcome> saveOutcomesByOffering(String offeringId, List<CourseOutcome> cos) {
        System.out.println("[OutcomeService] saveOutcomesByOffering called | offeringId: " + offeringId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new com.dypiu.nba.exception.ResourceNotFoundException("Course Offering not found: " + offeringId));
        return saveCOs(offering.getCourseId(), cos);
    }

    @Transactional
    public CourseMappingMatrixDto getMappingsByOffering(String offeringId) {
        System.out.println("[OutcomeService] getMappingsByOffering called | offeringId: " + offeringId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new com.dypiu.nba.exception.ResourceNotFoundException("Course Offering not found: " + offeringId));
        return getCourseMappings(offering.getCourseId());
    }

    @Transactional
    public CourseMappingMatrixDto saveMappingsByOffering(String offeringId, CourseMappingMatrixDto dto) {
        System.out.println("[OutcomeService] saveMappingsByOffering called | offeringId: " + offeringId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new com.dypiu.nba.exception.ResourceNotFoundException("Course Offering not found: " + offeringId));
        return saveCourseMappings(offering.getCourseId(), dto);
    }
}

