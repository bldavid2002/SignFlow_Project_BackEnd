package com.signflow.backend.controller;


import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

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
}
