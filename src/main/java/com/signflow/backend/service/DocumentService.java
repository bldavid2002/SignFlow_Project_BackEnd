package com.signflow.backend.service;

import com.signflow.backend.Entity.Document;
import com.signflow.backend.Entity.DocumentStatus;
import com.signflow.backend.Entity.User;
import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.repository.DocumentRepository;
import com.signflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tools.jackson.core.PrettyPrinter;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    private final S3Client s3Client;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;


    public DocumentResponse uploadedDocument(MultipartFile file, String userEmail) throws IOException{
            User owner = userRepository.findByEmail(userEmail)
                    .orElseThrow(()-> new RuntimeException("User not found"));

        String fileKey = UUID.randomUUID() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        Document document = Document.builder()
                .title(file.getOriginalFilename())
                .fileUrl(fileKey)
                .status(DocumentStatus.UPLOADED)
                .owner(owner)
                .build();

        Document saveDoc = documentRepository.save(document);
        return mapToDto(saveDoc);

    }

    public List<DocumentResponse> getAllUserDocuments(String userEmail) {
        User owner = userRepository.findByEmail(userEmail).orElseThrow();
        return documentRepository.findAllByOwnerId(owner.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void deleteDocument(UUID documentId, String userEmail) throws IOException{
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found."));

        if(!document.getOwner().getEmail().equals(userEmail)){
            throw new RuntimeException("You do not have permission to delete this document");
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(document.getFileUrl())
                        .build();
        s3Client.deleteObject(deleteObjectRequest);

        documentRepository.delete(document);
    }

    private DocumentResponse mapToDto(Document doc){
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .status(doc.getStatus().name())
                .createdAt(doc.getCreatedAt())
                .build();
    }


    public byte[] downloadDocumentBytes(UUID documentId, String userEmail){
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getEmail().equals(userEmail)){
            throw new RuntimeException("You do not have permission to download this document");
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(document.getFileUrl())
                .build();

        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
    }

    public Document getDocument(UUID documentId, String userEmail) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getEmail().equals(userEmail)){
            throw new RuntimeException("You do not have permission to view this document");
        }

        return document;
    }
}
