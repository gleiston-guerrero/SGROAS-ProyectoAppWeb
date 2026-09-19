package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.entity.VerificationCode;
import ec.edu.uteq.sgroas.repository.VerificationCodeRepository;
import ec.edu.uteq.sgroas.service.VerificationCodeService.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    private static final String EMAIL = "carlos@sgroas.com";

    @Mock
    private VerificationCodeRepository repository;

    @InjectMocks
    private VerificationCodeService service;

    private String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private VerificationCode verificationRecord(String codigoClaro) {
        return VerificationCode.builder()
                .id(1L)
                .email(EMAIL)
                .codeHash(sha256(codigoClaro))
                .type(Type.VERIFICACION.name())
                .expiresAt(Instant.now().plusSeconds(600))
                .attempts(0)
                .used(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void generateInvalidatesPreviousAndSavesNew() {
        when(repository.save(any(VerificationCode.class))).thenAnswer(inv -> inv.getArgument(0));

        String codigo = service.generate(EMAIL, Type.VERIFICACION);

        verify(repository).deleteByEmailAndType(EMAIL, Type.VERIFICACION.name());
        verify(repository).save(argThat(reg ->
                reg.getCodeHash().equals(sha256(codigo))
                        && reg.getEmail().equals(EMAIL)
                        && reg.getType().equals(Type.VERIFICACION.name())
                        && !reg.isUsed()));
        assertTrue(codigo.matches("\\d{6}"));
    }

    @Test
    void canResendWithoutRecordShouldBeTrue() {
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.empty());

        assertTrue(service.canResend(EMAIL, Type.VERIFICACION));
    }

    @Test
    void canResendWithRecentCodeShouldBeFalse() {
        VerificationCode reciente = verificationRecord("123456");
        reciente.setCreatedAt(Instant.now().minusSeconds(10));
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.of(reciente));

        assertFalse(service.canResend(EMAIL, Type.VERIFICACION));
    }

    @Test
    void canResendWithOldCodeShouldBeTrue() {
        VerificationCode antiguo = verificationRecord("123456");
        antiguo.setCreatedAt(Instant.now().minusSeconds(120));
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.of(antiguo));

        assertTrue(service.canResend(EMAIL, Type.VERIFICACION));
    }

    @Test
    void validateCorrectCodeMarksItUsed() {
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.of(verificationRecord("123456")));

        service.validate(EMAIL, Type.VERIFICACION, "123456");

        verify(repository).save(argThat(VerificationCode::isUsed));
    }

    @Test
    void validateIncorrectCodeIncrementsAttemptsAndFails() {
        VerificationCode reg = verificationRecord("123456");
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.of(reg));

        assertThrows(IllegalArgumentException.class,
                () -> service.validate(EMAIL, Type.VERIFICACION, "999999"));
        verify(repository).save(argThat(c -> c.getAttempts() == 1 && !c.isUsed()));
    }

    @Test
    void validateWithoutRecordFails() {
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.validate(EMAIL, Type.VERIFICACION, "123456"));
    }

    @Test
    void validateAlreadyUsedCodeFails() {
        VerificationCode usado = verificationRecord("123456");
        usado.setUsed(true);
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.of(usado));

        assertThrows(IllegalArgumentException.class,
                () -> service.validate(EMAIL, Type.VERIFICACION, "123456"));
    }

    @Test
    void validateExpiredCodeFails() {
        VerificationCode expirado = verificationRecord("123456");
        expirado.setExpiresAt(Instant.now().minusSeconds(60));
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.of(expirado));

        assertThrows(IllegalArgumentException.class,
                () -> service.validate(EMAIL, Type.VERIFICACION, "123456"));
    }

    @Test
    void validateWithExhaustedAttemptsFails() {
        VerificationCode agotado = verificationRecord("123456");
        agotado.setAttempts(5);
        when(repository.findFirstByEmailAndTypeOrderByCreatedAtDesc(
                EMAIL, Type.VERIFICACION.name())).thenReturn(Optional.of(agotado));

        assertThrows(IllegalArgumentException.class,
                () -> service.validate(EMAIL, Type.VERIFICACION, "123456"));
    }
}
