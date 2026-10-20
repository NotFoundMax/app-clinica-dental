package pe.edu.idat.appclinica.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.repository.UsuarioRepository;

/**
 * Une la tabla usuario con Spring Security.
 *
 * El username es el correo. La autoridad se construye como ROLE_ + nombre
 * del rol, que es el formato que espera hasRole("ADMIN").
 */
@Service
public class DetalleUsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DetalleUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));

        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "PACIENTE";

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPassword())
                .authorities("ROLE_" + rol)
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}
