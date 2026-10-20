package pe.edu.idat.appclinica.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.idat.appclinica.dto.CitaForm;
import pe.edu.idat.appclinica.exception.AccesoDenegadoException;
import pe.edu.idat.appclinica.exception.HorarioNoDisponibleException;
import pe.edu.idat.appclinica.exception.OperacionNoPermitidaException;
import pe.edu.idat.appclinica.exception.RecursoNoEncontradoException;
import pe.edu.idat.appclinica.model.Cita;
import pe.edu.idat.appclinica.model.EstadoCita;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.security.AuditoriaService;
import pe.edu.idat.appclinica.service.AgendaService;
import pe.edu.idat.appclinica.service.CalendarioService;
import pe.edu.idat.appclinica.service.CitaService;
import pe.edu.idat.appclinica.service.UsuarioService;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final AgendaService agendaService;
    private final CalendarioService calendarioService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public CitaController(CitaService citaService,
                          AgendaService agendaService,
                          CalendarioService calendarioService,
                          UsuarioService usuarioService,
                          AuditoriaService auditoriaService) {
        this.citaService = citaService;
        this.agendaService = agendaService;
        this.calendarioService = calendarioService;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/agendar")
    public String formulario(@RequestParam(required = false) Integer odontologoId,
                             @RequestParam(required = false) Integer servicioId,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                             Model model) {

        CitaForm form = new CitaForm();
        form.setOdontologoId(odontologoId);
        form.setServicioId(servicioId);
        form.setFecha(fecha);

        prepararFormulario(model, form);
        return "citas/agendar";
    }

    @PostMapping("/agendar")
    public String agendar(@Valid @ModelAttribute("cita") CitaForm form,
                          BindingResult errores,
                          Principal principal,
                          HttpServletRequest request,
                          Model model,
                          RedirectAttributes redirect) {

        if (errores.hasErrors()) {
            prepararFormulario(model, form);
            return "citas/agendar";
        }

        Usuario paciente = usuarioActual(principal);

        try {
            Cita cita = citaService.agendar(form, paciente);

            auditoriaService.registrarCitaCreada(
                    paciente.getCorreo(),
                    request.getRemoteAddr(),
                    cita.getId(),
                    cita.getFecha(),
                    cita.getHora(),
                    cita.getServicio().getNombre());

            redirect.addFlashAttribute("mensaje",
                    "Tu cita quedó registrada. Te esperamos en la clínica.");
            return "redirect:/citas/mis-citas";

        } catch (HorarioNoDisponibleException | RecursoNoEncontradoException e) {
            model.addAttribute("error", e.getMessage());
            prepararFormulario(model, form);
            return "citas/agendar";
        }
    }

    @GetMapping("/mis-citas")
    public String misCitas(Principal principal, Model model) {
        Usuario paciente = usuarioActual(principal);
        List<Cita> citas = citaService.listarDelPaciente(paciente.getId());
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
                .collect(Collectors.toList());

        model.addAttribute("proximas", proximas);
        model.addAttribute("historial", historial);
        model.addAttribute("seccion", "mis-citas");
        return "citas/mis-citas";
    }

    @PostMapping("/cancelar")
    public String cancelar(@RequestParam Integer citaId,
                           Principal principal,
                           HttpServletRequest request,
                           RedirectAttributes redirect) {

        Usuario paciente = usuarioActual(principal);

        try {
            citaService.cancelar(citaId, paciente.getId());
            auditoriaService.registrarCitaCancelada(
                    paciente.getCorreo(), request.getRemoteAddr(), citaId);
            redirect.addFlashAttribute("mensaje", "La cita fue cancelada.");

        } catch (AccesoDenegadoException e) {
            auditoriaService.registrarAccesoDenegado(
                    paciente.getCorreo(), request.getRemoteAddr(),
                    "Intento de cancelar la cita " + citaId + " de otro paciente");
            throw e;

        } catch (OperacionNoPermitidaException | RecursoNoEncontradoException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/citas/mis-citas";
    }

    @GetMapping("/calendario")
    public String calendario(@RequestParam(required = false) String mes,
                             Principal principal,
                             Model model) {

        Usuario paciente = usuarioActual(principal);
        YearMonth mesConsultado = calendarioService.parsear(mes);
        List<Cita> citas = citaService.listarDelPaciente(paciente.getId());

        model.addAttribute("dias", calendarioService.construir(mesConsultado, citas));
        model.addAttribute("tituloMes", calendarioService.titulo(mesConsultado));
        model.addAttribute("mesAnterior", mesConsultado.minusMonths(1).toString());
        model.addAttribute("mesSiguiente", mesConsultado.plusMonths(1).toString());
        model.addAttribute("mesHoy", YearMonth.now().toString());
        model.addAttribute("modoAdmin", false);
        model.addAttribute("rutaBase", "/citas/calendario");
        model.addAttribute("seccion", "calendario");

        return "citas/calendario";
    }

    private void prepararFormulario(Model model, CitaForm form) {
        model.addAttribute("cita", form);
        model.addAttribute("odontologos", agendaService.listarOdontologosActivos());
        model.addAttribute("servicios", agendaService.listarServiciosActivos());
        model.addAttribute("hoyISO", LocalDate.now().toString());
        model.addAttribute("limiteISO", LocalDate.now().plusDays(90).toString());
        model.addAttribute("seccion", "agendar");

        if (form.getOdontologoId() != null
                && form.getServicioId() != null
                && form.getFecha() != null) {
            try {
                List<LocalTime> horarios = agendaService.horariosDisponibles(
                        form.getOdontologoId(), form.getFecha(), form.getServicioId());

                model.addAttribute("horarios", horarios);
                model.addAttribute("sinHorarios", horarios.isEmpty());
                model.addAttribute("odontologoConsulta",
                        agendaService.buscarOdontologoActivo(form.getOdontologoId()));
                model.addAttribute("servicioConsulta",
                        agendaService.buscarServicioActivo(form.getServicioId()));
                model.addAttribute("fechaConsulta", form.getFecha());

            } catch (RecursoNoEncontradoException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
    }

    private Usuario usuarioActual(Principal principal) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            throw new AccesoDenegadoException("La sesión no es válida");
        }
        return usuarioService.obtenerPorCorreo(principal.getName());
    }
}
