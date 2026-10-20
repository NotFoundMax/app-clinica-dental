package pe.edu.idat.appclinica.exception;

/**
 * El correo ya pertenece a una cuenta.
 */
public class UsuarioDuplicadoException extends RuntimeException {

    public UsuarioDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
