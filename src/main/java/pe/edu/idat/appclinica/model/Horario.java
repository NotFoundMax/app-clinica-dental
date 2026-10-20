package pe.edu.idat.appclinica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Bloque de atención semanal de un odontólogo.
 *
 * diaSemana sigue el estándar ISO: 1 = lunes, 7 = domingo.
 * Un odontólogo puede tener varios bloques por día (por ejemplo,
 * mañana y tarde) pero no pueden superponerse.
 */
@Entity
@Table(
        name = "horario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_horario",
                columnNames = {"odontologo_id", "dia_semana", "hora_inicio", "hora_fin"}
        )
)
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Odontologo odontologo;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Odontologo getOdontologo() {
        return odontologo;
    }

    public void setOdontologo(Odontologo odontologo) {
        this.odontologo = odontologo;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getDiaEtiqueta() {
        if (diaSemana == null) {
            return "";
        }
        return switch (diaSemana) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Día " + diaSemana;
        };
    }

    public String getDuracionHoras() {
        if (horaInicio == null || horaFin == null) {
            return "";
        }
        long minutos = Duration.between(horaInicio, horaFin).toMinutes();
        long horas = minutos / 60;
        long resto = minutos % 60;
        if (resto == 0) {
            return horas + " h";
        }
        return horas + " h " + resto + " min";
    }

    @Override
    public String toString() {
        return "Horario{id=" + id + ", dia=" + diaSemana + ", " + horaInicio + "-" + horaFin + "}";
    }
}
