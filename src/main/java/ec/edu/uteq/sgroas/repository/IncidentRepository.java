package ec.edu.uteq.sgroas.repository;

import ec.edu.uteq.sgroas.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Consulta los incidentes activos de forma paginada.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con los incidentes activos.
     */
    Page<Incident> findByActiveTrue(Pageable pageable);

    /**
     * Consulta los incidentes activos asociados a una asignacion de ruta especifica.
     * @param assignmentId identificador de la asignacion de ruta.
     * @return lista de incidentes activos de esa asignacion.
     */
    List<Incident> findByAssignmentIdAndActiveTrue(Long assignmentId);

    /**
     * Ejecuta el procedimiento almacenado que lista los incidentes filtrados por tipo/gravedad.
     * @param tipo tipo de incidente a filtrar.
     * @return lista de filas crudas devueltas por el procedimiento almacenado.
     */
    @Procedure(name = "Incident.incidentesPorGravedad")
    List<Object[]> incidentsBySeverity(@Param("p_tipo") String tipo);

    /**
     * Ejecuta el procedimiento almacenado que lista los incidentes ocurridos dentro de un rango de fechas.
     * @param fechaDesde fecha y hora inicial del rango.
     * @param fechaHasta fecha y hora final del rango.
     * @return lista de filas crudas devueltas por el procedimiento almacenado.
     */
    @Procedure(name = "Incident.obtenerIncidentesPorRango")
    List<Object[]> getIncidentsByRange(@Param("p_fecha_desde") Instant fechaDesde,
                                             @Param("p_fecha_hasta") Instant fechaHasta);

    /**
     * Ejecuta el procedimiento almacenado que calcula estadisticas generales de incidentes.
     * @return lista de filas crudas devueltas por el procedimiento almacenado.
     */
    @Procedure(name = "Incident.estadisticasGenerales")
    List<Object[]> generalStatistics();
}
