package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.*;
import ec.edu.uteq.sgroas.abd.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbdScheduleServiceTest {

    @Mock
    private ScheduleRepository programacionRepository;
    @Mock
    private AbdRouteRepository rutaAbdRepository;
    @Mock
    private UnitRepository unidadRepository;
    @Mock
    private AbdDriverRepository conductorAbdRepository;

    @InjectMocks
    private AbdScheduleService service;

    private City sampleCity() {
        return City.builder().idCiudad(1).nombre("Quevedo").build();
    }

    private Terminal terminal(int id, String nombre) {
        return Terminal.builder().idTerminal(id).nombre(nombre).ciudad(sampleCity()).build();
    }

    private AbdRoute sampleRoute() {
        return AbdRoute.builder().idRuta(1)
                .terminalOrigen(terminal(1, "T1")).terminalDestino(terminal(2, "T2"))
                .precioPasaje(new BigDecimal("2.50")).build();
    }

    private Unit sampleUnit(String estado) {
        return Unit.builder().idUnidad(1).placa("ABC-1234").numeroDisco("001")
                .modelo("Hiace").capacidad(14).anioFabricacion(2020).estado(estado).build();
    }

    private AbdDriver sampleDriver() {
        return AbdDriver.builder().idConductor(1).cedula("1200000001")
                .nombres("Carlos").licencia("E").telefono("0988888888").build();
    }

    private Schedule sampleSchedule() {
        return Schedule.builder().idProgramacion(1).fecha(LocalDate.now())
                .horaSalida(LocalTime.of(8, 0)).horaEstimadaLlegada(LocalTime.of(10, 0))
                .estado("Programado").ruta(sampleRoute()).unidad(sampleUnit("Activo")).conductor(sampleDriver()).build();
    }

    private AbdDtos.ScheduleRequest request(String estado) {
        return new AbdDtos.ScheduleRequest(LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(10, 0), estado, 1, 1, 1);
    }

    @Test
    void listWithoutFiltersUsesFindAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(programacionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleSchedule())));

        assertEquals(1, service.list(null, null, null, null, null, pageable).getTotalElements());
        verify(programacionRepository).findAll(pageable);
    }

    @Test
    void listIdsZeroOrNullAreNormalized() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(programacionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        service.list("  ", 0, -1, null, null, pageable);

        verify(programacionRepository).findAll(pageable);
    }

    @Test
    void listWithFiltersUsesSearch() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(programacionRepository.searchWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleSchedule())));

        assertEquals(1, service.list("Programado", 1, 1,
                LocalDate.now().minusDays(1), LocalDate.now(), pageable).getTotalElements());
    }

    @Test
    void createOkWithDefaultStatus() {
        when(rutaAbdRepository.findById(1)).thenReturn(Optional.of(sampleRoute()));
        when(unidadRepository.findById(1)).thenReturn(Optional.of(sampleUnit("Activo")));
        when(conductorAbdRepository.findById(1)).thenReturn(Optional.of(sampleDriver()));
        when(programacionRepository.save(any(Schedule.class))).thenReturn(sampleSchedule());

        assertNotNull(service.create(request(null)));
    }

    @Test
    void createWithInvalidTimeFails() {
        AbdDtos.ScheduleRequest bad = new AbdDtos.ScheduleRequest(LocalDate.now(),
                LocalTime.of(10, 0), LocalTime.of(8, 0), null, 1, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> service.create(bad));
        verify(programacionRepository, never()).save(any());
    }

    @Test
    void createWithoutRouteUnitOrDriverFails() {
        when(rutaAbdRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.create(request(null)));

        when(rutaAbdRepository.findById(1)).thenReturn(Optional.of(sampleRoute()));
        when(unidadRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.create(request(null)));

        when(unidadRepository.findById(1)).thenReturn(Optional.of(sampleUnit("Activo")));
        when(conductorAbdRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.create(request(null)));
    }

    @Test
    void createWithInactiveUnitFails() {
        when(rutaAbdRepository.findById(1)).thenReturn(Optional.of(sampleRoute()));
        when(unidadRepository.findById(1)).thenReturn(Optional.of(sampleUnit("Inactivo")));
        when(conductorAbdRepository.findById(1)).thenReturn(Optional.of(sampleDriver()));

        assertThrows(IllegalStateException.class, () -> service.create(request(null)));
    }

    @Test
    void updateWithoutStatusKeepsAndWithStatusChanges() {
        Schedule p = sampleSchedule();
        when(programacionRepository.findById(1)).thenReturn(Optional.of(p));
        when(rutaAbdRepository.findById(1)).thenReturn(Optional.of(sampleRoute()));
        when(unidadRepository.findById(1)).thenReturn(Optional.of(sampleUnit("Activo")));
        when(conductorAbdRepository.findById(1)).thenReturn(Optional.of(sampleDriver()));
        when(programacionRepository.save(any(Schedule.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("Programado", service.update(1, request(null)).estado());
        assertEquals("Completado", service.update(1, request("Completado")).estado());
    }

    @Test
    void updateNonexistentAndInactiveUnitFail() {
        when(programacionRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.update(99, request(null)));

        when(programacionRepository.findById(1)).thenReturn(Optional.of(sampleSchedule()));
        when(rutaAbdRepository.findById(1)).thenReturn(Optional.of(sampleRoute()));
        when(unidadRepository.findById(1)).thenReturn(Optional.of(sampleUnit("En Mantenimiento")));
        when(conductorAbdRepository.findById(1)).thenReturn(Optional.of(sampleDriver()));
        assertThrows(IllegalStateException.class, () -> service.update(1, request(null)));
    }

    @Test
    void deleteOkAndDoesNotExist() {
        when(programacionRepository.existsById(1)).thenReturn(true);
        service.delete(1);
        verify(programacionRepository).deleteById(1);

        when(programacionRepository.existsById(99)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.delete(99));
    }
}
