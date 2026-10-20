package pe.edu.idat.appclinica.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.idat.appclinica.exception.AccesoDenegadoException;
import pe.edu.idat.appclinica.exception.OperacionNoPermitidaException;
import pe.edu.idat.appclinica.model.Cita;
import pe.edu.idat.appclinica.model.EstadoCita;
import pe.edu.idat.appclinica.model.Usuario;
import pe.edu.idat.appclinica.repository.CitaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private AgendaService agendaService;

    @InjectMocks
    private CitaService citaService;

    @Test
    void permiteConfirmarUnaCitaPendiente() {
        Cita cita = cita(1, EstadoCita.PENDIENTE, LocalDate.now().plusDays(1));
        when(citaRepository.findById(1)).thenReturn(Optional.of(cita));

        citaService.cambiarEstado(1, EstadoCita.CONFIRMADA);

        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void rechazaAtenderUnaCitaPendiente() {
        Cita cita = cita(1, EstadoCita.PENDIENTE, LocalDate.now().plusDays(1));
        when(citaRepository.findById(1)).thenReturn(Optional.of(cita));

        assertThrows(OperacionNoPermitidaException.class,
                () -> citaService.cambiarEstado(1, EstadoCita.ATENDIDA));

        verify(citaRepository, never()).save(any());
    }

    @Test
    void rechazaCancelarUnaCitaDeOtroPaciente() {
        Cita cita = cita(1, EstadoCita.PENDIENTE, LocalDate.now().plusDays(1));
        cita.setPaciente(usuario(2));
        when(citaRepository.findById(1)).thenReturn(Optional.of(cita));

        assertThrows(AccesoDenegadoException.class, () -> citaService.cancelar(1, 3));

        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelaUnaCitaPropia() {
        Cita cita = cita(1, EstadoCita.PENDIENTE, LocalDate.now().plusDays(1));
        cita.setPaciente(usuario(2));
        when(citaRepository.findById(1)).thenReturn(Optional.of(cita));

        citaService.cancelar(1, 2);

        assertEquals(EstadoCita.CANCELADA, cita.getEstado());
        verify(citaRepository).save(cita);
    }

    private Cita cita(Integer id, EstadoCita estado, LocalDate fecha) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setEstado(estado);
        cita.setFecha(fecha);
        cita.setHora(LocalTime.of(9, 0));
        return cita;
    }

    private Usuario usuario(Integer id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }
}
