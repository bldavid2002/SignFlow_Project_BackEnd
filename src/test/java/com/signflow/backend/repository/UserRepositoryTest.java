package com.signflow.backend.repository;


import com.signflow.backend.Entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    public  void testSavedAndFindByEmail(){
        User testUser = User.builder()
                .email("david@testmail.com")
                .passwordHash("veryverysecrethash")
                .role("USER")
                .build();

        userRepository.save(testUser);

        Optional<User> foundUser = userRepository.findByEmail("david@testmail.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("david@testmail.com");
        assertThat(foundUser.get().getId()).isNotNull();
    }
}
