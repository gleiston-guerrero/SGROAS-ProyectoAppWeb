package ec.edu.uteq.sgroas.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    @Test
    void ipWithoutAttemptsShouldNotBeBlocked() {
        LoginRateLimiter limiter = new LoginRateLimiter();

        assertFalse(limiter.isBlocked("192.168.1.1"));
    }

    @Test
    void fewerThanSixAttemptsDoesNotBlock() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.recordFailedAttempt("192.168.1.1");
        }

        assertFalse(limiter.isBlocked("192.168.1.1"));
    }

    @Test
    void sixFailedAttemptsBlocks() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 6; i++) {
            limiter.recordFailedAttempt("192.168.1.1");
        }

        assertTrue(limiter.isBlocked("192.168.1.1"));
    }

    @Test
    void resetUnblocksIp() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 6; i++) {
            limiter.recordFailedAttempt("192.168.1.1");
        }
        assertTrue(limiter.isBlocked("192.168.1.1"));

        limiter.reset("192.168.1.1");

        assertFalse(limiter.isBlocked("192.168.1.1"));
    }

    @Test
    void expiredLockoutShouldUnblock() throws InterruptedException {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 6; i++) {
            limiter.recordFailedAttempt("192.168.1.1");
        }
        assertTrue(limiter.isBlocked("192.168.1.1"));

        Thread.sleep(61_000);

        assertFalse(limiter.isBlocked("192.168.1.1"));
    }
}
