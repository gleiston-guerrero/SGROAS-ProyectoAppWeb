package ec.edu.uteq.sgroas.abd.repository;

/**
 * Proyeccion con las rutas de mayor numero de programaciones.
 */
public interface TopRouteProjection {
    /**
     * Obtiene el identificador de la ruta.
     * @return identificador de la ruta.
     */
    Integer getId();

    /**
     * Obtiene la descripcion de la ruta con origen y destino.
     * @return descripcion de la ruta.
     */
    String getDescription();

    /**
     * Obtiene el total de programaciones de la ruta.
     * @return total de programaciones.
     */
    long getTotal();
}
