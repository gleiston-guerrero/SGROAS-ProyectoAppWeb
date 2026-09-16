package ec.edu.uteq.sgroas.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheConfigTest {

    @Test
    void cacheManagerSeConstruyeConTtlConfigurado() {
        CacheConfig config = new CacheConfig();
        ReflectionTestUtils.setField(config, "defaultTtl", 60L);

        CacheManager manager = config.cacheManager(new LettuceConnectionFactory());

        assertNotNull(manager);
        assertTrue(manager.getCacheNames().isEmpty());
    }
}