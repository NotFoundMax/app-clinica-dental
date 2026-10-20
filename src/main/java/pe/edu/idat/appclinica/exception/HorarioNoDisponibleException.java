package pe.edu.idat.appclinica.exception;

/**
 * El horario elegido dejó de estar disponible entre la consulta y el envío.
 *
 * La agenda se vuelve a validar siempre en el servidor. Nunca se confía
 * en la lista de horarios que mostró el navegador.
 */
public class HorarioNoDisponibleException extends RuntimeException {

    public HorarioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
