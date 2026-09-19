package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.RouteAssignmentResponse;
import ec.edu.uteq.sgroas.service.RouteAssignmentService;
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
class RouteAssignmentControllerTest {

    @Mock
    private RouteAssignmentService routeAssignmentService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(
                new RouteAssignmentController(routeAssignmentService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new SpringDataJacksonConfiguration.PageModule(
                                        new SpringDataWebSettings(PageSerializationMode.DIRECT)))
                                .build()))
                .build();
    }

    private RouteAssignmentResponse sampleResponse() {
        return new RouteAssignmentResponse(
                1L, 1L, "Carlos Mendoza", 1L, "GTU-001", 1L,
                "Quito - Guayaquil", LocalDate.now(), LocalDate.now(),
                LocalDate.now().plusDays(1), "ACTIVA", true,
                Instant.now(), Instant.now()
        );
    }

    @Test
    void listReturns200() throws Exception {
        when(routeAssignmentService.list(any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc().perform(get("/api/asignaciones"))
                .andExpect(status().isOk());
    }

    @Test
    void findByIdReturns200() throws Exception {
        when(routeAssignmentService.findById(1L)).thenReturn(sampleResponse());

        mockMvc().perform(get("/api/asignaciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehiclePlate").value("GTU-001"));
    }

    @Test
    void createReturns201() throws Exception {
        when(routeAssignmentService.create(any())).thenReturn(sampleResponse());

        mockMvc().perform(post("/api/asignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverId": 1,
                                  "vehicleId": 1,
                                  "routeId": 1,
                                  "assignmentDate": "2026-07-30",
                                  "startDate": "2026-07-30",
                                  "endDate": "2026-07-31",
                                  "status": "ACTIVA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        when(routeAssignmentService.update(any(), any())).thenReturn(sampleResponse());

        mockMvc().perform(put("/api/asignaciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverId": 1,
                                  "vehicleId": 1,
                                  "routeId": 1,
                                  "assignmentDate": "2026-07-30",
                                  "startDate": "2026-07-30",
                                  "endDate": "2026-07-31",
                                  "status": "ACTIVA"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc().perform(delete("/api/asignaciones/1"))
                .andExpect(status().isNoContent());
    }
}
