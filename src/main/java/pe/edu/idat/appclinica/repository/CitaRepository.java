package pe.edu.idat.appclinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.idat.appclinica.model.Cita;
import pe.edu.idat.appclinica.model.EstadoCita;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByPacienteIdOrderByFechaDescHoraDesc(Integer pacienteId);

    List<Cita> findByOdontologoIdAndFechaAndEstadoNot(
            Integer odontologoId, LocalDate fecha, EstadoCita estado);

    List<Cita> findByFechaBetweenOrderByFechaAscHoraAsc(LocalDate desde, LocalDate hasta);

    List<Cita> findAllByOrderByFechaDescHoraAsc();

    List<Cita> findByEstadoOrderByFechaAscHoraAsc(EstadoCita estado);

    List<Cita> findByFechaOrderByHoraAsc(LocalDate fecha);

    long countByEstado(EstadoCita estado);

    long countByPacienteId(Integer pacienteId);

    long countByPacienteIdAndEstado(Integer pacienteId, EstadoCita estado);

    long countByFechaAndEstadoNot(LocalDate fecha, EstadoCita estado);
}
