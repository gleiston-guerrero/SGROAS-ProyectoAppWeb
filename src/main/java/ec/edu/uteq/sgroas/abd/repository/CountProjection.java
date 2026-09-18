package ec.edu.uteq.sgroas.abd.repository;

/**
 * Proyeccion con el resultado de un conteo agrupado por clave.
 */
public interface CountProjection {
    /**
     * Obtiene la clave del grupo contado.
     * @return clave del grupo.
     */
    String getLabel();

    /**
     * Obtiene el total de elementos del grupo.
     * @return total de elementos.
     */
    long getTotal();
}
