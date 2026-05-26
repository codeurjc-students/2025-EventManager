package eventManager.service.impl;

import eventManager.constant.Constantes;
import eventManager.dto.UserCreateDTO;
import eventManager.dto.UserDTO;
import eventManager.dto.UserForgottenPassword;
import eventManager.entity.User;
import eventManager.entity.UserRole;
import eventManager.exception.CustomException;
import eventManager.mapper.UserMapper;
import eventManager.repository.UserRepository;
import eventManager.security.jwt.AuthResponse;
import eventManager.security.jwt.JwtTokenProvider;
import eventManager.security.jwt.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the authentication service, including registration, login,
 * token refresh, logout, and forgotten password changes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    private AuthServiceImpl authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private HttpServletResponse response;
    private UserCreateDTO userCreateDTO;
    private User testUser;
    private UserDTO testUserDTO;
    private org.springframework.security.core.userdetails.UserDetails springUserDetails;

    @BeforeEach
    void setUp() {
        response = mock(HttpServletResponse.class);

        authService = new AuthServiceImpl(authenticationManager, userDetailsService, jwtTokenProvider);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "userMapper", userMapper);

        userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("carlos.martinez@eventmanager.es");
        userCreateDTO.setUsername("carlos.martinez");
        userCreateDTO.setPassword("ClaveSegura2025");
        userCreateDTO.setFirstName("Carlos");
        userCreateDTO.setLastName("Martinez");
        userCreateDTO.setPhoneNumber("612345678");

        testUser = User.builder()
                .userId(1)
                .email("carlos.martinez@eventmanager.es")
                .username("carlos.martinez")
                .password("encodedPassword")
                .firstName("Carlos")
                .lastName("Martinez")
                .phoneNumber("612345678")
                .role(UserRole.USER)
                .build();

        testUserDTO = new UserDTO();
        testUserDTO.setUserId(1);
        testUserDTO.setUsername("carlos.martinez");

        springUserDetails = new org.springframework.security.core.userdetails.User(
                "carlos.martinez", "encodedPassword",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * Verifies that registering a new user completes successfully and generates
     * access tokens.
     */
    @Test
    @DisplayName("registerUser - Successful registration")
    void testRegister_Success() {
        when(userRepository.existsByEmailOrUsername("carlos.martinez@eventmanager.es", "carlos.martinez"))
                .thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura2025")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername("carlos.martinez")).thenReturn(springUserDetails);
        when(jwtTokenProvider.generateAccessToken(springUserDetails)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(springUserDetails)).thenReturn("refreshToken");

        AuthResponse result = authService.registerUser(userCreateDTO, response);

        assertNotNull(result);
        assertEquals(AuthResponse.Status.SUCCESS, result.getStatus());
        verify(userRepository).save(any(User.class));
        verify(response, times(2)).addCookie(any());
    }

    /**
     * Verifies that registering a user with an existing email or username throws
     * BAD_REQUEST.
     */
    @Test
    @DisplayName("registerUser - User already exists, throws BAD_REQUEST")
    void testRegister_UserAlreadyExists() {
        when(userRepository.existsByEmailOrUsername("carlos.martinez@eventmanager.es", "carlos.martinez"))
                .thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> authService.registerUser(userCreateDTO, response));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_ALREADY_REGISTERED, ex.getMessage());
    }

    /**
     * Verifies that an unexpected error during registration results in
     * INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("registerUser - Unexpected error, throws INTERNAL_SERVER_ERROR")
    void testRegister_UnexpectedError() {
        when(userRepository.existsByEmailOrUsername("carlos.martinez@eventmanager.es", "carlos.martinez"))
                .thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class,
                () -> authService.registerUser(userCreateDTO, response));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals("DB error", ex.getMessage());
    }

    /**
     * Verifies that login with valid credentials returns a successful response with
     * cookies.
     */
    @Test
    @DisplayName("login - Successful login")
    void testLogin_Success() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("carlos.martinez")
                .password("ClaveSegura2025")
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userDetailsService.loadUserByUsername("carlos.martinez")).thenReturn(springUserDetails);
        when(jwtTokenProvider.generateAccessToken(springUserDetails)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(springUserDetails)).thenReturn("refreshToken");

        AuthResponse result = authService.login(loginRequest, response);

        assertNotNull(result);
        assertEquals(AuthResponse.Status.SUCCESS, result.getStatus());
        verify(response, times(2)).addCookie(any());
    }

    /**
     * Verifies that invalid credentials during login raise UNAUTHORIZED.
     */
    @Test
    @DisplayName("login - Invalid credentials, throws UNAUTHORIZED")
    void testLogin_InvalidCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("wrong")
                .password("wrong")
                .build();

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        CustomException ex = assertThrows(CustomException.class, () -> authService.login(loginRequest, response));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(Constantes.MESSAGE_INCORRECT_USER_OR_PASSWORD, ex.getMessage());
    }

    /**
     * Verifies that a valid refresh token allows access token renewal.
     */
    @Test
    @DisplayName("refreshToken - Valid token, refreshes access token")
    void testRefreshToken_Success() {
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn("carlos.martinez");
        when(jwtTokenProvider.validateToken("validRefreshToken")).thenReturn(claims);
        when(userDetailsService.loadUserByUsername("carlos.martinez")).thenReturn(springUserDetails);
        when(jwtTokenProvider.generateAccessToken(springUserDetails)).thenReturn("newAccessToken");

        AuthResponse result = authService.refreshToken("validRefreshToken", response);

        assertEquals(AuthResponse.Status.SUCCESS, result.getStatus());
        verify(response).addCookie(any());
    }

    /**
     * Verifies that an invalid refresh token returns a FAILURE response without
     * throwing an exception.
     */
    @Test
    @DisplayName("refreshToken - Invalid token, returns FAILURE without exception")
    void testRefreshToken_InvalidToken() {
        when(jwtTokenProvider.validateToken("invalidToken")).thenThrow(new RuntimeException("Invalid token"));

        AuthResponse result = authService.refreshToken("invalidToken", response);

        assertEquals(AuthResponse.Status.FAILURE, result.getStatus());
    }

    /**
     * Verifies that logout clears the security context and removes cookies.
     */
    @Test
    @DisplayName("logout - Successful logout, clears context and cookies")
    void testLogout_Success() {
        AuthResponse result = authService.logout(response);

        assertEquals(AuthResponse.Status.SUCCESS, result.getStatus());
        verify(response, times(2)).addCookie(any());
    }

    /**
     * Verifies that forgotten password change succeeds when data is valid.
     */
    @Test
    @DisplayName("changeForgottenPassword - Successful change")
    void testChangeForgottenPassword_Success() {
        String usernameValue = "carlos.martinez";
        User userWithMatchingName = User.builder()
                .userId(1)
                .email("carlos.martinez@eventmanager.es")
                .username(usernameValue)
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();

        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("carlos.martinez@eventmanager.es");
        dto.setUsername(usernameValue);
        dto.setNewPassword("newPass123");
        dto.setNewPasswordConfirm("newPass123");

        when(userRepository.findByEmail("carlos.martinez@eventmanager.es"))
                .thenReturn(Optional.of(userWithMatchingName));
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");
        when(userRepository.save(userWithMatchingName)).thenReturn(userWithMatchingName);
        when(userMapper.convertUserToUserDTO(userWithMatchingName)).thenReturn(testUserDTO);

        UserDTO result = authService.changeForgottenPassword(dto);

        assertNotNull(result);
        verify(userRepository).save(userWithMatchingName);
        verify(passwordEncoder).encode("newPass123");
    }

    /**
     * Verifies that NOT_FOUND is thrown when the provided email does not exist.
     */
    @Test
    @DisplayName("changeForgottenPassword - Email not found, throws NOT_FOUND")
    void testChangeForgottenPassword_EmailNotFound() {
        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("laura.sanchez@eventmanager.es");

        when(userRepository.findByEmail("laura.sanchez@eventmanager.es")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> authService.changeForgottenPassword(dto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that BAD_REQUEST is thrown when the username does not match the
     * registered one.
     */
    @Test
    @DisplayName("changeForgottenPassword - Username mismatch, throws BAD_REQUEST")
    void testChangeForgottenPassword_UsernameMismatch() {
        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("carlos.martinez@eventmanager.es");
        dto.setUsername("wronguser");

        when(userRepository.findByEmail("carlos.martinez@eventmanager.es")).thenReturn(Optional.of(testUser));

        CustomException ex = assertThrows(CustomException.class, () -> authService.changeForgottenPassword(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USERNAME_DOES_NOT_MATCH, ex.getMessage());
    }

    /**
     * Verifies that BAD_REQUEST is thrown when the new password and confirmation do
     * not match.
     */
    @Test
    @DisplayName("changeForgottenPassword - Passwords do not match, throws BAD_REQUEST")
    void testChangeForgottenPassword_PasswordsMismatch() {
        String usernameValue = "carlos.martinez";
        User userWithMatchingName = User.builder()
                .userId(1)
                .email("carlos.martinez@eventmanager.es")
                .username(usernameValue)
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();

        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("carlos.martinez@eventmanager.es");
        dto.setUsername(usernameValue);
        dto.setNewPassword("newPass123");
        dto.setNewPasswordConfirm("differentPass");

        when(userRepository.findByEmail("carlos.martinez@eventmanager.es"))
                .thenReturn(Optional.of(userWithMatchingName));

        CustomException ex = assertThrows(CustomException.class, () -> authService.changeForgottenPassword(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_PASSWORDS_DO_NOT_MATCH, ex.getMessage());
    }
}
