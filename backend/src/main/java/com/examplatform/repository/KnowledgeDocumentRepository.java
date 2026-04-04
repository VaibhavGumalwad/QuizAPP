package com.examplatform.repository;

import com.examplatform.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByTopicContainingIgnoreCase(String topic);
    List<KnowledgeDocument> findByDocumentType(String documentType);
    List<KnowledgeDocument> findByTopicContainingIgnoreCaseAndDocumentType(String topic, String documentType);
}