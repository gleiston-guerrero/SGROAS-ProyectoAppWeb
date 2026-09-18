package ec.edu.uteq.sgroas.abd.repository;

import ec.edu.uteq.sgroas.abd.entity.AbdRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AbdRouteRepository extends JpaRepository<AbdRoute, Integer> {

    /**
     * Consulta si existe una ruta con el terminal de origen y destino dados.
     * @param originId identificador del terminal de origen.
     * @param destinationId identificador del terminal de destino.
     * @return verdadero si existe la ruta con ese origen y destino.
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM AbdRoute r
            WHERE r.terminalOrigen.idTerminal = :originId AND r.terminalDestino.idTerminal = :destinationId
            """)
    boolean existsByOriginAndDestinationTerminal(@Param("originId") Integer originId,
                                                  @Param("destinationId") Integer destinationId);

    /**
     * Consulta el total de programaciones de una ruta.
     * @param idRuta identificador de la ruta.
     * @return total de programaciones de la ruta.
     */
    @Query("""
            SELECT COUNT(p) FROM Schedule p WHERE p.ruta.idRuta = :idRuta
            """)
    long countSchedules(@Param("idRuta") Integer idRuta);

    /**
     * Consulta las rutas que coinciden con un texto de busqueda por terminales o ciudades.
     * @param search texto de busqueda, puede ser nulo para traer todas.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con las rutas encontradas.
     */
    @Query("""
            SELECT r FROM AbdRoute r
            WHERE (:search IS NULL
                       OR LOWER(r.terminalOrigen.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(r.terminalDestino.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(r.terminalOrigen.ciudad.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(r.terminalDestino.ciudad.nombre) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<AbdRoute> search(@Param("search") String search, Pageable pageable);

    /**
     * Consulta las 5 rutas con mayor numero de programaciones.
     * @return lista con las rutas mas programadas.
     */
    @Query(value = """
            SELECT r.id_ruta AS id,
                   t1.nombre || ' -> ' || t2.nombre AS description,
                   COUNT(p.id_programacion) AS total
            FROM programacion p
            JOIN ruta r ON r.id_ruta = p.id_ruta
            JOIN terminal t1 ON t1.id_terminal = r.id_terminal_origen
            JOIN terminal t2 ON t2.id_terminal = r.id_terminal_destino
            GROUP BY r.id_ruta, t1.nombre, t2.nombre
            ORDER BY total DESC
            LIMIT 5
            """, nativeQuery = true)
    List<TopRouteProjection> topRoutes();
}
