package eventManager.security;

import eventManager.security.jwt.JwtRequestFilter;
import eventManager.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para JwtRequestFilter, el filtro que intercepta las peticiones HTTP para verificar la autenticacion JWT y establecer el contexto de seguridad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtRequestFilter Tests")
class JwtRequestFilterTest {

    private JwtRequestFilter jwtRequestFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtRequestFilter = new JwtRequestFilter(userDetailsService, jwtTokenProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void invokeFilter() throws ServletException, IOException {
        jwtRequestFilter.doFilter(request, response, filterChain);
    }

    /**
     * Verifica que el endpoint de login es publico y no se valida ningun token JWT.
     */
    @Test
    @DisplayName("Endpoint /api/auth/login es publico, no valida JWT")
    void testPublicEndpoint_Login() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifica que el endpoint de registro es publico y no requiere autenticacion.
     */
    @Test
    @DisplayName("Endpoint /api/auth/register es publico")
    void testPublicEndpoint_Register() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifica que el endpoint de recuperacion de contrasena es publico y no requiere autenticacion.
     */
    @Test
    @DisplayName("Endpoint /api/auth/forgot-password es publico")
    void testPublicEndpoint_ForgotPassword() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/forgot-password");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifica que los recursos estaticos como archivos JavaScript son accesibles sin autenticacion.
     */
    @Test
    @DisplayName("Assets estaticos son publicos")
    void testPublicEndpoint_StaticResource() throws Exception {
        when(request.getRequestURI()).thenReturn("/assets/main.js");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifica que las rutas del frontend SPA no requieren validacion JWT.
     */
    @Test
    @DisplayName("Ruta SPA (no /api) es publica")
    void testPublicEndpoint_SpaRoute() throws Exception {
        when(request.getRequestURI()).thenReturn("/eventos");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifica que la interfaz de Swagger UI es accesible sin autenticacion.
     */
    @Test
    @DisplayName("Swagger UI es publico")
    void testPublicEndpoint_SwaggerUI() throws Exception {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifica que un endpoint protegido con cookie JWT valida establece correctamente el SecurityContext.
     */
    @Test
    @DisplayName("Endpoint protegido con cookie valida establece SecurityContext")
    void testProtectedEndpoint_ValidCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/events");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("testuser");
        when(jwtTokenProvider.validateToken(request, true)).thenReturn(claims);

        UserDetails userDetails = new User("testuser", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("testuser", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    /**
     * Verifica que sin cookie JWT presente, el SecurityContext permanece vacio y la peticion continua.
     */
    @Test
    @DisplayName("Endpoint protegido sin cookie limpia SecurityContext")
    void testProtectedEndpoint_NoCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/events");
        when(jwtTokenProvider.validateToken(request, true)).thenThrow(new IllegalArgumentException("No token found in cookies"));

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Verifica que con una cookie JWT invalida, el SecurityContext se limpia y la peticion continua.
     */
    @Test
    @DisplayName("Endpoint protegido con cookie invalida limpia SecurityContext")
    void testProtectedEndpoint_InvalidCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/events");
        when(jwtTokenProvider.validateToken(request, true)).thenThrow(new RuntimeException("Invalid JWT"));

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
