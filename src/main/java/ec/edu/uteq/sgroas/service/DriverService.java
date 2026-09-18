package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.DriverRequest;
import ec.edu.uteq.sgroas.dto.DriverResponse;
import ec.edu.uteq.sgroas.entity.Driver;
import ec.edu.uteq.sgroas.entity.DriverStatus;
import ec.edu.uteq.sgroas.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    // Self-injected via ObjectProvider (lazy, avoids a circular-dependency
    // error at construction time) so that calling listActiveCached() goes
    // through the Spring-managed proxy instead of a plain `this.` call --
    // @Cacheable only intercepts calls that go through the proxy, never a
    // same-class internal call, so without this the cache would silently
    // never be hit in production.
    private final ObjectProvider<DriverService> self;

    /**
     * Returns a paginated list of active drivers, optionally filtered by search text.
     * Only the unfiltered case (no search text) is cached: it is the common,
     * repeatable query (e.g. the initial list view / load tests), while a
     * free-text search is effectively unbounded in key space and would just
     * fill the cache with one-off entries.
     * @param search optional text filter applied to driver data.
     * @param pageable pagination and sorting configuration.
     * @return page of driver response records.
     */
    public Page<DriverResponse> list(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            CachedDriverPage cached = self.getObject().listActiveCached(pageable);
            return new org.springframework.data.domain.PageImpl<>(
                    cached.content(), pageable, cached.totalElements());
        }
        return driverRepository.searchActive(search.trim().toLowerCase(), pageable)
                .map(this::mapToResponse);
    }

    /**
     * Cached path for the unfiltered active-driver listing, keyed by page
     * number and size. Evicted on create/update/delete (see the
     * {@code @CacheEvict} methods below), so a cache hit is only ever
     * served between writes.
     * <p>Returns {@link CachedDriverPage}, not {@code Page<DriverResponse>}:
     * {@code PageImpl} has no usable constructor for Jackson, so caching it
     * directly through {@code Jackson2JsonRedisSerializer} throws
     * {@code InvalidDefinitionException} on the very first read-back from
     * Redis (verified in {@code CacheRedisSerializationTest}). A plain
     * record round-trips correctly.
     * @param pageable pagination and sorting configuration.
     * @return content and total count for the requested page.
     */
    @Cacheable(value = "conductores", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public CachedDriverPage listActiveCached(Pageable pageable) {
        Page<DriverResponse> page = driverRepository.findByActiveTrue(pageable).map(this::mapToResponse);
        return new CachedDriverPage(page.getContent(), page.getTotalElements());
    }

    public record CachedDriverPage(List<DriverResponse> content, long totalElements) {
    }

    /**
     * Retrieves a driver by its unique identifier.
     * @param id driver unique identifier.
     * @return the matching driver response.
     * @throws IllegalArgumentException if no active driver exists with that id.
     */
    public DriverResponse findById(Long id) {
        Driver conductor = getActiveDriver(id);
        return mapToResponse(conductor);
    }

    /**
     * Registers a new driver with the received request data. Caches are evicted after creation.
     * @param request driver data to register.
     * @return the created driver response.
     * @throws IllegalArgumentException if the national id or license is already in use.
     */
    @CacheEvict(value = "conductores", allEntries = true)
    public DriverResponse create(DriverRequest request) {
        validateUniqueNationalId(request.nationalId());
        validateUniqueLicense(request.licenseNumber());

        Driver conductor = Driver.builder()
                .firstNames(request.firstNames())
                .lastNames(request.lastNames())
                .nationalId(request.nationalId())
                .licenseNumber(request.licenseNumber())
                .licenseType(request.licenseType())
                .licenseExpiry(request.licenseExpiry())
                .phone(request.phone())
                .email(request.email())
                .status(toStatus(request.status()))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Driver conductorGuardado = driverRepository.save(conductor);
        return mapToResponse(conductorGuardado);
    }

    /**
     * Updates an existing active driver with the received request data. Caches are evicted after update.
     * @param id driver unique identifier.
     * @param request new driver data.
     * @return the updated driver response.
     * @throws IllegalArgumentException if the driver does not exist or identifiers collide.
     */
    @CacheEvict(value = "conductores", allEntries = true)
    public DriverResponse update(Long id, DriverRequest request) {
        Driver conductor = getActiveDriver(id);

        if (!conductor.getNationalId().equals(request.nationalId())
                && driverRepository.existsByNationalId(request.nationalId())) {
            throw new IllegalArgumentException("Ya existe un conductor con esa cedula");
        }

        if (!conductor.getLicenseNumber().equals(request.licenseNumber())
                && driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new IllegalArgumentException("Ya existe un conductor con ese numero de licencia");
        }

        conductor.setFirstNames(request.firstNames());
        conductor.setLastNames(request.lastNames());
        conductor.setNationalId(request.nationalId());
        conductor.setLicenseNumber(request.licenseNumber());
        conductor.setLicenseType(request.licenseType());
        conductor.setLicenseExpiry(request.licenseExpiry());
        conductor.setPhone(request.phone());
        conductor.setEmail(request.email());
        conductor.setStatus(toStatus(request.status()));
        conductor.setUpdatedAt(Instant.now());

        Driver conductorActualizado = driverRepository.save(conductor);
        return mapToResponse(conductorActualizado);
    }

    /**
     * Logically deactivates a driver so it stops being available. Caches are evicted after deactivation.
     * @param id driver unique identifier.
     */
    @CacheEvict(value = "conductores", allEntries = true)
    public void deactivate(Long id) {
        Driver conductor = getActiveDriver(id);
        conductor.setActive(false);
        conductor.setStatus(DriverStatus.INACTIVO);
        conductor.setUpdatedAt(Instant.now());
        driverRepository.save(conductor);
    }

    private Driver getActiveDriver(Long id) {
        return driverRepository.findById(id)
                .filter(Driver::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Driver no encontrado"));
    }

    private void validateUniqueNationalId(String nationalId) {
        if (driverRepository.existsByNationalId(nationalId)) {
            throw new IllegalArgumentException("Ya existe un conductor con esa cedula");
        }
    }

    private void validateUniqueLicense(String licenseNumber) {
        if (driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new IllegalArgumentException("Ya existe un conductor con ese numero de licencia");
        }
    }

    private DriverStatus toStatus(String status) {
        try {
            return DriverStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de conductor no valido");
        }
    }

    private Boolean licenseExpiring(LocalDate licenseExpiry) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(30);

        return !licenseExpiry.isBefore(hoy) && !licenseExpiry.isAfter(limite);
    }

    private DriverResponse mapToResponse(Driver conductor) {
        return new DriverResponse(
                conductor.getId(),
                conductor.getFirstNames(),
                conductor.getLastNames(),
                conductor.getNationalId(),
                conductor.getLicenseNumber(),
                conductor.getLicenseType(),
                conductor.getLicenseExpiry(),
                conductor.getPhone(),
                conductor.getEmail(),
                conductor.getStatus().name(),
                conductor.getActive(),
                licenseExpiring(conductor.getLicenseExpiry()),
                conductor.getCreatedAt(),
                conductor.getUpdatedAt()
        );
    }
}
