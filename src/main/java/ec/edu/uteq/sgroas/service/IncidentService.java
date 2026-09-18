package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.IncidentRequest;
import ec.edu.uteq.sgroas.dto.IncidentResponse;
import ec.edu.uteq.sgroas.entity.*;
import ec.edu.uteq.sgroas.repository.RouteAssignmentRepository;
import ec.edu.uteq.sgroas.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;

    // Self-injected via ObjectProvider so that calling listCached() goes
    // through the Spring-managed proxy instead of a plain `this.` call --
    // @Cacheable only intercepts calls that go through the proxy, never a
    // same-class internal call. Without this the cache never activated in
    // production (found during a rigorous audit, same defect as DriverService).
    private final ObjectProvider<IncidentService> self;

    /**
     * Returns a paginated list of active incidents.
     * @param pageable pagination and sorting configuration.
     * @return page of incident response records.
     */
    public Page<IncidentResponse> list(Pageable pageable) {
        CachedIncidentPage cached = self.getObject().listCached(pageable);
        return new PageImpl<>(cached.content(), pageable, cached.totalElements());
    }

    /**
     * Returns the cached list of active incidents for the given pageable, keyed by page.
     * <p>Returns {@link CachedIncidentPage}, not {@code Page<IncidentResponse>}:
     * {@code PageImpl} has no usable constructor for Jackson, so caching it
     * directly through {@code Jackson2JsonRedisSerializer} throws on the
     * first read-back from Redis. A plain record round-trips correctly.
     * @param pageable pagination and sorting configuration.
     * @return content and total count for the requested page.
     */
    @Cacheable(value = "incidentes", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public CachedIncidentPage listCached(Pageable pageable) {
        Page<IncidentResponse> page = incidentRepository.findByActiveTrue(pageable).map(this::mapToResponse);
        return new CachedIncidentPage(page.getContent(), page.getTotalElements());
    }

    public record CachedIncidentPage(List<IncidentResponse> content, long totalElements) {
    }

    /**
     * Retrieves an incident by its unique identifier.
     * @param id incident unique identifier.
     * @return the matching incident response.
     */
    public IncidentResponse findById(Long id) {
        Incident incidente = getActiveIncident(id);
        return mapToResponse(incidente);
    }

    /**
     * Registers a new incident associated with an active assignment. Caches are evicted after creation.
     * @param request incident data to register.
     * @return the created incident response.
     * @throws IllegalArgumentException if the assignment is not found or is inactive.
     */
    @CacheEvict(value = "incidentes", allEntries = true)
    public IncidentResponse create(IncidentRequest request) {
        RouteAssignment asignacion = routeAssignmentRepository.findById(request.assignmentId())
                .filter(RouteAssignment::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Asignacion no encontrada"));

        Incident incidente = Incident.builder()
                .assignment(asignacion)
                .reportedBy(request.reportedBy())
                .type(toType(request.type()))
                .description(request.description())
                .incidentDate(request.incidentDate())
                .location(request.location())
                .severity(toSeverity(request.severity()))
                .status(toStatus(request.status()))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Incident incidenteGuardado = incidentRepository.save(incidente);
        return mapToResponse(incidenteGuardado);
    }

    /**
     * Updates an existing active incident. Caches are evicted after update.
     * @param id incident unique identifier.
     * @param request new incident data.
     * @return the updated incident response.
     */
    @CacheEvict(value = "incidentes", allEntries = true)
    public IncidentResponse update(Long id, IncidentRequest request) {
        Incident incidente = getActiveIncident(id);

        RouteAssignment asignacion = routeAssignmentRepository.findById(request.assignmentId())
                .filter(RouteAssignment::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Asignacion no encontrada"));

        incidente.setAssignment(asignacion);
        incidente.setReportedBy(request.reportedBy());
        incidente.setType(toType(request.type()));
        incidente.setDescription(request.description());
        incidente.setIncidentDate(request.incidentDate());
        incidente.setLocation(request.location());
        incidente.setSeverity(toSeverity(request.severity()));
        incidente.setStatus(toStatus(request.status()));
        incidente.setUpdatedAt(Instant.now());

        Incident incidenteActualizado = incidentRepository.save(incidente);
        return mapToResponse(incidenteActualizado);
    }

    /**
     * Logically deactivates an incident and closes it. Caches are evicted after deactivation.
     * @param id incident unique identifier.
     */
    @CacheEvict(value = "incidentes", allEntries = true)
    public void deactivate(Long id) {
        Incident incidente = getActiveIncident(id);
        incidente.setActive(false);
        incidente.setStatus(IncidentStatus.CERRADO);
        incidente.setUpdatedAt(Instant.now());
        incidentRepository.save(incidente);
    }

    private Incident getActiveIncident(Long id) {
        return incidentRepository.findById(id)
                .filter(Incident::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Incident no encontrado"));
    }

    private IncidentType toType(String type) {
        try {
            return IncidentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de incidente no valido");
        }
    }

    private IncidentSeverity toSeverity(String severity) {
        try {
            return IncidentSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Gravedad de incidente no valida");
        }
    }

    private IncidentStatus toStatus(String status) {
        try {
            return IncidentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de incidente no valido");
        }
    }

    private IncidentResponse mapToResponse(Incident incidente) {
        return new IncidentResponse(
                incidente.getId(),
                incidente.getAssignment().getId(),
                incidente.getReportedBy(),
                incidente.getType().name(),
                incidente.getDescription(),
                incidente.getIncidentDate(),
                incidente.getLocation(),
                incidente.getSeverity().name(),
                incidente.getStatus().name(),
                incidente.getActive(),
                incidente.getCreatedAt(),
                incidente.getUpdatedAt()
        );
    }
}
