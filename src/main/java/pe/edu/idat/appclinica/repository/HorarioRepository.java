package pe.edu.idat.appclinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.idat.appclinica.model.Horario;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    List<Horario> findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(Integer odontologoId);

    List<Horario> findByOdontologoIdAndDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
            Integer odontologoId, Integer diaSemana);

    /**
     * Detecta cruces de bloques del mismo odontólogo y día.
     *
     * La consulta se arma con parámetros, no concatenando texto:
     *   hora_inicio < finNuevo AND hora_fin > inicioNuevo
     */
    boolean existsByOdontologoIdAndDiaSemanaAndActivoTrueAndHoraInicioLessThanAndHoraFinGreaterThan(
            Integer odontologoId,
            Integer diaSemana,
            LocalTime horaFinNueva,
            LocalTime horaInicioNueva);
}
