package ec.edu.uteq.sgroas.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderDataSourceConfigTest {

    @Test
    void createDataSourceShouldAssembleJdbcUrl() {
        HikariDataSource ds = RenderDataSourceConfig.createDataSource(
                "dpg-ejemplo-a", "5432", "sgroas_db", "sgroas", "secreto");

        assertEquals("jdbc:postgresql://dpg-ejemplo-a:5432/sgroas_db", ds.getJdbcUrl());
        assertEquals("sgroas", ds.getUsername());
        assertEquals("secreto", ds.getPassword());
        assertEquals("org.postgresql.Driver", ds.getDriverClassName());
    }
}