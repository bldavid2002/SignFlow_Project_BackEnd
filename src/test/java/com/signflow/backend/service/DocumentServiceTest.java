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

    @InjectMocks
    private DocumentService documentService;

    @TempDir
    Path tempDir;

    private User mockUser;

    @BeforeEach
    void setUp(){
        mockUser = User.builder().id(UUID.randomUUID()).email("test@email.com").build();
        ReflectionTestUtils.setField(documentService, "UPLOAD_DIRECTORY", tempDir.toString() + "/");
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

        DocumentResponse response = documentService.uploadedDocument(mockFile, mockUser.getEmail());

        assertThat(response.getTitle()).isEqualTo("test.pdf");
        assertThat(response.getStatus()).isEqualTo("UPLOADED");
        verify(documentRepository, times(1)).save(any(Document.class));
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


}
