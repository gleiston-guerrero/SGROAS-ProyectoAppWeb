package ec.edu.uteq.sgroas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.default-ttl}")
    private long defaultTtl;

    /**
     * Crea el administrador de cache con serializacion JSON y tiempo de vida configurado.
     * @param redisConnectionFactory fabrica con la conexion activa hacia el servidor Redis
     * @return administrador que guarda los valores de cache en formato JSON
     */
    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory redisConnectionFactory) {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .findAndAddModules()
                .build();

        // Sin esto, Jackson2JsonRedisSerializer<Object> deserializa cualquier
        // valor cacheado como un LinkedHashMap generico en vez del tipo real
        // (Page, records de DTO, etc.): no lanza excepcion, simplemente
        // devuelve el tipo equivocado en el segundo hit de cache (verificado
        // en CacheRedisSerializationTest). activateDefaultTyping incrusta el
        // nombre de la clase real en el JSON para que la reconstruya bien,
        // restringido a los paquetes propios de la app para no habilitar
        // deserializacion polimorfica insegura sobre clases arbitrarias.
        var validator = com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("ec.edu.uteq.sgroas.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build();
        mapper.activateDefaultTyping(
                validator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );

        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
