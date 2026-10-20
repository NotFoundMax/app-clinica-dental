package pe.edu.idat.appclinica;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Punto de entrada de la aplicación.
 * La zona horaria se fija en America/Lima para que las citas de hoy y los
 * horarios disponibles no cambien según la zona del servidor.
 */
@SpringBootApplication
public class AppClinicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppClinicaApplication.class, args);
    }

    @PostConstruct
    public void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
    }
}
