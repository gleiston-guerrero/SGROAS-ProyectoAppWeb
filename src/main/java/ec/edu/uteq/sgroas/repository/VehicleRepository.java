package ec.edu.uteq.sgroas.repository;

import ec.edu.uteq.sgroas.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Consulta los vehiculos activos de forma paginada.
     * @param pageable configuracion de paginacion y ordenamiento.
     * @return pagina con los vehiculos activos.
     */
    Page<Vehicle> findByActiveTrue(Pageable pageable);

    /**
     * Consulta si existe un vehiculo con la placa dada.
     * @param plate placa a verificar.
     * @return verdadero si existe un vehiculo con esa placa.
     */
    boolean existsByPlate(String plate);

    /**
     * Ejecuta el procedimiento almacenado que lista los vehiculos actualmente en mantenimiento.
     * @return lista de filas crudas devueltas por el procedimiento almacenado.
     */
    @Procedure(name = "Vehicle.vehiculosEnMantenimiento")
    List<Object[]> vehiclesInMaintenance();
}
