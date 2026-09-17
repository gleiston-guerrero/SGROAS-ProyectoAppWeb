package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    /**
     * Genera un token de refresco aleatorio y lo guarda asociado al correo indicado.
     * @param email correo del usuario propietario de la nueva sesion de refresco
     * @param refreshExpirationMs tiempo de vida del token expresado en milisegundos
     * @return valor del token de refresco recien creado
     */
    public String createRefreshToken(String email, Long refreshExpirationMs) {
        String refreshToken = UUID.randomUUID().toString();

        String key = "refresh:" + refreshToken;
        redisTemplate.opsForValue().set(
                key,
                email,
                Duration.ofMillis(refreshExpirationMs)
        );

        return refreshToken;
    }

    /**
     * Recupera el correo guardado detras de un token de refresco vigente.
     * @param refreshToken valor del token de refresco que se desea resolver
     * @return correo del usuario asociado al token consultado
     * @throws IllegalArgumentException cuando el token no existe o ya expiro
     */
    public String getEmailFromRefreshToken(String refreshToken) {
        String key = "refresh:" + refreshToken;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new IllegalArgumentException("Refresh token no valido o expirado");
        }

        return email;
    }

    /**
     * Elimina un token de refresco para impedir que vuelva a usarse.
     * @param refreshToken valor del token de refresco que se desea borrar
     */
    public void deleteRefreshToken(String refreshToken) {
        String key = "refresh:" + refreshToken;
        redisTemplate.delete(key);
    }

    /**
     * Anula un token de acceso agregandolo a la lista negra hasta su vencimiento.
     * Solo se registra cuando aun le queda tiempo de vida util.
     * @param accessToken token de acceso vigente que se desea invalidar
     */
    public void addAccessTokenToBlacklist(String accessToken) {
        String jti = jwtService.extractJti(accessToken);
        long tiempoRestante = jwtService.extractExpiration(accessToken).getTime()
                - System.currentTimeMillis();

        if (tiempoRestante > 0) {
            String key = "blacklist:" + jti;
            redisTemplate.opsForValue().set(
                    key,
                    "logout",
                    Duration.ofMillis(tiempoRestante)
            );
        }
    }

    /**
     * Revisa si un token de acceso ya fue anulado y figura en lista negra.
     * @param accessToken token de acceso que se desea verificar
     * @return verdadero cuando el token esta anulado, falso en caso contrario
     */
    public boolean accessTokenEnBlacklist(String accessToken) {
        String jti = jwtService.extractJti(accessToken);
        String key = "blacklist:" + jti;

        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
