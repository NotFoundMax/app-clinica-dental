package pe.edu.idat.appclinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.idat.appclinica.model.Odontologo;

import java.util.List;
import java.util.Optional;

@Repository
public interface OdontologoRepository extends JpaRepository<Odontologo, Integer> {

    List<Odontologo> findByActivoTrueOrderByApellidosAscNombresAsc();

    List<Odontologo> findAllByOrderByApellidosAscNombresAsc();

    Optional<Odontologo> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);
}
