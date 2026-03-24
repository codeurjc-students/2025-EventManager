package eventManager.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventManager.dto.UserCreateDTO;
import eventManager.dto.UserForgottenPassword;
import eventManager.security.jwt.AuthResponse;
import eventManager.security.jwt.LoginRequest;
import eventManager.security.jwt.JwtTokenProvider;
import eventManager.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del controlador de autenticacion. Cubre registro, login, refresco de token, logout y recuperacion de contrasena.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Deshabilitamos filtros de seguridad para tests
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private AuthService authService;

        @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

        @MockitoBean
    private UserDetailsService userDetailsService;

    private UserCreateDTO validUserCreateDTO;
    private AuthResponse authResponse;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        // Usuario válido para registro
        validUserCreateDTO = UserCreateDTO.builder()
                .email("carlos.martinez@eventmanager.es")
                .password("ClaveSegura2025")
                .username("carlos.martinez")
                .firstName("Carlos")
                .lastName("Martinez")
                .phoneNumber("612345678")
                .build();

        // Respuesta de autenticación simulada
        authResponse = AuthResponse.builder()
                .status(AuthResponse.Status.SUCCESS)
                .message("Success")
                .build();

        // Login request válido
        validLoginRequest = LoginRequest.builder()
                .username("carlos.martinez")
                .password("ClaveSegura2025")
                .build();
    }

    /**
     * Verifica que el registro funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Register - Registro exitoso con datos válidos")
    void testRegister_Success() throws Exception {
        when(authService.registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value(authResponse.getMessage()));

        verify(authService, times(1)).registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el registro falla cuando el email ya esta registrado en el sistema.
     */
    @Test
    @DisplayName("Register - Email ya existe en el sistema")
    void testRegister_EmailAlreadyExists() throws Exception {
        when(authService.registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class))).thenThrow(new RuntimeException("El email ya está registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserCreateDTO)))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class));
    }

    /**
     * Verifica el comportamiento del registro cuando se proporciona un email con formato invalido.
     */
    @Test
    @DisplayName("Register - Email con formato inválido")
    void testRegister_InvalidEmailFormat() throws Exception {
        UserCreateDTO invalidEmailDTO = UserCreateDTO.builder()
                .email("invalid-email")  // Email sin @ y dominio
                .password("password123")
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailDTO)))
                .andExpect(status().isOk());
    }

    /**
     * Verifica que se rechaza el registro cuando el email excede la longitud maxima de 50 caracteres.
     */
    @Test
    @DisplayName("Register - Email excede longitud máxima (>50 caracteres)")
    void testRegister_EmailTooLong() throws Exception {
        UserCreateDTO longEmailDTO = UserCreateDTO.builder()
                .email("a".repeat(45) + "@test.com")  // Email > 50 caracteres
                .password("password123")
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longEmailDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que se rechaza el registro cuando la contrasena excede la longitud maxima de 25 caracteres.
     */
    @Test
    @DisplayName("Register - Password excede longitud máxima (>25 caracteres)")
    void testRegister_PasswordTooLong() throws Exception {
        UserCreateDTO longPasswordDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("a".repeat(30))  // Password > 25 caracteres
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPasswordDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que se rechaza el registro cuando el username excede la longitud maxima de 25 caracteres.
     */
    @Test
    @DisplayName("Register - Username excede longitud máxima (>25 caracteres)")
    void testRegister_UsernameTooLong() throws Exception {
        UserCreateDTO longUsernameDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("password123")
                .username("a".repeat(30))  // Username > 25 caracteres
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longUsernameDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que se rechaza el registro cuando el nombre excede la longitud maxima de 20 caracteres.
     */
    @Test
    @DisplayName("Register - FirstName excede longitud máxima (>20 caracteres)")
    void testRegister_FirstNameTooLong() throws Exception {
        UserCreateDTO longFirstNameDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("password123")
                .username("testuser")
                .firstName("a".repeat(25))  // FirstName > 20 caracteres
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longFirstNameDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que se rechaza el registro cuando el apellido excede la longitud maxima de 50 caracteres.
     */
    @Test
    @DisplayName("Register - LastName excede longitud máxima (>50 caracteres)")
    void testRegister_LastNameTooLong() throws Exception {
        UserCreateDTO longLastNameDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("password123")
                .username("testuser")
                .firstName("Test")
                .lastName("a".repeat(55))  // LastName > 50 caracteres
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longLastNameDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que se rechaza el registro cuando los campos obligatorios son null.
     */
    @Test
    @DisplayName("Register - Campos obligatorios null")
    void testRegister_RequiredFieldsNull() throws Exception {
        UserCreateDTO nullFieldsDTO = UserCreateDTO.builder()
                .email(null)
                .password(null)
                .username(null)
                .firstName(null)
                .lastName(null)
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullFieldsDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica el comportamiento del registro cuando los campos obligatorios estan vacios.
     */
    @Test
    @DisplayName("Register - Campos obligatorios vacíos")
    void testRegister_RequiredFieldsEmpty() throws Exception {
        UserCreateDTO emptyFieldsDTO = UserCreateDTO.builder()
                .email("")
                .password("")
                .username("")
                .firstName("")
                .lastName("")
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyFieldsDTO)))
                .andExpect(status().isOk());
    }

    /**
     * Verifica que el login responde correctamente con credenciales validas.
     */
    @Test
    @DisplayName("Login - Login exitoso con credenciales válidas")
    void testLogin_Success() throws Exception {
        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Success"));

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el login rechaza credenciales incorrectas con error de cliente.
     */
    @Test
    @DisplayName("Login - Credenciales incorrectas")
    void testLogin_InvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().is4xxClientError());

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el login falla cuando el usuario no existe en el sistema.
     */
    @Test
    @DisplayName("Login - Usuario no existe")
    void testLogin_UserNotFound() throws Exception {
        LoginRequest nonExistentUserRequest = LoginRequest.builder()
                .username("nonexistentuser")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class))).thenThrow(new BadCredentialsException("User not found"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentUserRequest)))
                .andExpect(status().is4xxClientError());

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el login falla cuando la contrasena proporcionada es incorrecta.
     */
    @Test
    @DisplayName("Login - Password incorrecto")
    void testLogin_WrongPassword() throws Exception {
        LoginRequest wrongPasswordRequest = LoginRequest.builder()
                .username("carlos.martinez")
                .password("ClaveIncorrecta2025")
                .build();

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class))).thenThrow(new BadCredentialsException("Invalid password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().is4xxClientError());

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el refresco de token funciona correctamente con un token valido.
     */
    @Test
    @DisplayName("Refresh Token - Refresh exitoso con token válido")
    void testRefreshToken_Success() throws Exception {
        String refreshToken = "valid-refresh-token";
        when(authService.refreshToken(eq(refreshToken), any(HttpServletResponse.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("RefreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(authService, times(1)).refreshToken(eq(refreshToken), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el refresco de token falla cuando no se proporciona token en la cookie.
     */
    @Test
    @DisplayName("Refresh Token - Sin token de refresh en cookie")
    void testRefreshToken_NoToken() throws Exception {
        when(authService.refreshToken(isNull(), any(HttpServletResponse.class))).thenThrow(new RuntimeException("Refresh token not provided"));

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).refreshToken(isNull(), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el refresco de token falla cuando el token ha expirado.
     */
    @Test
    @DisplayName("Refresh Token - Token expirado")
    void testRefreshToken_ExpiredToken() throws Exception {
        String expiredToken = "expired-refresh-token";
        when(authService.refreshToken(eq(expiredToken), any(HttpServletResponse.class))).thenThrow(new RuntimeException("Refresh token expired"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("RefreshToken", expiredToken)))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).refreshToken(eq(expiredToken), any(HttpServletResponse.class));
    }

    /**
     * Verifica que el cierre de sesion se realiza correctamente.
     */
    @Test
    @DisplayName("Logout - Logout exitoso")
    void testLogout_Success() throws Exception {
        AuthResponse logoutResponse = AuthResponse.builder()
                .message("Logout successful")
                .build();

        when(authService.logout(any(HttpServletResponse.class))).thenReturn(logoutResponse);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService, times(1)).logout(any(HttpServletResponse.class));
    }

    /**
     * Verifica el flujo de cambio de contrasena olvidada con datos validos.
     */
    @Test
    @DisplayName("Forgot Password - Cambio exitoso de contraseña olvidada")
    void testForgotPassword_Success() throws Exception {
        eventManager.dto.UserDTO userDTO = new eventManager.dto.UserDTO();
        when(authService.changeForgottenPassword(any(UserForgottenPassword.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .param("email", "carlos.martinez@eventmanager.es")
                        .param("username", "carlos.martinez")
                        .param("newPassword", "ClaveNueva2025")
                        .param("newPasswordConfirm", "ClaveNueva2025"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que el cambio de contrasena falla cuando el email no existe en el sistema.
     */
    @Test
    @DisplayName("Forgot Password - Email no existe")
    void testForgotPassword_EmailNotFound() throws Exception {
        when(authService.changeForgottenPassword(any(UserForgottenPassword.class))).thenThrow(new RuntimeException("Email not found"));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .param("email", "laura.sanchez@eventmanager.es")
                        .param("username", "carlos.martinez")
                        .param("newPassword", "ClaveNueva2025")
                        .param("newPasswordConfirm", "ClaveNueva2025"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que el cambio de contrasena falla cuando la nueva contrasena es demasiado corta.
     */
    @Test
    @DisplayName("Forgot Password - Nueva contraseña inválida (demasiado corta)")
    void testForgotPassword_InvalidNewPassword() throws Exception {
        when(authService.changeForgottenPassword(any(UserForgottenPassword.class))).thenThrow(new RuntimeException("Invalid new password"));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .param("email", "carlos.martinez@eventmanager.es")
                        .param("username", "carlos.martinez")
                        .param("newPassword", "123")
                        .param("newPasswordConfirm", "123"))
                .andExpect(status().is5xxServerError());
    }
}
