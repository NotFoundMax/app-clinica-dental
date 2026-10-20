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
import pe.edu.idat.appclinica.dto.HorarioForm;
import pe.edu.idat.appclinica.dto.OdontologoForm;
import pe.edu.idat.appclinica.dto.ServicioForm;
import pe.edu.idat.appclinica.exception.DatosDuplicadosException;
import pe.edu.idat.appclinica.exception.OperacionNoPermitidaException;
import pe.edu.idat.appclinica.exception.RecursoNoEncontradoException;
import pe.edu.idat.appclinica.model.AuditoriaEvento;
import pe.edu.idat.appclinica.model.Cita;
import pe.edu.idat.appclinica.model.EstadoCita;
import pe.edu.idat.appclinica.model.Horario;
import pe.edu.idat.appclinica.model.Odontologo;
import pe.edu.idat.appclinica.model.Servicio;
import pe.edu.idat.appclinica.repository.AuditoriaEventoRepository;
import pe.edu.idat.appclinica.security.AuditoriaService;
import pe.edu.idat.appclinica.service.AgendaService;
import pe.edu.idat.appclinica.service.CalendarioService;
import pe.edu.idat.appclinica.service.CitaService;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Panel administrativo: citas, catálogo, horarios, auditoría y calendario.
 * Toda la ruta /admin/** está protegida con hasRole("ADMIN") en SecurityConfig.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CitaService citaService;
    private final AgendaService agendaService;
    private final CalendarioService calendarioService;
    private final AuditoriaService auditoriaService;
    private final AuditoriaEventoRepository auditoriaEventoRepository;

    public AdminController(CitaService citaService,
                           AgendaService agendaService,
                           CalendarioService calendarioService,
                           AuditoriaService auditoriaService,
                           AuditoriaEventoRepository auditoriaEventoRepository) {
        this.citaService = citaService;
        this.agendaService = agendaService;
        this.calendarioService = calendarioService;
        this.auditoriaService = auditoriaService;
        this.auditoriaEventoRepository = auditoriaEventoRepository;
    }

    // ------------------------------------------------------------------
    // Citas
    // ------------------------------------------------------------------

    @GetMapping("/citas")
    public String citas(@RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                        @RequestParam(required = false) String estado,
                        Model model) {

        EstadoCita estadoFiltro = parsearEstado(estado);

        model.addAttribute("citas", citaService.filtrar(fecha, estadoFiltro));
        model.addAttribute("fechaFiltro", fecha);
        model.addAttribute("estadoFiltro", estadoFiltro);
        model.addAttribute("estados", EstadoCita.values());
        model.addAttribute("pendientes", citaService.contarPorEstado(EstadoCita.PENDIENTE));
        model.addAttribute("confirmadas", citaService.contarPorEstado(EstadoCita.CONFIRMADA));
        model.addAttribute("citasHoy", citaService.contarCitasDeHoy());
        model.addAttribute("seccion", "admin-citas");

        return "admin/citas";
    }

    @PostMapping("/citas/estado")
    public String cambiarEstado(@RequestParam Integer citaId,
                                @RequestParam String estado,
                                Principal principal,
                                HttpServletRequest request,
                                RedirectAttributes redirect) {

        EstadoCita nuevo = parsearEstado(estado);
        if (nuevo == null) {
            redirect.addFlashAttribute("error", "Estado no válido.");
            return "redirect:/admin/citas";
        }

        try {
            Cita cita = citaService.buscar(citaId);
            String anterior = cita.getEstado().getEtiqueta();

            citaService.cambiarEstado(citaId, nuevo);

            auditoriaService.registrarCambioEstado(
                    principal != null ? principal.getName() : null,
                    request.getRemoteAddr(),
                    citaId,
                    anterior,
                    nuevo.getEtiqueta());

            redirect.addFlashAttribute("mensaje", "El estado de la cita fue actualizado.");

        } catch (OperacionNoPermitidaException | RecursoNoEncontradoException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/citas";
    }

    // ------------------------------------------------------------------
    // Odontólogos
    // ------------------------------------------------------------------

    @GetMapping("/odontologos")
    public String odontologos(Model model) {
        model.addAttribute("odontologos", agendaService.listarTodosLosOdontologos());
        if (!model.containsAttribute("odontologoForm")) {
            model.addAttribute("odontologoForm", new OdontologoForm());
        }
        model.addAttribute("seccion", "admin-odontologos");
        return "admin/odontologos";
    }

    @PostMapping("/odontologos")
    public String crearOdontologo(@Valid @ModelAttribute("odontologoForm") OdontologoForm form,
                                  BindingResult errores,
                                  Principal principal,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes redirect) {

        if (errores.hasErrors()) {
            model.addAttribute("odontologos", agendaService.listarTodosLosOdontologos());
            model.addAttribute("seccion", "admin-odontologos");
            return "admin/odontologos";
        }

        try {
            Odontologo odontologo = agendaService.crearOdontologo(form);

            auditoriaService.registrarAccionAdmin(
                    principal != null ? principal.getName() : null,
                    request.getRemoteAddr(),
                    "ODONTOLOGO_CREADO",
                    "odontologoId=" + odontologo.getId()
                            + ", correo=" + odontologo.getCorreo());

            redirect.addFlashAttribute("mensaje", "Odontólogo agregado al catálogo.");
            return "redirect:/admin/odontologos";

        } catch (DatosDuplicadosException e) {
            errores.rejectValue("correo", "correo.duplicado", e.getMessage());
            model.addAttribute("odontologos", agendaService.listarTodosLosOdontologos());
            model.addAttribute("seccion", "admin-odontologos");
            return "admin/odontologos";
        }
    }

    @PostMapping("/odontologos/estado")
    public String alternarOdontologo(@RequestParam Integer odontologoId,
                                     Principal principal,
                                     HttpServletRequest request,
                                     RedirectAttributes redirect) {
        try {
            Odontologo odontologo = agendaService.buscarOdontologo(odontologoId);
            agendaService.alternarOdontologo(odontologoId);

            boolean activo = Boolean.TRUE.equals(
                    agendaService.buscarOdontologo(odontologoId).getActivo());

            auditoriaService.registrarAccionAdmin(
                    principal != null ? principal.getName() : null,
                    request.getRemoteAddr(),
                    "ODONTOLOGO_ESTADO",
                    "odontologoId=" + odontologo.getId() + ", activo=" + activo);

            redirect.addFlashAttribute("mensaje",
                    activo ? "Odontólogo activado." : "Odontólogo desactivado.");

        } catch (RecursoNoEncontradoException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/odontologos";
    }

    // ------------------------------------------------------------------
    // Servicios
    // ------------------------------------------------------------------

    @GetMapping("/servicios")
    public String servicios(Model model) {
        model.addAttribute("servicios", agendaService.listarTodosLosServicios());
        if (!model.containsAttribute("servicioForm")) {
            model.addAttribute("servicioForm", new ServicioForm());
        }
        model.addAttribute("seccion", "admin-servicios");
        return "admin/servicios";
    }

    @PostMapping("/servicios")
    public String crearServicio(@Valid @ModelAttribute("servicioForm") ServicioForm form,
                                BindingResult errores,
                                Principal principal,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes redirect) {

        if (errores.hasErrors()) {
            model.addAttribute("servicios", agendaService.listarTodosLosServicios());
            model.addAttribute("seccion", "admin-servicios");
            return "admin/servicios";
        }

        try {
            Servicio servicio = agendaService.crearServicio(form);

            auditoriaService.registrarAccionAdmin(
                    principal != null ? principal.getName() : null,
                    request.getRemoteAddr(),
                    "SERVICIO_CREADO",
                    "servicioId=" + servicio.getId() + ", nombre=" + servicio.getNombre());

            redirect.addFlashAttribute("mensaje", "Tratamiento agregado al catálogo.");
            return "redirect:/admin/servicios";

        } catch (DatosDuplicadosException e) {
            errores.rejectValue("nombre", "nombre.duplicado", e.getMessage());
            model.addAttribute("servicios", agendaService.listarTodosLosServicios());
            model.addAttribute("seccion", "admin-servicios");
            return "admin/servicios";
        }
    }

    @PostMapping("/servicios/estado")
    public String alternarServicio(@RequestParam Integer servicioId,
                                   Principal principal,
                                   HttpServletRequest request,
                                   RedirectAttributes redirect) {
        try {
            Servicio servicio = agendaService.buscarServicio(servicioId);
            agendaService.alternarServicio(servicioId);

            boolean activo = Boolean.TRUE.equals(
                    agendaService.buscarServicio(servicioId).getActivo());

            auditoriaService.registrarAccionAdmin(
                    principal != null ? principal.getName() : null,
                    request.getRemoteAddr(),
                    "SERVICIO_ESTADO",
                    "servicioId=" + servicio.getId() + ", activo=" + activo);

            redirect.addFlashAttribute("mensaje",
                    activo ? "Tratamiento activado." : "Tratamiento desactivado.");

        } catch (RecursoNoEncontradoException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/servicios";
    }

    // ------------------------------------------------------------------
    // Horarios
    // ------------------------------------------------------------------

    @GetMapping("/horarios")
    public String horarios(@RequestParam(required = false) Integer odontologoId,
                           Model model) {
        cargarHorariosAdmin(model, odontologoId);
        return "admin/horarios";
    }

    @PostMapping("/horarios")
    public String crearHorario(@Valid @ModelAttribute("horarioForm") HorarioForm form,
                               BindingResult errores,
                               Principal principal,
                               HttpServletRequest request,
                               Model model,
                               RedirectAttributes redirect) {

        if (errores.hasErrors()) {
            cargarHorariosAdmin(model, form.getOdontologoId());
            return "admin/horarios";
        }

        try {
            Horario horario = agendaService.crearHorario(form);

            auditoriaService.registrarAccionAdmin(
                    principal != null ? principal.getName() : null,
                    request.getRemoteAddr(),
                    "HORARIO_CREADO",
                    "horarioId=" + horario.getId()
                            + ", odontologoId=" + horario.getOdontologo().getId()
                            + ", dia=" + horario.getDiaSemana());

            redirect.addFlashAttribute("mensaje", "Bloque de horario agregado.");
            return "redirect:/admin/horarios?odontologoId=" + form.getOdontologoId();

        } catch (DatosDuplicadosException e) {
            errores.reject("horario.duplicado", e.getMessage());
            cargarHorariosAdmin(model, form.getOdontologoId());
            return "admin/horarios";
        }
    }

    @PostMapping("/horarios/eliminar")
    public String eliminarHorario(@RequestParam Integer horarioId,
                                  @RequestParam(required = false) Integer odontologoId,
                                  Principal principal,
                                  HttpServletRequest request,
                                  RedirectAttributes redirect) {
        try {
            agendaService.eliminarHorario(horarioId);

            auditoriaService.registrarAccionAdmin(
                    principal != null ? principal.getName() : null,
                    request.getRemoteAddr(),
                    "HORARIO_ELIMINADO",
                    "horarioId=" + horarioId);

            redirect.addFlashAttribute("mensaje", "Bloque de horario eliminado.");

        } catch (RecursoNoEncontradoException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        if (odontologoId != null) {
            return "redirect:/admin/horarios?odontologoId=" + odontologoId;
        }
        return "redirect:/admin/horarios";
    }

    private void cargarHorariosAdmin(Model model, Integer odontologoId) {
        List<Odontologo> odontologos = agendaService.listarTodosLosOdontologos();

        Odontologo seleccionado = null;
        if (odontologoId != null) {
            try {
                seleccionado = agendaService.buscarOdontologo(odontologoId);
            } catch (RecursoNoEncontradoException ignored) {
                seleccionado = null;
            }
        }
        if (seleccionado == null && !odontologos.isEmpty()) {
            seleccionado = odontologos.get(0);
        }

        model.addAttribute("odontologos", odontologos);
        model.addAttribute("odontologoSeleccionado", seleccionado);
        model.addAttribute("horarios", seleccionado == null
                ? List.of()
                : agendaService.listarHorarios(seleccionado.getId()));
        model.addAttribute("seccion", "admin-horarios");

        if (!model.containsAttribute("horarioForm")) {
            HorarioForm form = new HorarioForm();
            if (seleccionado != null) {
                form.setOdontologoId(seleccionado.getId());
            }
            model.addAttribute("horarioForm", form);
        }
    }

    // ------------------------------------------------------------------
    // Auditoría
    // ------------------------------------------------------------------

    @GetMapping("/auditoria")
    public String auditoria(@RequestParam(required = false) String accion,
                            Model model) {

        List<AuditoriaEvento> eventos;
        if (accion != null && !accion.isBlank()) {
            eventos = auditoriaEventoRepository.findTop200ByAccionOrderByFechaDesc(accion);
        } else {
            eventos = auditoriaEventoRepository.findTop200ByOrderByFechaDesc();
        }

        model.addAttribute("eventos", eventos);
        model.addAttribute("acciones", auditoriaEventoRepository.findAccionesDistintas());
        model.addAttribute("accionFiltro", accion);
        model.addAttribute("seccion", "admin-auditoria");

        return "admin/auditoria";
    }

    // ------------------------------------------------------------------
    // Calendario general
    // ------------------------------------------------------------------

    @GetMapping("/calendario")
    public String calendario(@RequestParam(required = false) String mes,
                             Model model) {

        YearMonth mesConsultado = calendarioService.parsear(mes);

        model.addAttribute("dias", calendarioService.construir(
                mesConsultado, citaService.listarTodas()));
        model.addAttribute("tituloMes", calendarioService.titulo(mesConsultado));
        model.addAttribute("mesAnterior", mesConsultado.minusMonths(1).toString());
        model.addAttribute("mesSiguiente", mesConsultado.plusMonths(1).toString());
        model.addAttribute("mesHoy", YearMonth.now().toString());
        model.addAttribute("modoAdmin", true);
        model.addAttribute("rutaBase", "/admin/calendario");
        model.addAttribute("seccion", "admin-calendario");

        return "citas/calendario";
    }

    private EstadoCita parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoCita.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
