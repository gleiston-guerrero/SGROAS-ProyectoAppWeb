package ec.edu.uteq.sgroas.abd.controller;

import ec.edu.uteq.sgroas.abd.entity.AbdDriver;
import ec.edu.uteq.sgroas.abd.repository.AbdDriverRepository;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AbdDriverControllerTest {

    @Mock
    private AbdDriverRepository conductorAbdRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new AbdDriverController(conductorAbdRepository))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new SpringDataJacksonConfiguration.PageModule(
                                        new SpringDataWebSettings(PageSerializationMode.DIRECT)))
                                .build()))
                .build();
    }

    private AbdDriver conductor() {
        return AbdDriver.builder().idConductor(1).cedula("1200000001")
                .nombres("Carlos").licencia("E").build();
    }

    @Test
    void listWithoutSearchUsesFindAll() throws Exception {
        when(conductorAbdRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(conductor())));

        mockMvc().perform(get("/api/abd/conductores")).andExpect(status().isOk());

        verify(conductorAbdRepository).findAll(any(Pageable.class));
    }

    @Test
    void listBlankUsesFindAll() throws Exception {
        when(conductorAbdRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc().perform(get("/api/abd/conductores").param("search", "   "))
                .andExpect(status().isOk());

        verify(conductorAbdRepository).findAll(any(Pageable.class));
    }

    @Test
    void listWithSearchUsesSearch() throws Exception {
        when(conductorAbdRepository.search(eq("carlos"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conductor())));

        mockMvc().perform(get("/api/abd/conductores").param("search", " Carlos "))
                .andExpect(status().isOk());

        verify(conductorAbdRepository).search(eq("carlos"), any(Pageable.class));
    }
}
