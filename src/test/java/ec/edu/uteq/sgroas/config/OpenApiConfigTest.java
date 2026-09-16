package ec.edu.uteq.sgroas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    void openAPIDefineTituloVersionYSeguridad() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI api = config.openAPI();

        assertNotNull(api.getInfo());
        assertEquals("SGROAS API", api.getInfo().getTitle());
        assertEquals("0.9.0-rc", api.getInfo().getVersion());
        assertEquals("Sistema de Gestion de Recursos Operativos, Administrativos y de Seguridad",
                api.getInfo().getDescription());

        Map<String, SecurityScheme> esquemas = api.getComponents().getSecuritySchemes();
        assertNotNull(esquemas.get("cookieAuth"));
        assertEquals(SecurityScheme.Type.APIKEY, esquemas.get("cookieAuth").getType());
        assertEquals(SecurityScheme.In.COOKIE, esquemas.get("cookieAuth").getIn());
        assertEquals("access_token", esquemas.get("cookieAuth").getName());
    }
}