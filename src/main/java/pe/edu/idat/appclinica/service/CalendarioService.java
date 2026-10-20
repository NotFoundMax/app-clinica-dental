package pe.edu.idat.appclinica.service;

import org.springframework.stereotype.Service;
import pe.edu.idat.appclinica.dto.DiaCalendario;
import pe.edu.idat.appclinica.model.Cita;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Construye la cuadrícula del calendario mensual.
 *
 * La vista recibe 42 celdas (6 semanas), empezando en lunes.
 */
@Service
public class CalendarioService {

    private static final Locale ESPANOL = new Locale("es", "PE");
    private static final DateTimeFormatter TITULO_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", ESPANOL);

    public List<DiaCalendario> construir(YearMonth mes, List<Cita> citas) {
        Map<LocalDate, List<Cita>> porFecha = citas.stream()
                .filter(c -> c.getFecha() != null)
                .collect(Collectors.groupingBy(Cita::getFecha));

        LocalDate primerDia = mes.atDay(1);
        LocalDate inicio = primerDia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate hoy = LocalDate.now();

        List<DiaCalendario> dias = new ArrayList<>(42);

        for (int i = 0; i < 42; i++) {
            LocalDate fecha = inicio.plusDays(i);
            List<Cita> delDia = porFecha.getOrDefault(fecha, List.of());
            dias.add(new DiaCalendario(
                    fecha,
                    YearMonth.from(fecha).equals(mes),
                    fecha.equals(hoy),
                    delDia));
        }

        return dias;
    }

    public String titulo(YearMonth mes) {
        String texto = mes.atDay(1).format(TITULO_MES);
        return texto.substring(0, 1).toUpperCase(ESPANOL) + texto.substring(1);
    }

    public YearMonth parsear(String mes) {
        if (mes == null || !mes.matches("\\d{4}-\\d{2}")) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }
}
