package com.signflow.backend.controller;


import com.signflow.backend.config.JwtService;
import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.service.DocumentService;
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
}
