package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.UserRequest;
import ec.edu.uteq.sgroas.dto.UserResponse;
import ec.edu.uteq.sgroas.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Recupera la lista paginada de usuarios con filtro opcional por texto.
     * @param search criterio opcional para filtrar por nombre, correo u otros datos.
     * @param pageable configuración de paginación y ordenamiento solicitada por el cliente.
     * @return respuesta HTTP con la página de usuarios encontrados.
     */
    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.list(search, pageable));
    }

    /**
     * Obtiene el detalle de un usuario a partir de su identificador.
     * @param id identificador único del usuario que se desea consultar.
     * @return respuesta HTTP con los datos del usuario encontrado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * Registra un nuevo usuario y le envía el código de activación por correo.
     * @param request objeto con los datos personales, credenciales y rol del usuario.
     * @return respuesta HTTP con estado creado y los datos del usuario registrado.
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Reenvía el código de activación al correo de un usuario aún no verificado.
     * @param id identificador único del usuario al que se reenvía el código.
     * @return respuesta HTTP sin contenido que confirma el envío del código.
     */
    @PostMapping("/{id}/reenviar-activacion")
    public ResponseEntity<Void> resendActivation(@PathVariable Long id) {
        userService.resendActivationCode(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Modifica los datos de un usuario ya existente.
     * @param id identificador único del usuario que se desea modificar.
     * @param request objeto con los nuevos valores para actualizar el usuario.
     * @return respuesta HTTP con los datos actualizados del usuario.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request
    ) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    /**
     * Desactiva lógicamente un usuario para impedir su acceso al sistema.
     * @param id identificador único del usuario que se desea desactivar.
     * @return respuesta HTTP sin contenido que confirma la operación realizada.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
