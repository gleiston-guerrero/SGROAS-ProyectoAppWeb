package ec.edu.uteq.sgroas.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.uteq.sgroas.dto.DriverResponse;
import ec.edu.uteq.sgroas.dto.IncidentResponse;
import ec.edu.uteq.sgroas.service.DriverService.CachedDriverPage;
import ec.edu.uteq.sgroas.service.IncidentService.CachedIncidentPage;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba real, sin Redis levantado (el serializador de Jackson2JsonRedisSerializer
 * es exactamente lo que convierte a/desde bytes; Redis solo los almacena), de
 * la corrección real de P2 sobre @Cacheable + Redis + Page:
 *
 * <p>1. Con el ObjectMapper que CacheConfig usaba ANTES del fix (sin
 *    activateDefaultTyping): deserialize() nunca reconstruye el tipo real,
 *    ni Page ni un record simple -- devuelve un LinkedHashMap generico, sin
 *    lanzar ninguna excepcion. Es peor que un crash: falla en silencio.
 * <p>2. Con el ObjectMapper YA corregido (con activateDefaultTyping,
 *    restringido a paquetes propios): un {@code Page}/{@code PageImpl}
 *    sigue sin poder reconstruirse (no tiene un constructor que Jackson
 *    pueda usar) -- por eso {@code DriverService.listActiveCached} ya NO
 *    devuelve {@code Page<DriverResponse>} directamente, sino
 *    {@link CachedDriverPage}, un record simple que si tiene constructor
 *    canonico utilizable y si sobrevive el ciclo completo.
 */
class CacheRedisSerializationTest {

    private DriverResponse sampleDriver() {
        return new DriverResponse(1L, "Carlos", "Mendoza", "1200000001",
                "LIC-001", "E", LocalDate.now().plusDays(30), "0988888888",
                "carlos@sgroas.com", "ACTIVO", true, false, Instant.now(), Instant.now());
    }

    private ObjectMapper mapperWithoutFix() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .findAndAddModules()
                .build();
    }

    private ObjectMapper mapperWithFix() {
        ObjectMapper mapper = mapperWithoutFix();
        var validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("ec.edu.uteq.sgroas.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build();
        mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    @Test
    void withoutFix_cachedDriverPageComesBackAsGenericMap_notAsRealType() {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(mapperWithoutFix(), Object.class);
        CachedDriverPage cached = new CachedDriverPage(List.of(sampleDriver()), 1);

        byte[] bytes = serializer.serialize(cached);
        Object roundTrip = serializer.deserialize(bytes);

        assertInstanceOf(Map.class, roundTrip,
                "Sin activateDefaultTyping, Jackson deserializa a Object.class como un Map "
                        + "generico en vez del record real -- falla en silencio, sin excepcion");
    }

    @Test
    void withFix_pageImplStillCannotBeReconstructed() {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(mapperWithFix(), Object.class);
        Page<DriverResponse> page = new PageImpl<>(List.of(sampleDriver()), PageRequest.of(0, 10), 1);

        byte[] bytes = serializer.serialize(page);
        assertThrows(SerializationException.class, () -> serializer.deserialize(bytes),
                "PageImpl no tiene un constructor que Jackson pueda usar para deserializar, "
                        + "ni siquiera con el tipo real embebido: no es cacheable directamente");
    }

    @Test
    void withFix_cachedDriverPageRoundTripsAsRealType() {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(mapperWithFix(), Object.class);
        CachedDriverPage cached = new CachedDriverPage(List.of(sampleDriver()), 1);

        byte[] bytes = serializer.serialize(cached);
        Object roundTrip = serializer.deserialize(bytes);

        assertInstanceOf(CachedDriverPage.class, roundTrip);
        assertEquals(cached, roundTrip);
    }

    /**
     * Same fix, applied to IncidentService/RouteService/VehicleService/
     * RouteAssignmentService (found during a rigorous audit: all four had
     * the same self-invocation bug as DriverService, and three of them
     * also miscalculated totalElements as the current page's size instead
     * of the real total). Only one representative record is round-tripped
     * here since all four follow the exact same {@code List<DTO> + long}
     * shape as {@link CachedDriverPage}, already proven above.
     */
    @Test
    void withFix_cachedIncidentPageRoundTripsAsRealType() {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(mapperWithFix(), Object.class);
        IncidentResponse incident = new IncidentResponse(1L, 1L, "Carlos", "AVERIA_MECANICA",
                "Falla de motor", java.time.LocalDateTime.now(), "Quito", "ALTA", "ABIERTO",
                true, Instant.now(), Instant.now());
        CachedIncidentPage cached = new CachedIncidentPage(List.of(incident), 1);

        byte[] bytes = serializer.serialize(cached);
        Object roundTrip = serializer.deserialize(bytes);

        assertInstanceOf(CachedIncidentPage.class, roundTrip);
        assertEquals(cached, roundTrip);
    }
}
