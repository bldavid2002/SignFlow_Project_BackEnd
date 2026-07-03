package com.signflow.backend.controller;


import com.signflow.backend.Entity.Document;
import com.signflow.backend.config.JwtService;
import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.service.DocumentService;
import com.signflow.backend.service.SignatureService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private SignatureService signatureService;

    @Test
    void shouldUploadDocument() throws Exception{
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "contract.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());
        DocumentResponse response = DocumentResponse.builder()
                .title("contract.pdf")
                .status("UPLOADED")
                .build();

        when(documentService.uploadedDocument(any(), eq("test@email.com"))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/documents/upload").file(mockFile).principal(new UsernamePasswordAuthenticationToken("test@email.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("contract.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    void shouldGetMyDocuments() throws Exception{
        DocumentResponse doc1 = DocumentResponse.builder()
                .title("Doc1.pdf")
                .build();
        DocumentResponse doc2 = DocumentResponse.builder()
                .title("Doc2.pdf")
                .build();

        when(documentService.getAllUserDocuments("test@email.com")).thenReturn(List.of(doc1,doc2));

        mockMvc.perform(get("/api/v1/documents")
                .principal(new UsernamePasswordAuthenticationToken("test@email.com",null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Doc1.pdf"));

    }

    @Test
    void shouldDeleteDocument() throws Exception{
        UUID docId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/documents/" + docId)
                .principal(new UsernamePasswordAuthenticationToken("test@email.com",null)))
                .andExpect(status().isOk())
                .andExpect(content().string("Document deleted successfully. "));

    }

    @Test
    void shouldSignDocument() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentResponse response = DocumentResponse.builder()
                .title("contract.pdf")
                .status("SIGNED")
                .build();

        when(signatureService.signDocument(eq(docId), eq("test@email.com"))).thenReturn(response);
        mockMvc.perform(put("/api/v1/documents/" + docId + "/sign")
                .principal(new UsernamePasswordAuthenticationToken("test@email.com",null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"));

    }

    @Test
    void shouldWatermarkDocument() throws Exception {
        UUID docId = UUID.randomUUID();

        String watermarkText = "CONFIDENTIAL";

        DocumentResponse response = DocumentResponse.builder()
                .title("contract.pdf")
                .status("UPLOADED")
                .build();

        when(signatureService.watermarkDocument(eq(docId), eq(watermarkText), eq("test@email.com")))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/documents/" + docId + "/watermark")
                .param("text", watermarkText)
                .principal(new UsernamePasswordAuthenticationToken("test@email.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("contract.pdf"));

    }

    @Test
    void shouldDownloadDocument() throws Exception {
        UUID docId = UUID.randomUUID();
        byte[] fakePdfBytes = "Dummy PDF Content for Download".getBytes();

        Document mockDocument = Document.builder()
                .title("contract.pdf")
                .build();

        when(documentService.downloadDocumentBytes(eq(docId),eq("test@email.com")))
                .thenReturn(fakePdfBytes);

        when(documentService.getDocument(eq(docId),eq("test@email.com")))
                .thenReturn(mockDocument);

        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                .principal(new UsernamePasswordAuthenticationToken("test@email.com",null)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(fakePdfBytes));

    }
}
