package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.UserRequest;
import ec.edu.uteq.sgroas.dto.UserResponse;
import ec.edu.uteq.sgroas.entity.Role;
import ec.edu.uteq.sgroas.entity.User;
import ec.edu.uteq.sgroas.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;

    /**
     * Obtiene la pagina de usuarios activos, con filtro opcional por texto de busqueda.
     * @param search texto opcional para filtrar por nombre o correo, nulo o vacio para traer todo
     * @param pageable objeto con numero de pagina, tamanio y orden solicitados para la consulta
     * @return pagina con los usuarios activos encontrados
     */
    public Page<UserResponse> list(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return userRepository.findByActiveTrue(pageable).map(this::toResponse);
        }
        return userRepository.searchActive(search.trim().toLowerCase(), pageable)
                .map(this::toResponse);
    }

    /**
     * Recupera el detalle de un usuario existente por su identificador.
     * @param id identificador del usuario que se desea consultar
     * @return datos del usuario encontrado
     * @throws EntityNotFoundException cuando no existe un usuario con ese identificador
     */
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("User no encontrado con id: " + id));
    }

    /**
     * Crea el usuario y le envia por correo un codigo de activacion de seis digitos.
     * La cuenta nace sin verificar y no podra iniciar sesion hasta confirmar el codigo recibido.
     * @param request datos del usuario con nombre, correo, contrasenia y rol asignado
     * @return datos del usuario recien guardado
     * @throws IllegalArgumentException cuando ya existe otro usuario con el mismo correo
     */
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        User usuario = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.valueOf(request.role().toUpperCase()))
                .active(true)
                .verified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User guardado = userRepository.save(usuario);
        sendActivationCode(guardado);

        return toResponse(guardado);
    }

    /**
     * Reenvia el codigo de activacion a una cuenta que aun no verifica su correo.
     * El administrador usa esta accion cuando el usuario perdio o no recibio el primer codigo.
     * @param id identificador del usuario pendiente de verificacion que recibira el codigo
     * @throws EntityNotFoundException cuando no existe un usuario con ese identificador
     * @throws IllegalArgumentException cuando la cuenta ya verifico su correo o se pidio un codigo hace menos de un minuto
     */
    public void resendActivationCode(Long id) {
        User usuario = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User no encontrado con id: " + id));

        if (Boolean.TRUE.equals(usuario.getVerified())) {
            throw new IllegalArgumentException("Ese usuario ya verifico su correo");
        }
        if (!verificationCodeService.canResend(usuario.getEmail(),
                VerificationCodeService.Type.VERIFICACION)) {
            throw new IllegalArgumentException(
                    "El codigo se envio hace menos de un minuto. Espera antes de reenviar.");
        }
        sendActivationCode(usuario);
    }

    private void sendActivationCode(User usuario) {
        String codigo = verificationCodeService.generate(usuario.getEmail(),
                VerificationCodeService.Type.VERIFICACION);
        emailService.sendVerificationCode(usuario.getEmail(), usuario.getName(), codigo);
    }

    /**
     * Reemplaza los datos basicos de un usuario por los valores recibidos.
     * Solo cambia la contrasenia cuando se envia una nueva no vacia.
     * @param id identificador del usuario que se desea modificar
     * @param request nuevos datos del usuario con nombre, correo, rol y contrasenia opcional
     * @return datos del usuario ya actualizado
     * @throws EntityNotFoundException cuando no existe un usuario con ese identificador
     */
    public UserResponse update(Long id, UserRequest request) {
        User usuario = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User no encontrado con id: " + id));

        usuario.setName(request.name());
        usuario.setEmail(request.email());
        usuario.setRole(Role.valueOf(request.role().toUpperCase()));
        usuario.setUpdatedAt(Instant.now());

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return toResponse(userRepository.save(usuario));
    }

    /**
     * Marca un usuario como inactivo para impedir su acceso futuro al sistema.
     * @param id identificador del usuario que se desea dar de baja
     * @throws EntityNotFoundException cuando no existe un usuario con ese identificador
     */
    public void deactivate(Long id) {
        User usuario = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User no encontrado con id: " + id));
        usuario.setActive(false);
        usuario.setUpdatedAt(Instant.now());
        userRepository.save(usuario);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(), u.getName(), u.getEmail(),
                u.getRole().name(), u.getActive(),
                u.getCreatedAt(), u.getUpdatedAt()
        );
    }
}
