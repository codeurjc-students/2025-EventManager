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
 * Tests unitarios para AccessControlUtils, que centraliza las validaciones de permisos y control de acceso sobre usuarios, eventos y tickets.
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
     * Verifica que se obtiene correctamente el nombre de usuario desde el SecurityContext.
     */
    @Test
    @DisplayName("getAuthenticatedUsername - Retorna username del SecurityContext")
    void testGetAuthenticatedUsername() {
        assertEquals("testuser", accessControlUtils.getAuthenticatedUsername());
    }

    /**
     * Verifica que se retorna el DTO del usuario autenticado consultando por su nombre de usuario.
     */
    @Test
    @DisplayName("getAuthenticatedUser - Retorna UserDTO del usuario autenticado")
    void testGetAuthenticatedUser_Success() {
        when(userService.getUserInformationByUsername("testuser")).thenReturn(testUserDTO);
        UserDTO result = accessControlUtils.getAuthenticatedUser();
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userService).getUserInformationByUsername("testuser");
    }

    /**
     * Verifica que el propietario de la cuenta puede acceder sin que se lance excepcion.
     */
    @Test
    @DisplayName("validateUserOwnership - Propietario, no lanza excepcion")
    void testValidateUserOwnership_Owner() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        assertDoesNotThrow(() -> accessControlUtils.validateUserOwnership(1));
    }

    /**
     * Verifica que se lanza FORBIDDEN cuando el usuario autenticado no es propietario de la cuenta.
     */
    @Test
    @DisplayName("validateUserOwnership - No propietario, lanza FORBIDDEN")
    void testValidateUserOwnership_NonOwner() {
        User otherUser = User.builder().userId(2).username("otheruser").build();
        when(userRepository.findById(2)).thenReturn(Optional.of(otherUser));
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserOwnership(2));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_FORBIDDEN_ACCESS, ex.getMessage());
    }

    /**
     * Verifica que se lanza NOT_FOUND cuando el usuario no existe en la base de datos.
     */
    @Test
    @DisplayName("validateUserOwnership - Usuario no encontrado, lanza NOT_FOUND")
    void testValidateUserOwnership_UserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserOwnership(999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que retorna true cuando el usuario autenticado es HOST del evento indicado.
     */
    @Test
    @DisplayName("isUserHostOfEvent - Es HOST, retorna true")
    void testIsUserHostOfEvent_True() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertTrue(accessControlUtils.isUserHostOfEvent(1));
    }

    /**
     * Verifica que retorna false cuando el usuario autenticado no es HOST del evento.
     */
    @Test
    @DisplayName("isUserHostOfEvent - No es HOST, retorna false")
    void testIsUserHostOfEvent_False() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        assertFalse(accessControlUtils.isUserHostOfEvent(1));
    }

    /**
     * Verifica que se lanza excepcion cuando el usuario no existe al comprobar si es HOST.
     */
    @Test
    @DisplayName("isUserHostOfEvent - Usuario no encontrado, lanza excepcion")
    void testIsUserHostOfEvent_UserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> accessControlUtils.isUserHostOfEvent(1));
    }

    /**
     * Verifica que no se lanza excepcion cuando el usuario autenticado es HOST del evento.
     */
    @Test
    @DisplayName("validateUserIsHost - Es HOST, no lanza excepcion")
    void testValidateUserIsHost_IsHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlUtils.validateUserIsHost(1));
    }

    /**
     * Verifica que se lanza FORBIDDEN cuando el usuario no es HOST del evento.
     */
    @Test
    @DisplayName("validateUserIsHost - No es HOST, lanza FORBIDDEN")
    void testValidateUserIsHost_NotHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserIsHost(1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_HOST, ex.getMessage());
    }

    /**
     * Verifica que no se lanza excepcion cuando el usuario esta registrado en el evento.
     */
    @Test
    @DisplayName("validateUserRegisteredInEvent - Registrado, no lanza excepcion")
    void testValidateUserRegisteredInEvent_Registered() {
        when(userService.getUserInformationByUsername("testuser")).thenReturn(testUserDTO);
        TicketDTO ticketDTO = new TicketDTO();
        when(ticketService.getTicketByEventAndUser(1, 1)).thenReturn(ticketDTO);
        assertDoesNotThrow(() -> accessControlUtils.validateUserRegisteredInEvent(1));
    }

    /**
     * Verifica que se lanza FORBIDDEN cuando el usuario no esta registrado en el evento.
     */
    @Test
    @DisplayName("validateUserRegisteredInEvent - No registrado, lanza FORBIDDEN")
    void testValidateUserRegisteredInEvent_NotRegistered() {
        when(userService.getUserInformationByUsername("testuser")).thenReturn(testUserDTO);
        when(ticketService.getTicketByEventAndUser(1, 1)).thenThrow(new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT));
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserRegisteredInEvent(1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT, ex.getMessage());
    }

    /**
     * Verifica que el propietario del ticket puede acceder sin que se lance excepcion.
     */
    @Test
    @DisplayName("validateTicketAccess - Propietario del ticket, no lanza excepcion")
    void testValidateTicketAccess_Owner() {
        Event event = Event.builder().eventId(1).build();
        Ticket ticket = Ticket.builder().ticketId(10).userId(testUser).eventId(event).build();
        when(ticketRepository.findById(10)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        assertDoesNotThrow(() -> accessControlUtils.validateTicketAccess(10));
    }

    /**
     * Verifica que el HOST del evento puede acceder al ticket aunque no sea su propietario.
     */
    @Test
    @DisplayName("validateTicketAccess - HOST del evento, no lanza excepcion")
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
     * Verifica que se lanza FORBIDDEN cuando el usuario no es ni propietario del ticket ni HOST.
     */
    @Test
    @DisplayName("validateTicketAccess - Ni propietario ni HOST, lanza FORBIDDEN")
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
     * Verifica que se lanza NOT_FOUND cuando el ticket no existe en la base de datos.
     */
    @Test
    @DisplayName("validateTicketAccess - Ticket no encontrado, lanza NOT_FOUND")
    void testValidateTicketAccess_TicketNotFound() {
        when(ticketRepository.findById(999)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateTicketAccess(999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que no se lanza excepcion cuando el usuario es HOST del evento asociado al codigo.
     */
    @Test
    @DisplayName("validateUserIsHostByEventCode - Delega a validateUserIsHost")
    void testValidateUserIsHostByEventCode_IsHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlUtils.validateUserIsHostByEventCode("ABC123", 1));
    }

    /**
     * Verifica que se lanza FORBIDDEN cuando el usuario no es HOST del evento asociado al codigo.
     */
    @Test
    @DisplayName("validateUserIsHostByEventCode - No es HOST, lanza FORBIDDEN")
    void testValidateUserIsHostByEventCode_NotHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateUserIsHostByEventCode("ABC123", 1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_HOST, ex.getMessage());
    }

    /**
     * Verifica que el HOST del evento puede gestionar regalos sin que se lance excepcion.
     */
    @Test
    @DisplayName("validateHostOrGiftCreator - Es HOST, no lanza excepcion")
    void testValidateHostOrGiftCreator_IsHost() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlUtils.validateHostOrGiftCreator(1, "otheruser"));
    }

    /**
     * Verifica que el creador del regalo puede gestionarlo sin que se lance excepcion.
     */
    @Test
    @DisplayName("validateHostOrGiftCreator - Es creador del regalo, no lanza excepcion")
    void testValidateHostOrGiftCreator_IsCreator() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        assertDoesNotThrow(() -> accessControlUtils.validateHostOrGiftCreator(1, "testuser"));
    }

    /**
     * Verifica que se lanza FORBIDDEN cuando el usuario no es ni HOST ni creador del regalo.
     */
    @Test
    @DisplayName("validateHostOrGiftCreator - Ni HOST ni creador, lanza FORBIDDEN")
    void testValidateHostOrGiftCreator_Neither() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(1, 1, "HOST")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class, () -> accessControlUtils.validateHostOrGiftCreator(1, "otheruser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(Constantes.MESSAGE_GIFT_UPDATE_FORBIDDEN, ex.getMessage());
    }
}
