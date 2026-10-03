package com.keystone.security;

import com.keystone.config.JwtTokenProvider;
import com.keystone.entity.User;
import com.keystone.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        long expirationMs = 3600000; // 1 hour
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);

        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .userEmail("john.doe@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
    }

    @Test
    void generateToken_ShouldReturnValidTokenString() {
        String token = jwtTokenProvider.generateToken(testUser);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtTokenProvider.generateToken(testUser);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_WithTamperedToken_ShouldReturnFalse() {
        String token = jwtTokenProvider.generateToken(testUser) + "tampered";
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void getUserEmailFromToken_ShouldExtractCorrectEmail() {
        String token = jwtTokenProvider.generateToken(testUser);
        String email = jwtTokenProvider.getUserEmailFromToken(token);
        assertEquals("john.doe@example.com", email);
    }

    @Test
    void getRoleFromToken_ShouldExtractCorrectRole() {
        String token = jwtTokenProvider.generateToken(testUser);
        String role = jwtTokenProvider.getRoleFromToken(token);
        assertEquals("ADMIN", role);
    }
}
