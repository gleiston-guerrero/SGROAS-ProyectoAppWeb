package ec.edu.uteq.sgroas.security;

import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameShouldReturnUser() {
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

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@sgroas.com");

        assertEquals("admin@sgroas.com", userDetails.getUsername());
        assertEquals("password-encriptado", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsernameWithNonexistentEmailShouldThrowException() {
        when(userRepository.findByEmail("desconocido@sgroas.com"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("desconocido@sgroas.com"));
    }

    @Test
    void loadUserByUsernameWithInactiveUserShouldThrowException() {
        User inactivo = User.builder()
                .id(1L)
                .name("Administrador")
                .email("admin@sgroas.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(false)
                .build();
        when(userRepository.findByEmail("admin@sgroas.com"))
                .thenReturn(Optional.of(inactivo));

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("admin@sgroas.com"));
    }
}
