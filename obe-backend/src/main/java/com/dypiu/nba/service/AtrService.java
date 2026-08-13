package com.dypiu.nba.service;

import com.dypiu.nba.entity.CourseAtr;
import com.dypiu.nba.entity.ProgrammeAtr;
import com.dypiu.nba.repository.CourseAtrRepository;
import com.dypiu.nba.repository.ProgrammeAtrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtrService {

    private final CourseAtrRepository courseAtrRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;

    @Transactional(readOnly = true)
    public List<CourseAtr> getCourseAtrs(String courseId) {
        return courseAtrRepository.findByCourseId(courseId);
    }

    @Transactional
    public List<CourseAtr> saveCourseAtrs(String courseId, List<CourseAtr> atrs) {
        atrs.forEach(a -> {
            a.setCourseId(courseId);
            if (a.getId() == null) a.setId("atr-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return courseAtrRepository.saveAll(atrs);
    }

    @Transactional(readOnly = true)
    public Optional<ProgrammeAtr> getProgrammeAtr(String programmeId) {
        return programmeAtrRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public ProgrammeAtr saveProgrammeAtr(ProgrammeAtr atr) {
        if (atr.getId() == null) atr.setId("patr-" + UUID.randomUUID().toString().substring(0, 8));
        return programmeAtrRepository.save(atr);
    }
}
