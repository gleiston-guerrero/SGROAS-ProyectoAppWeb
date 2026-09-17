package ec.edu.uteq.sgroas.security;

import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.repository.UserRepository;
import ec.edu.uteq.sgroas.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContextAfterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withoutTokenContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void headerTokenShouldContinueChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn("admin@sgroas.com");
        when(customUserDetailsService.loadUserByUsername("admin@sgroas.com"))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        "admin@sgroas.com", "hash", java.util.List.of()));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void cookieTokenShouldContinueChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies())
                .thenReturn(new Cookie[]{new Cookie("access_token", "access-token")});
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn("admin@sgroas.com");
        when(customUserDetailsService.loadUserByUsername("admin@sgroas.com"))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        "admin@sgroas.com", "hash", java.util.List.of()));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blacklistedTokenShouldRespondUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void tokenWithNullEmailShouldContinueChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validTokenShouldSetAuthentication() throws Exception {
        User usuario = User.builder()
                .id(1L)
                .name("Administrador SGROAS")
                .email("admin@sgroas.com")
                .passwordHash("hash")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "admin@sgroas.com", "hash", java.util.List.of());

        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/conductores");
        when(tokenService.accessTokenEnBlacklist("access-token")).thenReturn(false);
        when(jwtService.extractEmail("access-token")).thenReturn("admin@sgroas.com");
        when(customUserDetailsService.loadUserByUsername("admin@sgroas.com"))
                .thenReturn(userDetails);
        when(jwtService.isTokenValid("access-token", "admin@sgroas.com")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin@sgroas.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }
}
