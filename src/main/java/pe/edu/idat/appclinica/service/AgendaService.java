package pe.edu.idat.appclinica.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.idat.appclinica.dto.HorarioForm;
import pe.edu.idat.appclinica.dto.OdontologoForm;
import pe.edu.idat.appclinica.dto.ServicioForm;
import pe.edu.idat.appclinica.exception.DatosDuplicadosException;
import pe.edu.idat.appclinica.exception.RecursoNoEncontradoException;
import pe.edu.idat.appclinica.model.Cita;
import pe.edu.idat.appclinica.model.EstadoCita;
import pe.edu.idat.appclinica.model.Horario;
import pe.edu.idat.appclinica.model.Odontologo;
import pe.edu.idat.appclinica.model.Servicio;
import pe.edu.idat.appclinica.repository.CitaRepository;
import pe.edu.idat.appclinica.repository.HorarioRepository;
import pe.edu.idat.appclinica.repository.OdontologoRepository;
import pe.edu.idat.appclinica.repository.ServicioRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Catálogo y agenda de la clínica.
 *
 * La disponibilidad se calcula en el servidor:
 *   - Se leen los bloques de atención del día.
 *   - Se generan inicios cada 30 minutos.
 *   - Se descartan los que no caben completos en el bloque.
 *   - Se descartan los que se cruzan con citas activas.
 *   - Se descartan los que ya pasaron si la fecha es hoy.
 */
@Service
public class AgendaService {

    private final OdontologoRepository odontologoRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioRepository horarioRepository;
    private final CitaRepository citaRepository;

    public AgendaService(OdontologoRepository odontologoRepository,
                         ServicioRepository servicioRepository,
                         HorarioRepository horarioRepository,
                         CitaRepository citaRepository) {
        this.odontologoRepository = odontologoRepository;
        this.servicioRepository = servicioRepository;
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
    }

    public List<Odontologo> listarOdontologosActivos() {
        return odontologoRepository.findByActivoTrueOrderByApellidosAscNombresAsc();
    }

    public List<Odontologo> listarTodosLosOdontologos() {
        return odontologoRepository.findAllByOrderByApellidosAscNombresAsc();
    }

    public List<Servicio> listarServiciosActivos() {
        return servicioRepository.findByActivoTrueOrderByNombreAsc();
    }

    public List<Servicio> listarTodosLosServicios() {
        return servicioRepository.findAllByOrderByNombreAsc();
    }

    public List<Horario> listarHorarios(Integer odontologoId) {
        return horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(odontologoId);
    }

    public Odontologo buscarOdontologo(Integer id) {
        return odontologoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El odontólogo no existe"));
    }

    public Odontologo buscarOdontologoActivo(Integer id) {
        Odontologo odontologo = buscarOdontologo(id);
        if (!Boolean.TRUE.equals(odontologo.getActivo())) {
            throw new RecursoNoEncontradoException("El odontólogo no está disponible");
        }
        return odontologo;
    }

    public Servicio buscarServicio(Integer id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El tratamiento no existe"));
    }

    public Servicio buscarServicioActivo(Integer id) {
        Servicio servicio = buscarServicio(id);
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new RecursoNoEncontradoException("El tratamiento no está disponible");
        }
        return servicio;
    }

    @Transactional
    public Odontologo crearOdontologo(OdontologoForm form) {
        String correo = form.getCorreo().trim().toLowerCase(Locale.ROOT);
        if (odontologoRepository.existsByCorreoIgnoreCase(correo)) {
            throw new DatosDuplicadosException("Ya existe un odontólogo con ese correo");
        }

        Odontologo odontologo = new Odontologo();
        odontologo.setNombres(form.getNombres().trim());
        odontologo.setApellidos(form.getApellidos().trim());
        odontologo.setEspecialidad(form.getEspecialidad().trim());
        odontologo.setCorreo(correo);
        odontologo.setTelefono(normalizarOpcional(form.getTelefono()));
        odontologo.setConsultorio(normalizarOpcional(form.getConsultorio()));
        odontologo.setActivo(true);

        return odontologoRepository.save(odontologo);
    }

    @Transactional
    public void alternarOdontologo(Integer id) {
        Odontologo odontologo = buscarOdontologo(id);
        odontologo.setActivo(!Boolean.TRUE.equals(odontologo.getActivo()));
        odontologoRepository.save(odontologo);
    }

    @Transactional
    public Servicio crearServicio(ServicioForm form) {
        String nombre = form.getNombre().trim();
        if (servicioRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DatosDuplicadosException("Ya existe un tratamiento con ese nombre");
        }

        Servicio servicio = new Servicio();
        servicio.setNombre(nombre);
        servicio.setDescripcion(normalizarOpcional(form.getDescripcion()));
        servicio.setDuracionMinutos(form.getDuracionMinutos());
        servicio.setPrecio(form.getPrecio());
        servicio.setActivo(true);

        return servicioRepository.save(servicio);
    }

    @Transactional
    public void alternarServicio(Integer id) {
        Servicio servicio = buscarServicio(id);
        servicio.setActivo(!Boolean.TRUE.equals(servicio.getActivo()));
        servicioRepository.save(servicio);
    }

    @Transactional
    public Horario crearHorario(HorarioForm form) {
        Odontologo odontologo = buscarOdontologo(form.getOdontologoId());

        boolean seCruza = horarioRepository
                .existsByOdontologoIdAndDiaSemanaAndActivoTrueAndHoraInicioLessThanAndHoraFinGreaterThan(
                        odontologo.getId(),
                        form.getDiaSemana(),
                        form.getHoraFin(),
                        form.getHoraInicio());

        if (seCruza) {
            throw new DatosDuplicadosException(
                    "Ese bloque se cruza con otro horario del mismo odontólogo");
        }

        Horario horario = new Horario();
        horario.setOdontologo(odontologo);
        horario.setDiaSemana(form.getDiaSemana());
        horario.setHoraInicio(form.getHoraInicio());
        horario.setHoraFin(form.getHoraFin());
        horario.setActivo(true);

        return horarioRepository.save(horario);
    }

    @Transactional
    public void eliminarHorario(Integer id) {
        if (!horarioRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("El bloque de horario no existe");
        }
        horarioRepository.deleteById(id);
    }

    /**
     * Horarios de inicio disponibles para un odontólogo, una fecha y un
     * tratamiento. Devuelve una lista vacía si no hay atención ese día.
     */
    public List<LocalTime> horariosDisponibles(Integer odontologoId,
                                               LocalDate fecha,
                                               Integer servicioId) {
        Odontologo odontologo = buscarOdontologoActivo(odontologoId);
        Servicio servicio = buscarServicioActivo(servicioId);

        if (fecha == null) {
            return List.of();
        }

        LocalDate hoy = LocalDate.now();
        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(90))) {
            return List.of();
        }

        List<Horario> bloques = horarioRepository
                .findByOdontologoIdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
                        odontologo.getId(), fecha.getDayOfWeek().getValue());

        if (bloques.isEmpty()) {
            return List.of();
        }

        List<Cita> citasActivas = citaRepository.findByOdontologoIdAndFechaAndEstadoNot(
                odontologo.getId(), fecha, EstadoCita.CANCELADA);

        List<Intervalo> ocupados = citasActivas.stream()
                .filter(c -> c.getHora() != null && c.getServicio() != null)
                .map(c -> new Intervalo(
                        c.getHora(),
                        c.getHora().plusMinutes(c.getServicio().getDuracionMinutos())))
                .toList();

        int duracion = servicio.getDuracionMinutos();
        LocalTime ahora = LocalTime.now();
        Set<LocalTime> disponibles = new TreeSet<>();

        for (Horario bloque : bloques) {
            LocalTime cursor = bloque.getHoraInicio();

            while (!cursor.plusMinutes(duracion).isAfter(bloque.getHoraFin())) {
                LocalTime inicioSlot = cursor;
                LocalTime fin = cursor.plusMinutes(duracion);
                boolean yaPaso = fecha.equals(hoy) && !cursor.isAfter(ahora);
                boolean seCruza = ocupados.stream()
                        .anyMatch(i -> inicioSlot.isBefore(i.fin()) && i.inicio().isBefore(fin));

                if (!yaPaso && !seCruza) {
                    disponibles.add(cursor);
                }
                cursor = cursor.plusMinutes(30);
            }
        }

        return new ArrayList<>(disponibles);
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private record Intervalo(LocalTime inicio, LocalTime fin) {
    }
}
