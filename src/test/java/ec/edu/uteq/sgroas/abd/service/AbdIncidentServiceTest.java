package ec.edu.uteq.sgroas.abd.service;

import ec.edu.uteq.sgroas.abd.dto.AbdDtos;
import ec.edu.uteq.sgroas.abd.entity.AbdIncident;
import ec.edu.uteq.sgroas.abd.entity.Unit;
import ec.edu.uteq.sgroas.abd.repository.AlertRepository;
import ec.edu.uteq.sgroas.abd.repository.AbdIncidentRepository;
import ec.edu.uteq.sgroas.abd.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbdIncidentServiceTest {

    @Mock
    private AbdIncidentRepository incidenteRepository;
    @Mock
    private AlertRepository alertaRepository;
    @Mock
    private UnitRepository unidadRepository;

    @InjectMocks
    private AbdIncidentService service;

    private Unit unidad() {
        return Unit.builder().idUnidad(1).placa("ABC-1234").numeroDisco("001")
                .modelo("Hiace").capacidad(14).anioFabricacion(2020).estado("Activo").build();
    }

    private AbdIncident incidente(String nivel) {
        return AbdIncident.builder().idIncidente(1).tipo("Choque")
                .descripcion("Choque leve").nivelSugerido(nivel)
                .fechaIncidente(LocalDateTime.now()).evidencia("foto.jpg")
                .estado("Reportado").unidad(unidad()).build();
    }

    private AbdDtos.AbdIncidentRequest request(String nivel, String evidencia, String estado) {
        return new AbdDtos.AbdIncidentRequest("Choque", "Choque leve", nivel, evidencia, estado, 1);
    }

    @Test
    void listWithoutFiltersUsesFindAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(incidenteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(incidente("BAJO"))));

        assertEquals(1, service.list(null, "  ", null, pageable).getTotalElements());
    }

    @Test
    void listWithSearchUsesSearch() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(incidenteRepository.searchWithFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(incidente("MEDIO"))));

        assertEquals(1, service.list("Reportado", "MEDIO", "choque", pageable).getTotalElements());
    }

    @Test
    void listOnlyStatusUsesFindByStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(incidenteRepository.findByStatusIgnoreCase(eq("reportado"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(incidente("BAJO"))));

        assertEquals(1, service.list("Reportado", null, null, pageable).getTotalElements());
    }

    @Test
    void listOnlyLevelUsesFindByLevel() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(incidenteRepository.findBySuggestedLevelIgnoreCase(eq("alto"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(incidente("ALTO"))));

        assertEquals(1, service.list(null, "ALTO", null, pageable).getTotalElements());
    }

    @Test
    void createHighLevelGeneratesAlert() {
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad()));
        when(incidenteRepository.save(any(AbdIncident.class))).thenReturn(incidente("ALTO"));

        assertNotNull(service.create(request("ALTO", "foto.jpg", null)));
        verify(alertaRepository).save(any());
    }

    @Test
    void createLowLevelDoesNotGenerateAlert() {
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad()));
        when(incidenteRepository.save(any(AbdIncident.class))).thenReturn(incidente("BAJO"));

        assertNotNull(service.create(request("BAJO", null, "Reportado")));
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void createWithoutUnitFails() {
        when(unidadRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(request("BAJO", null, null)));
    }

    @Test
    void updateWithEvidenceAndStatus() {
        AbdIncident i = incidente("MEDIO");
        when(incidenteRepository.findById(1)).thenReturn(Optional.of(i));
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad()));
        when(incidenteRepository.save(any(AbdIncident.class))).thenAnswer(a -> a.getArgument(0));

        AbdDtos.AbdIncidentResponse r = service.update(1, request("MEDIO", "nueva.jpg", "Cerrado"));

        assertEquals("nueva.jpg", r.evidencia());
        assertEquals("Cerrado", r.estado());
    }

    @Test
    void updateWithoutEvidenceOrStatusKeeps() {
        AbdIncident i = incidente("MEDIO");
        when(incidenteRepository.findById(1)).thenReturn(Optional.of(i));
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad()));
        when(incidenteRepository.save(any(AbdIncident.class))).thenAnswer(a -> a.getArgument(0));

        AbdDtos.AbdIncidentResponse r = service.update(1, request("MEDIO", null, null));

        assertEquals("foto.jpg", r.evidencia());
        assertEquals("Reportado", r.estado());
    }

    @Test
    void updateNonexistentOrWithoutUnitFails() {
        when(incidenteRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.update(99, request("BAJO", null, null)));

        when(incidenteRepository.findById(1)).thenReturn(Optional.of(incidente("BAJO")));
        when(unidadRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.update(1, request("BAJO", null, null)));
    }

    @Test
    void deleteOkAndDoesNotExist() {
        when(incidenteRepository.existsById(1)).thenReturn(true);
        service.delete(1);
        verify(incidenteRepository).deleteById(1);

        when(incidenteRepository.existsById(99)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.delete(99));
    }
}
