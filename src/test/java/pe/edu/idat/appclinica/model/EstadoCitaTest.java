package pe.edu.idat.appclinica.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoCitaTest {

    @Test
    void pendienteSoloPermiteConfirmarOCancelar() {
        assertEquals(List.of(EstadoCita.CONFIRMADA, EstadoCita.CANCELADA),
                EstadoCita.PENDIENTE.getSiguientes());
    }

    @Test
    void confirmadaSoloPermiteAtenderOCancelar() {
        assertEquals(List.of(EstadoCita.ATENDIDA, EstadoCita.CANCELADA),
                EstadoCita.CONFIRMADA.getSiguientes());
    }

    @Test
    void losEstadosFinalesNoTienenSiguientes() {
        assertEquals(List.of(), EstadoCita.ATENDIDA.getSiguientes());
        assertEquals(List.of(), EstadoCita.CANCELADA.getSiguientes());
    }
}
