package ec.edu.uteq.sgroas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EmailServiceTest {

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService();
    }

    @Test
    void configuredWithoutHostIsFalse() {
        ReflectionTestUtils.setField(service, "host", null);

        assertFalse(service.isConfigured());
    }

    @Test
    void configuredWithBlankHostIsFalse() {
        ReflectionTestUtils.setField(service, "host", "   ");

        assertFalse(service.isConfigured());
    }

    @Test
    void configuredWithHostIsTrue() {
        ReflectionTestUtils.setField(service, "host", "smtp.sgroas.com");

        assertTrue(service.isConfigured());
    }

    @Test
    void consoleModeShouldNotThrowWhenSendingCodes() {
        ReflectionTestUtils.setField(service, "host", "");

        assertDoesNotThrow(() -> service.sendVerificationCode(
                "carlos@sgroas.com", "Carlos Mendoza", "123456"));
        assertDoesNotThrow(() -> service.sendResetCode(
                "carlos@sgroas.com", "654321"));
    }

    @Test
    void sendWithSmtpWithoutServerThrowsIllegalState() {
        ReflectionTestUtils.setField(service, "host", "127.0.0.1");
        ReflectionTestUtils.setField(service, "port", 25);
        ReflectionTestUtils.setField(service, "username", "");
        ReflectionTestUtils.setField(service, "password", "");

        assertThrows(IllegalStateException.class,
                () -> service.sendVerificationCode(
                        "carlos@sgroas.com", "Carlos Mendoza", "123456"));
    }

    @Test
    void sendWithSmtpAndCredentialsWithoutServerThrowsIllegalState() {
        ReflectionTestUtils.setField(service, "host", "127.0.0.1");
        ReflectionTestUtils.setField(service, "port", 25);
        ReflectionTestUtils.setField(service, "username", "smtp-user");
        ReflectionTestUtils.setField(service, "password", "smtp-pass");

        assertThrows(IllegalStateException.class,
                () -> service.sendResetCode(
                        "carlos@sgroas.com", "654321"));
    }
}