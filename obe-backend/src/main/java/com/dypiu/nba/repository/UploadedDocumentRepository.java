package com.dypiu.nba.repository;

import com.dypiu.nba.entity.DocumentType;
import com.dypiu.nba.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, String> {
    List<UploadedDocument> findByProgrammeBatchCourseId(String programmeBatchCourseId);
    List<UploadedDocument> findByProgrammeBatchId(String programmeBatchId);
    List<UploadedDocument> findByProgrammeBatchIdAndDocumentType(String programmeBatchId, DocumentType documentType);
    Optional<UploadedDocument> findFirstByProgrammeBatchCourseIdAndDocumentTypeOrderByUploadedAtDesc(String programmeBatchCourseId, DocumentType documentType);
    Optional<UploadedDocument> findFirstByProgrammeBatchIdAndDocumentTypeOrderByUploadedAtDesc(String programmeBatchId, DocumentType documentType);
    void deleteByProgrammeBatchCourseIdAndDocumentType(String programmeBatchCourseId, DocumentType documentType);
    void deleteByProgrammeBatchIdAndDocumentType(String programmeBatchId, DocumentType documentType);
}
