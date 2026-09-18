package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.Unit;
import ec.edu.uteq.sgroas.abd.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbdUnitServiceTest {

    @Mock
    private UnitRepository unidadRepository;

    @InjectMocks
    private AbdUnitService service;

    private Unit unidadEjemplo() {
        return Unit.builder()
                .idUnidad(1).placa("ABC-1234").numeroDisco("001")
                .modelo("Hiace").capacidad(14).anioFabricacion(2020)
                .estado("Activo").build();
    }

    private AbdDtos.UnitRequest requestEjemplo(String estado) {
        return new AbdDtos.UnitRequest("ABC-1234", "001", "Hiace", 14, 2020, estado);
    }

    @Test
    void listWithoutFiltersUsesFindAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(unidadRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(unidadEjemplo())));

        Page<AbdDtos.UnitResponse> page = service.list(null, null, pageable);

        assertEquals(1, page.getTotalElements());
        verify(unidadRepository).findAll(pageable);
        verify(unidadRepository, never()).searchWithFilters(any(), any(), any());
    }

    @Test
    void listWithBlanksUsesFindAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(unidadRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        service.list("   ", "  ", pageable);

        verify(unidadRepository).findAll(pageable);
    }

    @Test
    void listWithFiltersUsesSearch() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(unidadRepository.searchWithFilters(eq("activo"), eq("abc"), any()))
                .thenReturn(new PageImpl<>(List.of(unidadEjemplo())));

        Page<AbdDtos.UnitResponse> page = service.list("  Activo ", " ABC ", pageable);

        assertEquals(1, page.getTotalElements());
        verify(unidadRepository).searchWithFilters(eq("activo"), eq("abc"), any());
    }

    @Test
    void findByIdOkAndNotFound() {
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidadEjemplo()));
        assertEquals("ABC-1234", service.findById(1).placa());

        when(unidadRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findById(99));
    }

    @Test
    void createWithNullStatusUsesActiveByDefault() {
        when(unidadRepository.existsByLicensePlateIgnoreCase("ABC-1234")).thenReturn(false);
        when(unidadRepository.existsByNumeroDiscoIgnoreCase("001")).thenReturn(false);
        when(unidadRepository.save(any(Unit.class))).thenReturn(unidadEjemplo());

        AbdDtos.UnitResponse r = service.create(requestEjemplo(null));

        assertNotNull(r);
        verify(unidadRepository).save(any(Unit.class));
    }

    @Test
    void createWithDuplicatePlateFails() {
        when(unidadRepository.existsByLicensePlateIgnoreCase("ABC-1234")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(requestEjemplo("Activo")));
        verify(unidadRepository, never()).save(any());
    }

    @Test
    void createWithDuplicateDiskFails() {
        when(unidadRepository.existsByLicensePlateIgnoreCase("ABC-1234")).thenReturn(false);
        when(unidadRepository.existsByNumeroDiscoIgnoreCase("001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(requestEjemplo("Activo")));
    }

    @Test
    void updateWithoutStatusKeepsCurrent() {
        Unit actual = unidadEjemplo();
        when(unidadRepository.findById(1)).thenReturn(Optional.of(actual));
        when(unidadRepository.save(any(Unit.class))).thenReturn(actual);

        AbdDtos.UnitResponse r = service.update(1, requestEjemplo(null));

        assertEquals("Activo", r.estado());
        verify(unidadRepository, never()).existsByLicensePlateIgnoreCase(any());
    }

    @Test
    void updateWithStatusChangesIt() {
        Unit actual = unidadEjemplo();
        actual.setPlaca("XYZ-9999");
        actual.setNumeroDisco("009");
        when(unidadRepository.findById(1)).thenReturn(Optional.of(actual));
        when(unidadRepository.existsByLicensePlateIgnoreCase("ABC-1234")).thenReturn(false);
        when(unidadRepository.existsByNumeroDiscoIgnoreCase("001")).thenReturn(false);
        when(unidadRepository.save(any(Unit.class))).thenAnswer(i -> i.getArgument(0));

        AbdDtos.UnitResponse r = service.update(1, requestEjemplo("Inactivo"));

        assertEquals("Inactivo", r.estado());
    }

    @Test
    void updateWithDuplicatePlateFails() {
        Unit actual = unidadEjemplo();
        actual.setPlaca("OTRA-0000");
        when(unidadRepository.findById(1)).thenReturn(Optional.of(actual));
        when(unidadRepository.existsByLicensePlateIgnoreCase("ABC-1234")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.update(1, requestEjemplo("Activo")));
    }

    @Test
    void updateWithDuplicateDiskFails() {
        Unit actual = unidadEjemplo();
        actual.setNumeroDisco("009");
        when(unidadRepository.findById(1)).thenReturn(Optional.of(actual));
        when(unidadRepository.existsByNumeroDiscoIgnoreCase("001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.update(1, requestEjemplo("Activo")));
    }

    @Test
    void updateNonexistentFails() {
        when(unidadRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(99, requestEjemplo("Activo")));
    }

    @Test
    void deleteOkAndDoesNotExist() {
        when(unidadRepository.existsById(1)).thenReturn(true);
        service.delete(1);
        verify(unidadRepository).deleteById(1);

        when(unidadRepository.existsById(99)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.delete(99));
    }
}
