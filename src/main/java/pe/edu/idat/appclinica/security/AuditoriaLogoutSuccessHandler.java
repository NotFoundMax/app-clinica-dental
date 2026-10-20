package pe.edu.idat.appclinica.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Registra el cierre de sesión y vuelve al login.
 */
@Component
public class AuditoriaLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuditoriaService auditoriaService;

    public AuditoriaLogoutSuccessHandler(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication)
            throws IOException, ServletException {

        if (authentication != null) {
            auditoriaService.registrarLogout(
                    authentication.getName(), request.getRemoteAddr());
        }

        response.sendRedirect(request.getContextPath() + "/auth/login?logout");
    }
}
