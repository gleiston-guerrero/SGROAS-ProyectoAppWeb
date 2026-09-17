package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.dto.DriverRequest;
import ec.edu.uteq.sgroas.dto.DriverResponse;
import ec.edu.uteq.sgroas.entity.Driver;
import ec.edu.uteq.sgroas.entity.DriverStatus;
import ec.edu.uteq.sgroas.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ObjectProvider<DriverService> self;

    @InjectMocks
    private DriverService driverService;

    private Driver conductorBase() {
        return Driver.builder()
                .id(1L)
                .firstNames("Carlos Alberto")
                .lastNames("Mendoza Vera")
                .nationalId("1200000001")
                .licenseNumber("LIC-001-2026")
                .licenseType("E")
                .licenseExpiry(LocalDate.now().plusDays(20))
                .phone("0988888888")
                .email("carlos.mendoza@sgroas.com")
                .status(DriverStatus.ACTIVO)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private DriverRequest requestBase() {
        return new DriverRequest(
                "Carlos Alberto",
                "Mendoza Vera",
                "1200000001",
                "LIC-001-2026",
                "E",
                LocalDate.now().plusDays(20),
                "0988888888",
                "carlos.mendoza@sgroas.com",
                "ACTIVO"
        );
    }

    @Test
    void createDriverCorrectly() {
        DriverRequest request = new DriverRequest(
                "Carlos Alberto",
                "Mendoza Vera",
                "1200000001",
                "LIC-001-2026",
                "E",
                LocalDate.of(2026, 7, 15),
                "0988888888",
                "carlos.mendoza@sgroas.com",
                "ACTIVO"
        );

        Driver conductorGuardado = Driver.builder()
                .id(1L)
                .firstNames("Carlos Alberto")
                .lastNames("Mendoza Vera")
                .nationalId("1200000001")
                .licenseNumber("LIC-001-2026")
                .licenseType("E")
                .licenseExpiry(LocalDate.of(2026, 7, 15))
                .phone("0988888888")
                .email("carlos.mendoza@sgroas.com")
                .status(DriverStatus.ACTIVO)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(driverRepository.existsByNationalId("1200000001"))
                .thenReturn(false);
        when(driverRepository.existsByLicenseNumber("LIC-001-2026"))
                .thenReturn(false);
        when(driverRepository.save(any(Driver.class)))
                .thenReturn(conductorGuardado);

        DriverResponse response = driverService.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Alberto", response.firstNames());
        assertEquals("1200000001", response.nationalId());
        assertEquals("ACTIVO", response.status());
        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    void createDriverWithDuplicateNationalIdThrowsException() {
        DriverRequest request = new DriverRequest(
                "Carlos Alberto",
                "Mendoza Vera",
                "1200000001",
                "LIC-001-2026",
                "E",
                LocalDate.of(2026, 7, 15),
                "0988888888",
                "carlos.mendoza@sgroas.com",
                "ACTIVO"
        );

        when(driverRepository.existsByNationalId("1200000001"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> driverService.create(request)
        );

        assertEquals("Ya existe un conductor con esa cedula", exception.getMessage());
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void findDriverByIdCorrectly() {
        Driver conductor = Driver.builder()
                .id(1L)
                .firstNames("Carlos Alberto")
                .lastNames("Mendoza Vera")
                .nationalId("1200000001")
                .licenseNumber("LIC-001-2026")
                .licenseType("E")
                .licenseExpiry(LocalDate.now().plusDays(20))
                .phone("0988888888")
                .email("carlos.mendoza@sgroas.com")
                .status(DriverStatus.ACTIVO)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(conductor));

        DriverResponse response = driverService.findById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Alberto", response.firstNames());
        assertTrue(response.licenseExpiring());
    }

    @Test
    void listWithoutSearchUsesFindByActiveTrue() {
        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);
        when(self.getObject()).thenReturn(driverService);
        when(driverRepository.findByActiveTrue(pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(conductorBase())));

        var pagina = driverService.list(null, pageable);

        assertEquals(1, pagina.getTotalElements());
        verify(driverRepository).findByActiveTrue(pageable);
        verify(driverRepository, never()).searchActive(any(), any());
    }

    @Test
    void listWithBlankSearchUsesFindByActiveTrue() {
        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);
        when(self.getObject()).thenReturn(driverService);
        when(driverRepository.findByActiveTrue(pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(conductorBase())));

        var pagina = driverService.list("   ", pageable);

        assertEquals(1, pagina.getTotalElements());
        verify(driverRepository).findByActiveTrue(pageable);
    }

    @Test
    void listWithSearchUsesSearchActive() {
        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);
        when(driverRepository.searchActive("carlos", pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(conductorBase())));

        var pagina = driverService.list("  Carlos ", pageable);

        assertEquals(1, pagina.getTotalElements());
        verify(driverRepository).searchActive("carlos", pageable);
        verify(driverRepository, never()).findByActiveTrue(pageable);
    }

    @Test
    void updateWithDuplicateNationalIdThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorBase()));
        when(driverRepository.existsByNationalId("0999999999")).thenReturn(true);

        DriverRequest request = new DriverRequest(
                "Carlos Alberto", "Mendoza Vera", "0999999999", "LIC-001-2026",
                "E", LocalDate.now().plusDays(20), "0988888888",
                "carlos.mendoza@sgroas.com", "ACTIVO");

        assertThrows(IllegalArgumentException.class,
                () -> driverService.update(1L, request));
    }

    @Test
    void updateWithDuplicateLicenseThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorBase()));
        when(driverRepository.existsByLicenseNumber("LIC-999-2026")).thenReturn(true);

        DriverRequest request = new DriverRequest(
                "Carlos Alberto", "Mendoza Vera", "1200000001", "LIC-999-2026",
                "E", LocalDate.now().plusDays(20), "0988888888",
                "carlos.mendoza@sgroas.com", "ACTIVO");

        assertThrows(IllegalArgumentException.class,
                () -> driverService.update(1L, request));
    }

    @Test
    void updateModifiesAndSaves() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorBase()));
        when(driverRepository.save(any(Driver.class))).thenReturn(conductorBase());

        DriverResponse response = driverService.update(1L, requestBase());

        assertEquals(1L, response.id());
        assertEquals("1200000001", response.nationalId());
        verify(driverRepository).save(any(Driver.class));
        verify(driverRepository, never()).existsByNationalId(any());
        verify(driverRepository, never()).existsByLicenseNumber(any());
    }

    @Test
    void deactivateMarksInactive() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorBase()));

        driverService.deactivate(1L);

        verify(driverRepository).save(argThat(c ->
                !c.getActive() && c.getStatus() == DriverStatus.INACTIVO));
    }

    @Test
    void expiredLicenseShouldMarkLicenseExpiringFalse() {
        Driver vencido = conductorBase();
        vencido.setLicenseExpiry(LocalDate.now().minusDays(5));
        when(driverRepository.findById(1L)).thenReturn(Optional.of(vencido));

        DriverResponse response = driverService.findById(1L);

        assertFalse(response.licenseExpiring());
    }

    @Test
    void farLicenseShouldMarkLicenseExpiringFalse() {
        Driver lejana = conductorBase();
        lejana.setLicenseExpiry(LocalDate.now().plusDays(60));
        when(driverRepository.findById(1L)).thenReturn(Optional.of(lejana));

        DriverResponse response = driverService.findById(1L);

        assertFalse(response.licenseExpiring());
    }
}
