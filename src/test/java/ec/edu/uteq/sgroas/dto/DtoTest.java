package ec.edu.uteq.sgroas.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void errorResponseShouldPreserveValues() {
        Instant ahora = Instant.now();
        ErrorResponse response = new ErrorResponse(
                ahora, 400, "Bad Request", "Mensaje", "/api/test",
                Map.of("campo", "error")
        );

        assertEquals(ahora, response.timestamp());
        assertEquals(400, response.status());
        assertEquals("Bad Request", response.error());
        assertEquals("Mensaje", response.message());
        assertEquals("/api/test", response.path());
        assertEquals("error", response.details().get("campo"));
        assertEquals(response, new ErrorResponse(
                ahora, 400, "Bad Request", "Mensaje", "/api/test",
                Map.of("campo", "error")));
        assertEquals(response.hashCode(), new ErrorResponse(
                ahora, 400, "Bad Request", "Mensaje", "/api/test",
                Map.of("campo", "error")).hashCode());
        assertTrue(response.toString().contains("Mensaje"));
    }

    @Test
    void emailRequestShouldPreserveValues() {
        EmailRequest request = new EmailRequest("maria@sgroas.com");

        assertEquals("maria@sgroas.com", request.email());
    }

    @Test
    void verifyEmailRequestPreservesValues() {
        VerifyEmailRequest request = new VerifyEmailRequest(
                "maria@sgroas.com", "123456"
        );

        assertEquals("maria@sgroas.com", request.email());
        assertEquals("123456", request.codigo());
    }

    @Test
    void resetPasswordRequestShouldPreserveValues() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "maria@sgroas.com", "654321", "nuevaClave1"
        );

        assertEquals("maria@sgroas.com", request.email());
        assertEquals("654321", request.codigo());
        assertEquals("nuevaClave1", request.nuevaPassword());
    }

    @Test
    void refreshTokenRequestShouldPreserveValues() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        assertEquals("refresh-token", request.refreshToken());
    }

    @Test
    void userRequestPreservesValues() {
        UserRequest request = new UserRequest(
                "Carlos Mendoza", "carlos@sgroas.com", "123456", "ROLE_ADMIN"
        );

        assertEquals("Carlos Mendoza", request.name());
        assertEquals("carlos@sgroas.com", request.email());
        assertEquals("123456", request.password());
        assertEquals("ROLE_ADMIN", request.role());
    }

    @Test
    void userResponsePreservesValues() {
        Instant ahora = Instant.now();
        UserResponse response = new UserResponse(
                1L, "Carlos Mendoza", "carlos@sgroas.com",
                "ROLE_ADMIN", true, ahora, ahora
        );

        assertEquals(1L, response.id());
        assertEquals("Carlos Mendoza", response.name());
        assertEquals("carlos@sgroas.com", response.email());
        assertEquals("ROLE_ADMIN", response.role());
        assertTrue(response.active());
        assertEquals(ahora, response.createdAt());
        assertEquals(ahora, response.updatedAt());
    }

    @Test
    void vehicleRequestPreservesValues() {
        VehicleRequest request = new VehicleRequest(
                "GTU-001", "Toyota", "Hiace", 2020, 14,
                "MOT-123", "CHAS-123", "Blanco", "ACTIVO"
        );

        assertEquals("GTU-001", request.plate());
        assertEquals("Toyota", request.brand());
        assertEquals("Hiace", request.model());
        assertEquals(2020, request.year());
        assertEquals(14, request.capacity());
        assertEquals("MOT-123", request.engineNumber());
        assertEquals("CHAS-123", request.chassisNumber());
        assertEquals("Blanco", request.color());
        assertEquals("ACTIVO", request.status());
    }

    @Test
    void vehicleResponsePreservesValues() {
        Instant ahora = Instant.now();
        VehicleResponse response = new VehicleResponse(
                1L, "GTU-001", "Toyota", "Hiace", 2020, 14,
                "MOT-123", "CHAS-123", "Blanco", "ACTIVO", true, ahora, ahora
        );

        assertEquals(1L, response.id());
        assertEquals("GTU-001", response.plate());
        assertEquals("Toyota", response.brand());
        assertEquals("Hiace", response.model());
        assertEquals(2020, response.year());
        assertEquals(14, response.capacity());
        assertEquals("MOT-123", response.engineNumber());
        assertEquals("CHAS-123", response.chassisNumber());
        assertEquals("Blanco", response.color());
        assertEquals("ACTIVO", response.status());
        assertTrue(response.active());
    }

    @Test
    void routeRequestPreservesValues() {
        RouteRequest request = new RouteRequest(
                "R-001", "Quito - Guayaquil", "Quito", "Guayaquil",
                420.0, 480, "ACTIVA"
        );

        assertEquals("R-001", request.code());
        assertEquals("Quito - Guayaquil", request.name());
        assertEquals("Quito", request.origin());
        assertEquals("Guayaquil", request.destination());
        assertEquals(420.0, request.distanceKm());
        assertEquals(480, request.durationMin());
        assertEquals("ACTIVA", request.status());
    }

    @Test
    void routeResponsePreservesValues() {
        Instant ahora = Instant.now();
        RouteResponse response = new RouteResponse(
                1L, "R-001", "Quito - Guayaquil", "Quito", "Guayaquil",
                420.0, 480, "ACTIVA", true, ahora, ahora
        );

        assertEquals(1L, response.id());
        assertEquals("R-001", response.code());
        assertEquals("Quito - Guayaquil", response.name());
        assertEquals("Quito", response.origin());
        assertEquals("Guayaquil", response.destination());
        assertEquals(420.0, response.distanceKm());
        assertEquals(480, response.durationMin());
        assertEquals("ACTIVA", response.status());
        assertTrue(response.active());
    }

    @Test
    void incidentRequestPreservesValues() {
        LocalDateTime fecha = LocalDateTime.now();
        IncidentRequest request = new IncidentRequest(
                1L, "Carlos Mendoza", "AVERIA_MECANICA", "Falla en el motor",
                fecha, "Km 12", "MEDIA", "REPORTADO"
        );

        assertEquals(1L, request.assignmentId());
        assertEquals("Carlos Mendoza", request.reportedBy());
        assertEquals("AVERIA_MECANICA", request.type());
        assertEquals("Falla en el motor", request.description());
        assertEquals(fecha, request.incidentDate());
        assertEquals("Km 12", request.location());
        assertEquals("MEDIA", request.severity());
        assertEquals("REPORTADO", request.status());
    }

    @Test
    void incidentResponsePreservesValues() {
        Instant ahora = Instant.now();
        LocalDateTime fecha = LocalDateTime.now();
        IncidentResponse response = new IncidentResponse(
                1L, 1L, "Carlos Mendoza", "AVERIA_MECANICA",
                "Falla en el motor", fecha, "Km 12", "MEDIA",
                "REPORTADO", true, ahora, ahora
        );

        assertEquals(1L, response.id());
        assertEquals(1L, response.assignmentId());
        assertEquals("Carlos Mendoza", response.reportedBy());
        assertEquals("AVERIA_MECANICA", response.type());
        assertEquals("Falla en el motor", response.description());
        assertEquals(fecha, response.incidentDate());
        assertEquals("Km 12", response.location());
        assertEquals("MEDIA", response.severity());
        assertEquals("REPORTADO", response.status());
        assertTrue(response.active());
    }

    @Test
    void routeAssignmentRequestPreservesValues() {
        LocalDate fecha = LocalDate.now();
        RouteAssignmentRequest request = new RouteAssignmentRequest(
                1L, 1L, 1L, fecha, fecha, fecha.plusDays(1), "ACTIVA"
        );

        assertEquals(1L, request.driverId());
        assertEquals(1L, request.vehicleId());
        assertEquals(1L, request.routeId());
        assertEquals(fecha, request.assignmentDate());
        assertEquals(fecha, request.startDate());
        assertEquals(fecha.plusDays(1), request.endDate());
        assertEquals("ACTIVA", request.status());
    }

    @Test
    void routeAssignmentResponsePreservesValues() {
        Instant ahora = Instant.now();
        LocalDate fecha = LocalDate.now();
        RouteAssignmentResponse response = new RouteAssignmentResponse(
                1L, 1L, "Carlos Mendoza", 1L, "GTU-001", 1L,
                "Quito - Guayaquil", fecha, fecha, fecha.plusDays(1),
                "ACTIVA", true, ahora, ahora
        );

        assertEquals(1L, response.id());
        assertEquals(1L, response.driverId());
        assertEquals("Carlos Mendoza", response.driverName());
        assertEquals(1L, response.vehicleId());
        assertEquals("GTU-001", response.vehiclePlate());
        assertEquals(1L, response.routeId());
        assertEquals("Quito - Guayaquil", response.routeName());
        assertEquals(fecha, response.assignmentDate());
        assertEquals(fecha, response.startDate());
        assertEquals(fecha.plusDays(1), response.endDate());
        assertEquals("ACTIVA", response.status());
        assertTrue(response.active());
    }
}
