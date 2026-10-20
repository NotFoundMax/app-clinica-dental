package pe.edu.idat.appclinica.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.idat.appclinica.model.AuditoriaEvento;
import pe.edu.idat.appclinica.repository.AuditoriaEventoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auditoría de la aplicación.
 *
 * Cada evento se escribe en dos lugares:
 *   1. Un renglón JSON en el log (para grep y para un SIEM).
 *   2. La tabla auditoria_evento (para consultarlo desde el panel admin).
 *
 * Regla de oro: al log y a la tabla solo llega información enmascarada.
 * Nunca contraseñas, hashes, tokens, correos completos, teléfonos completos
 * ni el texto libre del motivo de la cita.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger("AUDITORIA");

    private final AuditoriaEventoRepository auditoriaRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public AuditoriaService(AuditoriaEventoRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String accion, String usuario, String ip, String detalle) {
        LocalDateTime fecha = LocalDateTime.now();
        String usuarioEnmascarado = enmascarar(usuario);
        String detalleEnmascarado = enmascarar(detalle);

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("fecha", fecha);
        evento.put("accion", accion);
        evento.put("usuario", usuarioEnmascarado);
        evento.put("ip", ip);
        evento.put("detalle", detalleEnmascarado);

        try {
            log.info(objectMapper.writeValueAsString(evento));
        } catch (Exception e) {
            log.warn("No se pudo formatear el evento de auditoría: accion={}", accion);
        }

        try {
            AuditoriaEvento registro = new AuditoriaEvento();
            registro.setFecha(fecha);
            registro.setAccion(accion);
            registro.setUsuario(usuarioEnmascarado);
            registro.setIp(ip);
            registro.setDetalle(detalleEnmascarado);
            auditoriaRepository.save(registro);
        } catch (Exception e) {
            // La auditoría no debe tumbar la operación de negocio.
            log.warn("No se pudo persistir el evento de auditoría: accion={}", accion);
        }
    }

    public void registrarRegistroExitoso(String correo, String ip) {
        registrar("REGISTRO", correo, ip, "Cuenta de paciente creada");
    }

    public void registrarIntentoRegistroDuplicado(String correo, String ip) {
        registrar("REGISTRO_DUPLICADO", correo, ip, "Correo ya existente");
    }

    public void registrarCaptchaFallido(String correo, String ip) {
        registrar("CAPTCHA_FALLIDO", correo, ip, "Código de verificación inválido o expirado");
    }

    public void registrarLoginExitoso(String correo, String ip) {
        registrar("LOGIN_EXITOSO", correo, ip, null);
    }

    public void registrarIntentoLoginFallido(String correo, String ip) {
        registrar("LOGIN_FALLIDO", correo, ip, "Credenciales incorrectas");
    }

    public void registrarLogout(String correo, String ip) {
        registrar("LOGOUT", correo, ip, null);
    }

    public void registrarCitaCreada(String correo, String ip, Integer citaId,
                                    LocalDate fecha, LocalTime hora, String servicio) {
        registrar("CITA_CREADA", correo, ip,
                "citaId=" + citaId + ", fecha=" + fecha + ", hora=" + hora
                        + ", servicio=" + servicio);
    }

    public void registrarCitaCancelada(String correo, String ip, Integer citaId) {
        registrar("CITA_CANCELADA", correo, ip, "citaId=" + citaId);
    }

    public void registrarCambioEstado(String correo, String ip, Integer citaId,
                                      String estadoAnterior, String estadoNuevo) {
        registrar("CITA_ESTADO", correo, ip,
                "citaId=" + citaId + ", de=" + estadoAnterior + ", a=" + estadoNuevo);
    }

    public void registrarAccesoDenegado(String correo, String ip, String detalle) {
        registrar("ACCESO_DENEGADO", correo, ip, detalle);
    }

    public void registrarAccionAdmin(String correo, String ip, String accion, String detalle) {
        registrar(accion, correo, ip, detalle);
    }

    /**
     * Enmascara correos y secuencias numéricas largas.
     *
     * Ejemplos:
     *   magc1988@gmail.com      -> ma***@gmail.com
     *   Llamó al 987654321      -> Llamó al ************
     */
    String enmascarar(String texto) {
        if (texto == null) {
            return null;
        }
        String resultado = texto.replaceAll(
                "([A-Za-z0-9._%+-]{2})[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+)",
                "$1***$2");
        resultado = resultado.replaceAll("\\d{6,}", "************");
        return resultado;
    }
}
