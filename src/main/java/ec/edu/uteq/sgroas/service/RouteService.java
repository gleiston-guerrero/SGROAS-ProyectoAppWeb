package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.RouteRequest;
import ec.edu.uteq.sgroas.dto.RouteResponse;
import ec.edu.uteq.sgroas.entity.RouteStatus;
import ec.edu.uteq.sgroas.entity.Route;
import ec.edu.uteq.sgroas.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;

    /**
     * Returns a paginated list of active routes.
     * @param pageable pagination and sorting configuration.
     * @return page of route response records.
     */
    public Page<RouteResponse> list(Pageable pageable) {
        List<RouteResponse> contenido = listCached(pageable);
        return new PageImpl<>(contenido, pageable, contenido.size());
    }

    /**
     * Returns the cached list of active routes for the given pageable, keyed by page.
     * @param pageable pagination and sorting configuration.
     * @return list of route response records.
     */
    @Cacheable(value = "rutas", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public List<RouteResponse> listCached(Pageable pageable) {
        return routeRepository.findByActiveTrue(pageable)
                .map(this::mapToResponse)
                .getContent();
    }

    /**
     * Retrieves a route by its unique identifier.
     * @param id route unique identifier.
     * @return the matching route response.
     */
    public RouteResponse findById(Long id) {
        Route ruta = getActiveRoute(id);
        return mapToResponse(ruta);
    }

    /**
     * Registers a new route. Caches are evicted after creation.
     * @param request route data to register.
     * @return the created route response.
     * @throws IllegalArgumentException if the route code is already in use.
     */
    @CacheEvict(value = "rutas", allEntries = true)
    public RouteResponse create(RouteRequest request) {
        if (routeRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Ya existe una ruta con ese codigo");
        }

        Route ruta = Route.builder()
                .code(request.code())
                .name(request.name())
                .origin(request.origin())
                .destination(request.destination())
                .distanceKm(request.distanceKm())
                .durationMin(request.durationMin())
                .status(toStatus(request.status()))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Route rutaGuardada = routeRepository.save(ruta);
        return mapToResponse(rutaGuardada);
    }

    /**
     * Updates an existing active route. Caches are evicted after update.
     * @param id route unique identifier.
     * @param request new route data.
     * @return the updated route response.
     */
    @CacheEvict(value = "rutas", allEntries = true)
    public RouteResponse update(Long id, RouteRequest request) {
        Route ruta = getActiveRoute(id);

        if (!ruta.getCode().equals(request.code())
                && routeRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Ya existe una ruta con ese codigo");
        }

        ruta.setCode(request.code());
        ruta.setName(request.name());
        ruta.setOrigin(request.origin());
        ruta.setDestination(request.destination());
        ruta.setDistanceKm(request.distanceKm());
        ruta.setDurationMin(request.durationMin());
        ruta.setStatus(toStatus(request.status()));
        ruta.setUpdatedAt(Instant.now());

        Route rutaActualizada = routeRepository.save(ruta);
        return mapToResponse(rutaActualizada);
    }

    /**
     * Logically deactivates a route. Caches are evicted after deactivation.
     * @param id route unique identifier.
     */
    @CacheEvict(value = "rutas", allEntries = true)
    public void deactivate(Long id) {
        Route ruta = getActiveRoute(id);
        ruta.setActive(false);
        ruta.setStatus(RouteStatus.INACTIVA);
        ruta.setUpdatedAt(Instant.now());
        routeRepository.save(ruta);
    }

    private Route getActiveRoute(Long id) {
        return routeRepository.findById(id)
                .filter(Route::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Route no encontrada"));
    }

    private RouteStatus toStatus(String status) {
        try {
            return RouteStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de ruta no valido");
        }
    }

    private RouteResponse mapToResponse(Route ruta) {
        return new RouteResponse(
                ruta.getId(),
                ruta.getCode(),
                ruta.getName(),
                ruta.getOrigin(),
                ruta.getDestination(),
                ruta.getDistanceKm(),
                ruta.getDurationMin(),
                ruta.getStatus().name(),
                ruta.getActive(),
                ruta.getCreatedAt(),
                ruta.getUpdatedAt()
        );
    }
}
