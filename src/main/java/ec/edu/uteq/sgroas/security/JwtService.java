package ec.edu.uteq.sgroas.security;

import ec.edu.uteq.sgroas.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private Long jwtExpirationMs;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    @Value("${app.jwt.audience}")
    private String jwtAudience;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Crea un token firmado con los datos del user para autenticar sus peticiones.
     * @param user entidad con correo, nombre y rol que se guardan en el token
     * @return token compacto listo para enviar en cabecera o cookie
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .issuer(jwtIssuer)
                .subject(user.getEmail())
                .audience().add(jwtAudience).and()
                .issuedAt(now)
                .notBefore(now)
                .expiration(expiration)
                .claim("nombre", user.getName())
                .claim("rol", user.getRole().name())
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Recupera el correo del propietario a partir de un token firmado.
     * @param token texto compacto previamente generado por este servicio
     * @return correo guardado en el asunto del token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Recupera el identificador unico del token para controlar revocaciones.
     * @param token texto compacto previamente generado por este servicio
     * @return identificador unico asignado al crear el token
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Recupera la fecha de vencimiento contenida en un token firmado.
     * @param token texto compacto previamente generado por este servicio
     * @return fecha a partir de la cual el token deja de aceptarse
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Informa el tiempo de vida configurado para los tokens emitidos.
     * @return duracion de vigencia en milisegundos definida en propiedades
     */
    public Long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Comprueba que un token pertenezca al user esperado y siga vigente.
     * @param token texto compacto a validar con firma y fecha de expiration
     * @param email correo esperado del propietario para comparar con el asunto
     * @return verdadero cuando el correo coincide y el token no ha expirado
     */
    public boolean isTokenValid(String token, String email) {
        String tokenEmail = extractEmail(token);
        return tokenEmail.equals(email) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
