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
 * Unit tests for JwtRequestFilter, the filter that intercepts HTTP requests to
 * validate JWT authentication and set the security context.
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
     * Verifies that the login endpoint is public and no JWT token is validated.
     */
    @Test
    @DisplayName("Endpoint /api/auth/login is public, does not validate JWT")
    void testPublicEndpoint_Login() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifies that the registration endpoint is public and does not require
     * authentication.
     */
    @Test
    @DisplayName("Endpoint /api/auth/register is public")
    void testPublicEndpoint_Register() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifies that the password recovery endpoint is public and does not require
     * authentication.
     */
    @Test
    @DisplayName("Endpoint /api/auth/forgot-password is public")
    void testPublicEndpoint_ForgotPassword() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/forgot-password");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifies that static resources like JavaScript files are accessible without
     * authentication.
     */
    @Test
    @DisplayName("Static assets are public")
    void testPublicEndpoint_StaticResource() throws Exception {
        when(request.getRequestURI()).thenReturn("/assets/main.js");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifies that SPA frontend routes do not require JWT validation.
     */
    @Test
    @DisplayName("SPA route (non-/api) is public")
    void testPublicEndpoint_SpaRoute() throws Exception {
        when(request.getRequestURI()).thenReturn("/eventos");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifies that the Swagger UI is accessible without authentication.
     */
    @Test
    @DisplayName("Swagger UI is public")
    void testPublicEndpoint_SwaggerUI() throws Exception {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    /**
     * Verifies that a protected endpoint with a valid JWT cookie correctly sets the
     * SecurityContext.
     */
    @Test
    @DisplayName("Protected endpoint with valid cookie sets SecurityContext")
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
     * Verifies that without a JWT cookie, the SecurityContext remains empty and the
     * request continues.
     */
    @Test
    @DisplayName("Protected endpoint without cookie clears SecurityContext")
    void testProtectedEndpoint_NoCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/events");
        when(jwtTokenProvider.validateToken(request, true))
                .thenThrow(new IllegalArgumentException("No token found in cookies"));

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Verifies that with an invalid JWT cookie, the SecurityContext is cleared and
     * the request continues.
     */
    @Test
    @DisplayName("Protected endpoint with invalid cookie clears SecurityContext")
    void testProtectedEndpoint_InvalidCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/events");
        when(jwtTokenProvider.validateToken(request, true)).thenThrow(new RuntimeException("Invalid JWT"));

        invokeFilter();

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
