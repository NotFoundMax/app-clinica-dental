package pe.edu.idat.appclinica.model;

import java.util.List;

/**
 * Estados posibles de una cita.
 *
 * Transiciones permitidas:
 *   PENDIENTE  -> CONFIRMADA o CANCELADA
 *   CONFIRMADA -> ATENDIDA o CANCELADA
 *   ATENDIDA   -> (final)
 *   CANCELADA  -> (final)
 */
public enum EstadoCita {

    PENDIENTE("Pendiente", "pendiente"),
    CONFIRMADA("Confirmada", "confirmada"),
    ATENDIDA("Atendida", "atendida"),
    CANCELADA("Cancelada", "cancelada");

    private final String etiqueta;
    private final String claseCss;

    EstadoCita(String etiqueta, String claseCss) {
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClaseCss() {
        return claseCss;
    }

    /**
     * Estados a los que se puede pasar desde este estado.
     * El panel administrativo solo ofrece estas opciones.
     */
    public List<EstadoCita> getSiguientes() {
        return switch (this) {
            case PENDIENTE -> List.of(CONFIRMADA, CANCELADA);
            case CONFIRMADA -> List.of(ATENDIDA, CANCELADA);
            case ATENDIDA, CANCELADA -> List.of();
        };
    }
}
