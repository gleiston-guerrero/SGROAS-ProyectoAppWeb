package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.DriverResponse;
import ec.edu.uteq.sgroas.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock
    private DriverService driverService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new DriverController(driverService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new SpringDataJacksonConfiguration.PageModule(
                                        new SpringDataWebSettings(PageSerializationMode.DIRECT)))
                                .build()))
                .build();
    }

    private DriverResponse responseEjemplo() {
        return new DriverResponse(
                1L, "Carlos Alberto", "Mendoza Vera", "1200000001", "LIC-001-2026",
                "E", LocalDate.now().plusDays(20), "0988888888",
                "carlos.mendoza@sgroas.com", "ACTIVO", true, true,
                Instant.now(), Instant.now()
        );
    }

    @Test
    void listReturns200() throws Exception {
        when(driverService.list(any(), any()))
                .thenReturn(new PageImpl<>(List.of(responseEjemplo())));

        mockMvc().perform(get("/api/conductores"))
                .andExpect(status().isOk());
    }

    @Test
    void findByIdReturns200() throws Exception {
        when(driverService.findById(1L)).thenReturn(responseEjemplo());

        mockMvc().perform(get("/api/conductores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nationalId").value("1200000001"));
    }

    @Test
    void createReturns201() throws Exception {
        when(driverService.create(any())).thenReturn(responseEjemplo());

        mockMvc().perform(post("/api/conductores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstNames": "Carlos Alberto",
                                  "lastNames": "Mendoza Vera",
                                  "nationalId": "1200000001",
                                  "licenseNumber": "LIC-001-2026",
                                  "licenseType": "E",
                                  "licenseExpiry": "2026-12-31",
                                  "phone": "0988888888",
                                  "email": "carlos.mendoza@sgroas.com",
                                  "status": "ACTIVO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        when(driverService.update(any(), any())).thenReturn(responseEjemplo());

        mockMvc().perform(put("/api/conductores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstNames": "Carlos Alberto",
                                  "lastNames": "Mendoza Vera",
                                  "nationalId": "1200000001",
                                  "licenseNumber": "LIC-001-2026",
                                  "licenseType": "E",
                                  "licenseExpiry": "2026-12-31",
                                  "phone": "0988888888",
                                  "email": "carlos.mendoza@sgroas.com",
                                  "status": "ACTIVO"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc().perform(delete("/api/conductores/1"))
                .andExpect(status().isNoContent());
    }
}
