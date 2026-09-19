package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.AuthResponse;
import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.exception.GlobalExceptionHandler;
import ec.edu.uteq.sgroas.repository.UserRepository;
import ec.edu.uteq.sgroas.security.JwtService;
import ec.edu.uteq.sgroas.security.LoginRateLimiter;
import ec.edu.uteq.sgroas.service.AuthService;
import ec.edu.uteq.sgroas.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(
                        new AuthController(authService, loginRateLimiter, jwtService,
                                tokenService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuthResponse authResponse() {
        return new AuthResponse(
                "access-token", "refresh-token", "Bearer",
                3600000L, "Administrador SGROAS",
                "admin@sgroas.com", "ROLE_ADMIN"
        );
    }

    private User activeUser() {
        return User.builder()
                .id(1L)
                .name("Administrador SGROAS")
                .email("admin@sgroas.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .verified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void meFromCookieShouldReturn200WithUserData() throws Exception {
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn("admin@sgroas.com");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(activeUser()));

        mockMvc().perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@sgroas.com"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.name").value("Administrador SGROAS"));
    }

    @Test
    void meFromAuthorizationHeaderShouldReturn200() throws Exception {
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn("admin@sgroas.com");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(activeUser()));

        mockMvc().perform(get("/api/auth/me")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@sgroas.com"));
    }

    @Test
    void meWithoutTokenShouldReturn401() throws Exception {
        mockMvc().perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithBlacklistedTokenShouldReturn401() throws Exception {
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(true);

        mockMvc().perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithInvalidEmailShouldReturn401() throws Exception {
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn(null);

        mockMvc().perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithInactiveUserShouldReturn401() throws Exception {
        User inactivo = activeUser();
        inactivo.setActive(false);
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn("admin@sgroas.com");
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(inactivo));

        mockMvc().perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendCodeShouldReturn200WithGenericMessage() throws Exception {
        mockMvc().perform(post("/api/auth/resend-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "carlos@sgroas.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void verifyEmailReturns200AndCookie() throws Exception {
        when(authService.verifyEmail(any(), any())).thenReturn(authResponse());

        mockMvc().perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@sgroas.com",
                                  "codigo": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.email").value("admin@sgroas.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void forgotPasswordShouldReturn200WithGenericMessage() throws Exception {
        mockMvc().perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@sgroas.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void resetPasswordShouldReturn200() throws Exception {
        mockMvc().perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@sgroas.com",
                                  "codigo": "654321",
                                  "nuevaPassword": "nueva-clave-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void loginSuccessShouldReturn200AndCookieWithoutTokenInBody() throws Exception {
        when(loginRateLimiter.isBlocked("127.0.0.1")).thenReturn(false);
        when(authService.login(any())).thenReturn(authResponse());

        mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@sgroas.com",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.email").value("admin@sgroas.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void loginWithBlockedIpShouldReturn429() throws Exception {
        when(loginRateLimiter.isBlocked("127.0.0.1")).thenReturn(true);

        mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@sgroas.com",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Demasiadas solicitudes"));
    }

    @Test
    void loginWithInvalidCredentialsShouldReturn401() throws Exception {
        when(loginRateLimiter.isBlocked("127.0.0.1")).thenReturn(false);
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@sgroas.com",
                                  "password": "incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithCookieShouldReturn200WithoutTokenInBody() throws Exception {
        when(authService.refresh(any())).thenReturn(authResponse());

        mockMvc().perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.email").value("admin@sgroas.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void refreshWithBodyShouldReturn200ForCompatibility() throws Exception {
        when(authService.refresh(any())).thenReturn(authResponse());

        mockMvc().perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refreshWithoutTokenShouldReturn401() throws Exception {
        mockMvc().perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithCookiesShouldReturn204() throws Exception {
        mockMvc().perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-token"))
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void logoutWithCookieShouldReturn204() throws Exception {
        mockMvc().perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void logoutWithoutCookieShouldReturn204() throws Exception {
        mockMvc().perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());
    }
}
