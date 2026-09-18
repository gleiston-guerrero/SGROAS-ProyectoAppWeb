package ec.edu.uteq.sgroas.abd.repository;

import ec.edu.uteq.sgroas.abd.entity.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TerminalRepository extends JpaRepository<Terminal, Integer> {

    /**
     * Consulta los terminales de una ciudad ordenados por identificador ascendente.
     * @param cityId identificador de la ciudad.
     * @return lista de terminales de la ciudad.
     */
    @Query("SELECT t FROM Terminal t WHERE t.ciudad.idCiudad = :cityId ORDER BY t.idTerminal ASC")
    List<Terminal> findByCityIdOrderByIdAsc(@Param("cityId") Integer cityId);
}
