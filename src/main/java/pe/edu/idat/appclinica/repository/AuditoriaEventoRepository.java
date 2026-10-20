package pe.edu.idat.appclinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.idat.appclinica.model.AuditoriaEvento;

import java.util.List;

@Repository
public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    List<AuditoriaEvento> findTop200ByOrderByFechaDesc();

    List<AuditoriaEvento> findTop200ByAccionOrderByFechaDesc(String accion);

    @Query("select distinct a.accion from AuditoriaEvento a order by a.accion")
    List<String> findAccionesDistintas();
}
