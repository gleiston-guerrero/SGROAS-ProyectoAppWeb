package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.repository.DriverRepository;
import ec.edu.uteq.sgroas.repository.IncidentRepository;
import ec.edu.uteq.sgroas.repository.RouteAssignmentRepository;
import ec.edu.uteq.sgroas.repository.RouteRepository;
import ec.edu.uteq.sgroas.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteAssignmentRepository routeAssignmentRepository;

    @InjectMocks
    private ReportService reportService;

    private Object[] filaGeneral() {
        return new Object[] {10L, 8L, 6L, 5L, 4L, 3L, 9L, 7L, 2L, 1L};
    }

    @Test
    void generalStatisticsMapsAllTenTotals() {
        when(incidentRepository.generalStatistics()).thenReturn(List.<Object[]>of(filaGeneral()));

        List<Map<String, Object>> resultado = reportService.generalStatistics();

        assertEquals(1, resultado.size());
        Map<String, Object> fila = resultado.get(0);
        assertEquals(10L, fila.get("total_conductores"));
        assertEquals(8L, fila.get("conductores_activos"));
        assertEquals(6L, fila.get("total_vehiculos"));
        assertEquals(5L, fila.get("vehiculos_activos"));
        assertEquals(4L, fila.get("total_rutas"));
        assertEquals(3L, fila.get("rutas_activas"));
        assertEquals(9L, fila.get("total_asignaciones"));
        assertEquals(7L, fila.get("asignaciones_activas"));
        assertEquals(2L, fila.get("total_incidentes"));
        assertEquals(1L, fila.get("incidentes_abiertos"));
    }

    @Test
    void incidentsBySeverityMapsSeverityTotalAndDate() {
        when(incidentRepository.incidentsBySeverity("ROBO"))
                .thenReturn(List.<Object[]>of(new Object[] {"ALTA", 2L, "2026-08-01"}));

        List<Map<String, Object>> resultado = reportService.incidentsBySeverity("ROBO");

        assertEquals("ALTA", resultado.get(0).get("gravedad"));
        assertEquals(2L, resultado.get(0).get("total_incidentes"));
        assertEquals("2026-08-01", resultado.get(0).get("ultimo_incidente"));
    }

    @Test
    void incidentsByRangeMapsFullDetail() {
        when(incidentRepository.getIncidentsByRange(Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")))
                .thenReturn(List.<Object[]>of(new Object[] {
                        1L, "ACCIDENTE", "ALTA", "REPORTADO", "Colision leve",
                        "2026-08-01", "Km 12", "Carlos Mendoza", "GTU-001", "R-01"}));

        List<Map<String, Object>> resultado = reportService.incidentsByRange(
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T23:59:59Z"));

        Map<String, Object> fila = resultado.get(0);
        assertEquals(1L, fila.get("incidente_id"));
        assertEquals("ACCIDENTE", fila.get("tipo"));
        assertEquals("ALTA", fila.get("gravedad"));
        assertEquals("REPORTADO", fila.get("estado"));
        assertEquals("Colision leve", fila.get("descripcion"));
        assertEquals("2026-08-01", fila.get("fecha_incidente"));
        assertEquals("Km 12", fila.get("ubicacion"));
        assertEquals("Carlos Mendoza", fila.get("conductor_nombre"));
        assertEquals("GTU-001", fila.get("vehiculo_placa"));
        assertEquals("R-01", fila.get("ruta_codigo"));
    }

    @Test
    void licensesExpiringMapsLicenseData() {
        when(driverRepository.licensesExpiring(30))
                .thenReturn(List.<Object[]>of(new Object[] {
                        1L, "Carlos Mendoza", "1200000001", "LIC-001", "E",
                        "2026-09-30", true}));

        List<Map<String, Object>> resultado = reportService.licensesExpiring(30);

        Map<String, Object> fila = resultado.get(0);
        assertEquals(1L, fila.get("conductor_id"));
        assertEquals("Carlos Mendoza", fila.get("nombre_completo"));
        assertEquals("1200000001", fila.get("cedula"));
        assertEquals("LIC-001", fila.get("numero_licencia"));
        assertEquals("E", fila.get("tipo_licencia"));
        assertEquals("2026-09-30", fila.get("fecha_vencimiento"));
        assertEquals(true, fila.get("asignacion_activa"));
    }

    @Test
    void vehiclesInMaintenanceMapsAssociatedCounts() {
        when(vehicleRepository.vehiclesInMaintenance())
                .thenReturn(List.<Object[]>of(new Object[] {1L, "GTU-001", "Toyota", "Hiace", 2020, 3L, 1L}));

        List<Map<String, Object>> resultado = reportService.vehiclesInMaintenance();

        Map<String, Object> fila = resultado.get(0);
        assertEquals(1L, fila.get("vehiculo_id"));
        assertEquals("GTU-001", fila.get("placa"));
        assertEquals("Toyota", fila.get("marca"));
        assertEquals("Hiace", fila.get("modelo"));
        assertEquals(2020, fila.get("anio"));
        assertEquals(3L, fila.get("total_asignaciones"));
        assertEquals(1L, fila.get("total_incidentes"));
    }

    @Test
    void routePerformanceMapsAllTenReportFields() {
        when(routeRepository.routePerformanceReport())
                .thenReturn(List.<Object[]>of(new Object[] {
                        1L, "R-01", "Quito-Guayaquil", 5L, 2L, 1L, 1L, 0L, 0L, 420.0}));

        List<Map<String, Object>> resultado = reportService.routePerformanceReport();

        Map<String, Object> fila = resultado.get(0);
        assertEquals(1L, fila.get("ruta_id"));
        assertEquals("R-01", fila.get("ruta_codigo"));
        assertEquals("Quito-Guayaquil", fila.get("ruta_nombre"));
        assertEquals(5L, fila.get("total_asignaciones"));
        assertEquals(2L, fila.get("total_incidentes"));
        assertEquals(1L, fila.get("incidentes_criticos"));
        assertEquals(1L, fila.get("incidentes_altos"));
        assertEquals(0L, fila.get("incidentes_medios"));
        assertEquals(0L, fila.get("incidentes_bajos"));
        assertEquals(420.0, fila.get("promedio_distancia_km"));
    }

    @Test
    void activeAssignmentsByDriverMapsActiveAssignment() {
        when(routeAssignmentRepository.activeAssignmentsByDriver(1L))
                .thenReturn(List.<Object[]>of(new Object[] {
                        1L, "GTU-001", "Toyota", "R-01", "Quito-Guayaquil",
                        "2026-08-01", "2026-08-15"}));

        List<Map<String, Object>> resultado = reportService.activeAssignmentsByDriver(1L);

        Map<String, Object> fila = resultado.get(0);
        assertEquals(1L, fila.get("asignacion_id"));
        assertEquals("GTU-001", fila.get("vehiculo_placa"));
        assertEquals("Toyota", fila.get("vehiculo_marca"));
        assertEquals("R-01", fila.get("ruta_codigo"));
        assertEquals("Quito-Guayaquil", fila.get("ruta_nombre"));
        assertEquals("2026-08-01", fila.get("fecha_inicio"));
        assertEquals("2026-08-15", fila.get("fecha_fin"));
    }
}