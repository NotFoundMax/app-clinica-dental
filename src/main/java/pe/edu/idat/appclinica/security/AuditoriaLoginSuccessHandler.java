package pe.edu.idat.appclinica.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Registra el login exitoso y redirige según el rol.
 */
@Component
public class AuditoriaLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuditoriaService auditoriaService;

    public AuditoriaLoginSuccessHandler(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        auditoriaService.registrarLoginExitoso(
                authentication.getName(), request.getRemoteAddr());

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        String destino = esAdmin ? "/admin/citas" : "/auth/home";
        response.sendRedirect(request.getContextPath() + destino);
    }
}
