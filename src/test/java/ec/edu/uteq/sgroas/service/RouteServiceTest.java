package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.RouteRequest;
import ec.edu.uteq.sgroas.dto.RouteResponse;
import ec.edu.uteq.sgroas.entity.RouteStatus;
import ec.edu.uteq.sgroas.entity.Route;
import ec.edu.uteq.sgroas.repository.RouteRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ObjectProvider<RouteService> self;

    @InjectMocks
    private RouteService routeService;

    private Route sampleRoute() {
        return Route.builder()
                .id(1L)
                .code("R-001")
                .name("Quito - Guayaquil")
                .origin("Quito")
                .destination("Guayaquil")
                .distanceKm(420.0)
                .durationMin(480)
                .status(RouteStatus.ACTIVA)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private RouteRequest sampleRequest() {
        return new RouteRequest(
                "R-001", "Quito - Guayaquil", "Quito", "Guayaquil",
                420.0, 480, "ACTIVA"
        );
    }

    @Test
    void listReturnsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(self.getObject()).thenReturn(routeService);
        when(routeRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleRoute())));

        Page<RouteResponse> pagina = routeService.list(pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("R-001", pagina.getContent().get(0).code());
    }

    @Test
    void findByIdReturnsRoute() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(sampleRoute()));

        RouteResponse response = routeService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("Quito", response.origin());
    }

    @Test
    void findByIdNonexistentThrowsException() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> routeService.findById(99L));
    }

    @Test
    void createSavesAndReturns() {
        when(routeRepository.existsByCode("R-001")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenReturn(sampleRoute());

        RouteResponse response = routeService.create(sampleRequest());

        assertEquals("R-001", response.code());
        verify(routeRepository).save(any(Route.class));
    }

    @Test
    void createWithDuplicateCodeThrowsException() {
        when(routeRepository.existsByCode("R-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> routeService.create(sampleRequest()));
    }

    @Test
    void createWithInvalidStatusThrowsException() {
        when(routeRepository.existsByCode("R-001")).thenReturn(false);
        RouteRequest request = new RouteRequest(
                "R-001", "Quito - Guayaquil", "Quito", "Guayaquil",
                420.0, 480, "INVALIDO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> routeService.create(request));
    }

    @Test
    void updateModifiesAndReturns() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(sampleRoute()));
        when(routeRepository.save(any(Route.class))).thenReturn(sampleRoute());

        RouteResponse response = routeService.update(1L, sampleRequest());

        assertEquals(1L, response.id());
    }

    @Test
    void updateWithDuplicateCodeThrowsException() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(sampleRoute()));
        when(routeRepository.existsByCode("R-999")).thenReturn(true);

        RouteRequest request = new RouteRequest(
                "R-999", "Quito - Guayaquil", "Quito", "Guayaquil",
                420.0, 480, "ACTIVA"
        );

        assertThrows(IllegalArgumentException.class,
                () -> routeService.update(1L, request));
    }

    @Test
    void deactivateChangesStatus() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(sampleRoute()));

        routeService.deactivate(1L);

        verify(routeRepository).save(argThat(r ->
                !r.getActive() && r.getStatus() == RouteStatus.INACTIVA));
    }
}
