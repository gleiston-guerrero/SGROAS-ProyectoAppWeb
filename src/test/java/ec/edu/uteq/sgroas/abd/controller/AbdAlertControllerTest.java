package ec.edu.uteq.sgroas.abd.controller;

import ec.edu.uteq.sgroas.abd.entity.Alert;
import ec.edu.uteq.sgroas.abd.entity.AbdIncident;
import ec.edu.uteq.sgroas.abd.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AbdAlertControllerTest {

    @Mock
    private AlertRepository alertaRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new AbdAlertController(alertaRepository))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new SpringDataJacksonConfiguration.PageModule(
                                        new SpringDataWebSettings(PageSerializationMode.DIRECT)))
                                .build()))
                .build();
    }

    @Test
    void listWithDateAndIncidentMapsAll() throws Exception {
        AbdIncident incidente = AbdIncident.builder()
                .idIncidente(7).tipo("Choque").build();
        Alert alerta = Alert.builder().idAlerta(1).nivelRiesgo("ALTO")
                .descripcion("Riesgo alto").fecha(LocalDateTime.of(2026, 8, 1, 10, 0))
                .incidente(incidente).build();
        when(alertaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(alerta)));

        mockMvc().perform(get("/api/abd/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nivelRiesgo").value("ALTO"))
                .andExpect(jsonPath("$.content[0].idIncidente").value(7));
    }

    @Test
    void listWithNullsMapsNull() throws Exception {
        Alert alerta = Alert.builder().idAlerta(2).nivelRiesgo("BAJO")
                .descripcion("Sin datos").build();
        when(alertaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(alerta)));

        mockMvc().perform(get("/api/abd/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idAlerta").value(2));
    }
}
