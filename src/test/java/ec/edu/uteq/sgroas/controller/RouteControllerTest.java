package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.RouteResponse;
import ec.edu.uteq.sgroas.service.RouteService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {

    @Mock
    private RouteService routeService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new RouteController(routeService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new SpringDataJacksonConfiguration.PageModule(
                                        new SpringDataWebSettings(PageSerializationMode.DIRECT)))
                                .build()))
                .build();
    }

    private RouteResponse sampleResponse() {
        return new RouteResponse(
                1L, "R-001", "Quito - Guayaquil", "Quito", "Guayaquil",
                420.0, 480, "ACTIVA", true, Instant.now(), Instant.now()
        );
    }

    @Test
    void listReturns200() throws Exception {
        when(routeService.list(any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc().perform(get("/api/rutas"))
                .andExpect(status().isOk());
    }

    @Test
    void findByIdReturns200() throws Exception {
        when(routeService.findById(1L)).thenReturn(sampleResponse());

        mockMvc().perform(get("/api/rutas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Quito - Guayaquil"));
    }

    @Test
    void createReturns201() throws Exception {
        when(routeService.create(any())).thenReturn(sampleResponse());

        mockMvc().perform(post("/api/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "R-001",
                                  "name": "Quito - Guayaquil",
                                  "origin": "Quito",
                                  "destination": "Guayaquil",
                                  "distanceKm": 420.0,
                                  "durationMin": 480,
                                  "status": "ACTIVA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        when(routeService.update(any(), any())).thenReturn(sampleResponse());

        mockMvc().perform(put("/api/rutas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "R-001",
                                  "name": "Quito - Guayaquil",
                                  "origin": "Quito",
                                  "destination": "Guayaquil",
                                  "distanceKm": 420.0,
                                  "durationMin": 480,
                                  "status": "ACTIVA"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc().perform(delete("/api/rutas/1"))
                .andExpect(status().isNoContent());
    }
}
