package pe.edu.idat.appclinica.dto;

import pe.edu.idat.appclinica.model.Cita;

import java.time.LocalDate;
import java.util.List;

/**
 * Celda del calendario mensual.
 *
 * Se construye en el servidor para que Thymeleaf solo tenga que recorrer
 * una lista de 42 días, sin lógica de fechas en la vista.
 */
public class DiaCalendario {

    private final LocalDate fecha;
    private final boolean delMes;
    private final boolean hoy;
    private final List<Cita> citas;

    public DiaCalendario(LocalDate fecha, boolean delMes, boolean hoy, List<Cita> citas) {
        this.fecha = fecha;
        this.delMes = delMes;
        this.hoy = hoy;
        this.citas = citas;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public boolean isDelMes() {
        return delMes;
    }

    public boolean isHoy() {
        return hoy;
    }

    public List<Cita> getCitas() {
        return citas;
    }

    public int getDia() {
        return fecha.getDayOfMonth();
    }

    public boolean isTieneCitas() {
        return citas != null && !citas.isEmpty();
    }
}
