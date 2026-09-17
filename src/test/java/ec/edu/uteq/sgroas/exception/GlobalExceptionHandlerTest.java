package ec.edu.uteq.sgroas.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void erroresDeValidacionDebenRetornarProblemDetail() {
        when(request.getRequestURI()).thenReturn("/api/conductores");
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("conductor", "cedula", "La cedula es obligatoria")
        ));
        MethodArgumentNotValidException ex =
                org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail detail = handler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), detail.getStatus());
        assertEquals("Error de validacion", detail.getTitle());
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) detail.getProperties().get("errors");
        assertEquals("La cedula es obligatoria", errores.get("cedula"));
    }

    @Test
    void argumentosInvalidosDebenRetornarBadRequest() {
        when(request.getRequestURI()).thenReturn("/api/conductores");

        ProblemDetail detail = handler.handleInvalidArguments(
                new IllegalArgumentException("Driver no encontrado"), request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), detail.getStatus());
        assertEquals("Driver no encontrado", detail.getDetail());
    }

    @Test
    void credencialesInvalidasDebenRetornarNoAutorizado() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        ProblemDetail detail = handler.handleInvalidCredentials(
                new BadCredentialsException("Credenciales invalidas"), request);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), detail.getStatus());
        assertEquals("Credenciales invalidas", detail.getTitle());
    }

    @Test
    void routeNonexistentReturnsNotFound() {
        when(request.getRequestURI()).thenReturn("/api/auth/register");

        ProblemDetail detail = handler.handleNotFound(
                new org.springframework.web.servlet.resource.NoResourceFoundException(
                        null, "api/auth/register"),
                request);

        assertEquals(HttpStatus.NOT_FOUND.value(), detail.getStatus());
        assertEquals("Recurso no encontrado", detail.getTitle());
    }

    @Test
    void generalErrorShouldReturnInternalServerError() {
        when(request.getRequestURI()).thenReturn("/api/conductores");

        ProblemDetail detail = handler.handleGeneralError(
                new RuntimeException("Falla inesperada"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), detail.getStatus());
        assertTrue(detail.getDetail().contains("Falla inesperada"));
    }

    @Test
    void unverifiedEmailShouldReturnForbidden() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        ProblemDetail detail = handler.handleUnverifiedEmail(
                new UnverifiedEmailException("Correo no verificado"), request);

        assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
        assertEquals("Correo sin verificar", detail.getTitle());
        assertEquals("Correo no verificado", detail.getDetail());
    }

    @Test
    void accessDeniedShouldReturnForbidden() {
        when(request.getRequestURI()).thenReturn("/api/abd/unidades");

        ProblemDetail detail = handler.handleAccessDenied(
                new org.springframework.security.access.AccessDeniedException("Denegado"), request);

        assertEquals(HttpStatus.FORBIDDEN.value(), detail.getStatus());
        assertEquals("Acceso denegado", detail.getTitle());
        assertEquals("No tiene permisos para realizar esta operacion", detail.getDetail());
    }
}
