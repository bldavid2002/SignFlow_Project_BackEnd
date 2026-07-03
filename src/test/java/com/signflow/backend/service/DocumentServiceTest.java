package com.signflow.backend.service;

import com.signflow.backend.Entity.Document;
import com.signflow.backend.Entity.DocumentStatus;
import com.signflow.backend.Entity.User;
import com.signflow.backend.dto.DocumentResponse;
import com.signflow.backend.repository.DocumentRepository;
import com.signflow.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.print.Doc;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private DocumentService documentService;

    @TempDir
    Path tempDir;

    private User mockUser;

    @BeforeEach
    void setUp(){
        mockUser = User.builder().id(UUID.randomUUID()).email("test@email.com").build();
        ReflectionTestUtils.setField(documentService, "bucketName", "test-bucket");
    }

    @Test
    void shouldUploadDocumentSuccessfully() throws IOException{
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Dummy PDF Content".getBytes());

        Document saveDoc = Document.builder()
                .id(UUID.randomUUID())
                .title("test.pdf")
                .status(DocumentStatus.UPLOADED)
                .createdAt(null)
                .build();
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(documentRepository.save(any(Document.class))).thenReturn(saveDoc);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        DocumentResponse response = documentService.uploadedDocument(mockFile, mockUser.getEmail());

        assertThat(response.getTitle()).isEqualTo("test.pdf");
        assertThat(response.getStatus()).isEqualTo("UPLOADED");
        verify(documentRepository, times(1)).save(any(Document.class));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void shouldThrowExceptionWhenDeletingSomeoneElseDocument(){
        User hacker = User.builder()
                .email("attacker@gmail.com").build();
        Document myDocument = Document.builder()
                .id(UUID.randomUUID())
                .owner(mockUser)
                .build();

        when(documentRepository.findById(myDocument.getId())).thenReturn(Optional.of(myDocument));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
           documentService.deleteDocument(myDocument.getId(), hacker.getEmail());
        });

        assertThat(exception.getMessage()).isEqualTo("You do not have permission to delete this document");
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void shouldDownloadDocumentBytesSuccessfully(){
        Document mockDoc = Document.builder()
                .id(UUID.randomUUID())
                .title("contract.pdf")
                .fileUrl("s3-key-123")
                .owner(mockUser)
                .build();

        byte[] fakePdfBytes = "Fake PDF Data".getBytes();

        when(documentRepository.findById(mockDoc.getId())).thenReturn(Optional.of(mockDoc));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), fakePdfBytes));

        byte[] result = documentService.downloadDocumentBytes(mockDoc.getId(), mockUser.getEmail());
        assertThat(result).isEqualTo(fakePdfBytes);
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));

    }

    @Test
    void shouldGetDocumentSuccessfully() {
        Document mockDoc = Document.builder()
                .id(UUID.randomUUID())
                .title("contract.pdf")
                .owner(mockUser)
                .build();
        when(documentRepository.findById(mockDoc.getId())).thenReturn(Optional.of(mockDoc));
        Document result = documentService.getDocument(mockDoc.getId(), mockUser.getEmail());

        assertThat(result.getTitle()).isEqualTo("contract.pdf");
    }

}
