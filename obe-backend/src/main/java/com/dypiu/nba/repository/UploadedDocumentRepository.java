package com.dypiu.nba.repository;

import com.dypiu.nba.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, String> {
    List<UploadedDocument> findByCourseId(String courseId);
    Optional<UploadedDocument> findFirstByCourseIdAndDocumentTypeOrderByUploadedAtDesc(String courseId, String documentType);
    void deleteByCourseIdAndDocumentType(String courseId, String documentType);
}
