package eventManager.service.impl;

import eventManager.constant.Constantes;
import eventManager.dto.UserDTO;
import eventManager.dto.UserPasswordDTO;
import eventManager.dto.UserUpdateDTO;
import eventManager.entity.User;
import eventManager.entity.UserRole;
import eventManager.exception.CustomException;
import eventManager.mapper.UserMapper;
import eventManager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de usuarios, incluyendo consulta, actualizacion, eliminacion y cambio de contrasena.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccessControlUtils accessControlUtils;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
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
        testUserDTO.setEmail("carlos.martinez@eventmanager.es");
        testUserDTO.setUsername("carlos.martinez");
        testUserDTO.setFirstName("Carlos");
        testUserDTO.setLastName("Martinez");
        testUserDTO.setPhoneNumber("612345678");
    }

    /**
     * Verifica que se obtiene correctamente la informacion de un usuario buscando por su nombre de usuario.
     */
    @Test
    @DisplayName("getUserInformationByUsername - Exitoso")
    void testGetByUsername_Success() {
        when(userRepository.findByUsername("carlos.martinez")).thenReturn(Optional.of(testUser));
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserInformationByUsername("carlos.martinez");

        assertNotNull(result);
        assertEquals("carlos.martinez", result.getUsername());
        verify(userRepository).findByUsername("carlos.martinez");
    }

    /**
     * Verifica que buscar un nombre de usuario inexistente lanza una excepcion BAD_REQUEST.
     */
    @Test
    @DisplayName("getUserInformationByUsername - No encontrado")
    void testGetByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUserInformationByUsername("nonexistent"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifica que se obtiene correctamente la informacion de un usuario por su identificador.
     */
    @Test
    @DisplayName("getUserInformation - Exitoso")
    void testGetUserInfo_Success() {
        doNothing().when(accessControlUtils).validateUserOwnership(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserInformation(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
    }

    /**
     * Verifica que un usuario no propietario no puede consultar la informacion de otro usuario.
     */
    @Test
    @DisplayName("getUserInformation - No propietario, lanza FORBIDDEN")
    void testGetUserInfo_NotOwner() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils).validateUserOwnership(2);

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUserInformation(2));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que consultar un usuario inexistente por identificador lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("getUserInformation - No encontrado")
    void testGetUserInfo_NotFound() {
        doNothing().when(accessControlUtils).validateUserOwnership(999);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUserInformation(999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifica que se recupera correctamente la entidad User a partir de su identificador.
     */
    @Test
    @DisplayName("getUser - Exitoso, retorna entidad User")
    void testGetUser_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        User result = userService.getUser(1);

        assertNotNull(result);
        assertEquals("carlos.martinez", result.getUsername());
    }

    /**
     * Verifica que buscar un usuario inexistente por identificador lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("getUser - No encontrado")
    void testGetUser_NotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUser(999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifica que la actualizacion de los datos de un usuario se realiza correctamente.
     */
    @Test
    @DisplayName("updateUser - Actualizacion exitosa")
    void testUpdateUser_Success() {
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("698765432")
                .build();

        doNothing().when(accessControlUtils).validateUserOwnership(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.updateUser(1, updateDTO);

        assertNotNull(result);
        verify(userRepository).save(testUser);
        assertEquals("Updated", testUser.getFirstName());
        assertEquals("Name", testUser.getLastName());
    }

    /**
     * Verifica que un usuario no propietario no puede actualizar los datos de otro usuario.
     */
    @Test
    @DisplayName("updateUser - No propietario")
    void testUpdateUser_NotOwner() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils).validateUserOwnership(2);

        CustomException ex = assertThrows(CustomException.class, () -> userService.updateUser(2, UserUpdateDTO.builder().build()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que intentar actualizar un usuario inexistente lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("updateUser - Usuario no encontrado")
    void testUpdateUser_NotFound() {
        doNothing().when(accessControlUtils).validateUserOwnership(999);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.updateUser(999, UserUpdateDTO.builder().build()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifica que la eliminacion de un usuario existente se completa correctamente.
     */
    @Test
    @DisplayName("deleteUser - Eliminacion exitosa")
    void testDeleteUser_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.deleteUser(1);

        assertNotNull(result);
        verify(userRepository).delete(testUser);
    }

    /**
     * Verifica que intentar eliminar un usuario inexistente lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("deleteUser - No encontrado")
    void testDeleteUser_NotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.deleteUser(999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifica que el cambio de contrasena se realiza correctamente cuando los datos son validos.
     */
    @Test
    @DisplayName("updateUserPassword - Cambio exitoso")
    void testUpdatePassword_Success() {
        UserPasswordDTO passwordDTO = UserPasswordDTO.builder()
                .password("oldPassword")
                .newPassword("newPassword123")
                .newPasswordConfirm("newPassword123")
                .build();

        doNothing().when(accessControlUtils).validateUserOwnership(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.updateUserPassword(1, passwordDTO);

        assertNotNull(result);
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(testUser);
    }

    /**
     * Verifica que un usuario no propietario no puede cambiar la contrasena de otro usuario.
     */
    @Test
    @DisplayName("updateUserPassword - No propietario")
    void testUpdatePassword_NotOwner() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils).validateUserOwnership(2);

        CustomException ex = assertThrows(CustomException.class, () -> userService.updateUserPassword(2, UserPasswordDTO.builder().build()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que proporcionar la contrasena actual incorrecta lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("updateUserPassword - Password actual incorrecta")
    void testUpdatePassword_WrongCurrentPassword() {
        UserPasswordDTO passwordDTO = UserPasswordDTO.builder()
                .password("wrongOldPassword")
                .newPassword("newPassword123")
                .newPasswordConfirm("newPassword123")
                .build();

        doNothing().when(accessControlUtils).validateUserOwnership(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", "encodedPassword")).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> userService.updateUserPassword(1, passwordDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_INCORRECT_PASSWORD, ex.getMessage());
    }

    /**
     * Verifica que si la nueva contrasena y su confirmacion no coinciden se lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("updateUserPassword - Nuevas passwords no coinciden")
    void testUpdatePassword_PasswordsMismatch() {
        UserPasswordDTO passwordDTO = UserPasswordDTO.builder()
                .password("oldPassword")
                .newPassword("newPassword123")
                .newPasswordConfirm("differentPassword")
                .build();

        doNothing().when(accessControlUtils).validateUserOwnership(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> userService.updateUserPassword(1, passwordDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_PASSWORDS_DO_NOT_MATCH, ex.getMessage());
    }

    /**
     * Verifica que intentar cambiar la contrasena de un usuario inexistente lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("updateUserPassword - Usuario no encontrado")
    void testUpdatePassword_UserNotFound() {
        doNothing().when(accessControlUtils).validateUserOwnership(999);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.updateUserPassword(999, UserPasswordDTO.builder().password("old").newPassword("new").newPasswordConfirm("new").build()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
