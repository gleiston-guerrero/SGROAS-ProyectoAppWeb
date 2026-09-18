package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.VehicleRequest;
import ec.edu.uteq.sgroas.dto.VehicleResponse;
import ec.edu.uteq.sgroas.entity.VehicleStatus;
import ec.edu.uteq.sgroas.entity.Vehicle;
import ec.edu.uteq.sgroas.repository.VehicleRepository;
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
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    // Self-injected via ObjectProvider so that calling listCached() goes
    // through the Spring-managed proxy instead of a plain `this.` call --
    // @Cacheable only intercepts calls that go through the proxy, never a
    // same-class internal call. Without this the cache never activated in
    // production (found during a rigorous audit, same defect as DriverService).
    private final ObjectProvider<VehicleService> self;

    /**
     * Returns a paginated list of active vehicles.
     * @param pageable pagination and sorting configuration.
     * @return page of vehicle response records.
     */
    public Page<VehicleResponse> list(Pageable pageable) {
        CachedVehiclePage cached = self.getObject().listCached(pageable);
        return new PageImpl<>(cached.content(), pageable, cached.totalElements());
    }

    /**
     * Returns the cached list of active vehicles for the given pageable, keyed by page.
     * <p>Returns {@link CachedVehiclePage}, not {@code Page<VehicleResponse>}:
     * {@code PageImpl} has no usable constructor for Jackson, so caching it
     * directly through {@code Jackson2JsonRedisSerializer} throws on the
     * first read-back from Redis. A plain record round-trips correctly.
     * @param pageable pagination and sorting configuration.
     * @return content and total count for the requested page.
     */
    @Cacheable(value = "vehiculos", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public CachedVehiclePage listCached(Pageable pageable) {
        Page<VehicleResponse> page = vehicleRepository.findByActiveTrue(pageable).map(this::mapToResponse);
        return new CachedVehiclePage(page.getContent(), page.getTotalElements());
    }

    public record CachedVehiclePage(List<VehicleResponse> content, long totalElements) {
    }

    /**
     * Retrieves a vehicle by its unique identifier.
     * @param id vehicle unique identifier.
     * @return the matching vehicle response.
     */
    public VehicleResponse findById(Long id) {
        Vehicle vehiculo = getActiveVehicle(id);
        return mapToResponse(vehiculo);
    }

    /**
     * Registers a new vehicle. Caches are evicted after creation.
     * @param request vehicle data to register.
     * @return the created vehicle response.
     * @throws IllegalArgumentException if the plate is already in use.
     */
    @CacheEvict(value = "vehiculos", allEntries = true)
    public VehicleResponse create(VehicleRequest request) {
        if (vehicleRepository.existsByPlate(request.plate())) {
            throw new IllegalArgumentException("Ya existe un vehiculo con esa placa");
        }

        Vehicle vehiculo = Vehicle.builder()
                .plate(request.plate())
                .brand(request.brand())
                .model(request.model())
                .year(request.year())
                .capacity(request.capacity())
                .engineNumber(request.engineNumber())
                .chassisNumber(request.chassisNumber())
                .color(request.color())
                .status(toStatus(request.status()))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Vehicle vehiculoGuardado = vehicleRepository.save(vehiculo);
        return mapToResponse(vehiculoGuardado);
    }

    /**
     * Updates an existing active vehicle. Caches are evicted after update.
     * @param id vehicle unique identifier.
     * @param request new vehicle data.
     * @return the updated vehicle response.
     */
    @CacheEvict(value = "vehiculos", allEntries = true)
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehiculo = getActiveVehicle(id);

        if (!vehiculo.getPlate().equals(request.plate())
                && vehicleRepository.existsByPlate(request.plate())) {
            throw new IllegalArgumentException("Ya existe un vehiculo con esa placa");
        }

        vehiculo.setPlate(request.plate());
        vehiculo.setBrand(request.brand());
        vehiculo.setModel(request.model());
        vehiculo.setYear(request.year());
        vehiculo.setCapacity(request.capacity());
        vehiculo.setEngineNumber(request.engineNumber());
        vehiculo.setChassisNumber(request.chassisNumber());
        vehiculo.setColor(request.color());
        vehiculo.setStatus(toStatus(request.status()));
        vehiculo.setUpdatedAt(Instant.now());

        Vehicle vehiculoActualizado = vehicleRepository.save(vehiculo);
        return mapToResponse(vehiculoActualizado);
    }

    /**
     * Logically deactivates a vehicle. Caches are evicted after deactivation.
     * @param id vehicle unique identifier.
     */
    @CacheEvict(value = "vehiculos", allEntries = true)
    public void deactivate(Long id) {
        Vehicle vehiculo = getActiveVehicle(id);
        vehiculo.setActive(false);
        vehiculo.setStatus(VehicleStatus.FUERA_DE_SERVICIO);
        vehiculo.setUpdatedAt(Instant.now());
        vehicleRepository.save(vehiculo);
    }

    private Vehicle getActiveVehicle(Long id) {
        return vehicleRepository.findById(id)
                .filter(Vehicle::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle no encontrado"));
    }

    private VehicleStatus toStatus(String status) {
        try {
            return VehicleStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de vehiculo no valido");
        }
    }

    private VehicleResponse mapToResponse(Vehicle vehiculo) {
        return new VehicleResponse(
                vehiculo.getId(),
                vehiculo.getPlate(),
                vehiculo.getBrand(),
                vehiculo.getModel(),
                vehiculo.getYear(),
                vehiculo.getCapacity(),
                vehiculo.getEngineNumber(),
                vehiculo.getChassisNumber(),
                vehiculo.getColor(),
                vehiculo.getStatus().name(),
                vehiculo.getActive(),
                vehiculo.getCreatedAt(),
                vehiculo.getUpdatedAt()
        );
    }
}
