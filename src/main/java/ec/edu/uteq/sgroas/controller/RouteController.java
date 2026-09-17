package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.RouteRequest;
import ec.edu.uteq.sgroas.dto.RouteResponse;
import ec.edu.uteq.sgroas.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * Recupera la lista paginada de rutas registradas en el sistema.
     * @param pageable configuración de paginación y ordenamiento solicitada por el cliente.
     * @return respuesta HTTP con la página de rutas encontradas.
     */
    @GetMapping
    public ResponseEntity<Page<RouteResponse>> list(
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(routeService.list(pageable));
    }

    /**
     * Obtiene el detalle de una ruta a partir de su identificador.
     * @param id identificador único de la ruta que se desea consultar.
     * @return respuesta HTTP con los datos de la ruta encontrada.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.findById(id));
    }

    /**
     * Registra una nueva ruta con los datos recibidos en la solicitud.
     * @param request objeto con el origen, destino y demás datos de la ruta.
     * @return respuesta HTTP con estado creado y los datos de la ruta registrada.
     */
    @PostMapping
    public ResponseEntity<RouteResponse> create(
            @Valid @RequestBody RouteRequest request
    ) {
        RouteResponse response = routeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Modifica los datos de una ruta ya existente.
     * @param id identificador único de la ruta que se desea modificar.
     * @param request objeto con los nuevos valores para actualizar la ruta.
     * @return respuesta HTTP con los datos actualizados de la ruta.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RouteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RouteRequest request
    ) {
        return ResponseEntity.ok(routeService.update(id, request));
    }

    /**
     * Desactiva lógicamente una ruta para que deje de estar disponible.
     * @param id identificador único de la ruta que se desea desactivar.
     * @return respuesta HTTP sin contenido que confirma la operación realizada.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        routeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
