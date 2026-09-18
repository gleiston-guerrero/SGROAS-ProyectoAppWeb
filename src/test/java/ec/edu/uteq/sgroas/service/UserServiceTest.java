package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.UserRequest;
import ec.edu.uteq.sgroas.dto.UserResponse;
import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User usuarioEjemplo() {
        return User.builder()
                .id(1L)
                .name("Carlos Mendoza")
                .email("carlos@sgroas.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private UserRequest requestEjemplo() {
        return new UserRequest("Carlos Mendoza", "carlos@sgroas.com", "123456", "ROLE_ADMIN");
    }

    @Test
    void listReturnsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(usuarioEjemplo())));

        Page<UserResponse> pagina = userService.list(null, pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("ROLE_ADMIN", pagina.getContent().get(0).role());
    }

    @Test
    void listWithSearchUsesSearchActive() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.searchActive("carlos", pageable))
                .thenReturn(new PageImpl<>(List.of(usuarioEjemplo())));

        Page<UserResponse> pagina = userService.list("  Carlos  ", pageable);

        assertEquals(1, pagina.getTotalElements());
        verify(userRepository).searchActive("carlos", pageable);
        verify(userRepository, never()).findByActiveTrue(pageable);
    }

    @Test
    void listWithBlankSearchUsesFindByActiveTrue() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(usuarioEjemplo())));

        Page<UserResponse> pagina = userService.list("   ", pageable);

        assertEquals(1, pagina.getTotalElements());
        verify(userRepository).findByActiveTrue(pageable);
        verify(userRepository, never()).searchActive(any(), any());
    }

    @Test
    void findByIdReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));

        UserResponse response = userService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("carlos@sgroas.com", response.email());
    }

    @Test
    void findByIdNonexistentThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.findById(99L));
    }

    @Test
    void createSavesUnverifiedAndSendsActivationCode() {
        when(userRepository.existsByEmail("carlos@sgroas.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash-encrypted");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(verificationCodeService.generate("carlos@sgroas.com",
                VerificationCodeService.Type.VERIFICACION)).thenReturn("123456");

        UserResponse response = userService.create(requestEjemplo());

        assertEquals("carlos@sgroas.com", response.email());
        verify(userRepository).save(argThat(u ->
                Boolean.FALSE.equals(u.getVerified()) && Boolean.TRUE.equals(u.getActive())));
        verify(emailService).sendVerificationCode(
                "carlos@sgroas.com", "Carlos Mendoza", "123456");
    }

    @Test
    void createWithDuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("carlos@sgroas.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.create(requestEjemplo()));
    }

    @Test
    void resendActivationShouldSendNewCode() {
        User sinVerificar = usuarioEjemplo();
        sinVerificar.setVerified(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sinVerificar));
        when(verificationCodeService.canResend("carlos@sgroas.com",
                VerificationCodeService.Type.VERIFICACION)).thenReturn(true);
        when(verificationCodeService.generate("carlos@sgroas.com",
                VerificationCodeService.Type.VERIFICACION)).thenReturn("654321");

        userService.resendActivationCode(1L);

        verify(emailService).sendVerificationCode(
                "carlos@sgroas.com", "Carlos Mendoza", "654321");
    }

    @Test
    void resendActivationWithAlreadyVerifiedAccountShouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));

        assertThrows(IllegalArgumentException.class,
                () -> userService.resendActivationCode(1L));
    }

    @Test
    void resendActivationWithinWaitPeriodShouldThrowException() {
        User sinVerificar = usuarioEjemplo();
        sinVerificar.setVerified(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sinVerificar));
        when(verificationCodeService.canResend("carlos@sgroas.com",
                VerificationCodeService.Type.VERIFICACION)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.resendActivationCode(1L));
        verify(emailService, never()).sendVerificationCode(any(), any(), any());
    }

    @Test
    void resendActivationWithNonexistentUserShouldThrowException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.resendActivationCode(99L));
    }

    @Test
    void updateModifiesAndReturns() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));
        when(userRepository.save(any(User.class))).thenReturn(usuarioEjemplo());

        UserResponse response = userService.update(1L, requestEjemplo());

        assertEquals(1L, response.id());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateWithoutPasswordKeepsPasswordHash() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));
        when(userRepository.save(any(User.class))).thenReturn(usuarioEjemplo());

        UserRequest request = new UserRequest(
                "Carlos Mendoza", "carlos@sgroas.com", null, "ROLE_ADMIN");

        UserResponse response = userService.update(1L, request);

        assertEquals("Carlos Mendoza", response.name());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateWithBlankPasswordKeepsPasswordHash() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));
        when(userRepository.save(any(User.class))).thenReturn(usuarioEjemplo());

        UserRequest request = new UserRequest(
                "Carlos Mendoza", "carlos@sgroas.com", "   ", "ROLE_ADMIN");

        UserResponse response = userService.update(1L, request);

        assertEquals("Carlos Mendoza", response.name());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateNonexistentThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.update(99L, requestEjemplo()));
    }

    @Test
    void deactivateMarksInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo()));

        userService.deactivate(1L);

        verify(userRepository).save(argThat(u -> !u.getActive()));
    }

    @Test
    void deactivateNonexistentThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.deactivate(99L));
    }
}
