package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtService jwtService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void configurarValueOperations() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void generateRefreshTokenStoresInRedis() {
        String refreshToken = tokenService.createRefreshToken("admin@sgroas.com", 604800000L);

        assertNotNull(refreshToken);
        verify(valueOperations).set(
                eq("refresh:" + refreshToken),
                eq("admin@sgroas.com"),
                eq(Duration.ofMillis(604800000L))
        );
    }

    @Test
    void getEmailReturnsEmail() {
        when(valueOperations.get("refresh:token-valido")).thenReturn("admin@sgroas.com");

        String email = tokenService.getEmailFromRefreshToken("token-valido");

        assertEquals("admin@sgroas.com", email);
    }

    @Test
    void getEmailWithNonexistentTokenThrowsException() {
        when(valueOperations.get("refresh:token-inexistente")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> tokenService.getEmailFromRefreshToken("token-inexistente"));
    }

    @Test
    void deleteRefreshTokenRemovesFromRedis() {
        when(redisTemplate.delete("refresh:token-viejo")).thenReturn(true);

        tokenService.deleteRefreshToken("token-viejo");

        verify(redisTemplate).delete("refresh:token-viejo");
    }

    @Test
    void addAccessTokenToBlacklistWithFutureExpiration() {
        when(jwtService.extractJti("access-token")).thenReturn("jti-123");
        when(jwtService.extractExpiration("access-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 3600000L));

        tokenService.addAccessTokenToBlacklist("access-token");

        verify(valueOperations).set(
                eq("blacklist:jti-123"),
                eq("logout"),
                any(Duration.class)
        );
    }

    @Test
    void addExpiredAccessTokenToBlacklistShouldNotStore() {
        when(jwtService.extractJti("access-token")).thenReturn("jti-123");
        when(jwtService.extractExpiration("access-token"))
                .thenReturn(new Date(System.currentTimeMillis() - 1000L));

        tokenService.addAccessTokenToBlacklist("access-token");

        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
    }

    @Test
    void accessTokenInBlacklistShouldReturnTrue() {
        when(jwtService.extractJti("access-token")).thenReturn("jti-123");
        when(redisTemplate.hasKey("blacklist:jti-123")).thenReturn(true);

        assertTrue(tokenService.accessTokenEnBlacklist("access-token"));
    }

    @Test
    void accessTokenNotInBlacklistShouldReturnFalse() {
        when(jwtService.extractJti("access-token")).thenReturn("jti-123");
        when(redisTemplate.hasKey("blacklist:jti-123")).thenReturn(false);

        assertFalse(tokenService.accessTokenEnBlacklist("access-token"));
    }
}
