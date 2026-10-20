package pe.edu.idat.appclinica.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.idat.appclinica.dto.CitaForm;
import pe.edu.idat.appclinica.exception.AccesoDenegadoException;
import pe.edu.idat.appclinica.exception.HorarioNoDisponibleException;
import pe.edu.idat.appclinica.exception.OperacionNoPermitidaException;
import pe.edu.idat.appclinica.exception.RecursoNoEncontradoException;
import pe.edu.idat.appclinica.model.Cita;
import pe.edu.idat.appclinica.model.EstadoCita;
import pe.edu.idat.appclinica.model.Odontologo;
import pe.edu.idat.appclinica.model.Servicio;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.repository.CitaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final AgendaService agendaService;

    public CitaService(CitaRepository citaRepository, AgendaService agendaService) {
        this.citaRepository = citaRepository;
        this.agendaService = agendaService;
    }

    /**
     * Reserva una cita.
     *
     * El paciente llega desde la sesión. El odontólogo, el tratamiento y la
     * hora se vuelven a validar en el servidor: si alguien altera el HTML,
     * la operación falla.
     */
    @Transactional
    public Cita agendar(CitaForm form, Usuario paciente) {
        if (form.getFecha() == null || form.getHora() == null) {
            throw new HorarioNoDisponibleException("Selecciona una fecha y un horario");
        }

        Odontologo odontologo = agendaService.buscarOdontologoActivo(form.getOdontologoId());
        Servicio servicio = agendaService.buscarServicioActivo(form.getServicioId());

        LocalDate hoy = LocalDate.now();
        if (form.getFecha().isBefore(hoy)) {
            throw new HorarioNoDisponibleException("No se puede reservar una fecha pasada");
        }
        if (form.getFecha().isAfter(hoy.plusDays(90))) {
            throw new HorarioNoDisponibleException("Solo se puede reservar con 90 días de anticipación");
        }

        List<LocalTime> disponibles = agendaService.horariosDisponibles(
                odontologo.getId(), form.getFecha(), servicio.getId());

        if (!disponibles.contains(form.getHora())) {
            throw new HorarioNoDisponibleException(
                    "Ese horario ya no está disponible. Elige otro, por favor.");
        }

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setOdontologo(odontologo);
        cita.setServicio(servicio);
        cita.setFecha(form.getFecha());
        cita.setHora(form.getHora());
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setMotivo(form.getMotivo() == null || form.getMotivo().isBlank()
                ? null : form.getMotivo().trim());
        cita.setFechaCreacion(LocalDateTime.now());

        return citaRepository.save(cita);
    }

    /**
     * Cancela una cita del paciente conectado.
     *
     * Verifica pertenencia (anti-IDOR) y que la cita todavía se pueda cancelar.
     */
    @Transactional
    public void cancelar(Integer citaId, Integer pacienteId) {
        Cita cita = buscar(citaId);

        if (cita.getPaciente() == null || !cita.getPaciente().getId().equals(pacienteId)) {
            throw new AccesoDenegadoException("Esa cita no pertenece a tu cuenta");
        }

        if (!cita.isPuedeCancelar()) {
            throw new OperacionNoPermitidaException(
                    "Esta cita ya no se puede cancelar. Comunícate con la clínica.");
        }

        cita.setEstado(EstadoCita.CANCELADA);
        cita.setFechaActualizacion(LocalDateTime.now());
        citaRepository.save(cita);
    }

    /**
     * Cambia el estado desde el panel administrativo.
     * Solo se permiten transiciones coherentes.
     */
    @Transactional
    public void cambiarEstado(Integer citaId, EstadoCita nuevoEstado) {
        Cita cita = buscar(citaId);

        boolean permitido = switch (cita.getEstado()) {
            case PENDIENTE -> nuevoEstado == EstadoCita.CONFIRMADA
                    || nuevoEstado == EstadoCita.CANCELADA;
            case CONFIRMADA -> nuevoEstado == EstadoCita.ATENDIDA
                    || nuevoEstado == EstadoCita.CANCELADA;
            case ATENDIDA, CANCELADA -> false;
        };

        if (!permitido) {
            throw new OperacionNoPermitidaException(
                    "No se puede pasar de " + cita.getEstado().getEtiqueta()
                            + " a " + nuevoEstado.getEtiqueta());
        }

        cita.setEstado(nuevoEstado);
        cita.setFechaActualizacion(LocalDateTime.now());
        citaRepository.save(cita);
    }

    public Cita buscar(Integer id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cita no existe"));
    }

    public List<Cita> listarDelPaciente(Integer pacienteId) {
        return citaRepository.findByPacienteIdOrderByFechaDescHoraDesc(pacienteId);
    }

    public List<Cita> listarTodas() {
        return citaRepository.findAllByOrderByFechaDescHoraAsc();
    }

    public List<Cita> filtrar(LocalDate fecha, EstadoCita estado) {
        if (fecha != null) {
            return citaRepository.findByFechaOrderByHoraAsc(fecha);
        }
        if (estado != null) {
            return citaRepository.findByEstadoOrderByFechaAscHoraAsc(estado);
        }
        return citaRepository.findAllByOrderByFechaDescHoraAsc();
    }

    public long contarPorEstado(EstadoCita estado) {
        return citaRepository.countByEstado(estado);
    }

    public long contarCitasDeHoy() {
        return citaRepository.countByFechaAndEstadoNot(LocalDate.now(), EstadoCita.CANCELADA);
    }
}
