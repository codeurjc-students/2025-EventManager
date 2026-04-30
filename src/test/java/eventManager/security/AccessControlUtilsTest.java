package eventManager.security;

import eventManager.constant.Constantes;
import eventManager.dto.TicketDTO;
import eventManager.dto.UserDTO;
import eventManager.entity.Event;
import eventManager.entity.Ticket;
import eventManager.entity.User;
import eventManager.entity.UserRole;
import eventManager.exception.CustomException;
import eventManager.repository.TicketRepository;
import eventManager.repository.UserRepository;
import eventManager.service.TicketService;
import eventManager.service.UserService;
import eventManager.service.impl.AccessControlUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccessControlUtils, which centralizes permission and access
 * control checks for users, events, and tickets.
 */
@DisplayName("AccessControlUtils Tests")
class AccessControlUtilsTest {

    private AccessControlUtils accessControlUtils;

    private UserRepository userRepository;

    private TicketRepository ticketRepository;

    private UserService userService;

    private TicketService ticketService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        accessControlUtils = new AccessControlUtils();
        userRepository = mock(UserRepository.class);
        ticketRepository = mock(TicketRepository.class);
        userService = mock(UserService.class);
        ticketService = mock(TicketService.class);

        ReflectionTestUtils.setField(accessControlUtils, "userRepository", userRepository);
        ReflectionTestUtils.setField(accessControlUtils, "ticketRepository", ticketRepository);
        ReflectionTestUtils.setField(accessControlUtils, "userService", userService);
        ReflectionTestUtils.setField(accessControlUtils, "ticketService", ticketService);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        testUser = User.builder()
                .userId(1)
                .username("testuser")
                .email("test@test.com")
                .password("encoded")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .role(UserRole.USER)
                .build();

        testUserDTO = new UserDTO();
        testUserDTO.setUserId(1);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifies that the username is retrieved correctly from the SecurityContext.
     */
    @Test
    @DisplayName("getAuthenticatedUsername - Returns username from SecurityContext")
    void testGetAuthenticatedUsername() {
        assertEquals("testuser", accessControlUtils.getAuthenticatedUsername());
    }

    /**
     * Verifies that the authenticated user's DTO is returned by looking up the
     * username.
     */
    @Test
    @DisplayName("getAuthenticatedUser - Returns authenticated UserDTO")
    void testGetAuthenticatedUser_Success() {
        when(userService.getUserInformationByUsername("testuser")).thenReturn(testUserDTO);
        UserDTO result = accessControlUtils.getAuthenticatedUser();
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userService).getUserInformationByUsername("testuser");
    }

    /**
     * Verifies that the account owner can access without throwing an exception.
     */
    @Test
    @DisplayName("validateUserOwnership - Owner access, no exception")
    void testValidateUserOwnership_Owner() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        assertDoesNotThrow(() -> accessControlUtils.validateUserOwnership(1));
    }

    /**
     * Verifies that FORBIDDEN is thrown when the authenticated user is not the
     * account owner.
     */
    @Test
    @DisplayName("validateUserOwnership - Non-owner access, throws FORBIDDEN")
    void testValidateUserOwnership_NonOwner() {
        User otherUser = User.builder().userId(2).username("otheruser").build();
        when(userRepository.findById(2)).thenReturn(Optional.of(otherUser));
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserOwnership(2));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_FORBIDDEN_ACCESS, ex.getMessage());
    }

    /**
     * Verifies that NOT_FOUND is thrown when the user does not exist in the
     * database.
     */
    @Test
    @DisplayName("validateUserOwnership - User not found, throws NOT_FOUND")
    void testValidateUserOwnership_UserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserOwnership(999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that it returns true when the authenticated user is HOST of the
     * event.
     */
    @Test
    @DisplayName("isUserHostOfEvent - Is HOST, returns true")
    void testIsUserHostOfEvent_True() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertTrue(accessControlUtils.isUserHostOfEvent(1));
    }

    /**
     * Verifies that it returns false when the authenticated user is not HOST of the
     * event.
     */
    @Test
    @DisplayName("isUserHostOfEvent - Not HOST, returns false")
    void testIsUserHostOfEvent_False() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        assertFalse(accessControlUtils.isUserHostOfEvent(1));
    }

    /**
     * Verifies that an exception is thrown when the user does not exist while
     * checking HOST status.
     */
    @Test
    @DisplayName("isUserHostOfEvent - User not found, throws exception")
    void testIsUserHostOfEvent_UserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> accessControlUtils.isUserHostOfEvent(1));
    }

    /**
     * Verifies that no exception is thrown when the authenticated user is HOST of
     * the event.
     */
    @Test
    @DisplayName("validateUserIsHost - Is HOST, no exception")
    void testValidateUserIsHost_IsHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlUtils.validateUserIsHost(1));
    }

    /**
     * Verifies that FORBIDDEN is thrown when the user is not HOST of the event.
     */
    @Test
    @DisplayName("validateUserIsHost - Not HOST, throws FORBIDDEN")
    void testValidateUserIsHost_NotHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserIsHost(1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_HOST, ex.getMessage());
    }

    /**
     * Verifies that no exception is thrown when the user is registered in the
     * event.
     */
    @Test
    @DisplayName("validateUserRegisteredInEvent - Registered, no exception")
    void testValidateUserRegisteredInEvent_Registered() {
        when(userService.getUserInformationByUsername("testuser")).thenReturn(testUserDTO);
        TicketDTO ticketDTO = new TicketDTO();
        when(ticketService.getTicketByEventAndUser(1, 1)).thenReturn(ticketDTO);
        assertDoesNotThrow(() -> accessControlUtils.validateUserRegisteredInEvent(1));
    }

    /**
     * Verifies that FORBIDDEN is thrown when the user is not registered in the
     * event.
     */
    @Test
    @DisplayName("validateUserRegisteredInEvent - Not registered, throws FORBIDDEN")
    void testValidateUserRegisteredInEvent_NotRegistered() {
        when(userService.getUserInformationByUsername("testuser")).thenReturn(testUserDTO);
        when(ticketService.getTicketByEventAndUser(1, 1))
                .thenThrow(new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT));
        CustomException ex = assertThrows(CustomException.class,
                () -> accessControlUtils.validateUserRegisteredInEvent(1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT, ex.getMessage());
    }

    /**
     * Verifies that the ticket owner can access without throwing an exception.
     */
    @Test
    @DisplayName("validateTicketAccess - Ticket owner, no exception")
    void testValidateTicketAccess_Owner() {
        Event event = Event.builder().eventId(1).build();
        Ticket ticket = Ticket.builder().ticketId(10).userId(testUser).eventId(event).build();
        when(ticketRepository.findById(10)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        assertDoesNotThrow(() -> accessControlUtils.validateTicketAccess(10));
    }

    /**
     * Verifies that the event HOST can access the ticket even if not the owner.
     */
    @Test
    @DisplayName("validateTicketAccess - Event HOST, no exception")
    void testValidateTicketAccess_Host() {
        User otherUser = User.builder().userId(2).username("otheruser").build();
        Event event = Event.builder().eventId(1).build();
        Ticket ticket = Ticket.builder().ticketId(10).userId(otherUser).eventId(event).build();
        when(ticketRepository.findById(10)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlUtils.validateTicketAccess(10));
    }

    /**
     * Verifies that FORBIDDEN is thrown when the user is neither ticket owner nor
     * HOST.
     */
    @Test
    @DisplayName("validateTicketAccess - Neither owner nor HOST, throws FORBIDDEN")
    void testValidateTicketAccess_Unauthorized() {
        User otherUser = User.builder().userId(2).username("otheruser").build();
        Event event = Event.builder().eventId(1).build();
        Ticket ticket = Ticket.builder().ticketId(10).userId(otherUser).eventId(event).build();
        when(ticketRepository.findById(10)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateTicketAccess(10));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that NOT_FOUND is thrown when the ticket does not exist in the
     * database.
     */
    @Test
    @DisplayName("validateTicketAccess - Ticket not found, throws NOT_FOUND")
    void testValidateTicketAccess_TicketNotFound() {
        when(ticketRepository.findById(999)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateTicketAccess(999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that no exception is thrown when the user is HOST of the event
     * associated with the code.
     */
    @Test
    @DisplayName("validateUserIsHostByEventCode - Delegates to validateUserIsHost")
    void testValidateUserIsHostByEventCode_IsHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlUtils.validateUserIsHostByEventCode("ABC123", 1));
    }

    /**
     * Verifies that FORBIDDEN is thrown when the user is not HOST of the event
     * associated with the code.
     */
    @Test
    @DisplayName("validateUserIsHostByEventCode - Not HOST, throws FORBIDDEN")
    void testValidateUserIsHostByEventCode_NotHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class,
                () -> accessControlUtils.validateUserIsHostByEventCode("ABC123", 1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_HOST, ex.getMessage());
    }

    /**
     * Verifies that the event HOST can manage gifts without throwing an exception.
     */
    @Test
    @DisplayName("validateHostOrGiftCreator - Is HOST, no exception")
    void testValidateHostOrGiftCreator_IsHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlUtils.validateHostOrGiftCreator(1, "otheruser"));
    }

    /**
     * Verifies that the gift creator can manage it without throwing an exception.
     */
    @Test
    @DisplayName("validateHostOrGiftCreator - Is gift creator, no exception")
    void testValidateHostOrGiftCreator_IsCreator() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        assertDoesNotThrow(() -> accessControlUtils.validateHostOrGiftCreator(1, "testuser"));
    }

    /**
     * Verifies that FORBIDDEN is thrown when the user is neither HOST nor gift
     * creator.
     */
    @Test
    @DisplayName("validateHostOrGiftCreator - Neither HOST nor creator, throws FORBIDDEN")
    void testValidateHostOrGiftCreator_Neither() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class,
                () -> accessControlUtils.validateHostOrGiftCreator(1, "otheruser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_GIFT_UPDATE_FORBIDDEN, ex.getMessage());
    }
}
