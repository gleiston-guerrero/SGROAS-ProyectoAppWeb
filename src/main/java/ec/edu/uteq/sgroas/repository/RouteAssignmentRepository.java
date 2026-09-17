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

    @EntityGraph(attributePaths = {"driver", "vehicle", "route"})
    Page<RouteAssignment> findByActiveTrue(Pageable pageable);

    @Query("SELECT a FROM RouteAssignment a JOIN FETCH a.driver JOIN FETCH a.vehicle JOIN FETCH a.route WHERE a.id = :id")
    Optional<RouteAssignment> findWithDetails(@Param("id") Long id);

    List<RouteAssignment> findByDriverIdAndActiveTrue(Long driverId);

    List<RouteAssignment> findByVehicleIdAndActiveTrue(Long vehicleId);

    List<RouteAssignment> findByRouteIdAndActiveTrue(Long routeId);

    @Procedure(name = "RouteAssignment.asignacionesActivasPorConductor")
    List<Object[]> activeAssignmentsByDriver(@Param("p_conductor_id") Long conductorId);
}
