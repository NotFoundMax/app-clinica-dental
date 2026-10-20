package pe.edu.idat.appclinica.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import pe.edu.idat.appclinica.security.AuditoriaService;

import java.security.Principal;

@Controller
public class InicioController {

    private final AuditoriaService auditoriaService;

    public InicioController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/auth/home";
    }

    /**
     * Spring Security redirige aquí cuando un usuario autenticado intenta
     * entrar a una ruta para la que no tiene rol.
     */
    @RequestMapping(value = "/acceso-denegado",
            method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado(Principal principal, HttpServletRequest request) {
        auditoriaService.registrarAccesoDenegado(
                principal != null ? principal.getName() : null,
                request.getRemoteAddr(),
                "Ruta protegida sin permisos suficientes");
        return "error/403";
    }
}
