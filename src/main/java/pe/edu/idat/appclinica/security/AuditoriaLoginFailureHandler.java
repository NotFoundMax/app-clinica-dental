package pe.edu.idat.appclinica.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Registra el intento fallido sin revelar si el correo existe.
 */
@Component
public class AuditoriaLoginFailureHandler implements AuthenticationFailureHandler {

    private final AuditoriaService auditoriaService;

    public AuditoriaLoginFailureHandler(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        auditoriaService.registrarIntentoLoginFallido(
                request.getParameter("correo"), request.getRemoteAddr());

        response.sendRedirect(request.getContextPath() + "/auth/login?error");
    }
}
