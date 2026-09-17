package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.RouteAssignmentRequest;
import ec.edu.uteq.sgroas.dto.RouteAssignmentResponse;
import ec.edu.uteq.sgroas.service.RouteAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class RouteAssignmentController {

    private final RouteAssignmentService routeAssignmentService;

    /**
     * Recupera la lista paginada de asignaciones de rutas registradas en el sistema.
     * @param pageable configuración de paginación y ordenamiento solicitada por el cliente.
     * @return respuesta HTTP con la página de asignaciones encontradas.
     */
    @GetMapping
    public ResponseEntity<Page<RouteAssignmentResponse>> list(
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(routeAssignmentService.list(pageable));
    }

    /**
     * Obtiene el detalle de una asignación de ruta a partir de su identificador.
     * @param id identificador único de la asignación que se desea consultar.
     * @return respuesta HTTP con los datos de la asignación encontrada.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RouteAssignmentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(routeAssignmentService.findById(id));
    }

    /**
     * Registra una nueva asignación de ruta con los datos recibidos en la solicitud.
     * @param request objeto con los datos de ruta, vehículo y conductor para la asignación.
     * @return respuesta HTTP con estado creado y los datos de la asignación registrada.
     */
    @PostMapping
    public ResponseEntity<RouteAssignmentResponse> create(
            @Valid @RequestBody RouteAssignmentRequest request
    ) {
        RouteAssignmentResponse response = routeAssignmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Modifica los datos de una asignación de ruta ya existente.
     * @param id identificador único de la asignación que se desea modificar.
     * @param request objeto con los nuevos valores para actualizar la asignación.
     * @return respuesta HTTP con los datos actualizados de la asignación.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RouteAssignmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RouteAssignmentRequest request
    ) {
        return ResponseEntity.ok(routeAssignmentService.update(id, request));
    }

    /**
     * Desactiva lógicamente una asignación para que deje de estar vigente.
     * @param id identificador único de la asignación que se desea desactivar.
     * @return respuesta HTTP sin contenido que confirma la operación realizada.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        routeAssignmentService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
