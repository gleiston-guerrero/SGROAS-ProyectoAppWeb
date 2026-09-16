package ec.edu.uteq.sgroas.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisConfigTest {

    @Test
    void fabricaYPlantillaSeConstruyenConHostYPuerto() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisHost", "localhost");
        ReflectionTestUtils.setField(config, "redisPort", 6379);

        LettuceConnectionFactory fabrica = config.redisConnectionFactory();

        assertNotNull(fabrica);
        StringRedisTemplate plantilla = config.stringRedisTemplate(fabrica);
        assertNotNull(plantilla);
        assertNotNull(plantilla.getConnectionFactory());
    }
}