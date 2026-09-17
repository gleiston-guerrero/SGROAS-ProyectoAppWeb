package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.AuthResponse;
import ec.edu.uteq.sgroas.dto.LoginRequest;
import ec.edu.uteq.sgroas.dto.RefreshTokenRequest;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.exception.UnverifiedEmailException;
import ec.edu.uteq.sgroas.repository.UserRepository;
import ec.edu.uteq.sgroas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Autenticacion de SGROAS. No existe registro publico: los usuarios los crea
 * el ADMIN desde el modulo Usuarios (UserService), que envia por correo un
 * codigo de activacion; aqui solo se valida ese codigo y se gestionan las
 * sesiones y el restablecimiento de contrasena.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private Long refreshExpirationMs;

    /**
     * Valida las credenciales del usuario y genera los tokens de la sesion.
     * @param request datos de acceso con correo y contrasenia ingresados por el usuario
     * @return respuesta con los tokens generados y los datos basicos de la sesion
     * @throws BadCredentialsException cuando el correo no existe, la contrasenia no coincide o la cuenta esta inactiva
     * @throws UnverifiedEmailException cuando la cuenta aun no confirma su correo de activacion
     */
    public AuthResponse login(LoginRequest request) {
        User usuario = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        if (Boolean.FALSE.equals(usuario.getVerified())) {
            throw new UnverifiedEmailException(
                    "Tu cuenta aun no esta verificada. Revisa tu correo e ingresa el codigo de 6 digitos.");
        }

        if (!usuario.getActive()) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        return buildAuthResponse(usuario);
    }

    /**
     * Confirma el codigo de activacion enviado por el administrador e inicia sesion.
     * Activa la cuenta verificada y devuelve los tokens de acceso de la primera sesion.
     * @param email correo de la cuenta que desea confirmar su activacion
     * @param codigo codigo de seis digitos recibido por correo para activar la cuenta
     * @return respuesta con los tokens generados y los datos basicos de la sesion
     * @throws IllegalArgumentException cuando no existe una cuenta con ese correo o el codigo es invalido o expiro
     */
    public AuthResponse verifyEmail(String email, String codigo) {
        User usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe una cuenta con ese email"));

        verificationCodeService.validate(email, VerificationCodeService.Type.VERIFICACION, codigo);

        usuario.setVerified(true);
        usuario.setActive(true);
        usuario.setUpdatedAt(Instant.now());
        userRepository.save(usuario);

        return buildAuthResponse(usuario);
    }

    /**
     * Reenvia el codigo de activacion si la cuenta sigue sin verificar y respetando la espera minima.
     * Si la cuenta ya esta verificada o se pidio un codigo hace poco, no se envia nada nuevo.
     * @param email correo de la cuenta pendiente de verificacion que solicita otro codigo
     */
    public void resendVerificationCode(String email) {
        userRepository.findByEmail(email)
                .filter(u -> Boolean.FALSE.equals(u.getVerified()))
                .filter(u -> verificationCodeService.canResend(email,
                        VerificationCodeService.Type.VERIFICACION))
                .ifPresent(this::sendActivationCode);
    }

    /**
     * Envia un codigo para restablecer la contrasena manteniendo una respuesta generica al solicitante.
     * Solo genera codigo cuando la cuenta existe, esta activa y respeto la espera minima de reenvio.
     * @param email correo de la cuenta que solicita recuperar su contrasenia
     */
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email)
                .filter(User::getActive)
                .filter(u -> verificationCodeService.canResend(email,
                        VerificationCodeService.Type.RESET_PASSWORD))
                .ifPresent(u -> {
                    String codigo = verificationCodeService.generate(email,
                            VerificationCodeService.Type.RESET_PASSWORD);
                    emailService.sendResetCode(email, codigo);
                });
    }

    /**
     * Valida el codigo recibido y reemplaza la contrasena anterior por la nueva.
     * Deja la cuenta activa y marcada como verificada despues del cambio.
     * @param email correo de la cuenta que desea cambiar su contrasenia
     * @param codigo codigo de seis digitos recibido por correo para autorizar el cambio
     * @param nuevaPassword contrasenia nueva en claro que sera cifrada antes de guardarse
     * @throws IllegalArgumentException cuando no existe una cuenta con ese correo o el codigo es invalido o expiro
     */
    public void resetPassword(String email, String codigo, String nuevaPassword) {
        User usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe una cuenta con ese email"));

        verificationCodeService.validate(email, VerificationCodeService.Type.RESET_PASSWORD, codigo);

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setActive(true);
        usuario.setVerified(true);
        usuario.setUpdatedAt(Instant.now());
        userRepository.save(usuario);
    }

    private void sendActivationCode(User usuario) {
        String codigo = verificationCodeService.generate(usuario.getEmail(),
                VerificationCodeService.Type.VERIFICACION);
        emailService.sendVerificationCode(usuario.getEmail(), usuario.getName(), codigo);
    }

    /**
     * Rota el token de refresco vigente y entrega un nuevo par de tokens de sesion.
     * Elimina el token anterior para que no pueda reutilizarse.
     * @param request datos con el token de refresco vigente que se desea renovar
     * @return respuesta con los tokens nuevos y los datos basicos de la sesion
     * @throws IllegalArgumentException cuando el token de refresco no existe o ya expiro
     * @throws BadCredentialsException cuando el usuario asociado ya no esta activo
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        String email = tokenService.getEmailFromRefreshToken(request.refreshToken());

        User usuario = userRepository.findByEmail(email)
                .filter(User::getActive)
                .orElseThrow(() -> new BadCredentialsException("User no valido"));

        tokenService.deleteRefreshToken(request.refreshToken());

        return buildAuthResponse(usuario);
    }

    /**
     * Cierra la sesion invalidando el token de acceso y eliminando el de refresco.
     * El acceso anulado queda en lista negra hasta que caduque su vigencia original.
     * @param accessToken token de acceso vigente que se desea invalidar
     * @param request datos con el token de refresco asociado a la misma sesion
     */
    public void logout(String accessToken, RefreshTokenRequest request) {
        tokenService.addAccessTokenToBlacklist(accessToken);
        tokenService.deleteRefreshToken(request.refreshToken());
    }

    private AuthResponse buildAuthResponse(User usuario) {
        String accessToken = jwtService.generateToken(usuario);
        String refreshToken = tokenService.createRefreshToken(
                usuario.getEmail(),
                refreshExpirationMs
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getExpirationMs(),
                usuario.getName(),
                usuario.getEmail(),
                usuario.getRole().name()
        );
    }
}