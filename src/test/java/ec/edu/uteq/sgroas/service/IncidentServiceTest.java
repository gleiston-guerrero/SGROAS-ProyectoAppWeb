package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.IncidentRequest;
import ec.edu.uteq.sgroas.dto.IncidentResponse;
import ec.edu.uteq.sgroas.entity.*;
import ec.edu.uteq.sgroas.repository.RouteAssignmentRepository;
import ec.edu.uteq.sgroas.repository.IncidentRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private RouteAssignmentRepository routeAssignmentRepository;

    @Mock
    private ObjectProvider<IncidentService> self;

    @InjectMocks
    private IncidentService incidentService;

    private RouteAssignment sampleAssignment() {
        Driver conductor = Driver.builder()
                .id(1L).firstNames("Carlos").lastNames("Mendoza")
                .nationalId("1200000001").licenseNumber("LIC-001")
                .licenseType("E").licenseExpiry(LocalDate.now().plusDays(30))
                .phone("0988888888").email("carlos@sgroas.com")
                .status(DriverStatus.ACTIVO).active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        Vehicle vehiculo = Vehicle.builder()
                .id(1L).plate("GTU-001").brand("Toyota").model("Hiace")
                .year(2020).capacity(14).engineNumber("MOT")
                .chassisNumber("CHAS").color("Blanco")
                .status(VehicleStatus.ACTIVO).active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        Route ruta = Route.builder()
                .id(1L).code("R-001").name("Quito-Guayaquil")
                .origin("Quito").destination("Guayaquil").distanceKm(420.0)
                .durationMin(480).status(RouteStatus.ACTIVA).active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        return RouteAssignment.builder()
                .id(1L).driver(conductor).vehicle(vehiculo).route(ruta)
                .assignmentDate(LocalDate.now()).startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1)).status(AssignmentStatus.ACTIVA)
                .active(true).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private Incident sampleIncident() {
        return Incident.builder()
                .id(1L)
                .assignment(sampleAssignment())
                .reportedBy("Carlos Mendoza")
                .type(IncidentType.AVERIA_MECANICA)
                .description("Falla en el motor")
                .incidentDate(LocalDateTime.now())
                .location("Km 12 Via Quito")
                .severity(IncidentSeverity.MEDIA)
                .status(IncidentStatus.REPORTADO)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private IncidentRequest sampleRequest() {
        return new IncidentRequest(
                1L, "Carlos Mendoza", "AVERIA_MECANICA", "Falla en el motor",
                LocalDateTime.now(), "Km 12 Via Quito", "MEDIA", "REPORTADO"
        );
    }

    @Test
    void listReturnsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(self.getObject()).thenReturn(incidentService);
        when(incidentRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleIncident())));

        Page<IncidentResponse> pagina = incidentService.list(pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("AVERIA_MECANICA", pagina.getContent().get(0).type());
    }

    @Test
    void findByIdReturnsIncident() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(sampleIncident()));

        IncidentResponse response = incidentService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals(1L, response.assignmentId());
    }

    @Test
    void findByIdNonexistentThrowsException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.findById(99L));
    }

    @Test
    void createSavesAndReturns() {
        when(routeAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(sampleAssignment()));
        when(incidentRepository.save(any(Incident.class)))
                .thenReturn(sampleIncident());

        IncidentResponse response = incidentService.create(sampleRequest());

        assertNotNull(response);
        assertEquals("MEDIA", response.severity());
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void createWithNonexistentAssignmentThrowsException() {
        when(routeAssignmentRepository.findById(99L)).thenReturn(Optional.empty());

        IncidentRequest request = new IncidentRequest(
                99L, "Carlos Mendoza", "AVERIA_MECANICA", "Falla",
                LocalDateTime.now(), "Km 12", "MEDIA", "REPORTADO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.create(request));
    }

    @Test
    void createWithInvalidTypeThrowsException() {
        when(routeAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(sampleAssignment()));

        IncidentRequest request = new IncidentRequest(
                1L, "Carlos Mendoza", "TIPO_INVALIDO", "Falla",
                LocalDateTime.now(), "Km 12", "MEDIA", "REPORTADO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.create(request));
    }

    @Test
    void createWithInvalidSeverityThrowsException() {
        when(routeAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(sampleAssignment()));

        IncidentRequest request = new IncidentRequest(
                1L, "Carlos Mendoza", "AVERIA_MECANICA", "Falla",
                LocalDateTime.now(), "Km 12", "INVALIDA", "REPORTADO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.create(request));
    }

    @Test
    void createWithInvalidStatusThrowsException() {
        when(routeAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(sampleAssignment()));

        IncidentRequest request = new IncidentRequest(
                1L, "Carlos Mendoza", "AVERIA_MECANICA", "Falla",
                LocalDateTime.now(), "Km 12", "MEDIA", "INVALIDO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.create(request));
    }

    @Test
    void updateModifiesAndReturns() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(sampleIncident()));
        when(routeAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(sampleAssignment()));
        when(incidentRepository.save(any(Incident.class)))
                .thenReturn(sampleIncident());

        IncidentResponse response = incidentService.update(1L, sampleRequest());

        assertEquals(1L, response.id());
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void deactivateChangesStatus() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(sampleIncident()));

        incidentService.deactivate(1L);

        verify(incidentRepository).save(argThat(i ->
                !i.getActive() && i.getStatus() == IncidentStatus.CERRADO));
    }
}
