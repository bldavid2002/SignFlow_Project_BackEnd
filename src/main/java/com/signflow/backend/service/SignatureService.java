package com.signflow.backend.service;


import com.signflow.backend.Entity.Document;
import com.signflow.backend.Entity.DocumentStatus;
import com.signflow.backend.Entity.User;
import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.repository.DocumentRepository;
import com.signflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    public DocumentResponse signDocument(UUID documentId, String userEmail) throws IOException{
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOwner().getEmail().equals(userEmail)){
            throw new RuntimeException("You do noz have permission to sign this document");
        }

        if (document.getStatus() == DocumentStatus.SIGNED){
            throw new RuntimeException("This document is already signed");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(document.getFileUrl())
                .build();

        ResponseBytes<GetObjectResponse> s3ObjectBytes = s3Client.getObjectAsBytes(getObjectRequest);

        byte[] signedBytes = stampSignatureOnPdf(s3ObjectBytes.asByteArray(), owner.getEmail());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(document.getFileUrl())
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(signedBytes));

        document.setStatus(DocumentStatus.SIGNED);
        Document saveDoc = documentRepository.save(document);

        return mapToDto(saveDoc);
    }

    private byte[] stampSignatureOnPdf(byte[] originalPdf, String email) throws IOException{
        try(PDDocument pdfDocument = Loader.loadPDF(originalPdf);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage lastPage = pdfDocument.getPage(pdfDocument.getNumberOfPages()-1);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String signatureText = "Digitally Signed by: " + email + " on " + timestamp;

            try(PDPageContentStream contentStream = new PDPageContentStream(
                    pdfDocument, lastPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50,50);
                contentStream.showText(signatureText);
                contentStream.endText();
            }

            pdfDocument.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private DocumentResponse mapToDto(Document doc){
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .status(doc.getStatus().name())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
