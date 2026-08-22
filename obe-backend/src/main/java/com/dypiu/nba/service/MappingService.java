package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MappingService {

    private final CoPoMappingRepository coPoMappingRepository;
    private final CoPsoMappingRepository coPsoMappingRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final MasterCourseRepository masterCourseRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUserScopeService currentUserScopeService;

    private CurrentUserScope getScope() {
        try {
            return currentUserScopeService != null ? currentUserScopeService.getCurrentUserScope() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void enforceOutcomeScope(String courseOutcomeId) {
        if (courseOutcomeId == null || courseOutcomeId.isBlank()) return;
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;

        CourseOutcome co = courseOutcomeRepository.findById(courseOutcomeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Outcome not found: " + courseOutcomeId));

        String offeringId = co.getProgrammeBatchCourseId();
        if (offeringId != null && !offeringId.isBlank()) {
            ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + offeringId));

            if (scope.isFaculty()) {
                boolean isCoord = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()));
                boolean isAssigned = isCoord || (offering.getAssignedFaculty() != null && (offering.getAssignedFaculty().contains(scope.getEmail()) || offering.getAssignedFaculty().contains(scope.getName())));
                if (!isAssigned) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
                }
                return;
            }

            if (offering.getMasterCourseId() != null) {
                MasterCourse c = masterCourseRepository.findById(offering.getMasterCourseId()).orElse(null);
                if (c != null && c.getMasterProgrammeId() != null) {
                    if (scope.isProgrammeCoordinator() && !c.getMasterProgrammeId().equals(scope.getRequiredProgrammeId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Course Outcome is outside your assigned programme scope.");
                    }
                    MasterProgramme p = masterProgrammeRepository.findById(c.getMasterProgrammeId()).orElse(null);
                    if (p != null && p.getDepartmentId() != null) {
                        if (scope.isHod() && !p.getDepartmentId().equals(scope.getRequiredDepartmentId())) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Course Outcome is outside your assigned department scope.");
                        }
                        Department d = departmentRepository.findById(p.getDepartmentId()).orElse(null);
                        if (d != null && d.getSchoolId() != null && scope.isDirector() && !d.getSchoolId().equals(scope.getRequiredSchoolId())) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Course Outcome is outside your assigned school scope.");
                        }
                    }
                }
            }
            return;
        }
    }

    @Transactional(readOnly = true)
    public List<CoPoMapping> getCoPoMappings(String courseOutcomeId) {
        System.out.println("[MappingService] getCoPoMappings called | courseOutcomeId: " + courseOutcomeId);
        enforceOutcomeScope(courseOutcomeId);
        return coPoMappingRepository.findByCourseOutcomeId(courseOutcomeId);
    }

    @Transactional
    public List<CoPoMapping> saveCoPoMappings(String courseOutcomeId, List<CoPoMapping> mappings) {
        System.out.println("[MappingService] saveCoPoMappings called | courseOutcomeId: " + courseOutcomeId + " | count: " + (mappings != null ? mappings.size() : 0));
        enforceOutcomeScope(courseOutcomeId);
        coPoMappingRepository.deleteByCourseOutcomeId(courseOutcomeId);
        mappings.forEach(m -> {
            m.setCourseOutcomeId(courseOutcomeId);
            if (m.getId() == null) m.setId("copo-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return coPoMappingRepository.saveAll(mappings);
    }

    @Transactional(readOnly = true)
    public List<CoPsoMapping> getCoPsoMappings(String courseOutcomeId) {
        System.out.println("[MappingService] getCoPsoMappings called | courseOutcomeId: " + courseOutcomeId);
        enforceOutcomeScope(courseOutcomeId);
        return coPsoMappingRepository.findByCourseOutcomeId(courseOutcomeId);
    }

    @Transactional
    public List<CoPsoMapping> saveCoPsoMappings(String courseOutcomeId, List<CoPsoMapping> mappings) {
        System.out.println("[MappingService] saveCoPsoMappings called | courseOutcomeId: " + courseOutcomeId + " | count: " + (mappings != null ? mappings.size() : 0));
        enforceOutcomeScope(courseOutcomeId);
        coPsoMappingRepository.deleteByCourseOutcomeId(courseOutcomeId);
        mappings.forEach(m -> {
            m.setCourseOutcomeId(courseOutcomeId);
            if (m.getId() == null) m.setId("copso-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return coPsoMappingRepository.saveAll(mappings);
    }
}
