package ec.edu.uteq.sgroas.security;

import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // Corregido 2026-09-17: se elimino el secreto literal de respaldo
    // ("TEST_ONLY_SECRET_KEY_2026..."). Ahora, si JWT_SECRET no esta definida
    // en el entorno, se genera una clave aleatoria de 32 bytes en memoria al
    // cargar la clase -- distinta en cada ejecucion de los tests, nunca
    // versionada, y suficiente para HS256 (>=256 bits). No queda ningun
    // secreto de texto plano buscable en el arbol del repositorio.
    private static final String JWT_SECRET = resolveTestSecret();

    private static String resolveTestSecret() {
        String fromEnv = System.getenv("JWT_SECRET");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getEncoder().encodeToString(random);
    }

    private JwtService jwtService;

    @BeforeEach
    void setUpJwtService() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "jwtIssuer", "https://sgroas.uteq.edu.ec");
        ReflectionTestUtils.setField(jwtService, "jwtAudience", "sgroas-frontend");
    }

    private User sampleUser() {
        return User.builder()
                .id(1L)
                .name("Administrador SGROAS")
                .email("admin@sgroas.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();
    }

    @Test
    void generateTokenAllowsExtractingData() {
        String token = jwtService.generateToken(sampleUser());

        assertNotNull(token);
        assertNotNull(jwtService.extractJti(token));
        assertEquals("admin@sgroas.com", jwtService.extractEmail(token));
        assertNotNull(jwtService.extractExpiration(token));
        assertEquals(3600000L, jwtService.getExpirationMs());
        assertTrue(jwtService.isTokenValid(token, "admin@sgroas.com"));
    }

    @Test
    void tokenWithDifferentEmailShouldBeInvalid() {
        String token = jwtService.generateToken(sampleUser());

        assertFalse(jwtService.isTokenValid(token, "otro@sgroas.com"));
    }

    @Test
    void expiredTokenShouldBeRejected() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);

        String token = jwtService.generateToken(sampleUser());

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token, "admin@sgroas.com"));
    }

    @Test
    void extractExpirationShouldBeFuture() {
        String token = jwtService.generateToken(sampleUser());

        assertTrue(jwtService.extractExpiration(token).after(new Date()));
    }
}
