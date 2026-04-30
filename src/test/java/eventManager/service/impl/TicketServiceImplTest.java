package eventManager.service.impl;

import eventManager.constant.Constantes;
import eventManager.dto.*;
import eventManager.entity.Event;
import eventManager.entity.Ticket;
import eventManager.entity.User;
import eventManager.entity.UserRole;
import eventManager.exception.CustomException;
import eventManager.mapper.EventMapper;
import eventManager.mapper.TicketMapper;
import eventManager.repository.EventRepository;
import eventManager.repository.TicketRepository;
import eventManager.search.TicketSpecificationsBuilder;
import eventManager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ticket service, including event info retrieval, ticket
 * listing, user enrollment, and ticket updates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImpl Tests")
class TicketServiceImplTest {

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private TicketSpecificationsBuilder ticketSpecificationBuilder;

    @Mock
    private UserService userService;

    @Mock
    private AccessControlUtils accessControlUtils;

    private Event testEvent;
    private User testUser;
    private Ticket testTicket;
    private EventDTO testEventDTO;
    private TicketDTO testTicketDTO;
    private EventTicketDTO testEventTicketDTO;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .eventId(1)
                .eventCode("ABC123")
                .eventName("Test Event")
                .date(LocalDateTime.of(2027, 6, 15, 10, 0))
                .status("ACTIVO")
                .build();

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

        testTicket = Ticket.builder()
                .ticketId(10)
                .eventId(testEvent)
                .userId(testUser)
                .role("GUEST")
                .guestNumber(1)
                .invitationConfirmation(null)
                .assistConfirmation(null)
                .notes("Test notes")
                .build();

        testEventDTO = new EventDTO();
        testEventDTO.setEventId(1);
        testEventDTO.setEventCode("ABC123");

        testTicketDTO = new TicketDTO();
        testTicketDTO.setTicketId(10);
        testTicketDTO.setRole("GUEST");

        testEventTicketDTO = new EventTicketDTO();
    }

    /**
     * Verifies that event information is retrieved correctly from a ticket.
     */
    @Test
    @DisplayName("getEventInformation - Success")
    void testGetEventInfo_Success() {
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);
        when(ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(testTicketDTO, testEventDTO))
                .thenReturn(testEventTicketDTO);

        EventTicketDTO result = ticketService.getEventInformation("ABC123", 10, 1);

        assertNotNull(result);
    }

    /**
     * Verifies that querying information with a non-existent ticket throws
     * NOT_FOUND.
     */
    @Test
    @DisplayName("getEventInformation - Ticket not found")
    void testGetEventInfo_TicketNotFound() {
        when(ticketRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> ticketService.getEventInformation("ABC123", 999, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that a user not registered in the event cannot access its
     * information.
     */
    @Test
    @DisplayName("getEventInformation - User not registered in event")
    void testGetEventInfo_UserNotRegistered() {
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));

        CustomException ex = assertThrows(CustomException.class,
                () -> ticketService.getEventInformation("ABC123", 10, 999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT, ex.getMessage());
    }

    /**
     * Verifies that querying information for a non-existent event throws NOT_FOUND.
     */
    @Test
    @DisplayName("getEventInformation - Event not found")
    void testGetEventInfo_EventNotFound() {
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> ticketService.getEventInformation("NOTFND", 10, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that ticket listing for an event is retrieved correctly with
     * pagination.
     */
    @Test
    @DisplayName("getEventTickets - Success")
    void testGetEventTickets_Success() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        doNothing().when(accessControlUtils).validateUserIsHost(1);

        Page<Ticket> ticketPage = new PageImpl<>(List.of(testTicket));
        Specification<Ticket> specification = (root, query, builder) -> builder.conjunction();
        when(ticketRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Ticket>>any(), any(Pageable.class)))
                .thenReturn(ticketPage);
        when(ticketSpecificationBuilder.build(anyList())).thenReturn(specification);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);

        ResultPaginationDTO result = ticketService.getEventTickets("ABC123", 1, 10, "ticketId", "ASC", null);

        assertNotNull(result);
        assertFalse(result.getData().isEmpty());
    }

    /**
     * Verifies that listing tickets for a non-existent event throws NOT_FOUND.
     */
    @Test
    @DisplayName("getEventTickets - Event not found")
    void testGetEventTickets_EventNotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> ticketService.getEventTickets("NOTFND", 1, 10, "ticketId", "ASC", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that a user without the HOST role cannot list tickets for the event.
     */
    @Test
    @DisplayName("getEventTickets - Not HOST")
    void testGetEventTickets_NotHost() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_HOST)).when(accessControlUtils)
                .validateUserIsHost(1);

        CustomException ex = assertThrows(CustomException.class,
                () -> ticketService.getEventTickets("ABC123", 1, 10, "ticketId", "ASC", null));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that enrolling a user as GUEST happens without automatic
     * confirmations.
     */
    @Test
    @DisplayName("enrollUserInEvent - Successful enrollment as GUEST")
    void testEnroll_SuccessAsGuest() {
        EnrollUserDTO enrollDTO = EnrollUserDTO.builder()
                .eventCode("ABC123")
                .userId(1)
                .role("GUEST")
                .guestNumber(2)
                .notes("Test")
                .build();

        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserId(1, 1)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);

        TicketDTO result = ticketService.enrollUserInEvent(enrollDTO);

        assertNotNull(result);
        verify(ticketRepository)
                .save(argThat(t -> t.getInvitationConfirmation() == null && t.getAssistConfirmation() == null));
    }

    /**
     * Verifies that enrolling as HOST auto-confirms both invitation and attendance.
     */
    @Test
    @DisplayName("enrollUserInEvent - Enrollment as HOST auto-confirms")
    void testEnroll_SuccessAsHost() {
        EnrollUserDTO enrollDTO = EnrollUserDTO.builder()
                .eventCode("ABC123")
                .userId(1)
                .role("HOST")
                .guestNumber(1)
                .notes("Host ticket")
                .build();

        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserId(1, 1)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);

        ticketService.enrollUserInEvent(enrollDTO);

        verify(ticketRepository).save(argThat(t -> Boolean.TRUE.equals(t.getInvitationConfirmation())
                && Boolean.TRUE.equals(t.getAssistConfirmation())));
    }

    /**
     * Verifies that enrolling in a non-existent event throws NOT_FOUND.
     */
    @Test
    @DisplayName("enrollUserInEvent - Event not found")
    void testEnroll_EventNotFound() {
        EnrollUserDTO enrollDTO = EnrollUserDTO.builder().eventCode("NOTFND").userId(1).role("GUEST").build();
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.enrollUserInEvent(enrollDTO));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that a user already registered in the event cannot enroll again.
     */
    @Test
    @DisplayName("enrollUserInEvent - Already registered")
    void testEnroll_AlreadyRegistered() {
        EnrollUserDTO enrollDTO = EnrollUserDTO.builder().eventCode("ABC123").userId(1).role("GUEST").build();
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserId(1, 1)).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.enrollUserInEvent(enrollDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_ALREADY_REGISTERED_IN_EVENT, ex.getMessage());
    }

    /**
     * Verifies that if the guest number is not specified, it defaults to 1.
     */
    @Test
    @DisplayName("enrollUserInEvent - Null guestNumber defaults to 1")
    void testEnroll_NullGuestNumber() {
        EnrollUserDTO enrollDTO = EnrollUserDTO.builder()
                .eventCode("ABC123").userId(1).role("GUEST").guestNumber(null).build();

        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.existsByEventId_EventIdAndUserId_UserId(1, 1)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);

        ticketService.enrollUserInEvent(enrollDTO);

        verify(ticketRepository).save(argThat(t -> t.getGuestNumber() == 1));
    }

    /**
     * Verifies that updating an existing ticket completes correctly.
     */
    @Test
    @DisplayName("updateTicket - Successful update")
    void testUpdateTicket_Success() {
        UpdateTicketDTO updateDTO = new UpdateTicketDTO();
        updateDTO.setNotes("Updated notes");

        doNothing().when(accessControlUtils).validateTicketAccess(10);
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(testTicket)).thenReturn(testTicket);
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);
        when(ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(testTicketDTO, testEventDTO))
                .thenReturn(testEventTicketDTO);

        EventTicketDTO result = ticketService.updateTicket("ABC123", 10, updateDTO);

        assertNotNull(result);
        assertEquals("Updated notes", testTicket.getNotes());
    }

    /**
     * Verifies that a user without authorization cannot update a ticket.
     */
    @Test
    @DisplayName("updateTicket - Not authorized")
    void testUpdateTicket_NotAuthorized() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils)
                .validateTicketAccess(10);

        CustomException ex = assertThrows(CustomException.class,
                () -> ticketService.updateTicket("ABC123", 10, new UpdateTicketDTO()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that attempting to update a non-existent ticket throws NOT_FOUND.
     */
    @Test
    @DisplayName("updateTicket - Ticket not found")
    void testUpdateTicket_TicketNotFound() {
        doNothing().when(accessControlUtils).validateTicketAccess(999);
        when(ticketRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> ticketService.updateTicket("ABC123", 999, new UpdateTicketDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that partial updates only modify provided fields without changing
     * others.
     */
    @Test
    @DisplayName("updateTicket - Partial update (only non-null fields)")
    void testUpdateTicket_PartialUpdate() {
        UpdateTicketDTO updateDTO = new UpdateTicketDTO();
        updateDTO.setGuestNumber(5);

        doNothing().when(accessControlUtils).validateTicketAccess(10);
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(testTicket)).thenReturn(testTicket);
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);
        when(ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(testTicketDTO, testEventDTO))
                .thenReturn(testEventTicketDTO);

        ticketService.updateTicket("ABC123", 10, updateDTO);

        assertEquals(5, testTicket.getGuestNumber());
        assertEquals("GUEST", testTicket.getRole());
        assertEquals("Test notes", testTicket.getNotes());
    }

    /**
     * Verifies that the eventId-to-ticketId map is retrieved correctly for a given
     * user and role.
     */
    @Test
    @DisplayName("getTicketsByUserAndRole - Returns eventId->ticketId map")
    void testGetTicketsByUserAndRole_Success() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByUserId_UserIdAndRole(1, "HOST")).thenReturn(List.of(testTicket));

        Map<Integer, Integer> result = ticketService.getTicketsByUserAndRole(1, "HOST");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(1));
    }

    /**
     * Verifies that when no tickets exist for the user and role, an empty map is
     * returned.
     */
    @Test
    @DisplayName("getTicketsByUserAndRole - No tickets, returns empty map")
    void testGetTicketsByUserAndRole_NoTickets() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByUserId_UserIdAndRole(1, "HOST")).thenReturn(Collections.emptyList());

        Map<Integer, Integer> result = ticketService.getTicketsByUserAndRole(1, "HOST");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Verifies that a user's ticket for a specific event is retrieved correctly.
     */
    @Test
    @DisplayName("getTicketByEventAndUser - Success")
    void testGetTicketByEventAndUser_Success() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByEventId_EventIdAndUserId_UserId(1, 1)).thenReturn(Optional.of(testTicket));
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);

        TicketDTO result = ticketService.getTicketByEventAndUser(1, 1);

        assertNotNull(result);
    }

    /**
     * Verifies that looking up a non-existent ticket for an event and user throws
     * NOT_FOUND.
     */
    @Test
    @DisplayName("getTicketByEventAndUser - Not found")
    void testGetTicketByEventAndUser_NotFound() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByEventId_EventIdAndUserId_UserId(99, 1)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.getTicketByEventAndUser(99, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
