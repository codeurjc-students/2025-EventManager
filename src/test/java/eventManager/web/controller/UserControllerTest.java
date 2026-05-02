package eventManager.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventManager.dto.UserDTO;
import eventManager.dto.UserPasswordDTO;
import eventManager.dto.UserUpdateDTO;
import eventManager.security.jwt.JwtTokenProvider;
import eventManager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the user controller. Covers authenticated profile, user
 * retrieval, updates, and password changes.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserDTO userDTO;
    private UserUpdateDTO userUpdateDTO;
    private UserPasswordDTO userPasswordDTO;

    @BeforeEach
    void setUp() {
        // Response UserDTO
        userDTO = new UserDTO();
        userDTO.setUserId(1);
        userDTO.setEmail("carlos.martinez@eventmanager.es");
        userDTO.setUsername("carlos.martinez");
        userDTO.setFirstName("Carlos");
        userDTO.setLastName("Martinez");
        userDTO.setPhoneNumber("612345678");

        // DTO for user update
        userUpdateDTO = UserUpdateDTO.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("698765432")
                .build();

        // DTO for password change
        userPasswordDTO = UserPasswordDTO.builder()
                .password("ClaveAnterior2024")
                .newPassword("ClaveNueva2025")
                .build();
    }

    /**
     * Verifies that the authenticated user profile is retrieved correctly.
     */
    @Test
    @DisplayName("Get Authenticated User Profile - Success")
    @WithMockUser(username = "carlos.martinez")
    void testGetAuthenticatedUserProfile_Success() throws Exception {
        when(userService.getUserInformationByUsername("carlos.martinez")).thenReturn(userDTO);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("carlos.martinez"))
                .andExpect(jsonPath("$.email").value("carlos.martinez@eventmanager.es"));

        verify(userService, times(1)).getUserInformationByUsername("carlos.martinez");
    }

    /**
     * Verifies that profile retrieval fails when the user is not authenticated.
     */
    @Test
    @DisplayName("Get Authenticated User Profile - User not authenticated")
    void testGetAuthenticatedUserProfile_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that a user is retrieved correctly by username.
     */
    @Test
    @DisplayName("Get User By Username - Success")
    @WithMockUser
    void testGetUserByUsername_Success() throws Exception {
        when(userService.getUserInformationByUsername("carlos.martinez")).thenReturn(userDTO);

        mockMvc.perform(get("/api/user")
                .param("username", "carlos.martinez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("carlos.martinez"))
                .andExpect(jsonPath("$.email").value("carlos.martinez@eventmanager.es"));

        verify(userService, times(1)).getUserInformationByUsername("carlos.martinez");
    }

    /**
     * Verifies that lookup by username fails when the user does not exist.
     */
    @Test
    @DisplayName("Get User By Username - User not found")
    @WithMockUser
    void testGetUserByUsername_NotFound() throws Exception {
        when(userService.getUserInformationByUsername("nonexistent"))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/user")
                .param("username", "nonexistent"))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).getUserInformationByUsername("nonexistent");
    }

    /**
     * Verifies that lookup by username fails when the parameter is not provided.
     */
    @Test
    @DisplayName("Get User By Username - Username is null")
    @WithMockUser
    void testGetUserByUsername_NullUsername() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that a user is retrieved correctly by ID.
     */
    @Test
    @DisplayName("Get User By ID - Success")
    @WithMockUser
    void testGetUserById_Success() throws Exception {
        when(userService.getUserInformation(1)).thenReturn(userDTO);

        mockMvc.perform(get("/api/user/{userId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("carlos.martinez"));

        verify(userService, times(1)).getUserInformation(1);
    }

    /**
     * Verifies that lookup by ID fails when the user does not exist.
     */
    @Test
    @DisplayName("Get User By ID - User not found")
    @WithMockUser
    void testGetUserById_NotFound() throws Exception {
        when(userService.getUserInformation(999)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/user/{userId}", 999))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).getUserInformation(999);
    }

    /**
     * Verifies that lookup by ID fails when a negative ID is provided.
     */
    @Test
    @DisplayName("Get User By ID - Invalid ID (negative)")
    @WithMockUser
    void testGetUserById_InvalidId() throws Exception {
        when(userService.getUserInformation(-1)).thenThrow(new RuntimeException("Invalid user ID"));

        mockMvc.perform(get("/api/user/{userId}", -1))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).getUserInformation(-1);
    }

    /**
     * Verifies that user update works correctly with valid data.
     */
    @Test
    @DisplayName("Update User - Successful update")
    @WithMockUser
    void testUpdateUser_Success() throws Exception {
        UserDTO updatedUserDTO = new UserDTO();
        updatedUserDTO.setUserId(1);
        updatedUserDTO.setFirstName("Updated");
        updatedUserDTO.setLastName("Name");
        updatedUserDTO.setPhoneNumber("987654321");

        when(userService.updateUser(eq(1), any(UserUpdateDTO.class))).thenReturn(updatedUserDTO);

        mockMvc.perform(put("/api/user/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));

        verify(userService, times(1)).updateUser(eq(1), any(UserUpdateDTO.class));
    }

    /**
     * Verifies that update fails when the user does not exist.
     */
    @Test
    @DisplayName("Update User - User not found")
    @WithMockUser
    void testUpdateUser_NotFound() throws Exception {
        when(userService.updateUser(eq(999), any(UserUpdateDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(put("/api/user/{userId}", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).updateUser(eq(999), any(UserUpdateDTO.class));
    }

    /**
     * Verifies that update is rejected when the first name is too long.
     */
    @Test
    @DisplayName("Update User - Invalid data (first name too long)")
    @WithMockUser
    void testUpdateUser_InvalidData() throws Exception {
        UserUpdateDTO invalidDTO = UserUpdateDTO.builder()
                .firstName("A".repeat(25)) // > 20 characters
                .lastName("User")
                .phoneNumber("612345678")
                .build();

        mockMvc.perform(put("/api/user/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies update behavior when fields are empty.
     */
    @Test
    @DisplayName("Update User - Empty fields")
    @WithMockUser
    void testUpdateUser_EmptyFields() throws Exception {
        UserUpdateDTO emptyDTO = UserUpdateDTO.builder()
                .firstName("")
                .lastName("")
                .phoneNumber("")
                .build();

        mockMvc.perform(put("/api/user/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyDTO)))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that update fails when the phone number format is invalid.
     */
    @Test
    @DisplayName("Update User - Invalid phone number")
    @WithMockUser
    void testUpdateUser_InvalidPhoneNumber() throws Exception {
        UserUpdateDTO invalidPhoneDTO = UserUpdateDTO.builder()
                .firstName("Carlos")
                .lastName("Martinez")
                .phoneNumber("invalid-phone")
                .build();

        when(userService.updateUser(eq(1), any(UserUpdateDTO.class)))
                .thenThrow(new RuntimeException("Invalid phone number format"));

        mockMvc.perform(put("/api/user/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPhoneDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that password change works correctly with valid data.
     */
    @Test
    @DisplayName("Update Password - Successful change")
    @WithMockUser
    void testUpdatePassword_Success() throws Exception {
        when(userService.updateUserPassword(eq(1), any(UserPasswordDTO.class))).thenReturn(userDTO);

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userPasswordDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));

        verify(userService, times(1)).updateUserPassword(eq(1), any(UserPasswordDTO.class));
    }

    /**
     * Verifies that password change fails when the old password is incorrect.
     */
    @Test
    @DisplayName("Update Password - Incorrect old password")
    @WithMockUser
    void testUpdatePassword_WrongOldPassword() throws Exception {
        when(userService.updateUserPassword(eq(1), any(UserPasswordDTO.class)))
                .thenThrow(new RuntimeException("Old password is incorrect"));

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userPasswordDTO)))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).updateUserPassword(eq(1), any(UserPasswordDTO.class));
    }

    /**
     * Verifies password change behavior when the new password is too short.
     */
    @Test
    @DisplayName("Update Password - New password too short")
    @WithMockUser
    void testUpdatePassword_NewPasswordTooShort() throws Exception {
        UserPasswordDTO shortPasswordDTO = UserPasswordDTO.builder()
                .password("ClaveAnterior2024")
                .newPassword("123") // Too short
                .build();

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortPasswordDTO)))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that password change is rejected when the new password exceeds 25
     * characters.
     */
    @Test
    @DisplayName("Update Password - New password too long")
    @WithMockUser
    void testUpdatePassword_NewPasswordTooLong() throws Exception {
        UserPasswordDTO longPasswordDTO = UserPasswordDTO.builder()
                .password("ClaveAnterior2024")
                .newPassword("A".repeat(30)) // > 25 characters
                .build();

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longPasswordDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies password change behavior when fields are null.
     */
    @Test
    @DisplayName("Update Password - Fields are null")
    @WithMockUser
    void testUpdatePassword_NullFields() throws Exception {
        UserPasswordDTO nullFieldsDTO = UserPasswordDTO.builder()
                .password(null)
                .newPassword(null)
                .build();

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullFieldsDTO)))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that password change fails when the new password is the same as the
     * old one.
     */
    @Test
    @DisplayName("Update Password - New password same as old")
    @WithMockUser
    void testUpdatePassword_SameAsOldPassword() throws Exception {
        UserPasswordDTO samePasswordDTO = UserPasswordDTO.builder()
                .password("ClaveSegura2025")
                .newPassword("ClaveSegura2025")
                .build();

        when(userService.updateUserPassword(eq(1), any(UserPasswordDTO.class)))
                .thenThrow(new RuntimeException("New password must be different from old password"));

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samePasswordDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that password change fails when the user does not exist.
     */
    @Test
    @DisplayName("Update Password - User not found")
    @WithMockUser
    void testUpdatePassword_UserNotFound() throws Exception {
        when(userService.updateUserPassword(eq(999), any(UserPasswordDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(put("/api/user/{userId}/change-password", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userPasswordDTO)))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).updateUserPassword(eq(999), any(UserPasswordDTO.class));
    }
}
