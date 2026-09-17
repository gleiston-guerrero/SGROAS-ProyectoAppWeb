package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.repository.RouteAssignmentRepository;
import ec.edu.uteq.sgroas.repository.DriverRepository;
import ec.edu.uteq.sgroas.repository.IncidentRepository;
import ec.edu.uteq.sgroas.repository.RouteRepository;
import ec.edu.uteq.sgroas.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de reportes que invoca los stored procedures mediante
 * procedimientos almacenados (estrategia hibrida CRUD-ORM + SP, ver ADR-006).
 * Transaccional: los cursores REFCURSOR de PostgreSQL solo pueden leerse
 * dentro de la misma transaccion JDBC (ver CATALOGO-SP.md).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final IncidentRepository incidentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;

    /**
     * Calcula los conteos globales de conductores, vehiculos, rutas, asignaciones e incidentes.
     * @return lista de filas con los totales y los conteos de registros activos y abiertos
     */
    public List<Map<String, Object>> generalStatistics() {
        return incidentRepository.generalStatistics().stream()
                .map(fila -> mapa(
                        "total_conductores", fila[0],
                        "conductores_activos", fila[1],
                        "total_vehiculos", fila[2],
                        "vehiculos_activos", fila[3],
                        "total_rutas", fila[4],
                        "rutas_activas", fila[5],
                        "total_asignaciones", fila[6],
                        "asignaciones_activas", fila[7],
                        "total_incidentes", fila[8],
                        "incidentes_abiertos", fila[9]))
                .collect(Collectors.toList());
    }

    /**
     * Agrupa los incidentes por nivel de gravedad segun el filtro indicado.
     * @param tipo criterio de gravedad enviado al procedimiento para filtrar el conteo
     * @return lista de filas con cada gravedad, su total de incidentes y la fecha del ultimo caso
     */
    public List<Map<String, Object>> incidentsBySeverity(String tipo) {
        return incidentRepository.incidentsBySeverity(tipo).stream()
                .map(fila -> mapa(
                        "gravedad", fila[0],
                        "total_incidentes", fila[1],
                        "ultimo_incidente", fila[2]))
                .collect(Collectors.toList());
    }

    /**
     * Recupera los incidentes ocurridos dentro del intervalo de fechas indicado.
     * @param desde fecha y hora inicial del intervalo por consultar
     * @param hasta fecha y hora final del intervalo por consultar
     * @return lista de filas con el detalle de cada incidente y sus datos de conductor, vehiculo y ruta
     */
    public List<Map<String, Object>> incidentsByRange(Instant desde, Instant hasta) {
        return incidentRepository.getIncidentsByRange(desde, hasta).stream()
                .map(fila -> mapa(
                        "incidente_id", fila[0],
                        "tipo", fila[1],
                        "gravedad", fila[2],
                        "estado", fila[3],
                        "descripcion", fila[4],
                        "fecha_incidente", fila[5],
                        "ubicacion", fila[6],
                        "conductor_nombre", fila[7],
                        "vehiculo_placa", fila[8],
                        "ruta_codigo", fila[9]))
                .collect(Collectors.toList());
    }

    /**
     * Localiza los conductores cuya licencia vence dentro del plazo indicado.
     * @param dias cantidad maxima de dias restantes para considerar una licencia proxima a vencer
     * @return lista de filas con los datos del conductor, su licencia y su asignacion vigente
     */
    public List<Map<String, Object>> licensesExpiring(Integer dias) {
        return driverRepository.licensesExpiring(dias).stream()
                .map(fila -> mapa(
                        "conductor_id", fila[0],
                        "nombre_completo", fila[1],
                        "cedula", fila[2],
                        "numero_licencia", fila[3],
                        "tipo_licencia", fila[4],
                        "fecha_vencimiento", fila[5],
                        "asignacion_activa", fila[6]))
                .collect(Collectors.toList());
    }

    /**
     * Recupera los vehiculos marcados en mantenimiento con sus conteos asociados.
     * @return lista de filas con los datos del vehiculo y sus totales de asignaciones e incidentes
     */
    public List<Map<String, Object>> vehiclesInMaintenance() {
        return vehicleRepository.vehiclesInMaintenance().stream()
                .map(fila -> mapa(
                        "vehiculo_id", fila[0],
                        "placa", fila[1],
                        "marca", fila[2],
                        "modelo", fila[3],
                        "anio", fila[4],
                        "total_asignaciones", fila[5],
                        "total_incidentes", fila[6]))
                .collect(Collectors.toList());
    }

    /**
     * Calcula el rendimiento de cada ruta con sus asignaciones e incidentes por gravedad.
     * @return lista de filas con los totales de asignaciones e incidentes de cada ruta evaluada
     */
    public List<Map<String, Object>> routePerformanceReport() {
        return routeRepository.routePerformanceReport().stream()
                .map(fila -> mapa(
                        "ruta_id", fila[0],
                        "ruta_codigo", fila[1],
                        "ruta_nombre", fila[2],
                        "total_asignaciones", fila[3],
                        "total_incidentes", fila[4],
                        "incidentes_criticos", fila[5],
                        "incidentes_altos", fila[6],
                        "incidentes_medios", fila[7],
                        "incidentes_bajos", fila[8],
                        "promedio_distancia_km", fila[9]))
                .collect(Collectors.toList());
    }

    /**
     * Recupera las asignaciones activas vinculadas al conductor indicado.
     * @param conductorId identificador del conductor cuyas asignaciones vigentes se desean consultar
     * @return lista de filas con cada asignacion activa y los datos de su vehiculo y ruta
     */
    public List<Map<String, Object>> activeAssignmentsByDriver(Long conductorId) {
        return routeAssignmentRepository.activeAssignmentsByDriver(conductorId).stream()
                .map(fila -> mapa(
                        "asignacion_id", fila[0],
                        "vehiculo_placa", fila[1],
                        "vehiculo_marca", fila[2],
                        "ruta_codigo", fila[3],
                        "ruta_nombre", fila[4],
                        "fecha_inicio", fila[5],
                        "fecha_fin", fila[6]))
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapa(Object... pares) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put((String) pares[i], pares[i + 1]);
        }
        return m;
    }
}