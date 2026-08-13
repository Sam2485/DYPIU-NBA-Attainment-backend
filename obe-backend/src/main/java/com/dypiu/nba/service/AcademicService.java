package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicService {

    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    // --- Schools ---
    @Transactional(readOnly = true)
    public List<School> getAllSchools() {
        return schoolRepository.findAll();
    }

    @Transactional
    public School saveSchool(School school) {
        if (school.getId() == null) school.setId("sch-" + UUID.randomUUID().toString().substring(0, 8));
        return schoolRepository.save(school);
    }

    // --- Departments ---
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional
    public Department saveDepartment(Department department) {
        if (department.getId() == null) department.setId("dept-" + UUID.randomUUID().toString().substring(0, 8));
        return departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(String id) {
        departmentRepository.deleteById(id);
    }

    // --- Programmes ---
    @Transactional(readOnly = true)
    public List<Programme> getAllProgrammes() {
        return programmeRepository.findAll();
    }

    @Transactional
    public Programme saveProgramme(Programme programme) {
        if (programme.getId() == null) programme.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        return programmeRepository.save(programme);
    }

    @Transactional
    public void deleteProgramme(String id) {
        programmeRepository.deleteById(id);
    }

    // --- Batches ---
    @Transactional(readOnly = true)
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesByProgramme(String programmeId) {
        return batchRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public Batch saveBatch(Batch batch) {
        if (batch.getId() == null) batch.setId("batch-" + UUID.randomUUID().toString().substring(0, 8));
        return batchRepository.save(batch);
    }

    @Transactional
    public void deleteBatch(String id) {
        batchRepository.deleteById(id);
    }

    // --- Courses ---
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByProgramme(String programmeId) {
        return courseRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public Course saveCourse(Course course) {
        if (course.getId() == null) course.setId("crs-" + UUID.randomUUID().toString().substring(0, 8));
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }

    // --- Students ---
    @Transactional(readOnly = true)
    public List<Student> getStudentsByBatch(String batchId) {
        return studentRepository.findByBatchId(batchId);
    }

    @Transactional
    public Student saveStudent(Student student) {
        if (student.getId() == null) student.setId("std-" + UUID.randomUUID().toString().substring(0, 8));
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(String id) {
        studentRepository.deleteById(id);
    }
}
