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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceExtraTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ObjectProvider<DriverService> self;

    @InjectMocks
    private DriverService driverService;

    private Driver conductorEjemplo() {
        return Driver.builder()
                .id(1L)
                .firstNames("Carlos Alberto")
                .lastNames("Mendoza Vera")
                .nationalId("1200000001")
                .licenseNumber("LIC-001-2026")
                .licenseType("E")
                .licenseExpiry(LocalDate.now().plusDays(40))
                .phone("0988888888")
                .email("carlos.mendoza@sgroas.com")
                .status(DriverStatus.ACTIVO)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private DriverRequest requestEjemplo() {
        return new DriverRequest(
                "Carlos Alberto", "Mendoza Vera", "1200000001", "LIC-001-2026",
                "E", LocalDate.of(2026, 7, 15), "0988888888",
                "carlos.mendoza@sgroas.com", "ACTIVO"
        );
    }

    @Test
    void listReturnsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(self.getObject()).thenReturn(driverService);
        when(driverRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(conductorEjemplo())));

        Page<DriverResponse> pagina = driverService.list(null, pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Carlos Alberto", pagina.getContent().get(0).firstNames());
    }

    @Test
    void findByIdNonexistentThrowsException() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> driverService.findById(99L));
    }

    @Test
    void findInactiveDriverThrowsException() {
        Driver inactivo = conductorEjemplo();
        inactivo.setActive(false);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(inactivo));

        assertThrows(IllegalArgumentException.class,
                () -> driverService.findById(1L));
    }

    @Test
    void createWithDuplicateLicenseThrowsException() {
        when(driverRepository.existsByNationalId("1200000001")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("LIC-001-2026")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> driverService.create(requestEjemplo()));
    }

    @Test
    void createWithInvalidStatusThrowsException() {
        when(driverRepository.existsByNationalId("1200000001")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("LIC-001-2026")).thenReturn(false);

        DriverRequest request = new DriverRequest(
                "Carlos Alberto", "Mendoza Vera", "1200000001", "LIC-001-2026",
                "E", LocalDate.of(2026, 7, 15), "0988888888",
                "carlos.mendoza@sgroas.com", "INVALIDO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> driverService.create(request));
    }

    @Test
    void updateModifiesAndReturns() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorEjemplo()));
        when(driverRepository.save(any(Driver.class))).thenReturn(conductorEjemplo());

        DriverResponse response = driverService.update(1L, requestEjemplo());

        assertEquals(1L, response.id());
        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    void updateWithOtherNationalIdThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorEjemplo()));
        when(driverRepository.existsByNationalId("1299999999")).thenReturn(true);

        DriverRequest request = new DriverRequest(
                "Carlos Alberto", "Mendoza Vera", "1299999999", "LIC-001-2026",
                "E", LocalDate.of(2026, 7, 15), "0988888888",
                "carlos.mendoza@sgroas.com", "ACTIVO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> driverService.update(1L, request));
    }

    @Test
    void updateWithOtherLicenseThrowsException() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorEjemplo()));
        when(driverRepository.existsByLicenseNumber("LIC-999-2026")).thenReturn(true);

        DriverRequest request = new DriverRequest(
                "Carlos Alberto", "Mendoza Vera", "1200000001", "LIC-999-2026",
                "E", LocalDate.of(2026, 7, 15), "0988888888",
                "carlos.mendoza@sgroas.com", "ACTIVO"
        );

        assertThrows(IllegalArgumentException.class,
                () -> driverService.update(1L, request));
    }

    @Test
    void deactivateChangesStatus() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductorEjemplo()));

        driverService.deactivate(1L);

        verify(driverRepository).save(argThat(c ->
                !c.getActive() && c.getStatus() == DriverStatus.INACTIVO));
    }

    @Test
    void expiredLicenseShouldNotBeMarkedAsExpiring() {
        Driver conductor = conductorEjemplo();
        conductor.setLicenseExpiry(LocalDate.now().minusDays(5));
        when(driverRepository.findById(1L)).thenReturn(Optional.of(conductor));

        DriverResponse response = driverService.findById(1L);

        assertFalse(response.licenseExpiring());
    }
}
