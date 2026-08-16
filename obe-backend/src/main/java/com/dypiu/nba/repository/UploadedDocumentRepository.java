package com.dypiu.nba.repository;

import com.dypiu.nba.entity.DocumentType;
import com.dypiu.nba.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, String> {
    List<UploadedDocument> findByCourseOfferingId(String courseOfferingId);
    List<UploadedDocument> findByBatchId(String batchId);
    List<UploadedDocument> findByBatchIdAndDocumentType(String batchId, DocumentType documentType);
    Optional<UploadedDocument> findFirstByCourseOfferingIdAndDocumentTypeOrderByUploadedAtDesc(String courseOfferingId, DocumentType documentType);
    Optional<UploadedDocument> findFirstByBatchIdAndDocumentTypeOrderByUploadedAtDesc(String batchId, DocumentType documentType);
    void deleteByCourseOfferingIdAndDocumentType(String courseOfferingId, DocumentType documentType);
    void deleteByBatchIdAndDocumentType(String batchId, DocumentType documentType);
}
