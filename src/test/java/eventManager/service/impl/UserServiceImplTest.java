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
 * Unit tests for the user service, including retrieval, updates, deletion, and
 * password changes.
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
     * Verifies that user information is retrieved correctly by username.
     */
    @Test
    @DisplayName("getUserInformationByUsername - Success")
    void testGetByUsername_Success() {
        when(userRepository.findByUsername("carlos.martinez")).thenReturn(Optional.of(testUser));
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserInformationByUsername("carlos.martinez");

        assertNotNull(result);
        assertEquals("carlos.martinez", result.getUsername());
        verify(userRepository).findByUsername("carlos.martinez");
    }

    /**
     * Verifies that looking up a non-existent username throws BAD_REQUEST.
     */
    @Test
    @DisplayName("getUserInformationByUsername - Not found")
    void testGetByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.getUserInformationByUsername("nonexistent"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifies that user information is retrieved correctly by ID.
     */
    @Test
    @DisplayName("getUserInformation - Success")
    void testGetUserInfo_Success() {
        doNothing().when(accessControlUtils).validateUserOwnership(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserInformation(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
    }

    /**
     * Verifies that a non-owner user cannot access another user's information.
     */
    @Test
    @DisplayName("getUserInformation - Not owner, throws FORBIDDEN")
    void testGetUserInfo_NotOwner() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils)
                .validateUserOwnership(2);

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUserInformation(2));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that looking up a non-existent user by ID throws BAD_REQUEST.
     */
    @Test
    @DisplayName("getUserInformation - Not found")
    void testGetUserInfo_NotFound() {
        doNothing().when(accessControlUtils).validateUserOwnership(999);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUserInformation(999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifies that the User entity is retrieved correctly by ID.
     */
    @Test
    @DisplayName("getUser - Success, returns User entity")
    void testGetUser_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        User result = userService.getUser(1);

        assertNotNull(result);
        assertEquals("carlos.martinez", result.getUsername());
    }

    /**
     * Verifies that looking up a non-existent user by ID throws BAD_REQUEST.
     */
    @Test
    @DisplayName("getUser - Not found")
    void testGetUser_NotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.getUser(999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifies that updating a user's data completes correctly.
     */
    @Test
    @DisplayName("updateUser - Successful update")
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
     * Verifies that a non-owner user cannot update another user's data.
     */
    @Test
    @DisplayName("updateUser - Not owner")
    void testUpdateUser_NotOwner() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils)
                .validateUserOwnership(2);

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.updateUser(2, UserUpdateDTO.builder().build()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that attempting to update a non-existent user throws BAD_REQUEST.
     */
    @Test
    @DisplayName("updateUser - User not found")
    void testUpdateUser_NotFound() {
        doNothing().when(accessControlUtils).validateUserOwnership(999);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.updateUser(999, UserUpdateDTO.builder().build()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifies that deleting an existing user completes correctly.
     */
    @Test
    @DisplayName("deleteUser - Successful deletion")
    void testDeleteUser_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userMapper.convertUserToUserDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.deleteUser(1);

        assertNotNull(result);
        verify(userRepository).delete(testUser);
    }

    /**
     * Verifies that attempting to delete a non-existent user throws BAD_REQUEST.
     */
    @Test
    @DisplayName("deleteUser - Not found")
    void testDeleteUser_NotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.deleteUser(999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifies that password change completes correctly when data is valid.
     */
    @Test
    @DisplayName("updateUserPassword - Successful change")
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
     * Verifies that a non-owner user cannot change another user's password.
     */
    @Test
    @DisplayName("updateUserPassword - Not owner")
    void testUpdatePassword_NotOwner() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils)
                .validateUserOwnership(2);

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.updateUserPassword(2, UserPasswordDTO.builder().build()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that providing an incorrect current password throws BAD_REQUEST.
     */
    @Test
    @DisplayName("updateUserPassword - Incorrect current password")
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
     * Verifies that BAD_REQUEST is thrown when the new password and confirmation do
     * not match.
     */
    @Test
    @DisplayName("updateUserPassword - New passwords do not match")
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
     * Verifies that attempting to change the password of a non-existent user throws
     * BAD_REQUEST.
     */
    @Test
    @DisplayName("updateUserPassword - User not found")
    void testUpdatePassword_UserNotFound() {
        doNothing().when(accessControlUtils).validateUserOwnership(999);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> userService.updateUserPassword(999,
                UserPasswordDTO.builder().password("old").newPassword("new").newPasswordConfirm("new").build()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
