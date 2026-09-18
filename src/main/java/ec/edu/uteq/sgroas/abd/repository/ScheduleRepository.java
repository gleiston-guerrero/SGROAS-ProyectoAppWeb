package ec.edu.uteq.sgroas.abd.repository;

import ec.edu.uteq.sgroas.abd.entity.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    /**
     * Consulta las programaciones con el estado dado de forma paginada sin distinguir mayusculas.
     * @param status estado de la programacion a filtrar.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con las programaciones del estado indicado.
     */
    @Query("SELECT p FROM Schedule p WHERE LOWER(p.estado) = LOWER(:status)")
    Page<Schedule> findByStatusIgnoreCase(@Param("status") String status, Pageable pageable);

    /**
     * Consulta las programaciones con filtros opcionales de estado, conductor, ruta y rango de fechas.
     * @param estado estado a filtrar, puede ser nulo.
     * @param idConductor identificador del conductor, puede ser nulo.
     * @param idRuta identificador de la ruta, puede ser nulo.
     * @param fechaDesde fecha inicial del rango, puede ser nula.
     * @param fechaHasta fecha final del rango, puede ser nula.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con las programaciones que cumplen los filtros.
     */
    @Query(value = """
            SELECT * FROM programacion p
            WHERE (CAST(:estado AS text) IS NULL OR LOWER(p.estado) = CAST(:estado AS text))
              AND (CAST(:idConductor AS integer) IS NULL OR p.id_conductor = :idConductor)
              AND (CAST(:idRuta AS integer) IS NULL OR p.id_ruta = :idRuta)
              AND (CAST(:fechaDesde AS date) IS NULL OR p.fecha >= CAST(:fechaDesde AS date))
              AND (CAST(:fechaHasta AS date) IS NULL OR p.fecha <= CAST(:fechaHasta AS date))
            ORDER BY p.id_programacion
            """, nativeQuery = true)
    Page<Schedule> searchWithFilters(@Param("estado") String estado,
                                        @Param("idConductor") Integer idConductor,
                                        @Param("idRuta") Integer idRuta,
                                        @Param("fechaDesde") LocalDate fechaDesde,
                                        @Param("fechaHasta") LocalDate fechaHasta,
                                        Pageable pageable);

    /**
     * Consulta el conteo de programaciones agrupadas por estado.
     * @return lista con el total por cada estado.
     */
    @Query(value = "SELECT estado AS label, COUNT(*) AS total FROM programacion GROUP BY estado ORDER BY total DESC",
           nativeQuery = true)
    List<CountProjection> countByStatus();

    /**
     * Consulta el conteo de programaciones por mes de los ultimos 180 dias.
     * @return lista con el total por cada mes.
     */
    @Query(value = """
            SELECT TO_CHAR(fecha, 'YYYY-MM') AS label, COUNT(*) AS total
            FROM programacion
            WHERE fecha >= CURRENT_DATE - INTERVAL '180 days'
            GROUP BY TO_CHAR(fecha, 'YYYY-MM')
            ORDER BY label
            """,
           nativeQuery = true)
    List<CountProjection> countByMonth();
}
