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
import org.openapitools.jackson.nullable.JsonNullable;
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
 * Pruebas unitarias del servicio de autenticacion, incluyendo registro, login, refresco de token, logout y cambio de contrasena olvidada.
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
     * Verifica que el registro de un nuevo usuario se completa correctamente y genera tokens de acceso.
     */
    @Test
    @DisplayName("registerUser - Registro exitoso")
    void testRegister_Success() {
        when(userRepository.existsByEmailOrUsername("carlos.martinez@eventmanager.es", "carlos.martinez")).thenReturn(false);
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
     * Verifica que registrar un usuario con email o username ya existente lanza una excepcion BAD_REQUEST.
     */
    @Test
    @DisplayName("registerUser - Usuario ya existe, lanza BAD_REQUEST")
    void testRegister_UserAlreadyExists() {
        when(userRepository.existsByEmailOrUsername("carlos.martinez@eventmanager.es", "carlos.martinez")).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> authService.registerUser(userCreateDTO, response));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_ALREADY_REGISTERED, ex.getMessage());
    }

    /**
     * Verifica que un error inesperado durante el registro produce una excepcion INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("registerUser - Error inesperado, lanza INTERNAL_SERVER_ERROR")
    void testRegister_UnexpectedError() {
        when(userRepository.existsByEmailOrUsername("carlos.martinez@eventmanager.es", "carlos.martinez")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class, () -> authService.registerUser(userCreateDTO, response));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals("DB error", ex.getMessage());
    }

    /**
     * Verifica que el inicio de sesion con credenciales validas devuelve una respuesta exitosa con cookies.
     */
    @Test
    @DisplayName("login - Login exitoso")
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
     * Verifica que las credenciales incorrectas durante el login provocan una excepcion UNAUTHORIZED.
     */
    @Test
    @DisplayName("login - Credenciales invalidas, lanza UNAUTHORIZED")
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
     * Verifica que un refresh token valido permite renovar el access token correctamente.
     */
    @Test
    @DisplayName("refreshToken - Token valido, renueva access token")
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
     * Verifica que un refresh token invalido devuelve una respuesta FAILURE sin lanzar excepcion.
     */
    @Test
    @DisplayName("refreshToken - Token invalido, retorna FAILURE sin excepcion")
    void testRefreshToken_InvalidToken() {
        when(jwtTokenProvider.validateToken("invalidToken")).thenThrow(new RuntimeException("Invalid token"));

        AuthResponse result = authService.refreshToken("invalidToken", response);

        assertEquals(AuthResponse.Status.FAILURE, result.getStatus());
    }

    /**
     * Verifica que el cierre de sesion limpia el contexto de seguridad y elimina las cookies.
     */
    @Test
    @DisplayName("logout - Logout exitoso, limpia contexto y cookies")
    void testLogout_Success() {
        AuthResponse result = authService.logout(response);

        assertEquals(AuthResponse.Status.SUCCESS, result.getStatus());
        verify(response, times(2)).addCookie(any());
    }

    /**
     * Verifica que el cambio de contrasena olvidada se realiza correctamente cuando los datos son validos.
     */
    @Test
    @DisplayName("changeForgottenPassword - Cambio exitoso")
    void testChangeForgottenPassword_Success() {
        String usernameValue = "carlos.martinez";
        User userWithMatchingName = User.builder()
                .userId(1)
                .email("carlos.martinez@eventmanager.es")
                .username(String.valueOf(JsonNullable.of(usernameValue)))
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();

        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("carlos.martinez@eventmanager.es");
        dto.setUsername(JsonNullable.of(usernameValue));
        dto.setNewPassword("newPass123");
        dto.setNewPasswordConfirm("newPass123");

        when(userRepository.findByEmail("carlos.martinez@eventmanager.es")).thenReturn(Optional.of(userWithMatchingName));
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");
        when(userRepository.save(userWithMatchingName)).thenReturn(userWithMatchingName);
        when(userMapper.convertUserToUserDTO(userWithMatchingName)).thenReturn(testUserDTO);

        UserDTO result = authService.changeForgottenPassword(dto);

        assertNotNull(result);
        verify(userRepository).save(userWithMatchingName);
        verify(passwordEncoder).encode("newPass123");
    }

    /**
     * Verifica que si el email proporcionado no existe en el sistema se lanza NOT_FOUND.
     */
    @Test
    @DisplayName("changeForgottenPassword - Email no encontrado, lanza NOT_FOUND")
    void testChangeForgottenPassword_EmailNotFound() {
        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("laura.sanchez@eventmanager.es");

        when(userRepository.findByEmail("laura.sanchez@eventmanager.es")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> authService.changeForgottenPassword(dto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que si el nombre de usuario no coincide con el registrado se lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("changeForgottenPassword - Username no coincide, lanza BAD_REQUEST")
    void testChangeForgottenPassword_UsernameMismatch() {
        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("carlos.martinez@eventmanager.es");
        dto.setUsername(JsonNullable.of("wronguser"));

        when(userRepository.findByEmail("carlos.martinez@eventmanager.es")).thenReturn(Optional.of(testUser));

        CustomException ex = assertThrows(CustomException.class, () -> authService.changeForgottenPassword(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USERNAME_DOES_NOT_MATCH, ex.getMessage());
    }

    /**
     * Verifica que si la nueva contrasena y su confirmacion no coinciden se lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("changeForgottenPassword - Passwords no coinciden, lanza BAD_REQUEST")
    void testChangeForgottenPassword_PasswordsMismatch() {
        String usernameValue = "carlos.martinez";
        User userWithMatchingName = User.builder()
                .userId(1)
                .email("carlos.martinez@eventmanager.es")
                .username(String.valueOf(JsonNullable.of(usernameValue)))
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();

        UserForgottenPassword dto = new UserForgottenPassword();
        dto.setEmail("carlos.martinez@eventmanager.es");
        dto.setUsername(JsonNullable.of(usernameValue));
        dto.setNewPassword("newPass123");
        dto.setNewPasswordConfirm("differentPass");

        when(userRepository.findByEmail("carlos.martinez@eventmanager.es")).thenReturn(Optional.of(userWithMatchingName));

        CustomException ex = assertThrows(CustomException.class, () -> authService.changeForgottenPassword(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_PASSWORDS_DO_NOT_MATCH, ex.getMessage());
    }
}
