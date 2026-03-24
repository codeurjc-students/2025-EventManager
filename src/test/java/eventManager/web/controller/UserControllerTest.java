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
 * Pruebas unitarias del controlador de usuarios. Cubre perfil autenticado, obtencion de usuario, actualizacion y cambio de contrasena.
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
        // UserDTO de respuesta
        userDTO = new UserDTO();
        userDTO.setUserId(1);
        userDTO.setEmail("carlos.martinez@eventmanager.es");
        userDTO.setUsername("carlos.martinez");
        userDTO.setFirstName("Carlos");
        userDTO.setLastName("Martinez");
        userDTO.setPhoneNumber("612345678");

        // DTO para actualizar usuario
        userUpdateDTO = UserUpdateDTO.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("698765432")
                .build();

        // DTO para cambiar contraseña
        userPasswordDTO = UserPasswordDTO.builder()
                .password("ClaveAnterior2024")
                .newPassword("ClaveNueva2025")
                .build();
    }

    /**
     * Verifica que se obtiene correctamente el perfil del usuario autenticado.
     */
    @Test
    @DisplayName("Get Authenticated User Profile - Exitoso")
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
     * Verifica que la obtencion del perfil falla cuando el usuario no esta autenticado.
     */
    @Test
    @DisplayName("Get Authenticated User Profile - Usuario no autenticado")
    void testGetAuthenticatedUserProfile_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que se obtiene correctamente un usuario por su username.
     */
    @Test
    @DisplayName("Get User By Username - Exitoso")
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
     * Verifica que la busqueda por username falla cuando el usuario no existe.
     */
    @Test
    @DisplayName("Get User By Username - Usuario no encontrado")
    @WithMockUser
    void testGetUserByUsername_NotFound() throws Exception {
        when(userService.getUserInformationByUsername("nonexistent")).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/user")
                        .param("username", "nonexistent"))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).getUserInformationByUsername("nonexistent");
    }

    /**
     * Verifica que la busqueda por username falla cuando no se proporciona el parametro.
     */
    @Test
    @DisplayName("Get User By Username - Username null")
    @WithMockUser
    void testGetUserByUsername_NullUsername() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que se obtiene correctamente un usuario por su identificador.
     */
    @Test
    @DisplayName("Get User By ID - Exitoso")
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
     * Verifica que la busqueda por identificador falla cuando el usuario no existe.
     */
    @Test
    @DisplayName("Get User By ID - Usuario no encontrado")
    @WithMockUser
    void testGetUserById_NotFound() throws Exception {
        when(userService.getUserInformation(999)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/user/{userId}", 999))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).getUserInformation(999);
    }

    /**
     * Verifica que la busqueda por identificador falla cuando se proporciona un ID negativo.
     */
    @Test
    @DisplayName("Get User By ID - ID inválido (negativo)")
    @WithMockUser
    void testGetUserById_InvalidId() throws Exception {
        when(userService.getUserInformation(-1)).thenThrow(new RuntimeException("Invalid user ID"));

        mockMvc.perform(get("/api/user/{userId}", -1))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).getUserInformation(-1);
    }

    /**
     * Verifica que la actualizacion de un usuario funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Update User - Actualización exitosa")
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
     * Verifica que la actualizacion falla cuando el usuario no existe.
     */
    @Test
    @DisplayName("Update User - Usuario no encontrado")
    @WithMockUser
    void testUpdateUser_NotFound() throws Exception {
        when(userService.updateUser(eq(999), any(UserUpdateDTO.class))).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(put("/api/user/{userId}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO)))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).updateUser(eq(999), any(UserUpdateDTO.class));
    }

    /**
     * Verifica que la actualizacion se rechaza cuando el nombre es demasiado largo.
     */
    @Test
    @DisplayName("Update User - Datos inválidos (firstName demasiado largo)")
    @WithMockUser
    void testUpdateUser_InvalidData() throws Exception {
        UserUpdateDTO invalidDTO = UserUpdateDTO.builder()
                .firstName("A".repeat(25))  // > 20 caracteres
                .lastName("User")
                .phoneNumber("612345678")
                .build();

        mockMvc.perform(put("/api/user/{userId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica el comportamiento de la actualizacion cuando los campos estan vacios.
     */
    @Test
    @DisplayName("Update User - Campos vacíos")
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
     * Verifica que la actualizacion falla cuando el formato del numero de telefono es invalido.
     */
    @Test
    @DisplayName("Update User - Número de teléfono inválido")
    @WithMockUser
    void testUpdateUser_InvalidPhoneNumber() throws Exception {
        UserUpdateDTO invalidPhoneDTO = UserUpdateDTO.builder()
                .firstName("Carlos")
                .lastName("Martinez")
                .phoneNumber("invalid-phone")
                .build();

        when(userService.updateUser(eq(1), any(UserUpdateDTO.class))).thenThrow(new RuntimeException("Invalid phone number format"));

        mockMvc.perform(put("/api/user/{userId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPhoneDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que el cambio de contrasena funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Update Password - Cambio exitoso")
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
     * Verifica que el cambio de contrasena falla cuando la contrasena antigua es incorrecta.
     */
    @Test
    @DisplayName("Update Password - Contraseña antigua incorrecta")
    @WithMockUser
    void testUpdatePassword_WrongOldPassword() throws Exception {
        when(userService.updateUserPassword(eq(1), any(UserPasswordDTO.class))).thenThrow(new RuntimeException("Old password is incorrect"));

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPasswordDTO)))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).updateUserPassword(eq(1), any(UserPasswordDTO.class));
    }

    /**
     * Verifica el comportamiento del cambio de contrasena cuando la nueva es demasiado corta.
     */
    @Test
    @DisplayName("Update Password - Nueva contraseña demasiado corta")
    @WithMockUser
    void testUpdatePassword_NewPasswordTooShort() throws Exception {
        UserPasswordDTO shortPasswordDTO = UserPasswordDTO.builder()
                .password("ClaveAnterior2024")
                .newPassword("123")  // Muy corta
                .build();

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPasswordDTO)))
                .andExpect(status().isOk());
    }

    /**
     * Verifica que se rechaza el cambio de contrasena cuando la nueva excede los 25 caracteres.
     */
    @Test
    @DisplayName("Update Password - Nueva contraseña demasiado larga")
    @WithMockUser
    void testUpdatePassword_NewPasswordTooLong() throws Exception {
        UserPasswordDTO longPasswordDTO = UserPasswordDTO.builder()
                .password("ClaveAnterior2024")
                .newPassword("A".repeat(30))  // > 25 caracteres
                .build();

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPasswordDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica el comportamiento del cambio de contrasena cuando los campos son null.
     */
    @Test
    @DisplayName("Update Password - Campos null")
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
     * Verifica que el cambio de contrasena falla cuando la nueva es igual a la antigua.
     */
    @Test
    @DisplayName("Update Password - Nueva contraseña igual a la antigua")
    @WithMockUser
    void testUpdatePassword_SameAsOldPassword() throws Exception {
        UserPasswordDTO samePasswordDTO = UserPasswordDTO.builder()
                .password("ClaveSegura2025")
                .newPassword("ClaveSegura2025")
                .build();

        when(userService.updateUserPassword(eq(1), any(UserPasswordDTO.class))).thenThrow(new RuntimeException("New password must be different from old password"));

        mockMvc.perform(put("/api/user/{userId}/change-password", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samePasswordDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que el cambio de contrasena falla cuando el usuario no existe.
     */
    @Test
    @DisplayName("Update Password - Usuario no encontrado")
    @WithMockUser
    void testUpdatePassword_UserNotFound() throws Exception {
        when(userService.updateUserPassword(eq(999), any(UserPasswordDTO.class))).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(put("/api/user/{userId}/change-password", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPasswordDTO)))
                .andExpect(status().is5xxServerError());

        verify(userService, times(1)).updateUserPassword(eq(999), any(UserPasswordDTO.class));
    }
}
