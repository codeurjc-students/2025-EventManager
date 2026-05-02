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
 * Unit tests for JwtTokenProvider, which generates, validates, and extracts JWT
 * tokens used in application authentication.
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
     * Verifies that a valid access token is generated with the correct claims.
     */
    @Test
    @DisplayName("generateAccessToken - Valid token with correct claims")
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
     * Verifies that a refresh token is generated with type REFRESH in its claims.
     */
    @Test
    @DisplayName("generateRefreshToken - Valid token with type=REFRESH")
    void testGenerateRefreshToken_Valid() {
        String token = jwtTokenProvider.generateRefreshToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtTokenProvider.validateToken(token);
        assertEquals("carlos.martinez", claims.getSubject());
        assertEquals("REFRESH", claims.get("type", String.class));
    }

    /**
     * Verifies that a valid token returns its claims correctly when validated.
     */
    @Test
    @DisplayName("validateToken(String) - Valid token returns claims")
    void testValidateToken_ValidToken() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        Claims claims = jwtTokenProvider.validateToken(token);

        assertNotNull(claims);
        assertEquals("carlos.martinez", claims.getSubject());
    }

    /**
     * Verifies that a malformed token throws an exception when validated.
     */
    @Test
    @DisplayName("validateToken(String) - Malformed token throws exception")
    void testValidateToken_MalformedToken() {
        assertThrows(Exception.class, () -> jwtTokenProvider.validateToken("this.is.not.a.valid.jwt"));
    }

    /**
     * Verifies that a manually tampered token is rejected when validated.
     */
    @Test
    @DisplayName("validateToken(String) - Tampered token throws exception")
    void testValidateToken_TamperedToken() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "El JWT debería tener tres partes");

        char[] payloadChars = parts[1].toCharArray();
        int tamperIndex = payloadChars.length / 2;
        payloadChars[tamperIndex] = payloadChars[tamperIndex] == 'A' ? 'B' : 'A';
        String tamperedPayload = new String(payloadChars);

        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(Exception.class, () -> jwtTokenProvider.validateToken(tampered));
    }

    /**
     * Verifies that an empty string is rejected when validated as a token.
     */
    @Test
    @DisplayName("validateToken(String) - Empty string throws exception")
    void testValidateToken_EmptyString() {
        assertThrows(Exception.class, () -> jwtTokenProvider.validateToken(""));
    }

    /**
     * Verifies that the token is correctly extracted from an Authorization header
     * with Bearer prefix.
     */
    @Test
    @DisplayName("tokenStringFromHeaders - Valid Bearer header")
    void testTokenFromHeaders_ValidBearer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer mytoken123");

        String token = jwtTokenProvider.tokenStringFromHeaders(request);
        assertEquals("mytoken123", token);
    }

    /**
     * Verifies that an exception is thrown when the Authorization header is
     * missing.
     */
    @Test
    @DisplayName("tokenStringFromHeaders - Missing header throws IllegalArgumentException")
    void testTokenFromHeaders_MissingHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jwtTokenProvider.tokenStringFromHeaders(request));
        assertTrue(ex.getMessage().contains("Missing Authorization header"));
    }

    /**
     * Verifies that an exception is thrown when the Authorization header uses a
     * scheme other than Bearer.
     */
    @Test
    @DisplayName("tokenStringFromHeaders - Basic header throws IllegalArgumentException")
    void testTokenFromHeaders_NonBearerHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jwtTokenProvider.tokenStringFromHeaders(request));
        assertTrue(ex.getMessage().contains("does not start with Bearer"));
    }

    /**
     * Verifies that a token received via a cookie is validated correctly.
     */
    @Test
    @DisplayName("validateToken(request, true) - Valid cookie")
    void testValidateToken_FromCookie_Valid() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("AuthToken", token);
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        Claims claims = jwtTokenProvider.validateToken(request, true);
        assertEquals("carlos.martinez", claims.getSubject());
    }

    /**
     * Verifies that an exception is thrown when validating from cookies and none
     * are present.
     */
    @Test
    @DisplayName("validateToken(request, true) - No cookies throws IllegalArgumentException")
    void testValidateToken_FromCookie_NoCookies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jwtTokenProvider.validateToken(request, true));
        assertTrue(ex.getMessage().contains("cookies"));
    }

    /**
     * Verifies that a token received via the Authorization header is validated
     * correctly.
     */
    @Test
    @DisplayName("validateToken(request, false) - From valid header")
    void testValidateToken_FromHeader_Valid() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        Claims claims = jwtTokenProvider.validateToken(request, false);
        assertEquals("carlos.martinez", claims.getSubject());
    }
}
