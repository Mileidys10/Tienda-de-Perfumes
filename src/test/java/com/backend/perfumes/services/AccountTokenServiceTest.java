package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTokenServiceTest {

    private AccountTokenService accountTokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        accountTokenService = new AccountTokenService();
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
    }

    @Test
    @DisplayName("Debe generar token de verificacion valido")
    void testGenerateVerificationToken() {
        String token = accountTokenService.generateVerificationToken(testUser);

        assertNotNull(token);
        assertTrue(accountTokenService.isVerificationToken(token));
        assertFalse(accountTokenService.isDeleteToken(token));
        assertTrue(accountTokenService.isValidUUID(token));
    }

    @Test
    @DisplayName("Debe generar token de eliminacion con prefijo del_")
    void testGenerateDeleteAccountToken() {
        String deleteToken = accountTokenService.generateDeleteAccountToken(testUser);

        assertNotNull(deleteToken);
        assertTrue(deleteToken.startsWith("del_"));
        assertTrue(accountTokenService.isDeleteToken(deleteToken));
        assertFalse(accountTokenService.isVerificationToken(deleteToken));
        assertTrue(accountTokenService.isValidUUID(deleteToken));
    }

    @Test
    @DisplayName("Debe manejar tokens nulos o invalidos sin lanzar excepcion")
    void testNullAndInvalidTokens() {
        assertFalse(accountTokenService.isDeleteToken(null));
        assertFalse(accountTokenService.isVerificationToken(null));
        assertFalse(accountTokenService.isValidUUID(null));
        assertFalse(accountTokenService.isValidUUID("not-a-valid-uuid"));
        assertFalse(accountTokenService.isValidUUID("del_not-a-valid-uuid"));
    }
}
