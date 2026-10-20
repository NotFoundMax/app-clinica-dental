package pe.edu.idat.appclinica.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos para reservar una cita.
 *
 * El paciente NO viaja en este formulario: se obtiene de la sesión.
 */
public class CitaForm {

    @NotNull(message = "Selecciona un odontólogo")
    private Integer odontologoId;

    @NotNull(message = "Selecciona un tratamiento")
    private Integer servicioId;

    @NotNull(message = "Selecciona una fecha")
    @FutureOrPresent(message = "La fecha no puede ser anterior a hoy")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotNull(message = "Selecciona un horario")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;

    @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
    private String motivo;

    public Integer getOdontologoId() {
        return odontologoId;
    }

    public void setOdontologoId(Integer odontologoId) {
        this.odontologoId = odontologoId;
    }

    public Integer getServicioId() {
        return servicioId;
    }

    public void setServicioId(Integer servicioId) {
        this.servicioId = servicioId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
