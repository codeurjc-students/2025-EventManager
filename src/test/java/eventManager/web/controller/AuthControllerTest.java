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
 * Unit tests for the authentication controller. Covers registration, login,
 * token refresh, logout, and password recovery.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for tests.
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
        // Valid user for registration
        validUserCreateDTO = UserCreateDTO.builder()
                .email("carlos.martinez@eventmanager.es")
                .password("ClaveSegura2025")
                .username("carlos.martinez")
                .firstName("Carlos")
                .lastName("Martinez")
                .phoneNumber("612345678")
                .build();

        // Mock authentication response
        authResponse = AuthResponse.builder()
                .status(AuthResponse.Status.SUCCESS)
                .message("Success")
                .build();

        // Valid login request
        validLoginRequest = LoginRequest.builder()
                .username("carlos.martinez")
                .password("ClaveSegura2025")
                .build();
    }

    /**
     * Verifies that registration works correctly with valid data.
     */
    @Test
    @DisplayName("Register - Successful registration with valid data")
    void testRegister_Success() throws Exception {
        when(authService.registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value(authResponse.getMessage()));

        verify(authService, times(1)).registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class));
    }

    /**
     * Verifies that registration fails when the email is already registered in the
     * system.
     */
    @Test
    @DisplayName("Register - Email already exists in the system")
    void testRegister_EmailAlreadyExists() throws Exception {
        when(authService.registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class)))
                .thenThrow(new RuntimeException("El email ya está registrado"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserCreateDTO)))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).registerUser(any(UserCreateDTO.class), any(HttpServletResponse.class));
    }

    /**
     * Verifies registration behavior when an email with invalid format is provided.
     */
    @Test
    @DisplayName("Register - Invalid email format")
    void testRegister_InvalidEmailFormat() throws Exception {
        UserCreateDTO invalidEmailDTO = UserCreateDTO.builder()
                .email("invalid-email") // Email without @ and domain
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
     * Verifies that registration is rejected when the email exceeds the maximum
     * length of 50 characters.
     */
    @Test
    @DisplayName("Register - Email exceeds max length (>50 characters)")
    void testRegister_EmailTooLong() throws Exception {
        UserCreateDTO longEmailDTO = UserCreateDTO.builder()
                .email("a".repeat(45) + "@test.com") // Email > 50 characters
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
     * Verifies that registration is rejected when the password exceeds the maximum
     * length of 25 characters.
     */
    @Test
    @DisplayName("Register - Password exceeds max length (>25 characters)")
    void testRegister_PasswordTooLong() throws Exception {
        UserCreateDTO longPasswordDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("a".repeat(30)) // Password > 25 characters
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
     * Verifies that registration is rejected when the username exceeds the maximum
     * length of 25 characters.
     */
    @Test
    @DisplayName("Register - Username exceeds max length (>25 characters)")
    void testRegister_UsernameTooLong() throws Exception {
        UserCreateDTO longUsernameDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("password123")
                .username("a".repeat(30)) // Username > 25 characters
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
     * Verifies that registration is rejected when the first name exceeds the
     * maximum length of 20 characters.
     */
    @Test
    @DisplayName("Register - First name exceeds max length (>20 characters)")
    void testRegister_FirstNameTooLong() throws Exception {
        UserCreateDTO longFirstNameDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("password123")
                .username("testuser")
                .firstName("a".repeat(25)) // FirstName > 20 characters
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longFirstNameDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that registration is rejected when the last name exceeds the maximum
     * length of 50 characters.
     */
    @Test
    @DisplayName("Register - Last name exceeds max length (>50 characters)")
    void testRegister_LastNameTooLong() throws Exception {
        UserCreateDTO longLastNameDTO = UserCreateDTO.builder()
                .email("test@example.com")
                .password("password123")
                .username("testuser")
                .firstName("Test")
                .lastName("a".repeat(55)) // LastName > 50 characters
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longLastNameDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that registration is rejected when required fields are null.
     */
    @Test
    @DisplayName("Register - Required fields are null")
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
     * Verifies registration behavior when required fields are empty.
     */
    @Test
    @DisplayName("Register - Required fields are empty")
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
     * Verifies that login responds correctly with valid credentials.
     */
    @Test
    @DisplayName("Login - Successful login with valid credentials")
    void testLogin_Success() throws Exception {
        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Success"));

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifies that login rejects invalid credentials with a client error.
     */
    @Test
    @DisplayName("Login - Invalid credentials")
    void testLogin_InvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().is4xxClientError());

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifies that login fails when the user does not exist in the system.
     */
    @Test
    @DisplayName("Login - User does not exist")
    void testLogin_UserNotFound() throws Exception {
        LoginRequest nonExistentUserRequest = LoginRequest.builder()
                .username("nonexistentuser")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUserRequest)))
                .andExpect(status().is4xxClientError());

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifies that login fails when the provided password is incorrect.
     */
    @Test
    @DisplayName("Login - Incorrect password")
    void testLogin_WrongPassword() throws Exception {
        LoginRequest wrongPasswordRequest = LoginRequest.builder()
                .username("carlos.martinez")
                .password("ClaveIncorrecta2025")
                .build();

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenThrow(new BadCredentialsException("Invalid password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().is4xxClientError());

        verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
    }

    /**
     * Verifies that token refresh works correctly with a valid token.
     */
    @Test
    @DisplayName("Refresh Token - Successful refresh with valid token")
    void testRefreshToken_Success() throws Exception {
        String refreshToken = "valid-refresh-token";
        when(authService.refreshToken(eq(refreshToken), any(HttpServletResponse.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("RefreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(authService, times(1)).refreshToken(eq(refreshToken), any(HttpServletResponse.class));
    }

    /**
     * Verifies that token refresh fails when no token is provided in the cookie.
     */
    @Test
    @DisplayName("Refresh Token - Missing refresh token cookie")
    void testRefreshToken_NoToken() throws Exception {
        when(authService.refreshToken(isNull(), any(HttpServletResponse.class)))
                .thenThrow(new RuntimeException("Refresh token not provided"));

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).refreshToken(isNull(), any(HttpServletResponse.class));
    }

    /**
     * Verifies that token refresh fails when the token has expired.
     */
    @Test
    @DisplayName("Refresh Token - Token expired")
    void testRefreshToken_ExpiredToken() throws Exception {
        String expiredToken = "expired-refresh-token";
        when(authService.refreshToken(eq(expiredToken), any(HttpServletResponse.class)))
                .thenThrow(new RuntimeException("Refresh token expired"));

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("RefreshToken", expiredToken)))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).refreshToken(eq(expiredToken), any(HttpServletResponse.class));
    }

    /**
     * Verifies that logout is performed correctly.
     */
    @Test
    @DisplayName("Logout - Successful logout")
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
     * Verifies the forgotten password flow with valid data.
     */
    @Test
    @DisplayName("Forgot Password - Successful forgotten password change")
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
     * Verifies that password change fails when the email does not exist in the
     * system.
     */
    @Test
    @DisplayName("Forgot Password - Email does not exist")
    void testForgotPassword_EmailNotFound() throws Exception {
        when(authService.changeForgottenPassword(any(UserForgottenPassword.class)))
                .thenThrow(new RuntimeException("Email not found"));

        mockMvc.perform(post("/api/auth/forgot-password")
                .param("email", "laura.sanchez@eventmanager.es")
                .param("username", "carlos.martinez")
                .param("newPassword", "ClaveNueva2025")
                .param("newPasswordConfirm", "ClaveNueva2025"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that password change fails when the new password is too short.
     */
    @Test
    @DisplayName("Forgot Password - New password invalid (too short)")
    void testForgotPassword_InvalidNewPassword() throws Exception {
        when(authService.changeForgottenPassword(any(UserForgottenPassword.class)))
                .thenThrow(new RuntimeException("Invalid new password"));

        mockMvc.perform(post("/api/auth/forgot-password")
                .param("email", "carlos.martinez@eventmanager.es")
                .param("username", "carlos.martinez")
                .param("newPassword", "123")
                .param("newPasswordConfirm", "123"))
                .andExpect(status().is5xxServerError());
    }
}
