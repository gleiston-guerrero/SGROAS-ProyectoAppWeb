package ec.edu.uteq.sgroas.abd.repository;

import ec.edu.uteq.sgroas.abd.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Integer> {

    /**
     * Consulta las ciudades de una provincia ordenadas por identificador ascendente.
     * @param provinceId identificador de la provincia.
     * @return lista de ciudades de la provincia.
     */
    @Query("SELECT c FROM City c WHERE c.provincia.idProvincia = :provinceId ORDER BY c.idCiudad ASC")
    List<City> findByProvinceIdOrderByIdAsc(@Param("provinceId") Integer provinceId);
}
