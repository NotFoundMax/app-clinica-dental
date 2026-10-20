package pe.edu.idat.appclinica.exception;

/**
 * El usuario autenticado intenta operar sobre un recurso ajeno.
 *
 * Es la barrera contra IDOR (Insecure Direct Object Reference):
 * el id de la cita llega por el formulario, pero la pertenencia se
 * verifica en el servidor contra el usuario de la sesión.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
