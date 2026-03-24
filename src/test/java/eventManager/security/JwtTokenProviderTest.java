package eventManager.security;

import eventManager.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para JwtTokenProvider, que se encarga de generar, validar y extraer tokens JWT utilizados en la autenticacion de la aplicacion.
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        userDetails = new User("carlos.martinez", "ClaveSegura2025", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * Verifica que se genera un token de acceso valido con los claims correctos.
     */
    @Test
    @DisplayName("generateAccessToken - Token válido con claims correctos")
    void testGenerateAccessToken_Valid() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtTokenProvider.validateToken(token);
        assertEquals("carlos.martinez", claims.getSubject());
        assertEquals("ACCESS", claims.get("type", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    /**
     * Verifica que se genera un token de refresco con el tipo REFRESH en sus claims.
     */
    @Test
    @DisplayName("generateRefreshToken - Token válido con type=REFRESH")
    void testGenerateRefreshToken_Valid() {
        String token = jwtTokenProvider.generateRefreshToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtTokenProvider.validateToken(token);
        assertEquals("carlos.martinez", claims.getSubject());
        assertEquals("REFRESH", claims.get("type", String.class));
    }

    /**
     * Comprueba que un token valido retorna correctamente sus claims al ser validado.
     */
    @Test
    @DisplayName("validateToken(String) - Token válido retorna claims")
    void testValidateToken_ValidToken() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        Claims claims = jwtTokenProvider.validateToken(token);

        assertNotNull(claims);
        assertEquals("carlos.martinez", claims.getSubject());
    }

    /**
     * Comprueba que un token con formato incorrecto lanza una excepcion al validarse.
     */
    @Test
    @DisplayName("validateToken(String) - Token malformado lanza excepción")
    void testValidateToken_MalformedToken() {
        assertThrows(Exception.class, () -> jwtTokenProvider.validateToken("this.is.not.a.valid.jwt"));
    }

    /**
     * Verifica que un token alterado manualmente es rechazado al intentar validarse.
     */
    @Test
    @DisplayName("validateToken(String) - Token manipulado lanza excepción")
    void testValidateToken_TamperedToken() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        String tampered = token.substring(0, token.length() - 1) + (token.charAt(token.length() - 1) == 'A' ? 'B' : 'A');

        assertThrows(Exception.class, () -> jwtTokenProvider.validateToken(tampered));
    }

    /**
     * Comprueba que un string vacio es rechazado al intentar validarse como token.
     */
    @Test
    @DisplayName("validateToken(String) - String vacío lanza excepción")
    void testValidateToken_EmptyString() {
        assertThrows(Exception.class, () -> jwtTokenProvider.validateToken(""));
    }

    /**
     * Verifica que se extrae correctamente el token desde un header Authorization con prefijo Bearer.
     */
    @Test
    @DisplayName("tokenStringFromHeaders - Header Bearer válido")
    void testTokenFromHeaders_ValidBearer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer mytoken123");

        String token = jwtTokenProvider.tokenStringFromHeaders(request);
        assertEquals("mytoken123", token);
    }

    /**
     * Comprueba que se lanza excepcion cuando no existe el header Authorization en la peticion.
     */
    @Test
    @DisplayName("tokenStringFromHeaders - Header ausente lanza IllegalArgumentException")
    void testTokenFromHeaders_MissingHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.tokenStringFromHeaders(request));
        assertTrue(ex.getMessage().contains("Missing Authorization header"));
    }

    /**
     * Verifica que se lanza excepcion cuando el header Authorization usa un esquema distinto a Bearer.
     */
    @Test
    @DisplayName("tokenStringFromHeaders - Header Basic lanza IllegalArgumentException")
    void testTokenFromHeaders_NonBearerHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.tokenStringFromHeaders(request));
        assertTrue(ex.getMessage().contains("does not start with Bearer"));
    }

    /**
     * Comprueba que se valida correctamente un token recibido a traves de una cookie.
     */
    @Test
    @DisplayName("validateToken(request, true) - Cookie válida")
    void testValidateToken_FromCookie_Valid() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("AuthToken", token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        Claims claims = jwtTokenProvider.validateToken(request, true);
        assertEquals("carlos.martinez", claims.getSubject());
    }

    /**
     * Verifica que se lanza excepcion al intentar validar desde cookies cuando no hay ninguna.
     */
    @Test
    @DisplayName("validateToken(request, true) - Sin cookies lanza IllegalArgumentException")
    void testValidateToken_FromCookie_NoCookies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.validateToken(request, true));
        assertTrue(ex.getMessage().contains("cookies"));
    }

    /**
     * Comprueba que se valida correctamente un token recibido a traves del header Authorization.
     */
    @Test
    @DisplayName("validateToken(request, false) - Desde header válido")
    void testValidateToken_FromHeader_Valid() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        Claims claims = jwtTokenProvider.validateToken(request, false);
        assertEquals("carlos.martinez", claims.getSubject());
    }
}
