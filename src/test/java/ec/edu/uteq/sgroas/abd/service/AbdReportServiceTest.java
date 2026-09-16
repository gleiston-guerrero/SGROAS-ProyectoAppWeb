package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.AbdIncident;
import ec.edu.uteq.sgroas.abd.entity.Alert;
import ec.edu.uteq.sgroas.abd.entity.Schedule;
import ec.edu.uteq.sgroas.abd.entity.Unit;
import ec.edu.uteq.sgroas.abd.repository.AbdIncidentRepository;
import ec.edu.uteq.sgroas.abd.repository.AbdRouteRepository;
import ec.edu.uteq.sgroas.abd.repository.AlertRepository;
import ec.edu.uteq.sgroas.abd.repository.CountProjection;
import ec.edu.uteq.sgroas.abd.repository.ScheduleRepository;
import ec.edu.uteq.sgroas.abd.repository.TopRouteProjection;
import ec.edu.uteq.sgroas.abd.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbdReportServiceTest {

    @Mock
    private ScheduleRepository programacionRepository;

    @Mock
    private AbdIncidentRepository incidenteRepository;

    @Mock
    private AlertRepository alertaRepository;

    @Mock
    private UnitRepository unidadRepository;

    @Mock
    private AbdRouteRepository rutaAbdRepository;

    @InjectMocks
    private AbdReportService abdReporteService;

    @Test
    void summaryConsolidaLosTotalesYLasActivas() {
        when(programacionRepository.count()).thenReturn(50L);
        when(programacionRepository.findByEstadoIgnoreCase(eq("Programado"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new Schedule()), PageRequest.of(0, 1), 3));
        when(incidenteRepository.count()).thenReturn(20L);
        when(incidenteRepository.findByNivelSugeridoIgnoreCase(eq("ALTO"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new AbdIncident()), PageRequest.of(0, 1), 2));
        when(alertaRepository.count()).thenReturn(5L);
        when(unidadRepository.count()).thenReturn(12L);
        when(unidadRepository.findByEstadoIgnoreCase(eq("En Mantenimiento"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new Unit()), PageRequest.of(0, 1), 1));
        when(rutaAbdRepository.count()).thenReturn(8L);

        AbdDtos.SummaryResponse resumen = abdReporteService.summary();

        assertEquals(50L, resumen.totalProgramaciones());
        assertEquals(3L, resumen.programacionesActivas());
        assertEquals(20L, resumen.totalIncidentes());
        assertEquals(2L, resumen.incidentesAltoNivel());
        assertEquals(5L, resumen.totalAlertas());
        assertEquals(12L, resumen.totalUnidades());
        assertEquals(1L, resumen.unidadesEnMantenimiento());
        assertEquals(8L, resumen.totalRutas());
    }

    @Test
    void incidentsByLevelMapeaNivelYTotal() {
        AbdIncidentRepository.CountByLevel nivel = mock(AbdIncidentRepository.CountByLevel.class);
        when(nivel.getNivel()).thenReturn("ALTO");
        when(nivel.getTotal()).thenReturn(3);
        when(incidenteRepository.countByLevel()).thenReturn(List.of(nivel));

        List<AbdDtos.CountResponse> resultado = abdReporteService.incidentsByLevel();

        assertEquals(1, resultado.size());
        assertEquals("ALTO", resultado.get(0).clave());
        assertEquals(3, resultado.get(0).total());
    }

    @Test
    void incidentsByStatusMapeaEstadoYTotal() {
        AbdIncidentRepository.CountByStatus estado = mock(AbdIncidentRepository.CountByStatus.class);
        when(estado.getEstado()).thenReturn("ATENDIDO");
        when(estado.getTotal()).thenReturn(7);
        when(incidenteRepository.countByStatus()).thenReturn(List.of(estado));

        List<AbdDtos.CountResponse> resultado = abdReporteService.incidentsByStatus();

        assertEquals(1, resultado.size());
        assertEquals("ATENDIDO", resultado.get(0).clave());
        assertEquals(7, resultado.get(0).total());
    }

    @Test
    void unitsByStatusMapeaClaveYTotal() {
        CountProjection proyeccion = mock(CountProjection.class);
        when(proyeccion.getClave()).thenReturn("OPERATIVA");
        when(proyeccion.getTotal()).thenReturn(4L);
        when(unidadRepository.countByStatus()).thenReturn(List.of(proyeccion));

        List<AbdDtos.CountResponse> resultado = abdReporteService.unitsByStatus();

        assertEquals(1, resultado.size());
        assertEquals("OPERATIVA", resultado.get(0).clave());
        assertEquals(4L, resultado.get(0).total());
    }

    @Test
    void schedulesByStatusMapeaClaveYTotal() {
        CountProjection proyeccion = mock(CountProjection.class);
        when(proyeccion.getClave()).thenReturn("PROGRAMADO");
        when(proyeccion.getTotal()).thenReturn(9L);
        when(programacionRepository.countByStatus()).thenReturn(List.of(proyeccion));

        List<AbdDtos.CountResponse> resultado = abdReporteService.schedulesByStatus();

        assertEquals(1, resultado.size());
        assertEquals("PROGRAMADO", resultado.get(0).clave());
        assertEquals(9L, resultado.get(0).total());
    }

    @Test
    void schedulesByMonthMapeaMesYTotal() {
        CountProjection proyeccion = mock(CountProjection.class);
        when(proyeccion.getClave()).thenReturn("2026-08");
        when(proyeccion.getTotal()).thenReturn(6L);
        when(programacionRepository.countByMonth()).thenReturn(List.of(proyeccion));

        List<AbdDtos.CountResponse> resultado = abdReporteService.schedulesByMonth();

        assertEquals(1, resultado.size());
        assertEquals("2026-08", resultado.get(0).clave());
        assertEquals(6L, resultado.get(0).total());
    }

    @Test
    void topRoutesMapeaIdDescripcionYTotal() {
        TopRouteProjection ruta = mock(TopRouteProjection.class);
        when(ruta.getId()).thenReturn(1);
        when(ruta.getDescripcion()).thenReturn("Quito - Guayaquil");
        when(ruta.getTotal()).thenReturn(11L);
        when(rutaAbdRepository.topRoutes()).thenReturn(List.of(ruta));

        List<AbdDtos.TopRouteResponse> resultado = abdReporteService.topRoutes();

        assertEquals(1, resultado.size());
        assertEquals(1, resultado.get(0).idRuta());
        assertEquals("Quito - Guayaquil", resultado.get(0).descripcion());
        assertEquals(11L, resultado.get(0).totalProgramaciones());
    }

    @Test
    void summaryDevuelveUnResumenNoNuloConColeccionesVacias() {
        when(programacionRepository.count()).thenReturn(0L);
        when(programacionRepository.findByEstadoIgnoreCase(eq("Programado"), any(PageRequest.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(incidenteRepository.count()).thenReturn(0L);
        when(incidenteRepository.findByNivelSugeridoIgnoreCase(eq("ALTO"), any(PageRequest.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(alertaRepository.count()).thenReturn(0L);
        when(unidadRepository.count()).thenReturn(0L);
        when(unidadRepository.findByEstadoIgnoreCase(eq("En Mantenimiento"), any(PageRequest.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(rutaAbdRepository.count()).thenReturn(0L);

        AbdDtos.SummaryResponse resumen = abdReporteService.summary();

        assertNotNull(resumen);
        assertEquals(0L, resumen.totalProgramaciones());
        assertFalse(resumen.totalRutas() > 0);
    }
}