package pe.edu.idat.appclinica.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

public class HorarioForm {

    @NotNull(message = "Selecciona un odontólogo")
    private Integer odontologoId;

    @NotNull(message = "Selecciona un día")
    @Min(value = 1, message = "El día debe estar entre lunes y domingo")
    @Max(value = 7, message = "El día debe estar entre lunes y domingo")
    private Integer diaSemana;

    @NotNull(message = "Indica la hora de inicio")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "Indica la hora de fin")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    @AssertTrue(message = "La hora de fin debe ser mayor que la hora de inicio")
    public boolean isRangoValido() {
        if (horaInicio == null || horaFin == null) {
            return true;
        }
        return horaFin.isAfter(horaInicio);
    }

    public Integer getOdontologoId() {
        return odontologoId;
    }

    public void setOdontologoId(Integer odontologoId) {
        this.odontologoId = odontologoId;
    }

    public Integer getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(Integer diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }
}
