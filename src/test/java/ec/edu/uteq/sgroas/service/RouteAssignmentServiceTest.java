package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.RouteAssignmentRequest;
import ec.edu.uteq.sgroas.dto.RouteAssignmentResponse;
import ec.edu.uteq.sgroas.entity.*;
import ec.edu.uteq.sgroas.repository.RouteAssignmentRepository;
import ec.edu.uteq.sgroas.repository.DriverRepository;
import ec.edu.uteq.sgroas.repository.RouteRepository;
import ec.edu.uteq.sgroas.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteAssignmentServiceTest {

    @Mock
    private RouteAssignmentRepository routeAssignmentRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ObjectProvider<RouteAssignmentService> self;

    @InjectMocks
    private RouteAssignmentService routeAssignmentService;

    private Driver sampleDriver() {
        return Driver.builder()
                .id(1L).firstNames("Carlos").lastNames("Mendoza")
                .nationalId("1200000001").licenseNumber("LIC-001")
                .licenseType("E").licenseExpiry(LocalDate.now().plusDays(30))
                .phone("0988888888").email("carlos@sgroas.com")
                .status(DriverStatus.ACTIVO).active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private Vehicle sampleVehicle() {
        return Vehicle.builder()
                .id(1L).plate("GTU-001").brand("Toyota").model("Hiace")
                .year(2020).capacity(14).engineNumber("MOT")
                .chassisNumber("CHAS").color("Blanco")
                .status(VehicleStatus.ACTIVO).active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private Route sampleRoute() {
        return Route.builder()
                .id(1L).code("R-001").name("Quito-Guayaquil")
                .origin("Quito").destination("Guayaquil").distanceKm(420.0)
                .durationMin(480).status(RouteStatus.ACTIVA).active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private RouteAssignment sampleAssignment() {
        return RouteAssignment.builder()
                .id(1L).driver(sampleDriver()).vehicle(sampleVehicle())
                .route(sampleRoute()).assignmentDate(LocalDate.now())
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1))
                .status(AssignmentStatus.ACTIVA).active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private RouteAssignmentRequest sampleRequest() {
        return new RouteAssignmentRequest(
                1L, 1L, 1L, LocalDate.now(), LocalDate.now(),
                LocalDate.now().plusDays(1), "ACTIVA"
        );
    }

    @Test
    void listReturnsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(self.getObject()).thenReturn(routeAssignmentService);
        when(routeAssignmentRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleAssignment())));

        Page<RouteAssignmentResponse> pagina = routeAssignmentService.list(pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Carlos Mendoza", pagina.getContent().get(0).driverName());
        assertEquals("GTU-001", pagina.getContent().get(0).vehiclePlate());
        assertEquals("Quito-Guayaquil", pagina.getContent().get(0).routeName());
    }

    @Test
    void findByIdReturnsAssignment() {
        when(routeAssignmentRepository.findWithDetails(1L))
                .thenReturn(Optional.of(sampleAssignment()));

        RouteAssignmentResponse response = routeAssignmentService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("ACTIVA", response.status());
    }

    @Test
    void findByIdNonexistentThrowsException() {
        when(routeAssignmentRepository.findWithDetails(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> routeAssignmentService.findById(99L));
    }

    @Test
    void createSavesAndReturns() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(sampleDriver()));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle()));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(sampleRoute()));
        when(routeAssignmentRepository.save(any(RouteAssignment.class)))
                .thenReturn(sampleAssignment());

        RouteAssignmentResponse response = routeAssignmentService.create(sampleRequest());

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(routeAssignmentRepository).save(any(RouteAssignment.class));
    }

    @Test
    void createWithoutDriverThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> routeAssignmentService.create(sampleRequest()));
    }

    @Test
    void createWithoutVehicleThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(sampleDriver()));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> routeAssignmentService.create(sampleRequest()));
    }

    @Test
    void createWithoutRouteThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(sampleDriver()));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle()));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> routeAssignmentService.create(sampleRequest()));
    }

    @Test
    void createWithInvalidStatusThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(sampleDriver()));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle()));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(sampleRoute()));

        RouteAssignmentRequest request = new RouteAssignmentRequest(
                1L, 1L, 1L, LocalDate.now(), LocalDate.now(),
                LocalDate.now().plusDays(1), "INVALIDO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> routeAssignmentService.create(request));
    }

    @Test
    void updateModifiesAndReturns() {
        when(routeAssignmentRepository.findWithDetails(1L))
                .thenReturn(Optional.of(sampleAssignment()));
        when(driverRepository.findById(1L)).thenReturn(Optional.of(sampleDriver()));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle()));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(sampleRoute()));
        when(routeAssignmentRepository.save(any(RouteAssignment.class)))
                .thenReturn(sampleAssignment());

        RouteAssignmentResponse response = routeAssignmentService.update(1L, sampleRequest());

        assertEquals(1L, response.id());
        verify(routeAssignmentRepository).save(any(RouteAssignment.class));
    }

    @Test
    void deactivateChangesStatus() {
        when(routeAssignmentRepository.findWithDetails(1L))
                .thenReturn(Optional.of(sampleAssignment()));

        routeAssignmentService.deactivate(1L);

        verify(routeAssignmentRepository).save(argThat(a ->
                !a.getActive() && a.getStatus() == AssignmentStatus.CANCELADA));
    }
}
