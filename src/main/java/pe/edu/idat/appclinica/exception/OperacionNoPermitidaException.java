package pe.edu.idat.appclinica.exception;

/**
 * La operación no es válida para el estado actual del recurso.
 * Por ejemplo: cancelar una cita ya atendida.
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
