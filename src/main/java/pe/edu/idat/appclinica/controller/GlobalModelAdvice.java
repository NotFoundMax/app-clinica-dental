package pe.edu.idat.appclinica.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.repository.UsuarioRepository;
import pe.edu.idat.appclinica.security.CaptchaService;

import java.security.Principal;
import java.time.Year;

/**
 * Datos comunes a todas las vistas: usuario de la sesión, si es admin,
 * año del pie de página y si el CAPTCHA está activo.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UsuarioRepository usuarioRepository;
    private final CaptchaService captchaService;

    public GlobalModelAdvice(UsuarioRepository usuarioRepository,
                             CaptchaService captchaService) {
        this.usuarioRepository = usuarioRepository;
        this.captchaService = captchaService;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(Principal principal) {
        if (principal == null || principal.getName() == null
                || "anonymousUser".equals(principal.getName())) {
            return null;
        }
        return usuarioRepository.findByCorreoIgnoreCase(principal.getName()).orElse(null);
    }

    @ModelAttribute("esAdmin")
    public boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @ModelAttribute("anioActual")
    public int anioActual() {
        return Year.now().getValue();
    }

    @ModelAttribute("captchaHabilitado")
    public boolean captchaHabilitado() {
        return captchaService.isHabilitado();
    }
}
