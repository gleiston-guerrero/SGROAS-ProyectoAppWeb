package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.AbdRole;
import ec.edu.uteq.sgroas.abd.entity.City;
import ec.edu.uteq.sgroas.abd.entity.Province;
import ec.edu.uteq.sgroas.abd.entity.Terminal;
import ec.edu.uteq.sgroas.abd.repository.AbdRoleRepository;
import ec.edu.uteq.sgroas.abd.repository.CityRepository;
import ec.edu.uteq.sgroas.abd.repository.ProvinceRepository;
import ec.edu.uteq.sgroas.abd.repository.TerminalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbdCatalogServiceTest {

    @Mock
    private ProvinceRepository provinciaRepository;

    @Mock
    private CityRepository ciudadRepository;

    @Mock
    private TerminalRepository terminalRepository;

    @Mock
    private AbdRoleRepository rolAbdRepository;

    @InjectMocks
    private AbdCatalogService abdCatalogoService;

    @Test
    void getCatalogsReuneLosCuatroListados() {
        Province provincia = Province.builder().idProvincia(1).nombre("Pichincha").build();
        City ciudad = City.builder().idCiudad(1).nombre("Quito").provincia(provincia).build();
        Terminal terminal = Terminal.builder().idTerminal(1).nombre("Terminal Quito").ciudad(ciudad).build();
        AbdRole rol = AbdRole.builder().idRol(1).nombre("ROLE_OPERADOR").descripcion("Operador").build();

        when(provinciaRepository.findAll()).thenReturn(List.of(provincia));
        when(ciudadRepository.findAll()).thenReturn(List.of(ciudad));
        when(terminalRepository.findAll()).thenReturn(List.of(terminal));
        when(rolAbdRepository.findAll()).thenReturn(List.of(rol));

        AbdDtos.CatalogsResponse catalogo = abdCatalogoService.getCatalogs();

        assertEquals(1, catalogo.provincias().size());
        assertEquals(1, catalogo.provincias().get(0).idProvincia());
        assertEquals("Pichincha", catalogo.provincias().get(0).nombre());

        assertEquals(1, catalogo.ciudades().size());
        assertEquals(1, catalogo.ciudades().get(0).idCiudad());
        assertEquals("Quito", catalogo.ciudades().get(0).nombre());
        assertEquals(1, catalogo.ciudades().get(0).idProvincia());
        assertEquals("Pichincha", catalogo.ciudades().get(0).nombreProvincia());

        assertEquals(1, catalogo.terminales().size());
        assertEquals(1, catalogo.terminales().get(0).idTerminal());
        assertEquals("Terminal Quito", catalogo.terminales().get(0).nombre());
        assertEquals(1, catalogo.terminales().get(0).idCiudad());
        assertEquals("Quito", catalogo.terminales().get(0).nombreCiudad());

        assertEquals(1, catalogo.roles().size());
        assertEquals(1, catalogo.roles().get(0).idRol());
        assertEquals("ROLE_OPERADOR", catalogo.roles().get(0).nombre());
        assertEquals("Operador", catalogo.roles().get(0).descripcion());
    }

    @Test
    void findTerminalReturnsEntityWhenItExists() {
        Terminal terminal = Terminal.builder().idTerminal(5).nombre("Terminal Guayaquil").build();
        when(terminalRepository.findById(5)).thenReturn(Optional.of(terminal));

        assertEquals(terminal, abdCatalogoService.findTerminal(5));
    }

    @Test
    void findTerminalThrowsExceptionWhenItDoesNotExist() {
        when(terminalRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> abdCatalogoService.findTerminal(99));
    }
}