package ec.edu.uteq.sgroas.config;

import ec.edu.uteq.sgroas.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Crea la configuracion de seguridad con el filtro encargado de validar tokens.
     * @param jwtAuthenticationFilter filtro que autentica cada peticion con el token recibido
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Expone el codificador usado para proteger y comparar las claves de acceso.
     * @return codificador con algoritmo de resumen adaptativo para claves
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Arma el gestor que verifica las credenciales contra los usuarios registrados.
     * @param userDetailsService servicio que carga los datos del usuario por su correo
     * @param passwordEncoder codificador usado para comparar la clave ingresada
     * @return gestor de autenticacion con proveedor basado en usuarios y claves
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authProvider);
    }

    /**
     * Define las reglas de acceso por rol, la sesion sin estado y las cabeceras seguras.
     * @param http configurador donde se registran rutas, filtros y politicas de seguridad
     * @return cadena de filtros con toda la proteccion de las peticiones HTTP
     * @throws Exception cuando la construccion de la cadena de seguridad falla
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/favicon.ico",
                                "/assets/**",
                                "/*.js", "/*.css", "/*.png", "/*.svg", "/*.ico",
                                "/*.woff", "/*.woff2", "/*.ttf", "/*.txt").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/docs/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // ---------- Control de acceso por rol (RBAC) ----------
                        // Usuarios del sistema: solo ADMINISTRACION
                        .requestMatchers("/api/usuarios/**").hasAuthority("ROLE_ADMIN")
                        // Escritura en flota, unidades y rutas: ADMIN y COORDINADOR
                        .requestMatchers(HttpMethod.POST, "/api/conductores/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/conductores/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/conductores/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.POST, "/api/abd/unidades/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/abd/unidades/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/abd/unidades/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.POST, "/api/abd/rutas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/abd/rutas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/abd/rutas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR")
                        // Programaciones: tambien el OPERADOR registra salidas
                        .requestMatchers(HttpMethod.POST, "/api/abd/programaciones/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR", "ROLE_OPERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/abd/programaciones/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR", "ROLE_OPERADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/abd/programaciones/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_COORDINADOR", "ROLE_OPERADOR")
                        // Incidentes: solo SEGURIDAD (y ADMIN) reportan o cierran
                        .requestMatchers(HttpMethod.POST, "/api/abd/incidentes/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SEGURIDAD")
                        .requestMatchers(HttpMethod.PUT, "/api/abd/incidentes/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SEGURIDAD")
                        .requestMatchers(HttpMethod.DELETE, "/api/abd/incidentes/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SEGURIDAD")
                        // Lectura general para cualquier usuario autenticado
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(content -> { })
                        .xssProtection(xss -> { })
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Content-Security-Policy",
                                "default-src 'self'; script-src 'self'; "
                                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                        + "font-src 'self' https://fonts.gstatic.com"
                        ))
                );

        return http.build();
    }

    /**
     * Define los origenes y metodos permitidos para las peticiones entre dominios.
     * @return fuente con la politica de acceso aplicada a todas las rutas
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}