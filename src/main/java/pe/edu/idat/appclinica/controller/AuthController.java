package pe.edu.idat.appclinica.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.idat.appclinica.dto.RegistroForm;
import pe.edu.idat.appclinica.exception.UsuarioDuplicadoException;
import pe.edu.idat.appclinica.model.Cita;
import pe.edu.idat.appclinica.model.EstadoCita;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.security.AuditoriaService;
import pe.edu.idat.appclinica.security.CaptchaService;
import pe.edu.idat.appclinica.service.CitaService;
import pe.edu.idat.appclinica.service.UsuarioService;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioService usuarioService;
    private final CitaService citaService;
    private final CaptchaService captchaService;
    private final AuditoriaService auditoriaService;

    public AuthController(UsuarioService usuarioService,
                          CitaService citaService,
                          CaptchaService captchaService,
                          AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.citaService = citaService;
        this.captchaService = captchaService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/login")
    public String login(Principal principal) {
        if (principal != null && !"anonymousUser".equals(principal.getName())) {
            return esAdmin() ? "redirect:/admin/citas" : "redirect:/auth/home";
        }
        return "auth/login";
    }

    @GetMapping("/frmRegistroUsuario")
    public String formularioRegistro(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new RegistroForm());
        }
        return "auth/registro";
    }

    @PostMapping("/guardarUsuario")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") RegistroForm form,
                                 BindingResult errores,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 Model model,
                                 RedirectAttributes redirect) {

        if (!captchaService.validar(form.getCaptcha(), session)) {
            errores.rejectValue("captcha", "captcha.invalido",
                    "El código de verificación no es correcto o expiró");
            auditoriaService.registrarCaptchaFallido(
                    form.getCorreo(), request.getRemoteAddr());
        }

        if (form.getPassword() != null && form.getConfirmarPassword() != null
                && !Objects.equals(form.getPassword(), form.getConfirmarPassword())) {
            errores.rejectValue("confirmarPassword", "password.noCoincide",
                    "Las contraseñas no coinciden");
        }

        if (errores.hasErrors()) {
            return "auth/registro";
        }

        try {
            usuarioService.registrar(form);

            auditoriaService.registrarRegistroExitoso(
                    form.getCorreo(), request.getRemoteAddr());

            redirect.addFlashAttribute("mensaje",
                    "Tu cuenta fue creada. Ya puedes iniciar sesión.");
            return "redirect:/auth/login";

        } catch (UsuarioDuplicadoException e) {
            auditoriaService.registrarIntentoRegistroDuplicado(
                    form.getCorreo(), request.getRemoteAddr());
            errores.rejectValue("correo", "correo.duplicado", e.getMessage());
            return "auth/registro";

        } catch (RuntimeException e) {
            log.error("No se pudo registrar la cuenta", e);
            errores.reject("registro.error",
                    "No se pudo crear la cuenta. Intenta nuevamente.");
            return "auth/registro";
        }
    }

    @GetMapping("/home")
    public String home(Principal principal, Model model) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            return "redirect:/auth/login";
        }

        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        List<Cita> citas = citaService.listarDelPaciente(usuario.getId());
        LocalDate hoy = LocalDate.now();

        List<Cita> proximas = citas.stream()
                .filter(c -> !c.getFecha().isBefore(hoy))
                .filter(c -> c.getEstado() == EstadoCita.PENDIENTE
                        || c.getEstado() == EstadoCita.CONFIRMADA)
                .sorted(Comparator.comparing(Cita::getInicio))
                .collect(Collectors.toList());

        List<Cita> historial = citas.stream()
                .filter(c -> c.getFecha().isBefore(hoy)
                        || c.getEstado() == EstadoCita.CANCELADA
                        || c.getEstado() == EstadoCita.ATENDIDA)
                .sorted(Comparator.comparing(Cita::getInicio).reversed())
                .limit(5)
                .collect(Collectors.toList());

        long atendidas = citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.ATENDIDA)
                .count();

        model.addAttribute("usuario", usuario);
        model.addAttribute("proxima", proximas.isEmpty() ? null : proximas.get(0));
        model.addAttribute("proximas", proximas.stream().limit(4).toList());
        model.addAttribute("historial", historial);
        model.addAttribute("totalCitas", citas.size());
        model.addAttribute("totalProximas", proximas.size());
        model.addAttribute("totalAtendidas", atendidas);
        model.addAttribute("seccion", "inicio");

        return "auth/home";
    }

    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
