package com.signflow.backend.controller;


import com.signflow.backend.Entity.Document;
import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.dto.VerificationResponse;
import com.signflow.backend.repository.DocumentRepository;
import com.signflow.backend.service.DocumentService;
import com.signflow.backend.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;


import java.io.IOException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    private final SignatureService signatureService;

    private final DocumentRepository documentRepository;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file")MultipartFile file,
            Authentication authentication
    )throws IOException {
        String userEmail = authentication.getName();
        DocumentResponse response = documentService.uploadedDocument(file, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(Authentication authentication){
        String userEmail = authentication.getName();
        List<DocumentResponse> documents = documentService.getAllUserDocuments(userEmail);
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable UUID id,
            Authentication authentication
    )throws  IOException{
        String userEmail = authentication.getName();
        documentService.deleteDocument(id, userEmail);
        return ResponseEntity.ok("Document deleted successfully. ");
    }

    @PutMapping("/{id}/sign")
    public ResponseEntity<DocumentResponse> signDocument(@PathVariable UUID id, Authentication authentication) throws IOException{
        String userEmail = authentication.getName();
        DocumentResponse response = signatureService.signDocument(id, userEmail);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/watermark")
    public ResponseEntity<DocumentResponse> watermarkDocument(
            @PathVariable UUID id,
            @RequestParam("text") String watermarkText,
            Authentication authentication
    )throws IOException {
        String userEmail = authentication.getName();
        DocumentResponse response = signatureService.watermarkDocument(id, watermarkText, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable UUID id,
            Authentication authentication
    )throws IOException{
        String userEmail = authentication.getName();

        byte[] pdfBytes = documentService.getDocumentBytes(id, userEmail);

        Document document = documentRepository.findById(id).orElseThrow();

//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_PDF);
//        headers.setContentDispositionFormData("attachment", document.getTitle());

       return ResponseEntity.ok()
               .header("Content-Disposition", "attachment; filename=\"" + document.getTitle() + "\"")
               .contentType(MediaType.APPLICATION_PDF)
               .body(pdfBytes);

    }

    @GetMapping("/{id}/verify")
    public ResponseEntity<VerificationResponse> verifyDocument(
            @PathVariable UUID id,
            Authentication authentication
    )throws IOException {
        String userEmail = authentication.getName();
        VerificationResponse response = signatureService.verifySignature(id, userEmail);
        return ResponseEntity.ok(response);
    }
    


}
