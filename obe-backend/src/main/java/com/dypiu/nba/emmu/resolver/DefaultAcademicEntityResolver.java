package com.dypiu.nba.emmu.resolver;

import com.dypiu.nba.emmu.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default authoritative implementation of AcademicEntityResolver for Emmu.
 * Enforces strict user-scoped candidate filtering before performing multi-strategy natural language matching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAcademicEntityResolver implements AcademicEntityResolver {

    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final UserRepository userRepository;
    private final CurrentUserScopeService currentUserScopeService;

    // =========================================================================
    // MAIN RESOLVE ENTRY POINT
    // =========================================================================

    @Override
    public EntityResolutionResult resolve(EntityResolutionRequest request, Principal principal) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return EntityResolutionResult.builder()
                    .status(ResolutionStatus.NOT_FOUND)
                    .matchedText(request != null ? request.getQuery() : null)
                    .confidence(0.0)
                    .candidateCount(0)
                    .candidates(Collections.emptyList())
                    .build();
        }

        String query = request.getQuery().trim();
        EmmuConversationContext ctx = request.getContext();
        AcademicEntityType targetType = request.getEntityType();

        if (targetType != null) {
            return switch (targetType) {
                case SCHOOL -> resolveSchool(query, ctx, principal);
                case DEPARTMENT -> resolveDepartment(query, ctx, principal);
                case PROGRAMME -> resolveProgramme(query, ctx, principal);
                case PROGRAMME_BATCH -> resolveBatch(query, ctx, principal);
                case COURSE -> resolveCourse(query, ctx, principal);
                case COURSE_OFFERING -> resolveCourseOffering(query, ctx, principal);
                case CO -> resolveCO(query, ctx, principal);
                case PO -> resolvePO(query, ctx, principal);
                case PSO -> resolvePSO(query, ctx, principal);
                case FACULTY -> resolveFaculty(query, ctx, principal);
                case SEMESTER -> resolveSemester(query, ctx, principal);
            };
        }

        // Auto-detect entity type based on linguistic cues
        AcademicEntityType detected = detectEntityType(query, ctx);
        if (detected != null) {
            return switch (detected) {
                case CO -> resolveCO(query, ctx, principal);
                case PO -> resolvePO(query, ctx, principal);
                case PSO -> resolvePSO(query, ctx, principal);
                case SEMESTER -> resolveSemester(query, ctx, principal);
                case PROGRAMME_BATCH -> resolveBatch(query, ctx, principal);
                case SCHOOL -> resolveSchool(query, ctx, principal);
                case DEPARTMENT -> resolveDepartment(query, ctx, principal);
                case FACULTY -> resolveFaculty(query, ctx, principal);
                case COURSE, COURSE_OFFERING -> resolveCourse(query, ctx, principal);
                case PROGRAMME -> resolveProgramme(query, ctx, principal);
            };
        }

        // Broad contextual fallback order: Outcome -> Course -> Batch -> Programme -> Dept -> School
        List<EntityResolutionResult> attempts = List.of(
                resolvePO(query, ctx, principal),
                resolveCO(query, ctx, principal),
                resolveCourse(query, ctx, principal),
                resolveBatch(query, ctx, principal),
                resolveProgramme(query, ctx, principal),
                resolveDepartment(query, ctx, principal)
        );

        for (EntityResolutionResult res : attempts) {
            if (res.getStatus() == ResolutionStatus.RESOLVED && res.getConfidence() >= 0.85) {
                return res;
            }
        }

        for (EntityResolutionResult res : attempts) {
            if (res.getStatus() == ResolutionStatus.RESOLVED || res.getStatus() == ResolutionStatus.AMBIGUOUS) {
                return res;
            }
        }

        return EntityResolutionResult.builder()
                .status(ResolutionStatus.NOT_FOUND)
                .matchedText(query)
                .confidence(0.0)
                .candidateCount(0)
                .candidates(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // 1. SCHOOL RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveSchool(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<School> authorizedSchools = getAuthorizedSchools(userScope);

        // Check Conversational References
        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("this school") || normQuery.equals("that school") || normQuery.equals("the school")) {
            if (context != null && context.getSchoolId() != null) {
                return matchById(authorizedSchools.stream().map(s -> new ScoredCandidate(s.getId(), s.getName(), s.getCode(), null, s)).toList(),
                        context.getSchoolId(), AcademicEntityType.SCHOOL, query, true);
            }
        }

        List<ScoredCandidate> candidates = authorizedSchools.stream()
                .map(s -> new ScoredCandidate(s.getId(), s.getName(), s.getCode(), "Director: " + (s.getDirectorName() != null ? s.getDirectorName() : "N/A"), s))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.SCHOOL, context);
    }

    // =========================================================================
    // 2. DEPARTMENT RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveDepartment(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<Department> authorizedDepartments = getAuthorizedDepartments(userScope, context);

        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("this department") || normQuery.equals("that department") || normQuery.equals("the department") || normQuery.equals("this dept")) {
            if (context != null && context.getDepartmentId() != null) {
                return matchById(authorizedDepartments.stream().map(d -> new ScoredCandidate(d.getId(), d.getName(), d.getCode(), null, d)).toList(),
                        context.getDepartmentId(), AcademicEntityType.DEPARTMENT, query, true);
            }
        }

        List<ScoredCandidate> candidates = authorizedDepartments.stream()
                .map(d -> new ScoredCandidate(d.getId(), d.getName(), d.getCode(), "HOD: " + (d.getHod() != null ? d.getHod() : "N/A"), d))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.DEPARTMENT, context);
    }

    // =========================================================================
    // 3. PROGRAMME RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveProgramme(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<MasterProgramme> authorizedProgrammes = getAuthorizedProgrammes(userScope, context);

        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("this programme") || normQuery.equals("that programme") || normQuery.equals("the programme") || normQuery.equals("this program") || normQuery.equals("it")) {
            if (context != null && context.getMasterProgrammeId() != null) {
                return matchById(authorizedProgrammes.stream().map(p -> new ScoredCandidate(p.getId(), p.getName(), p.getDegreeAwarded(), null, p)).toList(),
                        context.getMasterProgrammeId(), AcademicEntityType.PROGRAMME, query, true);
            }
        }

        List<ScoredCandidate> candidates = authorizedProgrammes.stream()
                .map(p -> new ScoredCandidate(p.getId(), p.getName(), p.getDegreeAwarded(), "Dept: " + p.getDepartmentName(), p))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.PROGRAMME, context);
    }

    // =========================================================================
    // 4. PROGRAMME BATCH RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveBatch(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<ProgrammeBatch> authorizedBatches = getAuthorizedBatches(userScope, context);

        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("this batch") || normQuery.equals("that batch") || normQuery.equals("the batch") || normQuery.equals("it")) {
            if (context != null && context.getProgrammeBatchId() != null) {
                return matchById(authorizedBatches.stream().map(b -> new ScoredCandidate(b.getId(), b.getName(), String.valueOf(b.getStartYear()), null, b)).toList(),
                        context.getProgrammeBatchId(), AcademicEntityType.PROGRAMME_BATCH, query, true);
            }
        }

        // Conversational 'next batch' / 'previous batch'
        if (normQuery.contains("next batch") && context != null && context.getProgrammeBatchId() != null) {
            ProgrammeBatch curr = authorizedBatches.stream().filter(b -> b.getId().equals(context.getProgrammeBatchId())).findFirst().orElse(null);
            if (curr != null) {
                ProgrammeBatch next = authorizedBatches.stream()
                        .filter(b -> b.getMasterProgrammeId().equals(curr.getMasterProgrammeId()) && b.getStartYear() > curr.getStartYear())
                        .min(Comparator.comparing(ProgrammeBatch::getStartYear)).orElse(null);
                if (next != null) {
                    return buildDirectSuccess(next.getId(), next.getName(), String.valueOf(next.getStartYear()), AcademicEntityType.PROGRAMME_BATCH, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, true);
                }
            }
        }

        if ((normQuery.contains("previous batch") || normQuery.contains("last batch") || normQuery.contains("prev batch")) && context != null && context.getProgrammeBatchId() != null) {
            ProgrammeBatch curr = authorizedBatches.stream().filter(b -> b.getId().equals(context.getProgrammeBatchId())).findFirst().orElse(null);
            if (curr != null) {
                ProgrammeBatch prev = authorizedBatches.stream()
                        .filter(b -> b.getMasterProgrammeId().equals(curr.getMasterProgrammeId()) && b.getStartYear() < curr.getStartYear())
                        .max(Comparator.comparing(ProgrammeBatch::getStartYear)).orElse(null);
                if (prev != null) {
                    return buildDirectSuccess(prev.getId(), prev.getName(), String.valueOf(prev.getStartYear()), AcademicEntityType.PROGRAMME_BATCH, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, true);
                }
            }
        }

        List<ScoredCandidate> candidates = authorizedBatches.stream()
                .map(b -> new ScoredCandidate(b.getId(), b.getName(), String.valueOf(b.getStartYear()), "Years: " + b.getStartYear() + "-" + b.getEndYear(), b))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.PROGRAMME_BATCH, context);
    }

    // =========================================================================
    // 5. COURSE / COURSE OFFERING RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveCourse(String query, EmmuConversationContext context, Principal principal) {
        return resolveCourseOffering(query, context, principal);
    }

    @Override
    public EntityResolutionResult resolveCourseOffering(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<ProgrammeBatchCourse> authorizedCourses = getAuthorizedCourseOfferings(userScope, context);

        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("this course") || normQuery.equals("that course") || normQuery.equals("the course") || normQuery.equals("it")) {
            if (context != null && context.getProgrammeBatchCourseId() != null) {
                return matchById(authorizedCourses.stream().map(c -> new ScoredCandidate(c.getId(), c.getName(), c.getCode(), null, c)).toList(),
                        context.getProgrammeBatchCourseId(), AcademicEntityType.COURSE_OFFERING, query, true);
            }
        }

        List<ScoredCandidate> candidates = authorizedCourses.stream()
                .map(c -> new ScoredCandidate(c.getId(), c.getEffectiveCourseName() != null ? c.getEffectiveCourseName() : c.getName(),
                        c.getEffectiveCourseCode() != null ? c.getEffectiveCourseCode() : c.getCode(),
                        "Semester: " + c.getSemester() + ", Faculty: " + (c.getAssignedFaculty() != null ? c.getAssignedFaculty() : "N/A"), c))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.COURSE_OFFERING, context);
    }

    // =========================================================================
    // 6. COURSE OUTCOME (CO) RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveCO(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<ProgrammeBatchCourse> authorizedCourses = getAuthorizedCourseOfferings(userScope, context);
        List<String> authorizedCourseIds = authorizedCourses.stream().map(ProgrammeBatchCourse::getId).toList();

        List<CourseOutcome> authorizedCos = authorizedCourseIds.isEmpty() ? Collections.emptyList() :
                courseOutcomeRepository.findByProgrammeBatchCourseIdIn(authorizedCourseIds);

        String normQuery = EntityMatchingUtils.normalize(query);

        // Conversational 'its CO2' / 'that CO'
        if (normQuery.startsWith("its ") || normQuery.startsWith("that co") || normQuery.startsWith("this co")) {
            String targetCo = normQuery.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT).replace("ITS", "").replace("THAT", "").replace("THIS", "").trim();
            if (context != null && context.getProgrammeBatchCourseId() != null) {
                CourseOutcome matching = authorizedCos.stream()
                        .filter(c -> c.getProgrammeBatchCourseId().equals(context.getProgrammeBatchCourseId()) &&
                                EntityMatchingUtils.normalizeCode(c.getCode()).equals(EntityMatchingUtils.normalizeCode(targetCo)))
                        .findFirst().orElse(null);
                if (matching != null) {
                    return buildDirectSuccess(matching.getId(), matching.getCode() + ": " + matching.getStatement(), matching.getCode(), AcademicEntityType.CO, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, true);
                }
            }
        }

        List<ScoredCandidate> candidates = authorizedCos.stream()
                .map(co -> new ScoredCandidate(co.getId(), co.getCode() + ": " + co.getStatement(), co.getCode(), "Target: " + co.getTargetLevel(), co))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.CO, context);
    }

    // =========================================================================
    // 7. PROGRAMME OUTCOME (PO) RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolvePO(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<ProgrammeBatch> authorizedBatches = getAuthorizedBatches(userScope, context);
        List<String> batchIds = authorizedBatches.stream().map(ProgrammeBatch::getId).toList();

        List<ProgrammeOutcome> authorizedPos = batchIds.isEmpty() ? Collections.emptyList() :
                batchIds.stream().flatMap(bId -> programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(bId).stream()).toList();

        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("that po") || normQuery.equals("this po") || normQuery.equals("the po") || normQuery.equals("the outcome") || normQuery.equals("it")) {
            if (context != null && context.getOutcomeCode() != null && ("PO".equalsIgnoreCase(context.getOutcomeType()) || context.getOutcomeCode().startsWith("PO"))) {
                ProgrammeOutcome po = authorizedPos.stream()
                        .filter(p -> EntityMatchingUtils.normalizeCode(p.getCode()).equals(EntityMatchingUtils.normalizeCode(context.getOutcomeCode())))
                        .findFirst().orElse(null);
                if (po != null) {
                    return buildDirectSuccess(po.getId(), po.getCode() + ": " + po.getStatement(), po.getCode(), AcademicEntityType.PO, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, true);
                }
            }
        }

        List<ScoredCandidate> candidates = authorizedPos.stream()
                .map(po -> new ScoredCandidate(po.getId(), po.getCode() + ": " + po.getStatement(), po.getCode(), "Target: " + po.getTarget(), po))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.PO, context);
    }

    // =========================================================================
    // 8. PROGRAMME SPECIFIC OUTCOME (PSO) RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolvePSO(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<ProgrammeBatch> authorizedBatches = getAuthorizedBatches(userScope, context);
        List<String> batchIds = authorizedBatches.stream().map(ProgrammeBatch::getId).toList();

        List<ProgrammeSpecificOutcome> authorizedPsos = batchIds.isEmpty() ? Collections.emptyList() :
                batchIds.stream().flatMap(bId -> programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(bId).stream()).toList();

        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("that pso") || normQuery.equals("this pso") || normQuery.equals("the pso")) {
            if (context != null && context.getOutcomeCode() != null && ("PSO".equalsIgnoreCase(context.getOutcomeType()) || context.getOutcomeCode().startsWith("PSO"))) {
                ProgrammeSpecificOutcome pso = authorizedPsos.stream()
                        .filter(p -> EntityMatchingUtils.normalizeCode(p.getCode()).equals(EntityMatchingUtils.normalizeCode(context.getOutcomeCode())))
                        .findFirst().orElse(null);
                if (pso != null) {
                    return buildDirectSuccess(pso.getId(), pso.getCode() + ": " + pso.getStatement(), pso.getCode(), AcademicEntityType.PSO, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, true);
                }
            }
        }

        List<ScoredCandidate> candidates = authorizedPsos.stream()
                .map(pso -> new ScoredCandidate(pso.getId(), pso.getCode() + ": " + pso.getStatement(), pso.getCode(), "Target: " + pso.getTarget(), pso))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.PSO, context);
    }

    // =========================================================================
    // 9. FACULTY RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveFaculty(String query, EmmuConversationContext context, Principal principal) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
        List<User> authorizedFaculty = getAuthorizedFaculty(userScope, context);

        String normQuery = EntityMatchingUtils.normalize(query);
        if (normQuery.equals("that faculty") || normQuery.equals("this teacher") || normQuery.equals("the professor") || normQuery.equals("the coordinator")) {
            if (context != null && context.getLastResolvedEntityType() == AcademicEntityType.FACULTY && context.getLastResolvedEntityId() != null) {
                return matchById(authorizedFaculty.stream().map(u -> new ScoredCandidate(String.valueOf(u.getId()), u.getName(), u.getEmail(), null, u)).toList(),
                        context.getLastResolvedEntityId(), AcademicEntityType.FACULTY, query, true);
            }
        }

        List<ScoredCandidate> candidates = authorizedFaculty.stream()
                .map(u -> new ScoredCandidate(String.valueOf(u.getId()), u.getName(), u.getEmail(), "Role: " + u.getRole(), u))
                .toList();

        return resolveGeneric(candidates, query, AcademicEntityType.FACULTY, context);
    }

    // =========================================================================
    // 10. SEMESTER RESOLUTION
    // =========================================================================

    @Override
    public EntityResolutionResult resolveSemester(String query, EmmuConversationContext context, Principal principal) {
        String normQuery = EntityMatchingUtils.normalize(query);

        // Conversational: next semester / previous semester
        if (normQuery.contains("next semester") || normQuery.contains("next sem")) {
            int currentSem = (context != null && context.getSemester() != null) ? context.getSemester() : 1;
            int nextSem = Math.min(8, currentSem + 1);
            return buildDirectSuccess(String.valueOf(nextSem), "Semester " + nextSem, "SEM" + nextSem, AcademicEntityType.SEMESTER, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, true);
        }

        if (normQuery.contains("previous semester") || normQuery.contains("prev semester") || normQuery.contains("last semester") || normQuery.contains("prev sem")) {
            int currentSem = (context != null && context.getSemester() != null) ? context.getSemester() : 2;
            int prevSem = Math.max(1, currentSem - 1);
            return buildDirectSuccess(String.valueOf(prevSem), "Semester " + prevSem, "SEM" + prevSem, AcademicEntityType.SEMESTER, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, true);
        }

        Optional<Integer> extractedNum = EntityMatchingUtils.extractInteger(query);
        if (extractedNum.isPresent()) {
            int sem = extractedNum.get();
            if (sem >= 1 && sem <= 10) {
                return buildDirectSuccess(String.valueOf(sem), "Semester " + sem, "SEM" + sem, AcademicEntityType.SEMESTER, query, ResolutionMethod.EXACT_CODE, 1.0, false);
            }
        }

        return EntityResolutionResult.builder()
                .entityType(AcademicEntityType.SEMESTER)
                .status(ResolutionStatus.NOT_FOUND)
                .matchedText(query)
                .confidence(0.0)
                .candidateCount(0)
                .candidates(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // CORE GENERIC RESOLUTION ENGINE
    // =========================================================================

    private EntityResolutionResult resolveGeneric(List<ScoredCandidate> candidates, String query, AcademicEntityType entityType, EmmuConversationContext context) {
        if (candidates == null || candidates.isEmpty()) {
            return EntityResolutionResult.builder()
                    .entityType(entityType)
                    .status(ResolutionStatus.NOT_FOUND)
                    .matchedText(query)
                    .confidence(0.0)
                    .candidateCount(0)
                    .candidates(Collections.emptyList())
                    .build();
        }

        String rawQuery = query.trim();
        String normQuery = EntityMatchingUtils.normalize(rawQuery);
        String codeQuery = EntityMatchingUtils.normalizeCode(rawQuery);

        // 1. Check direct canonical ID match
        for (ScoredCandidate cand : candidates) {
            if (cand.id.equalsIgnoreCase(rawQuery) || cand.id.equalsIgnoreCase(codeQuery)) {
                return buildDirectSuccess(cand.id, cand.name, cand.code, entityType, query, ResolutionMethod.EXACT_CODE, 1.0, false);
            }
        }

        // 2. Exact code or exact name match
        for (ScoredCandidate cand : candidates) {
            if (cand.code != null && EntityMatchingUtils.normalizeCode(cand.code).equals(codeQuery)) {
                return buildDirectSuccess(cand.id, cand.name, cand.code, entityType, query, ResolutionMethod.EXACT_CODE, 1.0, false);
            }
            if (cand.name != null && cand.name.equalsIgnoreCase(rawQuery)) {
                return buildDirectSuccess(cand.id, cand.name, cand.code, entityType, query, ResolutionMethod.EXACT_NAME, 1.0, false);
            }
        }

        // 3. Normalized name match
        for (ScoredCandidate cand : candidates) {
            if (cand.name != null && EntityMatchingUtils.normalize(cand.name).equals(normQuery)) {
                return buildDirectSuccess(cand.id, cand.name, cand.code, entityType, query, ResolutionMethod.NORMALIZED_NAME, 0.98, false);
            }
        }

        // 4. Dynamic Acronyms and Aliases Match
        List<ScoredCandidate> acronymMatches = new ArrayList<>();
        for (ScoredCandidate cand : candidates) {
            Set<String> candAcronyms = EntityMatchingUtils.generateAcronyms(cand.name);
            if (cand.code != null) candAcronyms.add(EntityMatchingUtils.normalizeCode(cand.code));
            if (candAcronyms.stream().anyMatch(a -> a.equalsIgnoreCase(codeQuery) || a.equalsIgnoreCase(rawQuery))) {
                cand.score = 0.92;
                cand.method = ResolutionMethod.ACRONYM_OR_ALIAS;
                acronymMatches.add(cand);
            }
        }

        if (acronymMatches.size() == 1) {
            ScoredCandidate best = acronymMatches.get(0);
            return buildDirectSuccess(best.id, best.name, best.code, entityType, query, ResolutionMethod.ACRONYM_OR_ALIAS, 0.92, false);
        } else if (acronymMatches.size() > 1) {
            return buildAmbiguityResult(acronymMatches, entityType, query);
        }

        // 5. Fuzzy / Levenshtein matching & token overlap
        List<ScoredCandidate> scoredList = new ArrayList<>();
        for (ScoredCandidate cand : candidates) {
            double nameSim = EntityMatchingUtils.similarity(cand.name, rawQuery);
            double tokenSim = EntityMatchingUtils.tokenSimilarity(cand.name, rawQuery);
            double codeSim = cand.code != null ? EntityMatchingUtils.similarity(cand.code, rawQuery) : 0.0;

            double maxScore = Math.max(nameSim, Math.max(tokenSim, codeSim));

            // Contextual boost if candidate belongs to active context
            if (context != null && maxScore >= 0.50) {
                if (cand.source instanceof ProgrammeBatchCourse pbc && pbc.getProgrammeBatchId().equalsIgnoreCase(context.getProgrammeBatchId())) {
                    maxScore = Math.min(1.0, maxScore + 0.10);
                } else if (cand.source instanceof ProgrammeBatch pb && pb.getMasterProgrammeId().equalsIgnoreCase(context.getMasterProgrammeId())) {
                    maxScore = Math.min(1.0, maxScore + 0.10);
                }
            }

            if (maxScore >= 0.65) {
                cand.score = maxScore;
                cand.method = ResolutionMethod.FUZZY_MATCH;
                scoredList.add(cand);
            }
        }

        scoredList.sort((a, b) -> Double.compare(b.score, a.score));

        if (scoredList.isEmpty()) {
            return EntityResolutionResult.builder()
                    .entityType(entityType)
                    .status(ResolutionStatus.NOT_FOUND)
                    .matchedText(query)
                    .confidence(0.0)
                    .candidateCount(0)
                    .candidates(Collections.emptyList())
                    .build();
        }

        ScoredCandidate top = scoredList.get(0);

        // Check Ambiguity: multiple candidates with close scores (difference <= 0.08 and score >= 0.75)
        List<ScoredCandidate> ambiguousSet = scoredList.stream()
                .filter(c -> c.score >= 0.75 && (top.score - c.score) <= 0.08)
                .toList();

        if (ambiguousSet.size() > 1) {
            return buildAmbiguityResult(ambiguousSet, entityType, query);
        }

        return buildDirectSuccess(top.id, top.name, top.code, entityType, query, top.method, top.score, false);
    }

    // =========================================================================
    // AUTHORIZED CANDIDATE RETRIEVAL (STRICT SCOPING)
    // =========================================================================

    private List<School> getAuthorizedSchools(CurrentUserScope userScope) {
        if (userScope == null) return Collections.emptyList();
        if (userScope.isIqac()) {
            return schoolRepository.findAll();
        }
        if (userScope.isDirector() && userScope.hasSchoolScope()) {
            return schoolRepository.findById(userScope.getSchoolId()).map(List::of).orElse(Collections.emptyList());
        }
        if (userScope.isHod() && userScope.hasDepartmentScope()) {
            Department d = departmentRepository.findById(userScope.getDepartmentId()).orElse(null);
            if (d != null) {
                return schoolRepository.findById(d.getSchoolId()).map(List::of).orElse(Collections.emptyList());
            }
        }
        if (userScope.hasSchoolScope()) {
            return schoolRepository.findById(userScope.getSchoolId()).map(List::of).orElse(Collections.emptyList());
        }
        return schoolRepository.findAll();
    }

    private List<Department> getAuthorizedDepartments(CurrentUserScope userScope, EmmuConversationContext context) {
        if (userScope == null) return Collections.emptyList();
        if (userScope.isIqac()) {
            if (context != null && context.getSchoolId() != null) {
                return departmentRepository.findBySchoolId(context.getSchoolId());
            }
            return departmentRepository.findAll();
        }
        if (userScope.isDirector() && userScope.hasSchoolScope()) {
            return departmentRepository.findBySchoolId(userScope.getSchoolId());
        }
        if (userScope.isHod() && userScope.hasDepartmentScope()) {
            return departmentRepository.findById(userScope.getDepartmentId()).map(List::of).orElse(Collections.emptyList());
        }
        if (userScope.hasDepartmentScope()) {
            return departmentRepository.findById(userScope.getDepartmentId()).map(List::of).orElse(Collections.emptyList());
        }
        return departmentRepository.findAll();
    }

    private List<MasterProgramme> getAuthorizedProgrammes(CurrentUserScope userScope, EmmuConversationContext context) {
        if (userScope == null) return Collections.emptyList();
        if (userScope.isIqac()) {
            return masterProgrammeRepository.findByDeletedAtIsNull();
        }
        if (userScope.isDirector() && userScope.hasSchoolScope()) {
            List<Department> depts = departmentRepository.findBySchoolId(userScope.getSchoolId());
            List<String> deptIds = depts.stream().map(Department::getId).toList();
            return masterProgrammeRepository.findByDeletedAtIsNull().stream()
                    .filter(p -> deptIds.contains(p.getDepartmentId())).toList();
        }
        if (userScope.isHod() && userScope.hasDepartmentScope()) {
            return masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(userScope.getDepartmentId());
        }
        if (userScope.isProgrammeCoordinator() && userScope.hasProgrammeScope()) {
            return masterProgrammeRepository.findById(userScope.getMasterProgrammeId()).map(List::of).orElse(Collections.emptyList());
        }
        return masterProgrammeRepository.findByDeletedAtIsNull();
    }

    private List<ProgrammeBatch> getAuthorizedBatches(CurrentUserScope userScope, EmmuConversationContext context) {
        List<MasterProgramme> progs = getAuthorizedProgrammes(userScope, context);
        if (progs.isEmpty()) return Collections.emptyList();
        List<String> progIds = progs.stream().map(MasterProgramme::getId).toList();
        return programmeBatchRepository.findByDeletedAtIsNull().stream()
                .filter(b -> progIds.contains(b.getMasterProgrammeId()))
                .toList();
    }

    private List<ProgrammeBatchCourse> getAuthorizedCourseOfferings(CurrentUserScope userScope, EmmuConversationContext context) {
        List<ProgrammeBatch> batches = getAuthorizedBatches(userScope, context);
        List<String> batchIds = batches.stream().map(ProgrammeBatch::getId).toList();
        if (batchIds.isEmpty()) return Collections.emptyList();

        List<ProgrammeBatchCourse> allCoursesInScope = programmeBatchCourseRepository.findByProgrammeBatchIdInAndDeletedAtIsNull(batchIds);

        if (userScope != null && userScope.isFaculty()) {
            // Strict Faculty Scoping: only courses assigned to user
            Long userId = userScope.getUserId();
            String userEmail = userScope.getEmail() != null ? userScope.getEmail().toLowerCase(Locale.ROOT) : "";
            String userName = userScope.getName() != null ? userScope.getName().toLowerCase(Locale.ROOT) : "";

            return allCoursesInScope.stream().filter(c ->
                    (userId != null && userId.equals(c.getCourseCoordinatorId())) ||
                    (c.getCourseCoordinatorName() != null && c.getCourseCoordinatorName().toLowerCase(Locale.ROOT).contains(userName)) ||
                    (c.getAssignedFaculty() != null && (c.getAssignedFaculty().toLowerCase(Locale.ROOT).contains(userEmail) ||
                            c.getAssignedFaculty().toLowerCase(Locale.ROOT).contains(userName)))
            ).toList();
        }

        return allCoursesInScope;
    }

    private List<User> getAuthorizedFaculty(CurrentUserScope userScope, EmmuConversationContext context) {
        if (userScope == null) return Collections.emptyList();
        List<User> allUsers = userRepository.findAll();
        if (userScope.isIqac()) {
            return allUsers;
        }
        if (userScope.isDirector() && userScope.hasSchoolScope()) {
            return allUsers.stream().filter(u -> userScope.getSchoolId().equalsIgnoreCase(u.getSchoolId())).toList();
        }
        if (userScope.isHod() && userScope.hasDepartmentScope()) {
            return allUsers.stream().filter(u -> userScope.getDepartmentId().equalsIgnoreCase(u.getDepartmentId())).toList();
        }
        return allUsers.stream().filter(u -> u.getId().equals(userScope.getUserId())).toList();
    }

    // =========================================================================
    // HELPER BUILDERS & DETECTORS
    // =========================================================================

    private AcademicEntityType detectEntityType(String query, EmmuConversationContext context) {
        String norm = EntityMatchingUtils.normalize(query);
        String code = EntityMatchingUtils.normalizeCode(query);

        if (code.matches("^PO\\d+$") || norm.startsWith("po ") || norm.startsWith("programme outcome")) return AcademicEntityType.PO;
        if (code.matches("^PSO\\d+$") || norm.startsWith("pso ") || norm.startsWith("programme specific outcome")) return AcademicEntityType.PSO;
        if (code.matches("^CO\\d+$") || norm.startsWith("co ") || norm.startsWith("course outcome") || norm.startsWith("its co")) return AcademicEntityType.CO;
        if (norm.startsWith("semester") || norm.startsWith("sem ") || norm.matches("^sem\\d+$") || norm.contains("next semester") || norm.contains("previous semester")) return AcademicEntityType.SEMESTER;
        if (norm.startsWith("batch") || norm.matches("^\\d{4}$") || norm.contains("batch 20") || norm.contains("next batch") || norm.contains("previous batch")) return AcademicEntityType.PROGRAMME_BATCH;
        if (norm.startsWith("school") || norm.contains("school of")) return AcademicEntityType.SCHOOL;
        if (norm.startsWith("department") || norm.startsWith("dept ") || norm.contains("department of")) return AcademicEntityType.DEPARTMENT;
        if (norm.startsWith("dr ") || norm.startsWith("prof ") || norm.startsWith("faculty") || norm.startsWith("teacher")) return AcademicEntityType.FACULTY;

        return null;
    }

    private EntityResolutionResult matchById(List<ScoredCandidate> candidates, String targetId, AcademicEntityType type, String query, boolean contextUsed) {
        ScoredCandidate found = candidates.stream().filter(c -> c.id.equalsIgnoreCase(targetId)).findFirst().orElse(null);
        if (found != null) {
            return buildDirectSuccess(found.id, found.name, found.code, type, query, ResolutionMethod.CONVERSATIONAL_REFERENCE, 1.0, contextUsed);
        }
        return EntityResolutionResult.builder()
                .entityType(type)
                .status(ResolutionStatus.NOT_FOUND)
                .matchedText(query)
                .confidence(0.0)
                .candidateCount(0)
                .candidates(Collections.emptyList())
                .contextUsed(contextUsed)
                .build();
    }

    private EntityResolutionResult buildDirectSuccess(String id, String name, String code, AcademicEntityType type, String matchedText, ResolutionMethod method, double confidence, boolean contextUsed) {
        EntityCandidateDto candidate = EntityCandidateDto.builder()
                .canonicalId(id)
                .name(name)
                .code(code)
                .confidenceScore(confidence)
                .matchType(method)
                .build();

        return EntityResolutionResult.builder()
                .entityType(type)
                .status(ResolutionStatus.RESOLVED)
                .canonicalId(id)
                .canonicalName(name)
                .canonicalCode(code)
                .matchedText(matchedText)
                .resolutionMethod(method)
                .confidence(confidence)
                .isAmbiguous(false)
                .requiresClarification(false)
                .candidateCount(1)
                .candidates(List.of(candidate))
                .contextUsed(contextUsed)
                .build();
    }

    private EntityResolutionResult buildAmbiguityResult(List<ScoredCandidate> candidates, AcademicEntityType type, String query) {
        List<EntityCandidateDto> dtos = candidates.stream()
                .map(c -> EntityCandidateDto.builder()
                        .canonicalId(c.id)
                        .name(c.name)
                        .code(c.code)
                        .additionalInfo(c.additionalInfo)
                        .confidenceScore(c.score)
                        .matchType(c.method != null ? c.method : ResolutionMethod.FUZZY_MATCH)
                        .build())
                .toList();

        String candidateNames = candidates.stream().map(c -> "'" + c.name + "'").collect(Collectors.joining(" or "));
        String prompt = "I found multiple possible matches: " + candidateNames + ". Which one would you like to explore?";

        return EntityResolutionResult.builder()
                .entityType(type)
                .status(ResolutionStatus.AMBIGUOUS)
                .matchedText(query)
                .resolutionMethod(ResolutionMethod.FUZZY_MATCH)
                .confidence(candidates.get(0).score)
                .isAmbiguous(true)
                .requiresClarification(true)
                .clarificationPrompt(prompt)
                .candidateCount(dtos.size())
                .candidates(dtos)
                .build();
    }

    private static class ScoredCandidate {
        String id;
        String name;
        String code;
        String additionalInfo;
        Object source;
        double score = 0.0;
        ResolutionMethod method;

        ScoredCandidate(String id, String name, String code, String additionalInfo, Object source) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.additionalInfo = additionalInfo;
            this.source = source;
        }
    }
}
