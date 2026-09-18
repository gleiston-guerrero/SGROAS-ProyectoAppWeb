package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.repository.AlertRepository;
import ec.edu.uteq.sgroas.abd.repository.AbdIncidentRepository;
import ec.edu.uteq.sgroas.abd.repository.ScheduleRepository;
import ec.edu.uteq.sgroas.abd.repository.AbdRouteRepository;
import ec.edu.uteq.sgroas.abd.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbdReportService {

    private final ScheduleRepository programacionRepository;
    private final AbdIncidentRepository incidenteRepository;
    private final AlertRepository alertaRepository;
    private final UnitRepository unidadRepository;
    private final AbdRouteRepository rutaAbdRepository;

    /**
     * Calcula los totales generales de programaciones, incidentes, alertas, unidades y rutas del sistema.
     * @return objeto con los conteos consolidados que conforman el panel de resumen.
     */
    public AbdDtos.SummaryResponse summary() {
        long totalProgramaciones = programacionRepository.count();
        long programacionesActivas = programacionRepository.findByStatusIgnoreCase("Programado",
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long totalIncidentes = incidenteRepository.count();
        long incidentesAlto = incidenteRepository.findBySuggestedLevelIgnoreCase("ALTO",
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long totalAlertas = alertaRepository.count();
        long totalUnidades = unidadRepository.count();
        long unidadesMantenimiento = unidadRepository.findByStatusIgnoreCase("En Mantenimiento",
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long totalRutas = rutaAbdRepository.count();

        return new AbdDtos.SummaryResponse(
                totalProgramaciones, programacionesActivas,
                totalIncidentes, incidentesAlto,
                totalAlertas, totalUnidades, unidadesMantenimiento,
                totalRutas
        );
    }

    /**
     * Agrupa la cantidad de incidentes registrados segun su nivel de riesgo sugerido.
     * @return lista con cada nivel de riesgo y el total de incidentes que le corresponde.
     */
    public java.util.List<AbdDtos.CountResponse> incidentsByLevel() {
        return incidenteRepository.countByLevel().stream()
                .map(f -> new AbdDtos.CountResponse(f.getLevel(), f.getTotal()))
                .toList();
    }

    /**
     * Agrupa la cantidad de incidentes registrados segun su estado actual de atencion.
     * @return lista con cada estado y el total de incidentes que le corresponde.
     */
    public java.util.List<AbdDtos.CountResponse> incidentsByStatus() {
        return incidenteRepository.countByStatus().stream()
                .map(f -> new AbdDtos.CountResponse(f.getStatus(), f.getTotal()))
                .toList();
    }

    /**
     * Agrupa la cantidad de unidades registradas segun su estado operativo.
     * @return lista con cada estado y el total de unidades que le corresponde.
     */
    public java.util.List<AbdDtos.CountResponse> unitsByStatus() {
        return unidadRepository.countByStatus().stream()
                .map(f -> new AbdDtos.CountResponse(f.getLabel(), f.getTotal()))
                .toList();
    }

    /**
     * Agrupa la cantidad de programaciones de viajes segun su estado actual.
     * @return lista con cada estado y el total de programaciones que le corresponde.
     */
    public java.util.List<AbdDtos.CountResponse> schedulesByStatus() {
        return programacionRepository.countByStatus().stream()
                .map(f -> new AbdDtos.CountResponse(f.getLabel(), f.getTotal()))
                .toList();
    }

    /**
     * Agrupa la cantidad de programaciones de viajes segun el mes en que estan previstas.
     * @return lista con cada mes y el total de programaciones que le corresponde.
     */
    public java.util.List<AbdDtos.CountResponse> schedulesByMonth() {
        return programacionRepository.countByMonth().stream()
                .map(f -> new AbdDtos.CountResponse(f.getLabel(), f.getTotal()))
                .toList();
    }

    /**
     * Recupera las rutas con mayor numero de programaciones para destacar las mas utilizadas.
     * @return lista con las rutas mas frecuentes y el total de viajes programados en cada una.
     */
    public java.util.List<AbdDtos.TopRouteResponse> topRoutes() {
        return rutaAbdRepository.topRoutes().stream()
                .map(f -> new AbdDtos.TopRouteResponse(f.getId(), f.getDescription(), f.getTotal()))
                .toList();
    }
}
