package ec.edu.uteq.sgroas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withoutAuthProtectedAccessResponds403() throws Exception {
        mockMvc.perform(get("/api/conductores"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndPointEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsEsPublico() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithDisallowedOriginResponds403() throws Exception {
        mockMvc.perform(get("/api/conductores")
                        .header("Origin", "https://origen-malicioso.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coordinador@sgroas.com", roles = {"COORDINADOR"})
    void coordinadorPuedeAccederAReportes() throws Exception {
        mockMvc.perform(get("/api/reportes/rendimiento-rutas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "seguridad@sgroas.com", roles = {"SEGURIDAD"})
    void seguridadNoPuedeAccederAReportes() throws Exception {
        mockMvc.perform(get("/api/reportes/rendimiento-rutas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@sgroas.com", roles = {"ADMIN"})
    void adminPuedeAccederAUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "coordinador@sgroas.com", roles = {"COORDINADOR"})
    void coordinadorNoPuedeAccederAUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }
}