package com.signflow.backend.repository;

import com.signflow.backend.Entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findAllByOwnerId(UUID userId);
}
