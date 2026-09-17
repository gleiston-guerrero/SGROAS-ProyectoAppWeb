package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.IncidentResponse;
import ec.edu.uteq.sgroas.service.IncidentService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    @Mock
    private IncidentService incidentService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new IncidentController(incidentService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new SpringDataJacksonConfiguration.PageModule(
                                        new SpringDataWebSettings(PageSerializationMode.DIRECT)))
                                .build()))
                .build();
    }

    private IncidentResponse responseEjemplo() {
        return new IncidentResponse(
                1L, 1L, "Carlos Mendoza", "AVERIA_MECANICA",
                "Falla en el motor", LocalDateTime.now(), "Km 12 Via Quito",
                "MEDIA", "REPORTADO", true, Instant.now(), Instant.now()
        );
    }

    @Test
    void listReturns200() throws Exception {
        when(incidentService.list(any()))
                .thenReturn(new PageImpl<>(List.of(responseEjemplo())));

        mockMvc().perform(get("/api/incidentes"))
                .andExpect(status().isOk());
    }

    @Test
    void findByIdReturns200() throws Exception {
        when(incidentService.findById(1L)).thenReturn(responseEjemplo());

        mockMvc().perform(get("/api/incidentes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("MEDIA"));
    }

    @Test
    void createReturns201() throws Exception {
        when(incidentService.create(any())).thenReturn(responseEjemplo());

        mockMvc().perform(post("/api/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignmentId": 1,
                                  "reportedBy": "Carlos Mendoza",
                                  "type": "AVERIA_MECANICA",
                                  "description": "Falla en el motor",
                                  "incidentDate": "2026-07-30T10:00:00",
                                  "location": "Km 12 Via Quito",
                                  "severity": "MEDIA",
                                  "status": "REPORTADO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        when(incidentService.update(any(), any())).thenReturn(responseEjemplo());

        mockMvc().perform(put("/api/incidentes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignmentId": 1,
                                  "reportedBy": "Carlos Mendoza",
                                  "type": "AVERIA_MECANICA",
                                  "description": "Falla en el motor",
                                  "incidentDate": "2026-07-30T10:00:00",
                                  "location": "Km 12 Via Quito",
                                  "severity": "MEDIA",
                                  "status": "REPORTADO"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc().perform(delete("/api/incidentes/1"))
                .andExpect(status().isNoContent());
    }
}
