package ec.edu.uteq.sgroas.abd.repository;

import ec.edu.uteq.sgroas.abd.entity.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Integer> {

    /**
     * Consulta las unidades con el estado dado de forma paginada sin distinguir mayusculas.
     * @param status estado de la unidad a filtrar.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con las unidades del estado indicado.
     */
    @Query("SELECT u FROM Unit u WHERE LOWER(u.estado) = LOWER(:status)")
    Page<Unit> findByStatusIgnoreCase(@Param("status") String status, Pageable pageable);

    /**
     * Consulta si existe una unidad con la placa dada sin distinguir mayusculas.
     * @param licensePlate placa a verificar.
     * @return verdadero si existe una unidad con esa placa.
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Unit u WHERE LOWER(u.placa) = LOWER(:licensePlate)")
    boolean existsByLicensePlateIgnoreCase(@Param("licensePlate") String licensePlate);

    /**
     * Consulta si existe una unidad con el numero de disco dado sin distinguir mayusculas.
     * @param numeroDisco numero de disco a verificar.
     * @return verdadero si existe una unidad con ese numero de disco.
     */
    boolean existsByNumeroDiscoIgnoreCase(String numeroDisco);

    /**
     * Consulta las unidades con filtros opcionales de estado y texto de busqueda por placa, disco o modelo.
     * @param estado estado a filtrar, puede ser nulo.
     * @param search texto de busqueda, puede ser nulo.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con las unidades que cumplen los filtros.
     */
    @Query(value = """
            SELECT * FROM unidad u
            WHERE (CAST(:estado AS text) IS NULL OR LOWER(u.estado) = CAST(:estado AS text))
              AND (CAST(:search AS text) IS NULL OR LOWER(u.placa) LIKE '%' || CAST(:search AS text) || '%'
                       OR LOWER(u.numero_disco) LIKE '%' || CAST(:search AS text) || '%'
                       OR LOWER(u.modelo) LIKE '%' || CAST(:search AS text) || '%')
            ORDER BY u.id_unidad
            """, nativeQuery = true)
    Page<Unit> searchWithFilters(@Param("estado") String estado,
                                  @Param("search") String search,
                                  Pageable pageable);

    /**
     * Consulta el conteo de unidades agrupadas por estado.
     * @return lista con el total por cada estado.
     */
    @Query(value = "SELECT estado AS label, COUNT(*) AS total FROM unidad GROUP BY estado ORDER BY total DESC",
           nativeQuery = true)
    List<CountProjection> countByStatus();
}
