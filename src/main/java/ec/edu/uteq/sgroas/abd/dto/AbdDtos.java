package ec.edu.uteq.sgroas.abd.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTOs de la capa ABD. Records inmutables con validacion Jakarta.
 */
public final class AbdDtos {

    private AbdDtos() {
    }

    // ---------- Catalogos ----------

    /**
     * Respuesta con los datos de una provincia.
     * @param idProvincia identificador de la provincia.
     * @param nombre nombre de la provincia.
     */
    public record ProvinceResponse(Integer idProvincia, String nombre) {
    }

    /**
     * Respuesta con los datos de una ciudad y su provincia.
     * @param idCiudad identificador de la ciudad.
     * @param nombre nombre de la ciudad.
     * @param idProvincia identificador de la provincia.
     * @param nombreProvincia nombre de la provincia.
     */
    public record CityResponse(Integer idCiudad, String nombre, Integer idProvincia, String nombreProvincia) {
    }

    /**
     * Respuesta con los datos de un terminal y su ciudad.
     * @param idTerminal identificador del terminal.
     * @param nombre nombre del terminal.
     * @param idCiudad identificador de la ciudad.
     * @param nombreCiudad nombre de la ciudad.
     */
    public record TerminalResponse(Integer idTerminal, String nombre, Integer idCiudad, String nombreCiudad) {
    }

    /**
     * Respuesta con los datos de un rol.
     * @param idRol identificador del rol.
     * @param nombre nombre del rol.
     * @param descripcion descripcion del rol.
     */
    public record RoleResponse(Integer idRol, String nombre, String descripcion) {
    }

    /**
     * Respuesta con las listas de catalogos de provincias, ciudades, terminales y roles.
     * @param provincias lista de provincias.
     * @param ciudades lista de ciudades.
     * @param terminales lista de terminales.
     * @param roles lista de roles.
     */
    public record CatalogsResponse(List<ProvinceResponse> provincias,
                                    List<CityResponse> ciudades,
                                    List<TerminalResponse> terminales,
                                    List<RoleResponse> roles) {
    }

    // ---------- Rutas (ABD) ----------

    /**
     * Peticion para crear o actualizar una ruta.
     * @param idTerminalOrigen identificador del terminal de origen.
     * @param idTerminalDestino identificador del terminal de destino.
     * @param precioPasaje precio del pasaje de la ruta.
     */
    public record AbdRouteRequest(
            @NotNull(message = "El terminal de origen es obligatorio") Integer idTerminalOrigen,
            @NotNull(message = "El terminal de destino es obligatorio") Integer idTerminalDestino,
            @NotNull @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
            @Digits(integer = 8, fraction = 2) BigDecimal precioPasaje
    ) {
    }

    /**
     * Respuesta con los datos de una ruta y su total de programaciones.
     * @param idRuta identificador de la ruta.
     * @param idTerminalOrigen identificador del terminal de origen.
     * @param terminalOrigen nombre del terminal de origen.
     * @param idTerminalDestino identificador del terminal de destino.
     * @param terminalDestino nombre del terminal de destino.
     * @param precioPasaje precio del pasaje.
     * @param totalProgramaciones total de programaciones de la ruta.
     */
    public record AbdRouteResponse(Integer idRuta, Integer idTerminalOrigen, String terminalOrigen,
                                  Integer idTerminalDestino, String terminalDestino,
                                  BigDecimal precioPasaje, long totalProgramaciones) {
    }

    // ---------- Unidades ----------

    /**
     * Peticion para crear o actualizar una unidad.
     * @param placa placa de la unidad.
     * @param numeroDisco numero de disco de la unidad.
     * @param modelo modelo de la unidad.
     * @param capacidad capacidad de pasajeros de la unidad.
     * @param anioFabricacion anio de fabricacion de la unidad.
     * @param estado estado de la unidad.
     */
    public record UnitRequest(
            @NotBlank @Size(max = 15) String placa,
            @NotBlank @Size(max = 10) String numeroDisco,
            @NotBlank @Size(max = 50) String modelo,
            @NotNull @Min(1) @Max(200) Integer capacidad,
            @Min(1950) @Max(2100) Integer anioFabricacion,
            @Pattern(regexp = "Activo|Inactivo|En Mantenimiento", message = "Estado invalido") String estado
    ) {
    }

    /**
     * Respuesta con los datos de una unidad.
     * @param idUnidad identificador de la unidad.
     * @param placa placa de la unidad.
     * @param numeroDisco numero de disco de la unidad.
     * @param modelo modelo de la unidad.
     * @param capacidad capacidad de pasajeros.
     * @param anioFabricacion anio de fabricacion.
     * @param estado estado de la unidad.
     */
    public record UnitResponse(Integer idUnidad, String placa, String numeroDisco, String modelo,
                                 Integer capacidad, Integer anioFabricacion, String estado) {
    }

    // ---------- Programaciones ----------

    /**
     * Peticion para crear o actualizar una programacion de viaje.
     * @param fecha fecha del viaje.
     * @param horaSalida hora de salida.
     * @param horaEstimadaLlegada hora estimada de llegada.
     * @param estado estado de la programacion.
     * @param idRuta identificador de la ruta.
     * @param idUnidad identificador de la unidad.
     * @param idConductor identificador del conductor.
     */
    public record ScheduleRequest(
            @NotNull LocalDate fecha,
            @NotNull LocalTime horaSalida,
            @NotNull LocalTime horaEstimadaLlegada,
            @Pattern(regexp = "Programado|En Curso|Completado|Cancelado", message = "Estado invalido") String estado,
            @NotNull Integer idRuta,
            @NotNull Integer idUnidad,
            @NotNull Integer idConductor
    ) {
    }

    /**
     * Respuesta con los datos de una programacion y sus relaciones.
     * @param idProgramacion identificador de la programacion.
     * @param fecha fecha del viaje.
     * @param horaSalida hora de salida.
     * @param horaEstimadaLlegada hora estimada de llegada.
     * @param estado estado de la programacion.
     * @param idRuta identificador de la ruta.
     * @param rutaDescripcion descripcion de la ruta.
     * @param idUnidad identificador de la unidad.
     * @param unidadPlaca placa de la unidad.
     * @param idConductor identificador del conductor.
     * @param conductorNombres nombres del conductor.
     */
    public record ScheduleResponse(Integer idProgramacion, LocalDate fecha, LocalTime horaSalida,
                                       LocalTime horaEstimadaLlegada, String estado,
                                       Integer idRuta, String rutaDescripcion,
                                       Integer idUnidad, String unidadPlaca,
                                       Integer idConductor, String conductorNombres) {
    }

    // ---------- Incidentes (ABD) ----------

    /**
     * Peticion para reportar o actualizar un incidente.
     * @param tipo tipo de incidente.
     * @param descripcion descripcion del incidente.
     * @param nivelSugerido nivel sugerido del incidente.
     * @param evidencia referencia a la evidencia del incidente.
     * @param estado estado del incidente.
     * @param idUnidad identificador de la unidad involucrada.
     */
    public record AbdIncidentRequest(
            @NotBlank @Size(max = 50) String tipo,
            @NotBlank String descripcion,
            @NotBlank @Pattern(regexp = "BAJO|MEDIO|ALTO", message = "Nivel sugerido debe ser BAJO, MEDIO o ALTO") String nivelSugerido,
            @Size(max = 255) String evidencia,
            @Pattern(regexp = "Reportado|En Revision|Cerrado", message = "Estado invalido") String estado,
            @NotNull Integer idUnidad
    ) {
    }

    /**
     * Respuesta con los datos de un incidente y su unidad.
     * @param idIncidente identificador del incidente.
     * @param tipo tipo de incidente.
     * @param descripcion descripcion del incidente.
     * @param nivelSugerido nivel sugerido del incidente.
     * @param fechaIncidente fecha del incidente.
     * @param evidencia referencia a la evidencia.
     * @param estado estado del incidente.
     * @param idUnidad identificador de la unidad.
     * @param unidadPlaca placa de la unidad.
     */
    public record AbdIncidentResponse(Integer idIncidente, String tipo, String descripcion,
                                       String nivelSugerido, String fechaIncidente, String evidencia,
                                       String estado, Integer idUnidad, String unidadPlaca) {
    }

    /**
     * Respuesta con los datos de una alerta y su incidente.
     * @param idAlerta identificador de la alerta.
     * @param nivelRiesgo nivel de riesgo de la alerta.
     * @param descripcion descripcion de la alerta.
     * @param fecha fecha de la alerta.
     * @param idIncidente identificador del incidente asociado.
     * @param incidenteTipo tipo del incidente asociado.
     */
    public record AlertResponse(Integer idAlerta, String nivelRiesgo, String descripcion,
                                 String fecha, Integer idIncidente, String incidenteTipo) {
    }

    // ---------- Reportes ----------

    /**
     * Respuesta con el conteo de elementos por clave.
     * @param clave clave del grupo contado.
     * @param total total de elementos del grupo.
     */
    public record CountResponse(String clave, long total) {
    }

    /**
     * Respuesta con una ruta destacada por programaciones.
     * @param idRuta identificador de la ruta.
     * @param descripcion descripcion de la ruta.
     * @param totalProgramaciones total de programaciones de la ruta.
     */
    public record TopRouteResponse(Integer idRuta, String descripcion, long totalProgramaciones) {
    }

    /**
     * Respuesta con el resumen de totales del panel de reportes.
     * @param totalProgramaciones total de programaciones.
     * @param programacionesActivas total de programaciones activas.
     * @param totalIncidentes total de incidentes.
     * @param incidentesAltoNivel total de incidentes de nivel alto.
     * @param totalAlertas total de alertas.
     * @param totalUnidades total de unidades.
     * @param unidadesEnMantenimiento total de unidades en mantenimiento.
     * @param totalRutas total de rutas.
     */
    public record SummaryResponse(long totalProgramaciones, long programacionesActivas,
                                  long totalIncidentes, long incidentesAltoNivel,
                                  long totalAlertas, long totalUnidades, long unidadesEnMantenimiento,
                                  long totalRutas) {
    }
}
