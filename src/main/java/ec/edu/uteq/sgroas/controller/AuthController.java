package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.AuthResponse;
import ec.edu.uteq.sgroas.dto.EmailRequest;
import ec.edu.uteq.sgroas.dto.LoginRequest;
import ec.edu.uteq.sgroas.dto.RefreshTokenRequest;
import ec.edu.uteq.sgroas.dto.ResetPasswordRequest;
import ec.edu.uteq.sgroas.dto.SessionResponse;
import ec.edu.uteq.sgroas.dto.VerifyEmailRequest;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.repository.UserRepository;
import ec.edu.uteq.sgroas.security.JwtService;
import ec.edu.uteq.sgroas.security.LoginRateLimiter;
import ec.edu.uteq.sgroas.service.AuthService;
import ec.edu.uteq.sgroas.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    // Nota de auditoria (2026-09-16): existia un @Value("${app.cookie.secure:false}")
    // que quedo sin usar despues de que las cookies se fijaran con .secure(true)
    // a fuego mas abajo. Se elimino el campo muerto en lugar de conectarlo,
    // porque una cookie de sesion sin el flag Secure es un riesgo real si algun
    // entorno terminara con app.cookie.secure=false por error de configuracion;
    // Secure siempre activo es la opcion mas segura y no depende de config externa.

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    /**
     * Recupera los datos de la sesión actual a partir del token vigente.
     * @param accessTokenCookie valor de la galleta con el token de acceso si fue enviada.
     * @param authorizationHeader cabecera de autorización con el token portador como alternativa.
     * @return respuesta HTTP con el perfil de la sesión o estado no autorizado.
     */
    @GetMapping("/me")
    public ResponseEntity<SessionResponse> me(
            @CookieValue(name = "access_token", required = false) String accessTokenCookie,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        String token = accessTokenCookie;
        if ((token == null || token.isBlank()) && authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }
        if (token == null || token.isBlank() || tokenService.accessTokenEnBlacklist(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = jwtService.extractEmail(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User usuario = userRepository.findByEmail(email)
                .filter(User::getActive)
                .orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new SessionResponse(
                usuario.getName(),
                usuario.getEmail(),
                usuario.getRole().name(),
                jwtService.getExpirationMs()
        ));
    }

    /*
     * No existe registro publico: los usuarios los crea el ADMIN desde el
     * modulo Usuarios (POST /api/usuarios) y ahi mismo se les envia por
     * correo el codigo de activacion que se confirma en /verify-email.
     *
     * Los tokens viajan SOLO en cookies HttpOnly (access_token +
     * refresh_token). El cuerpo devuelve el perfil de sesion sin JWT.
     */

    /**
     * Valida las credenciales y genera las galletas de sesión para el usuario.
     * @param request objeto con el correo y la contraseña enviados para ingresar.
     * @param httpRequest petición HTTP usada para obtener la dirección del cliente.
     * @return respuesta HTTP con el perfil de la sesión y las galletas de seguridad.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();
        if (loginRateLimiter.isBlocked(ip)) {
            ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
            detail.setTitle("Demasiadas solicitudes");
            detail.setDetail("Has superado el limite de intentos de inicio de sesion. Espera 60 segundos.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(detail);
        }
        try {
            AuthResponse response = authService.login(request);
            loginRateLimiter.reset(ip);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(response.accessToken()))
                    .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(response.refreshToken()))
                    .body(toSessionResponse(response));
        } catch (Exception e) {
            loginRateLimiter.recordFailedAttempt(ip);
            throw e;
        }
    }

    /**
     * Renueva la sesión generando nuevos tokens a partir del token de refresco.
     * @param refreshCookie valor de la galleta con el token de refresco si fue enviada.
     * @param body cuerpo opcional con el token de refresco como alternativa.
     * @return respuesta HTTP con el perfil renovado y las nuevas galletas de seguridad.
     */
    @PostMapping("/refresh")
    public ResponseEntity<SessionResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshCookie,
            @RequestBody(required = false) RefreshTokenRequest body
    ) {
        String refreshToken = refreshCookie;
        if ((refreshToken == null || refreshToken.isBlank()) && body != null) {
            refreshToken = body.refreshToken();
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(response.accessToken()))
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(response.refreshToken()))
                .body(toSessionResponse(response));
    }

    /**
     * Activa la cuenta al confirmar el código enviado al correo e inicia sesión.
     * @param request objeto con el correo y el código de verificación recibido.
     * @return respuesta HTTP con el perfil de la sesión recién activada.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<SessionResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        AuthResponse response = authService.verifyEmail(request.email(), request.codigo());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(response.accessToken()))
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(response.refreshToken()))
                .body(toSessionResponse(response));
    }

    /**
     * Envía un nuevo código de verificación sin revelar si la cuenta existe.
     * @param request objeto con el correo al que se reenvía el código de verificación.
     * @return respuesta HTTP con un mensaje genérico de confirmación del envío.
     */
    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, String>> resendCode(
            @Valid @RequestBody EmailRequest request
    ) {
        authService.resendVerificationCode(request.email());
        return ResponseEntity.ok(Map.of(
                "mensaje",
                "Si el correo corresponde a una cuenta pendiente, enviamos un nuevo codigo. Revisa tambien tu carpeta de spam."
        ));
    }

    /**
     * Genera y envía el código para restablecer la contraseña sin revelar cuentas.
     * @param request objeto con el correo al que se envía el código de restablecimiento.
     * @return respuesta HTTP con un mensaje genérico de confirmación del envío.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody EmailRequest request
    ) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of(
                "mensaje",
                "Si el correo esta registrado, enviamos un codigo para restablecer la contrasena."
        ));
    }

    /**
     * Actualiza la contraseña usando el código recibido por correo.
     * @param request objeto con el correo, el código y la nueva contraseña elegida.
     * @return respuesta HTTP con el mensaje de confirmación del cambio realizado.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request.email(), request.codigo(), request.nuevaPassword());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Contrasena actualizada correctamente. Ya puedes iniciar sesion."
        ));
    }

    /**
     * Cierra la sesión invalidando los tokens y eliminando las galletas de seguridad.
     * @param accessTokenCookie valor de la galleta con el token de acceso a invalidar.
     * @param refreshCookie valor de la galleta con el token de refresco a invalidar.
     * @param body cuerpo opcional con el token de refresco como alternativa.
     * @return respuesta HTTP sin contenido que confirma el cierre de la sesión.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "access_token", required = false) String accessTokenCookie,
            @CookieValue(name = "refresh_token", required = false) String refreshCookie,
            @RequestBody(required = false) RefreshTokenRequest body
    ) {
        String accessToken = accessTokenCookie == null ? "" : accessTokenCookie;
        String refreshToken = refreshCookie;
        if ((refreshToken == null || refreshToken.isBlank()) && body != null) {
            refreshToken = body.refreshToken();
        }
        authService.logout(accessToken,
                new RefreshTokenRequest(refreshToken == null ? "" : refreshToken));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, removeAccessTokenCookie())
                .header(HttpHeaders.SET_COOKIE, removeRefreshTokenCookie())
                .build();
    }

    private SessionResponse toSessionResponse(AuthResponse response) {
        return new SessionResponse(
                response.name(),
                response.email(),
                response.role(),
                response.expiresIn()
        );
    }

    private String buildAccessTokenCookie(String token) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build()
                .toString();
    }

    private String buildRefreshTokenCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build()
                .toString();
    }

    private String removeAccessTokenCookie() {
        return ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }

    private String removeRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build()
                .toString();
    }
}
