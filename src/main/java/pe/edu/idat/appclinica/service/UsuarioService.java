package pe.edu.idat.appclinica.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.idat.appclinica.dto.RegistroForm;
import pe.edu.idat.appclinica.exception.RecursoNoEncontradoException;
import pe.edu.idat.appclinica.exception.UsuarioDuplicadoException;
import pe.edu.idat.appclinica.model.Rol;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.repository.RolRepository;
import pe.edu.idat.appclinica.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crea una cuenta de paciente.
     *
     * El rol se asigna en el servidor. El formulario no puede elegir ser ADMIN.
     * La contraseña se guarda como hash Argon2id.
     */
    @Transactional
    public Usuario registrar(RegistroForm form) {
        String correo = form.getCorreo().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new UsuarioDuplicadoException("Ese correo ya está registrado");
        }

        Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
                .orElseThrow(() -> new IllegalStateException("Falta el rol PACIENTE en la base de datos"));

        Usuario usuario = new Usuario();
        usuario.setNombre(form.getNombre().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(normalizarOpcional(form.getTelefono()));
        usuario.setPassword(passwordEncoder.encode(form.getPassword()));
        usuario.setRol(rolPaciente);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo);
    }

    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    }

    public Usuario obtenerPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
