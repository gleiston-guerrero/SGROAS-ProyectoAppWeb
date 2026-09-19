package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.City;
import ec.edu.uteq.sgroas.abd.entity.AbdRoute;
import ec.edu.uteq.sgroas.abd.entity.Terminal;
import ec.edu.uteq.sgroas.abd.repository.AbdRouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbdRouteServiceTest {

    @Mock
    private AbdRouteRepository rutaAbdRepository;
    @Mock
    private AbdCatalogService catalogoAbdService;

    @InjectMocks
    private AbdRouteService service;

    private Terminal terminal(int id, String nombre) {
        return Terminal.builder().idTerminal(id).nombre(nombre)
                .ciudad(City.builder().idCiudad(1).nombre("Quevedo").build()).build();
    }

    private AbdRoute sampleRoute() {
        return AbdRoute.builder().idRuta(1)
                .terminalOrigen(terminal(1, "T1")).terminalDestino(terminal(2, "T2"))
                .precioPasaje(new BigDecimal("2.50")).build();
    }

    private AbdDtos.AbdRouteRequest request(int origen, int destino) {
        return new AbdDtos.AbdRouteRequest(origen, destino, new BigDecimal("2.50"));
    }

    @Test
    void listWithoutSearchUsesFindAllAndBlankToo() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(rutaAbdRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleRoute())));
        when(rutaAbdRepository.countSchedules(1)).thenReturn(0L);

        assertEquals(1, service.list(null, pageable).getTotalElements());
        assertEquals(1, service.list("   ", pageable).getTotalElements());
        verify(rutaAbdRepository, times(2)).findAll(pageable);
    }

    @Test
    void listWithSearchUsesSearch() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(rutaAbdRepository.search(eq("quevedo"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleRoute())));
        when(rutaAbdRepository.countSchedules(1)).thenReturn(3L);

        assertEquals(3L, service.list(" Quevedo ", pageable).getContent().get(0).totalProgramaciones());
    }

    @Test
    void findByIdOkAndNotFound() {
        when(rutaAbdRepository.findById(1)).thenReturn(Optional.of(sampleRoute()));
        when(rutaAbdRepository.countSchedules(1)).thenReturn(0L);
        assertEquals(1, service.findById(1).idRuta());

        when(rutaAbdRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findById(99));
    }

    @Test
    void createOkAndSameTerminalFails() {
        when(catalogoAbdService.findTerminal(1)).thenReturn(terminal(1, "T1"));
        when(catalogoAbdService.findTerminal(2)).thenReturn(terminal(2, "T2"));
        when(rutaAbdRepository.save(any(AbdRoute.class))).thenReturn(sampleRoute());
        when(rutaAbdRepository.countSchedules(1)).thenReturn(0L);

        assertNotNull(service.create(request(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> service.create(request(1, 1)));
    }

    @Test
    void updateOkSameTerminalAndNotFoundFail() {
        when(rutaAbdRepository.findById(1)).thenReturn(Optional.of(sampleRoute()));
        when(catalogoAbdService.findTerminal(1)).thenReturn(terminal(1, "T1"));
        when(catalogoAbdService.findTerminal(2)).thenReturn(terminal(2, "T2"));
        when(rutaAbdRepository.save(any(AbdRoute.class))).thenAnswer(i -> i.getArgument(0));
        when(rutaAbdRepository.countSchedules(1)).thenReturn(0L);

        assertNotNull(service.update(1, request(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> service.update(1, request(2, 2)));

        when(rutaAbdRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.update(99, request(1, 2)));
    }

    @Test
    void deleteOkAndDoesNotExist() {
        when(rutaAbdRepository.existsById(1)).thenReturn(true);
        service.delete(1);
        verify(rutaAbdRepository).deleteById(1);

        when(rutaAbdRepository.existsById(99)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.delete(99));
    }
}
