package com.dypiu.nba.service;

import com.dypiu.nba.entity.CoPoMapping;
import com.dypiu.nba.entity.CoPsoMapping;
import com.dypiu.nba.repository.CoPoMappingRepository;
import com.dypiu.nba.repository.CoPsoMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MappingService {

    private final CoPoMappingRepository coPoMappingRepository;
    private final CoPsoMappingRepository coPsoMappingRepository;

    @Transactional(readOnly = true)
    public List<CoPoMapping> getCoPoMappings(String courseOutcomeId) {
        System.out.println("[MappingService] getCoPoMappings called | courseOutcomeId: " + courseOutcomeId);
        return coPoMappingRepository.findByCourseOutcomeId(courseOutcomeId);
    }

    @Transactional
    public List<CoPoMapping> saveCoPoMappings(String courseOutcomeId, List<CoPoMapping> mappings) {
        System.out.println("[MappingService] saveCoPoMappings called | courseOutcomeId: " + courseOutcomeId + " | count: " + (mappings != null ? mappings.size() : 0));
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
        return coPsoMappingRepository.findByCourseOutcomeId(courseOutcomeId);
    }

    @Transactional
    public List<CoPsoMapping> saveCoPsoMappings(String courseOutcomeId, List<CoPsoMapping> mappings) {
        System.out.println("[MappingService] saveCoPsoMappings called | courseOutcomeId: " + courseOutcomeId + " | count: " + (mappings != null ? mappings.size() : 0));
        coPsoMappingRepository.deleteByCourseOutcomeId(courseOutcomeId);
        mappings.forEach(m -> {
            m.setCourseOutcomeId(courseOutcomeId);
            if (m.getId() == null) m.setId("copso-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return coPsoMappingRepository.saveAll(mappings);
    }
}
