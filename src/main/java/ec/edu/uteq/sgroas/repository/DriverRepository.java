package ec.edu.uteq.sgroas.repository;

import ec.edu.uteq.sgroas.entity.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    /**
     * Consulta los conductores activos de forma paginada.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con los conductores activos.
     */
    Page<Driver> findByActiveTrue(Pageable pageable);

    /**
     * Consulta los conductores activos que coinciden con un texto de busqueda
     * por nombres, apellidos, cedula o numero de licencia.
     * @param search texto de busqueda, puede ser nulo para traer todos los activos.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con los conductores activos encontrados.
     */
    @Query("""
            SELECT c FROM Driver c
            WHERE c.active = true
              AND (:search IS NULL OR LOWER(c.firstNames) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(c.lastNames) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR c.nationalId LIKE CONCAT('%', :search, '%')
                       OR LOWER(c.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Driver> searchActive(@Param("search") String search, Pageable pageable);

    /**
     * Consulta si existe un conductor con la cedula dada.
     * @param nationalId cedula a verificar.
     * @return verdadero si existe un conductor con esa cedula.
     */
    boolean existsByNationalId(String nationalId);

    /**
     * Consulta si existe un conductor con el numero de licencia dado.
     * @param licenseNumber numero de licencia a verificar.
     * @return verdadero si existe un conductor con ese numero de licencia.
     */
    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * Ejecuta el procedimiento almacenado que lista las licencias de conduccion
     * por vencer dentro del umbral de dias indicado.
     * @param diasUmbral numero de dias de anticipacion para considerar una licencia por vencer.
     * @return lista de filas crudas devueltas por el procedimiento almacenado.
     */
    @Procedure(name = "Driver.licenciasPorVencer")
    List<Object[]> licensesExpiring(@Param("p_dias_umbral") Integer diasUmbral);
}
