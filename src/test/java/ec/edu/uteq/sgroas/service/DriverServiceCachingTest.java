package ec.edu.uteq.sgroas.service;

import ec.edu.uteq.sgroas.entity.Driver;
import ec.edu.uteq.sgroas.entity.DriverStatus;
import ec.edu.uteq.sgroas.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves that DriverService.list() with no search text actually hits the
 * Spring cache in a real proxied context, not just that the annotation is
 * present. Regression test for the self-invocation bug: an @Cacheable
 * method called via a plain "this.foo()" call inside the same class never
 * goes through the Spring AOP proxy, so it silently never caches; this
 * test would fail (repository invoked twice) if that bug reappears.
 */
@SpringBootTest(classes = {DriverService.class, DriverServiceCachingTest.CachingTestConfig.class})
class DriverServiceCachingTest {

    @Configuration
    @EnableCaching
    static class CachingTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("conductores");
        }
    }

    @Autowired
    private DriverService driverService;

    @MockBean
    private DriverRepository driverRepository;

    private Driver conductor() {
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

    @Test
    void secondUnfilteredCallIsServedFromCacheNotFromRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(driverRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(conductor())));

        var first = driverService.list(null, pageable);
        var second = driverService.list(null, pageable);

        assertEquals(1, first.getTotalElements());
        assertEquals(1, second.getTotalElements());
        verify(driverRepository, times(1)).findByActiveTrue(pageable);
    }

    @Test
    void searchedCallIsNeverCached() {
        Pageable pageable = PageRequest.of(0, 10);
        when(driverRepository.searchActive("carlos", pageable))
                .thenReturn(new PageImpl<>(List.of(conductor())));

        driverService.list("Carlos", pageable);
        driverService.list("Carlos", pageable);

        verify(driverRepository, times(2)).searchActive("carlos", pageable);
    }
}
