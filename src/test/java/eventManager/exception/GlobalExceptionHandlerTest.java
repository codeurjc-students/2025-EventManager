package eventManager.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GlobalExceptionHandler, que gestiona de forma centralizada las excepciones lanzadas por los controladores REST de la aplicacion.
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    private ErrorResponse assertBodyPresent(ResponseEntity<ErrorResponse> response) {
        ErrorResponse body = response.getBody();
        assertNotNull(body, "La respuesta debería incluir un body");
        return body;
    }

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    /**
     * Verifica que una CustomException con BAD_REQUEST devuelve estado 400 y el mensaje correcto.
     */
    @Test
    @DisplayName("handleCustomException - BAD_REQUEST")
    void testHandleCustomException_BadRequest() {
        CustomException ex = new CustomException(HttpStatus.BAD_REQUEST, "Datos inválidos");
        ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, body.getStatus());
        assertEquals("Datos inválidos", body.getMessage());
    }

    /**
     * Verifica que una CustomException con NOT_FOUND devuelve estado 404 y el mensaje correcto.
     */
    @Test
    @DisplayName("handleCustomException - NOT_FOUND")
    void testHandleCustomException_NotFound() {
        CustomException ex = new CustomException(HttpStatus.NOT_FOUND, "Recurso no encontrado");
        ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, body.getStatus());
        assertEquals("Recurso no encontrado", body.getMessage());
    }

    /**
     * Verifica que una CustomException con FORBIDDEN devuelve estado 403.
     */
    @Test
    @DisplayName("handleCustomException - FORBIDDEN")
    void testHandleCustomException_Forbidden() {
        CustomException ex = new CustomException(HttpStatus.FORBIDDEN, "Acceso denegado");
        ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, body.getStatus());
    }

    /**
     * Verifica que una CustomException con INTERNAL_SERVER_ERROR devuelve estado 500.
     */
    @Test
    @DisplayName("handleCustomException - INTERNAL_SERVER_ERROR")
    void testHandleCustomException_InternalServerError() {
        CustomException ex = new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
        ResponseEntity<ErrorResponse> response = handler.handleCustomException(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, body.getStatus());
    }

    /**
     * Verifica que una excepcion de credenciales incorrectas devuelve estado 401.
     */
    @Test
    @DisplayName("handleBadCredentialsException - Devuelve 401")
    void testHandleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, body.getStatus());
        assertEquals("El usuario o la contraseña es incorrecto", body.getMessage());
    }

    /**
     * Verifica que un error de validacion en el campo eventCode devuelve el mensaje apropiado.
     */
    @Test
    @DisplayName("handleValidationExceptions - campo eventCode")
    void testHandleValidationExceptions_EventCodeField() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "eventCode", "must be 6 chars");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(body.getMessage().contains("código del evento"));
    }

    /**
     * Verifica que un error de validacion en el campo notes devuelve el mensaje apropiado.
     */
    @Test
    @DisplayName("handleValidationExceptions - campo notes")
    void testHandleValidationExceptions_NotesField() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "notes", "too long");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(body.getMessage().contains("notas"));
    }

    /**
     * Verifica que un error de validacion en un campo generico devuelve el mensaje por defecto.
     */
    @Test
    @DisplayName("handleValidationExceptions - otro campo genérico")
    void testHandleValidationExceptions_OtherField() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "email", "invalid format");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(body.getMessage().contains("Datos inválidos"));
    }

    /**
     * Verifica que una excepcion generica no controlada devuelve estado 500 con mensaje por defecto.
     */
    @Test
    @DisplayName("handleGenericException - Devuelve 500")
    void testHandleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);
        ErrorResponse body = assertBodyPresent(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, body.getStatus());
        assertEquals("Ha ocurrido un error inesperado", body.getMessage());
    }
}
