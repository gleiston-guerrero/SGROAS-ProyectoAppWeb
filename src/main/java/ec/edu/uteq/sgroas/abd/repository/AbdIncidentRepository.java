package ec.edu.uteq.sgroas.abd.repository;

import ec.edu.uteq.sgroas.abd.entity.AbdIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AbdIncidentRepository extends JpaRepository<AbdIncident, Integer> {

    /**
     * Consulta los incidentes con el estado dado de forma paginada sin distinguir mayusculas.
     * @param status estado del incidente a filtrar.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con los incidentes del estado indicado.
     */
    @Query("SELECT i FROM AbdIncident i WHERE LOWER(i.estado) = LOWER(:status)")
    Page<AbdIncident> findByStatusIgnoreCase(@Param("status") String status, Pageable pageable);

    /**
     * Consulta los incidentes con el nivel sugerido dado de forma paginada sin distinguir mayusculas.
     * @param nivelSugerido nivel sugerido a filtrar.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con los incidentes del nivel indicado.
     */
    Page<AbdIncident> findByNivelSugeridoIgnoreCase(String nivelSugerido, Pageable pageable);

    /**
     * Consulta los incidentes con filtros opcionales de estado, nivel y texto de busqueda por tipo, descripcion o placa.
     * @param estado estado a filtrar, puede ser nulo.
     * @param nivel nivel sugerido a filtrar, puede ser nulo.
     * @param search texto de busqueda, puede ser nulo.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con los incidentes que cumplen los filtros.
     */
    @Query(value = """
            SELECT * FROM incidente i
            WHERE (CAST(:estado AS text) IS NULL OR LOWER(i.estado) = CAST(:estado AS text))
              AND (CAST(:nivel AS text) IS NULL OR LOWER(i.nivel_sugerido) = CAST(:nivel AS text))
              AND (CAST(:search AS text) IS NULL OR LOWER(i.tipo) LIKE '%' || CAST(:search AS text) || '%'
                       OR LOWER(i.descripcion) LIKE '%' || CAST(:search AS text) || '%'
                       OR LOWER((SELECT placa FROM unidad WHERE id_unidad = i.id_unidad)) LIKE '%' || CAST(:search AS text) || '%')
            ORDER BY i.id_incidente
            """, nativeQuery = true)
    Page<AbdIncident> searchWithFilters(@Param("estado") String estado,
                                        @Param("nivel") String nivel,
                                        @Param("search") String search,
                                        Pageable pageable);

    /**
     * Consulta el conteo de incidentes agrupados por nivel sugerido.
     * @return lista con el total por cada nivel.
     */
    @Query("""
            select i.nivelSugerido as nivel, count(i) as total
            from AbdIncident i
            group by i.nivelSugerido
            order by total desc
            """)
    List<CountByLevel> countByLevel();

    /**
     * Consulta el conteo de incidentes agrupados por estado.
     * @return lista con el total por cada estado.
     */
    @Query("""
            select i.estado as status, count(i) as total
            from AbdIncident i
            group by i.estado
            """)
    List<CountByStatus> countByStatus();

    /**
     * Proyeccion con el conteo de incidentes por nivel sugerido.
     */
    interface CountByLevel {
        /**
         * Obtiene el nivel sugerido del grupo.
         * @return nivel sugerido.
         */
        String getNivel();

        /**
         * Obtiene el total de incidentes del nivel.
         * @return total de incidentes.
         */
        Integer getTotal();
    }

    /**
     * Proyeccion con el conteo de incidentes por estado.
     */
    interface CountByStatus {
        /**
         * Obtiene el estado del grupo.
         * @return estado de los incidentes.
         */
        String getStatus();

        /**
         * Obtiene el total de incidentes del estado.
         * @return total de incidentes.
         */
        Integer getTotal();
    }
}
