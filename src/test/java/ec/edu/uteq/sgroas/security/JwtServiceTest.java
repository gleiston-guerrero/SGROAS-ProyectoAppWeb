package ec.edu.uteq.sgroas.security;

import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // Nota de auditoria (2026-09-16): este valor de respaldo solo se usa si la
    // variable de entorno JWT_SECRET no esta definida, unicamente dentro de este
    // test unitario (nunca en application.properties ni en un perfil real). No
    // firma tokens reales ni protege datos: JwtService se instancia aqui con
    // "new JwtService()" y el valor se inyecta por reflection solo para este
    // test, por lo que no representa un secreto de produccion filtrado.
    private static final String JWT_SECRET =
            System.getenv().getOrDefault("JWT_SECRET",
                    "TEST_ONLY_SECRET_KEY_2026_NOT_FOR_PRODUCTION_MIN_32");

    private JwtService jwtService;

    @BeforeEach
    void setUpJwtService() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "jwtIssuer", "https://sgroas.uteq.edu.ec");
        ReflectionTestUtils.setField(jwtService, "jwtAudience", "sgroas-frontend");
    }

    private User usuarioEjemplo() {
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
        String token = jwtService.generateToken(usuarioEjemplo());

        assertNotNull(token);
        assertNotNull(jwtService.extractJti(token));
        assertEquals("admin@sgroas.com", jwtService.extractEmail(token));
        assertNotNull(jwtService.extractExpiration(token));
        assertEquals(3600000L, jwtService.getExpirationMs());
        assertTrue(jwtService.isTokenValid(token, "admin@sgroas.com"));
    }

    @Test
    void tokenWithDifferentEmailShouldBeInvalid() {
        String token = jwtService.generateToken(usuarioEjemplo());

        assertFalse(jwtService.isTokenValid(token, "otro@sgroas.com"));
    }

    @Test
    void expiredTokenShouldBeRejected() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);

        String token = jwtService.generateToken(usuarioEjemplo());

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token, "admin@sgroas.com"));
    }

    @Test
    void extractExpirationShouldBeFuture() {
        String token = jwtService.generateToken(usuarioEjemplo());

        assertTrue(jwtService.extractExpiration(token).after(new Date()));
    }
}
