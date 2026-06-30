package com.signflow.backend.repository;

import com.signflow.backend.Entity.Document;
import com.signflow.backend.Entity.DocumentStatus;
import com.signflow.backend.Entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveDocument() {
        User owner = User.builder()
                .email("testowner@email.com")
                .passwordHash("hashedpass")
                .role("USER")
                .build();
        userRepository.save(owner);

        Document document = Document.builder()
                .title("Contract.pdf")
                .fileUrl("/uploads/contract.pdf")
                .status(DocumentStatus.UPLOADED)
                .owner(owner)
                .build();
        Document saveDoc = documentRepository.save(document);

        List<Document> retrievedDoc = documentRepository.findAllByOwnerId(owner.getId());

        assertThat(retrievedDoc).isNotEmpty();
        assertThat(retrievedDoc.get(0).getTitle()).isEqualTo("Contract.pdf");
        assertThat(retrievedDoc.get(0).getOwner().getEmail()).isEqualTo("testowner@email.com");

    }

    @Test
    void shouldFindAllDocumentsByOwnerId(){
        User user1 = User.builder().email("user1@email.com").passwordHash("pass").role("USER").build();
        User user2 = User.builder().email("user2@email.com").passwordHash("pass").role("USER").build();
        userRepository.save(user1);
        userRepository.save(user2);

        documentRepository.save(Document.builder()
                .title("Doc1_User1.pdf").fileUrl("url1").status(DocumentStatus.UPLOADED).owner(user1).build());
        documentRepository.save(Document.builder()
                .title("Doc1_User1.pdf").fileUrl("url2").status(DocumentStatus.PENDING_SIGNATURE).owner(user1).build());
        documentRepository.save(Document.builder()
                .title("Doc1_User2.pdf").fileUrl("url3").status(DocumentStatus.SIGNED).owner(user2).build());

        List<Document> user1Docs = documentRepository.findAllByOwnerId(user1.getId());

        assertThat(user1Docs).hasSize(2);
        assertThat(user1Docs.get(0).getTitle()).contains("User1");
        assertThat(user1Docs.get(1).getTitle()).contains("User1");
    }
}
