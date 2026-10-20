package pe.edu.idat.appclinica.exception;

/**
 * Ya existe un registro con la misma clave en el catálogo
 * (por ejemplo, un servicio con el mismo nombre).
 */
public class DatosDuplicadosException extends RuntimeException {

    public DatosDuplicadosException(String mensaje) {
        super(mensaje);
    }
}
