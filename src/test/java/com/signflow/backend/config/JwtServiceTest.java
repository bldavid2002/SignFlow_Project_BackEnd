package com.signflow.backend.config;

import com.signflow.backend.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

    }

    @Test
    void shouldExtractUsernameFromToken() {
        User user = User.builder()
                .email("test@gmail.com")
                .passwordHash("password")
                .role("work")
                .build();
        String token = jwtService.generateToken(user);
        String userName = jwtService.extractUserName(token);
        assertEquals("test@gmail.com",userName);

    }
}
