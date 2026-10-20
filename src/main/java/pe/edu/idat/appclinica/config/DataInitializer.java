package pe.edu.idat.appclinica.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.idat.appclinica.model.Horario;
import pe.edu.idat.appclinica.model.Odontologo;
import pe.edu.idat.appclinica.model.Rol;
import pe.edu.idat.appclinica.model.Servicio;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.repository.HorarioRepository;
import pe.edu.idat.appclinica.repository.OdontologoRepository;
import pe.edu.idat.appclinica.repository.RolRepository;
import pe.edu.idat.appclinica.repository.ServicioRepository;
import pe.edu.idat.appclinica.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * Datos mínimos para que la aplicación funcione al primer arranque.
 *
 * Idempotente: si los datos ya existen, no los duplica.
 * No imprime contraseñas en el log.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final OdontologoRepository odontologoRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioRepository horarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository,
                           UsuarioRepository usuarioRepository,
                           OdontologoRepository odontologoRepository,
                           ServicioRepository servicioRepository,
                           HorarioRepository horarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.odontologoRepository = odontologoRepository;
        this.servicioRepository = servicioRepository;
        this.horarioRepository = horarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Rol admin = rol("ADMIN");
        Rol paciente = rol("PACIENTE");
        rol("ODONTOLOGO");

        crearUsuarioSiNoExiste("Administrador de la clínica",
                "admin@dentalcare.pe", "Admin123!", admin);
        crearUsuarioSiNoExiste("Paciente de demostración",
                "paciente@demo.pe", "Paciente123!", paciente);

        if (odontologoRepository.count() == 0) {
            crearOdontologos();
        }
        if (servicioRepository.count() == 0) {
            crearServicios();
        }
        if (horarioRepository.count() == 0) {
            crearHorarios();
        }

        log.info("Datos iniciales verificados. Usuarios, catálogo y agenda listos.");
    }

    private Rol rol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> rolRepository.save(new Rol(nombre)));
    }

    private void crearUsuarioSiNoExiste(String nombre, String correo,
                                        String passwordPlano, Rol rol) {
        String correoNormalizado = correo.toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreoIgnoreCase(correoNormalizado)) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correoNormalizado);
        usuario.setPassword(passwordEncoder.encode(passwordPlano));
        usuario.setRol(rol);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    private void crearOdontologos() {
        odontologoRepository.save(odontologo(
                "Valeria", "Ríos Mendoza", "Odontología general",
                "valeria.rios@dentalcare.pe", "987 654 321", "C-01"));
        odontologoRepository.save(odontologo(
                "Mateo", "Salazar Paredes", "Ortodoncia",
                "mateo.salazar@dentalcare.pe", "987 654 322", "C-02"));
        odontologoRepository.save(odontologo(
                "Camila", "Torres León", "Endodoncia",
                "camila.torres@dentalcare.pe", "987 654 323", "C-03"));
        odontologoRepository.save(odontologo(
                "Diego", "Fernández Rojas", "Odontopediatría",
                "diego.fernandez@dentalcare.pe", "987 654 324", "C-04"));
    }

    private Odontologo odontologo(String nombres, String apellidos, String especialidad,
                                  String correo, String telefono, String consultorio) {
        Odontologo odontologo = new Odontologo();
        odontologo.setNombres(nombres);
        odontologo.setApellidos(apellidos);
        odontologo.setEspecialidad(especialidad);
        odontologo.setCorreo(correo);
        odontologo.setTelefono(telefono);
        odontologo.setConsultorio(consultorio);
        odontologo.setActivo(true);
        return odontologo;
    }

    private void crearServicios() {
        servicioRepository.save(servicio("Consulta general",
                "Evaluación inicial y plan de tratamiento.",
                30, "60.00"));
        servicioRepository.save(servicio("Limpieza dental",
                "Profilaxis y eliminación de sarro.",
                45, "120.00"));
        servicioRepository.save(servicio("Blanqueamiento",
                "Blanqueamiento en consultorio con control profesional.",
                60, "350.00"));
        servicioRepository.save(servicio("Control de ortodoncia",
                "Ajuste y seguimiento del tratamiento de ortodoncia.",
                30, "150.00"));
        servicioRepository.save(servicio("Endodoncia",
                "Tratamiento de conducto con aislamiento absoluto.",
                90, "450.00"));
        servicioRepository.save(servicio("Extracción simple",
                "Extracción de pieza dental con indicación profesional.",
                45, "180.00"));
    }

    private Servicio servicio(String nombre, String descripcion,
                              int duracion, String precio) {
        Servicio servicio = new Servicio();
        servicio.setNombre(nombre);
        servicio.setDescripcion(descripcion);
        servicio.setDuracionMinutos(duracion);
        servicio.setPrecio(new BigDecimal(precio));
        servicio.setActivo(true);
        return servicio;
    }

    /**
     * Lunes a viernes de 08:00 a 13:00 y de 15:00 a 19:00.
     * Sábado solo de 08:00 a 13:00.
     */
    private void crearHorarios() {
        List<Odontologo> odontologos = odontologoRepository.findAllByOrderByApellidosAscNombresAsc();

        for (Odontologo odontologo : odontologos) {
            for (int dia = 1; dia <= 5; dia++) {
                horarioRepository.save(horario(odontologo, dia, "08:00", "13:00"));
                horarioRepository.save(horario(odontologo, dia, "15:00", "19:00"));
            }
            horarioRepository.save(horario(odontologo, 6, "08:00", "13:00"));
        }
    }

    private Horario horario(Odontologo odontologo, int dia,
                            String inicio, String fin) {
        Horario horario = new Horario();
        horario.setOdontologo(odontologo);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(LocalTime.parse(inicio));
        horario.setHoraFin(LocalTime.parse(fin));
        horario.setActivo(true);
        return horario;
    }
}
