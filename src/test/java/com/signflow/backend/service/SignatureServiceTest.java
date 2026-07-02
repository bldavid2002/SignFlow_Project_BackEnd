package com.signflow.backend.service;

import com.signflow.backend.Entity.Document;
import com.signflow.backend.Entity.DocumentStatus;
import com.signflow.backend.Entity.User;
import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.repository.DocumentRepository;
import com.signflow.backend.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SignatureServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    S3Client s3Client;

    @InjectMocks
    private SignatureService signatureService;

    private User mockUser;
    private Document mockDocument;

    @BeforeEach
    void setUp(){
        mockUser = User.builder().id(UUID.randomUUID()).email("test@email.com").build();

        mockDocument = Document.builder()
                .id(UUID.randomUUID())
                .title("contract.pdf")
                .fileUrl("s3-key-123")
                .status(DocumentStatus.UPLOADED)
                .owner(mockUser)
                .build();
        ReflectionTestUtils.setField(signatureService, "bucketName", "test-bucket");

    }

    private byte[] generateDummyPdf() throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            doc.addPage(new PDPage());
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void shouldSignDocumentSuccessfully() throws IOException {
        byte[] dummyPdf = generateDummyPdf();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(documentRepository.findById(mockDocument.getId())).thenReturn(Optional.of(mockDocument));

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), dummyPdf));

        when(documentRepository.save(any(Document.class))).thenReturn(mockDocument);

        DocumentResponse response = signatureService.signDocument(mockDocument.getId(), mockUser.getEmail());

        assertThat(response.getStatus()).isEqualTo("SIGNED");
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void shouldThrowExceptionDocumentAlreadySigned(){
        mockDocument.setStatus(DocumentStatus.SIGNED);

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(documentRepository.findById(mockDocument.getId())).thenReturn(Optional.of(mockDocument));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            signatureService.signDocument(mockDocument.getId(), mockUser.getEmail());
        });

        assertThat(exception.getMessage()).isEqualTo("This document is already signed");
        verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void shouldWatermarkDocumentSuccessfully() throws IOException{
        byte[] dummyPdf = generateDummyPdf();
        String watermarkText = "CONFIDENTIAL";

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(documentRepository.findById(mockDocument.getId())).thenReturn(Optional.of(mockDocument));


        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), dummyPdf));

        when(documentRepository.save(any(Document.class))).thenReturn(mockDocument);

        DocumentResponse response = signatureService.watermarkDocument(mockDocument.getId(), watermarkText, mockUser.getEmail());

        assertThat(response.getTitle()).isEqualTo("contract.pdf");
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
