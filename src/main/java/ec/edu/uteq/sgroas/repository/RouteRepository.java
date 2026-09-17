package ec.edu.uteq.sgroas.repository;

import ec.edu.uteq.sgroas.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    /**
     * Consulta las rutas activas de forma paginada.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con las rutas activas.
     */
    Page<Route> findByActiveTrue(Pageable pageable);

    /**
     * Consulta si existe una ruta con el codigo dado.
     * @param code codigo de la ruta a verificar.
     * @return verdadero si existe una ruta con ese codigo.
     */
    boolean existsByCode(String code);

    /**
     * Ejecuta el procedimiento almacenado que genera el reporte de rendimiento de rutas.
     * @return lista de filas crudas devueltas por el procedimiento almacenado.
     */
    @Procedure(name = "Route.reporteRendimientoRutas")
    List<Object[]> routePerformanceReport();
}
