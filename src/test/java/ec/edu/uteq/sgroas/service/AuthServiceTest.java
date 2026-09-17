package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.AuthResponse;
import ec.edu.uteq.sgroas.dto.LoginRequest;
import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.repository.UserRepository;
import ec.edu.uteq.sgroas.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginSuccessShouldReturnTokens() {
        User usuario = User.builder()
                .id(1L)
                .name("Administrador SGROAS")
                .email("admin@sgroas.com")
                .passwordHash("password-encriptado")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "password-encriptado"))
                .thenReturn(true);
        when(jwtService.generateToken(usuario))
                .thenReturn("access-token-prueba");
        when(jwtService.getExpirationMs())
                .thenReturn(3600000L);
        when(tokenService.createRefreshToken(eq("admin@sgroas.com"), any()))
                .thenReturn("refresh-token-prueba");

        AuthResponse response = authService.login(
                new LoginRequest("admin@sgroas.com", "123456")
        );

        assertNotNull(response);
        assertEquals("access-token-prueba", response.accessToken());
        assertEquals("refresh-token-prueba", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("ROLE_ADMIN", response.role());
    }

    @Test
    void loginWithIncorrectPasswordShouldThrowException() {
        User usuario = User.builder()
                .id(1L)
                .name("Administrador SGROAS")
                .email("admin@sgroas.com")
                .passwordHash("password-encriptado")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-mal", "password-encriptado"))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest("admin@sgroas.com", "clave-mal"))
        );
    }
}