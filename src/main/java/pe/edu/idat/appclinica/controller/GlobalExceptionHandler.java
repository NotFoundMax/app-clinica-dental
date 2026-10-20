package pe.edu.idat.appclinica.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.edu.idat.appclinica.exception.AccesoDenegadoException;
import pe.edu.idat.appclinica.exception.DatosDuplicadosException;
import pe.edu.idat.appclinica.exception.HorarioNoDisponibleException;
import pe.edu.idat.appclinica.exception.OperacionNoPermitidaException;
import pe.edu.idat.appclinica.exception.RecursoNoEncontradoException;

/**
 * Manejo centralizado de errores de las vistas.
 *
 * Al usuario se le muestra un mensaje claro, sin stack trace ni detalles
 * internos. La traza completa queda en el log del servidor.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({AccesoDenegadoException.class, AccessDeniedException.class})
    public ModelAndView accesoDenegado(RuntimeException ex) {
        ModelAndView mv = new ModelAndView("error/403");
        mv.setStatus(HttpStatus.FORBIDDEN);
        mv.addObject("mensaje", "No tienes permiso para realizar esta acción.");
        return mv;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ModelAndView noEncontrado(RecursoNoEncontradoException ex) {
        ModelAndView mv = new ModelAndView("error/404");
        mv.setStatus(HttpStatus.NOT_FOUND);
        mv.addObject("mensaje", ex.getMessage());
        return mv;
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ModelAndView paginaNoEncontrada(Exception ex) {
        ModelAndView mv = new ModelAndView("error/404");
        mv.setStatus(HttpStatus.NOT_FOUND);
        mv.addObject("mensaje", "La página que buscas no existe.");
        return mv;
    }

    @ExceptionHandler({HorarioNoDisponibleException.class,
            OperacionNoPermitidaException.class,
            DatosDuplicadosException.class})
    public ModelAndView conflicto(RuntimeException ex) {
        ModelAndView mv = new ModelAndView("error/error");
        mv.setStatus(HttpStatus.CONFLICT);
        mv.addObject("codigo", HttpStatus.CONFLICT.value());
        mv.addObject("mensaje", ex.getMessage());
        return mv;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView metodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        ModelAndView mv = new ModelAndView("error/error");
        mv.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        mv.addObject("codigo", HttpStatus.METHOD_NOT_ALLOWED.value());
        mv.addObject("mensaje", "El método HTTP no es válido para esta ruta.");
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView errorInesperado(Exception ex) {
        log.error("Error no controlado en la aplicación", ex);
        ModelAndView mv = new ModelAndView("error/error");
        mv.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mv.addObject("codigo", HttpStatus.INTERNAL_SERVER_ERROR.value());
        mv.addObject("mensaje", "Ocurrió un error inesperado. Intenta nuevamente.");
        return mv;
    }
}
