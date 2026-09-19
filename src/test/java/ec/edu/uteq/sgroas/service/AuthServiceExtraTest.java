package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.AuthResponse;
import ec.edu.uteq.sgroas.dto.LoginRequest;
import ec.edu.uteq.sgroas.dto.RefreshTokenRequest;
import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.exception.UnverifiedEmailException;
import ec.edu.uteq.sgroas.repository.UserRepository;
import ec.edu.uteq.sgroas.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceExtraTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setsRefreshExpiration() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
    }

    private User sampleUser() {
        return User.builder()
                .id(1L)
                .name("Administrador SGROAS")
                .email("admin@sgroas.com")
                .passwordHash("password-encriptado")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void simulateTokenGeneration(User usuario) {
        when(jwtService.generateToken(usuario)).thenReturn("access-token-prueba");
        when(tokenService.createRefreshToken(eq("admin@sgroas.com"), eq(604800000L)))
                .thenReturn("refresh-token-prueba");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
    }

    @Test
    void verifyCorrectEmailActivatesAccountAndReturnsTokens() {
        User usuario = sampleUser();
        usuario.setActive(false);
        usuario.setVerified(false);
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(usuario));
        simulateTokenGeneration(usuario);

        AuthResponse response = authService.verifyEmail("admin@sgroas.com", "654321");

        assertEquals("access-token-prueba", response.accessToken());
        verify(verificationCodeService).validate("admin@sgroas.com",
                VerificationCodeService.Type.VERIFICACION, "654321");
        verify(userRepository).save(argThat(u ->
                Boolean.TRUE.equals(u.getActive()) && Boolean.TRUE.equals(u.getVerified())));
    }

    @Test
    void loginWithUnverifiedEmailShouldThrowException() {
        User sinVerificar = sampleUser();
        sinVerificar.setActive(false);
        sinVerificar.setVerified(false);
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(sinVerificar));
        when(passwordEncoder.matches("123456", "password-encriptado")).thenReturn(true);

        assertThrows(UnverifiedEmailException.class,
                () -> authService.login(new LoginRequest("admin@sgroas.com", "123456")));
    }

    @Test
    void resetPasswordShouldUpdatePassword() {
        User usuario = sampleUser();
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nueva-clave-1")).thenReturn("hash-nuevo");

        authService.resetPassword("admin@sgroas.com", "111222", "nueva-clave-1");

        verify(verificationCodeService).validate("admin@sgroas.com",
                VerificationCodeService.Type.RESET_PASSWORD, "111222");
        verify(userRepository).save(argThat(u -> "hash-nuevo".equals(u.getPasswordHash())));
    }

    @Test
    void resendCodeShouldGenerateAndSendNewCode() {
        User sinVerificar = sampleUser();
        sinVerificar.setVerified(false);
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(sinVerificar));
        when(verificationCodeService.canResend("admin@sgroas.com",
                VerificationCodeService.Type.VERIFICACION)).thenReturn(true);
        when(verificationCodeService.generate("admin@sgroas.com",
                VerificationCodeService.Type.VERIFICACION)).thenReturn("999888");

        authService.resendVerificationCode("admin@sgroas.com");

        verify(emailService).sendVerificationCode(
                "admin@sgroas.com", "Administrador SGROAS", "999888");
    }

    @Test
    void resendCodeWithVerifiedAccountShouldNotSendAnything() {
        User verificado = sampleUser();
        verificado.setVerified(true);
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(verificado));

        authService.resendVerificationCode("admin@sgroas.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void requestPasswordResetShouldSendCode() {
        User usuario = sampleUser();
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(usuario));
        when(verificationCodeService.canResend("admin@sgroas.com",
                VerificationCodeService.Type.RESET_PASSWORD)).thenReturn(true);
        when(verificationCodeService.generate("admin@sgroas.com",
                VerificationCodeService.Type.RESET_PASSWORD)).thenReturn("112233");

        authService.requestPasswordReset("admin@sgroas.com");

        verify(emailService).sendResetCode("admin@sgroas.com", "112233");
    }

    @Test
    void refreshShouldRotateToken() {
        User usuario = sampleUser();
        when(tokenService.getEmailFromRefreshToken("refresh-token-prueba"))
                .thenReturn("admin@sgroas.com");
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(usuario));
        simulateTokenGeneration(usuario);

        AuthResponse response = authService.refresh(
                new RefreshTokenRequest("refresh-token-prueba")
        );

        assertNotNull(response);
        assertEquals("access-token-prueba", response.accessToken());
        verify(tokenService).deleteRefreshToken("refresh-token-prueba");
    }

    @Test
    void refreshWithInactiveUserShouldThrowException() {
        User inactivo = sampleUser();
        inactivo.setActive(false);
        when(tokenService.getEmailFromRefreshToken("refresh-token-prueba"))
                .thenReturn("admin@sgroas.com");
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(inactivo));

        assertThrows(BadCredentialsException.class,
                () -> authService.refresh(new RefreshTokenRequest("refresh-token-prueba")));
    }

    @Test
    void refreshWithNonexistentEmailShouldThrowException() {
        when(tokenService.getEmailFromRefreshToken("refresh-token-prueba"))
                .thenReturn("desconocido@sgroas.com");
        when(userRepository.findByEmail("desconocido@sgroas.com"))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.refresh(new RefreshTokenRequest("refresh-token-prueba")));
    }

    @Test
    void logoutShouldInvalidateTokens() {
        authService.logout("access-token-prueba",
                new RefreshTokenRequest("refresh-token-prueba"));

        verify(tokenService).addAccessTokenToBlacklist("access-token-prueba");
        verify(tokenService).deleteRefreshToken("refresh-token-prueba");
    }
}
