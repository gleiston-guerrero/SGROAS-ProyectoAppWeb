package ec.edu.uteq.sgroas.repository;

import ec.edu.uteq.sgroas.entity.RouteAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteAssignmentRepository extends JpaRepository<RouteAssignment, Long> {

    /**
     * Consulta las asignaciones de ruta activas de forma paginada, cargando en
     * la misma consulta el conductor, el vehiculo y la ruta asociados.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con las asignaciones activas.
     */
    @EntityGraph(attributePaths = {"driver", "vehicle", "route"})
    Page<RouteAssignment> findByActiveTrue(Pageable pageable);

    /**
     * Consulta una asignacion de ruta por su identificador, cargando en la misma
     * consulta el conductor, el vehiculo y la ruta asociados.
     * @param id identificador de la asignacion de ruta.
     * @return asignacion de ruta con sus detalles si existe.
     */
    @Query("SELECT a FROM RouteAssignment a JOIN FETCH a.driver JOIN FETCH a.vehicle JOIN FETCH a.route WHERE a.id = :id")
    Optional<RouteAssignment> findWithDetails(@Param("id") Long id);

    /**
     * Consulta las asignaciones de ruta activas de un conductor.
     * @param driverId identificador del conductor.
     * @return lista de asignaciones activas del conductor.
     */
    List<RouteAssignment> findByDriverIdAndActiveTrue(Long driverId);

    /**
     * Consulta las asignaciones de ruta activas de un vehiculo.
     * @param vehicleId identificador del vehiculo.
     * @return lista de asignaciones activas del vehiculo.
     */
    List<RouteAssignment> findByVehicleIdAndActiveTrue(Long vehicleId);

    /**
     * Consulta las asignaciones activas correspondientes a una ruta.
     * @param routeId identificador de la ruta.
     * @return lista de asignaciones activas de esa ruta.
     */
    List<RouteAssignment> findByRouteIdAndActiveTrue(Long routeId);

    /**
     * Ejecuta el procedimiento almacenado que lista las asignaciones de ruta
     * activas de un conductor.
     * @param conductorId identificador del conductor.
     * @return lista de filas crudas devueltas por el procedimiento almacenado.
     */
    @Procedure(name = "RouteAssignment.asignacionesActivasPorConductor")
    List<Object[]> activeAssignmentsByDriver(@Param("p_conductor_id") Long conductorId);
}
