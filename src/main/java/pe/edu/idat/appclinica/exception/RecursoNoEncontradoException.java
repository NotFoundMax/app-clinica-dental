package pe.edu.idat.appclinica.exception;

/**
 * El recurso pedido no existe. Se traduce en un 404, sin revelar detalles.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
