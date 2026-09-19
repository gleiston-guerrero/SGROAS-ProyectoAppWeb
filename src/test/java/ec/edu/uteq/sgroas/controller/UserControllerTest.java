package ec.edu.uteq.sgroas.controller;

import ec.edu.uteq.sgroas.dto.UserResponse;
import ec.edu.uteq.sgroas.service.UserService;
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
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new SpringDataJacksonConfiguration.PageModule(
                                        new SpringDataWebSettings(PageSerializationMode.DIRECT)))
                                .build()))
                .build();
    }

    private UserResponse sampleResponse() {
        return new UserResponse(
                1L, "Administrador SGROAS", "admin@sgroas.com",
                "ROLE_ADMIN", true, Instant.now(), Instant.now()
        );
    }

    @Test
    void listReturns200() throws Exception {
        when(userService.list(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc().perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    void findByIdReturns200() throws Exception {
        when(userService.findById(1L)).thenReturn(sampleResponse());

        mockMvc().perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@sgroas.com"));
    }

    @Test
    void createReturns201() throws Exception {
        when(userService.create(any())).thenReturn(sampleResponse());

        mockMvc().perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Administrador SGROAS",
                                  "email": "admin@sgroas.com",
                                  "password": "123456",
                                  "role": "ROLE_ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        when(userService.update(any(), any())).thenReturn(sampleResponse());

        mockMvc().perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Administrador SGROAS",
                                  "email": "admin@sgroas.com",
                                  "password": "123456",
                                  "role": "ROLE_ADMIN"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc().perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());
    }
}
