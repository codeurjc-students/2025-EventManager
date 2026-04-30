package eventManager.service.impl;

import eventManager.constant.Constantes;
import eventManager.dto.*;
import eventManager.entity.Event;
import eventManager.exception.CustomException;
import eventManager.mapper.EventMapper;
import eventManager.repository.EventRepository;
import eventManager.search.EventSpecificationsBuilder;
import eventManager.service.TicketService;
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
 * Unit tests for the event service, including retrieval, listing, creation, and
 * updates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventServiceImpl Tests")
class EventServiceImplTest {

    @InjectMocks
    private EventServiceImpl eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EventSpecificationsBuilder eventSpecificationBuilder;

    @Mock
    private UserService userService;

    @Mock
    private TicketService ticketService;

    @Mock
    private AccessControlUtils accessControlUtils;

    private Event testEvent;
    private EventDTO testEventDTO;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .eventId(1)
                .eventCode("ABC123")
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Place")
                .date(LocalDateTime.of(2027, 6, 15, 10, 0))
                .status("ACTIVO")
                .build();

        testEventDTO = new EventDTO();
        testEventDTO.setEventId(1);
        testEventDTO.setEventCode("ABC123");
        testEventDTO.setEventName("Test Event");
    }

    /**
     * Verifies that an existing event is retrieved correctly by its code.
     */
    @Test
    @DisplayName("getEvent - Success")
    void testGetEvent_Success() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);

        EventDTO result = eventService.getEvent("ABC123");

        assertNotNull(result);
        assertEquals("ABC123", result.getEventCode());
    }

    /**
     * Verifies that looking up an event with a non-existent code throws NOT_FOUND.
     */
    @Test
    @DisplayName("getEvent - Not found")
    void testGetEvent_NotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> eventService.getEvent("NOTFND"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(Constantes.MESSAGE_EVENT_DOES_NOT_EXIST, ex.getMessage());
    }

    /**
     * Verifies that the Event entity is retrieved correctly by its event code.
     */
    @Test
    @DisplayName("getEventByEventCode - Success")
    void testGetEventByCode_Success() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));

        Event result = eventService.getEventByEventCode("ABC123");

        assertNotNull(result);
        assertEquals("ABC123", result.getEventCode());
    }

    /**
     * Verifies that looking up an Event entity with a non-existent code throws
     * NOT_FOUND.
     */
    @Test
    @DisplayName("getEventByEventCode - Not found")
    void testGetEventByCode_NotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> eventService.getEventByEventCode("NOTFND"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that event listing returns paginated results correctly.
     */
    @Test
    @DisplayName("getEvents - With results")
    void testGetEvents_WithResults() {
        Map<Integer, Integer> ticketMap = new HashMap<>();
        ticketMap.put(1, 10);
        when(ticketService.getTicketsByUserAndRole(1, "HOST")).thenReturn(ticketMap);

        EventWithTicketDTO eventWithTicket = new EventWithTicketDTO();
        eventWithTicket.setEventId(1);
        when(eventMapper.convertEventToEventWithTicketDTO(testEvent)).thenReturn(eventWithTicket);

        Page<Event> eventPage = new PageImpl<>(List.of(testEvent));
        Specification<Event> specification = (root, query, builder) -> builder.conjunction();
        when(eventRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class)))
                .thenReturn(eventPage);
        when(eventSpecificationBuilder.build(anyList())).thenReturn(specification);

        ResultPaginationDTO result = eventService.getEvents(1, 10, "eventName", "ASC", null, 1, "HOST");

        assertNotNull(result);
        assertNotNull(result.getData());
    }

    /**
     * Verifies that if the user has no tickets, an empty event list is returned.
     */
    @Test
    @DisplayName("getEvents - No tickets, returns empty list")
    void testGetEvents_EmptyTickets() {
        when(ticketService.getTicketsByUserAndRole(1, "HOST")).thenReturn(new HashMap<>());

        ResultPaginationDTO result = eventService.getEvents(1, 10, "eventName", "ASC", null, 1, "HOST");

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPage().getTotalElements());
    }

    /**
     * Verifies that an unexpected error while listing events results in
     * INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("getEvents - Unexpected error throws INTERNAL_SERVER_ERROR")
    void testGetEvents_InternalError() {
        when(ticketService.getTicketsByUserAndRole(1, "HOST")).thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class,
                () -> eventService.getEvents(1, 10, "eventName", "ASC", null, 1, "HOST"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals(Constantes.MESSAGE_INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Verifies that event creation completes correctly and assigns the HOST ticket
     * to the creator.
     */
    @Test
    @DisplayName("createEvent - Successful creation")
    void testCreateEvent_Success() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1);
        when(userService.getUserInformation(1)).thenReturn(userDTO);
        when(eventRepository.existsByEventCode(anyString())).thenReturn(false);

        CreateUpdateEventDTO createDTO = new CreateUpdateEventDTO();
        createDTO.setEventName("New Event");
        when(eventMapper.convertCreateUpdateEventDTOToEvent(createDTO)).thenReturn(testEvent);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);

        TicketDTO ticketDTO = new TicketDTO();
        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class))).thenReturn(ticketDTO);

        EventDTO result = eventService.createEvent(1, createDTO);

        assertNotNull(result);
        verify(ticketService).enrollUserInEvent(argThat(dto -> "HOST".equals(dto.getRole())));
    }

    /**
     * Verifies that creating an event with a non-existent user throws BAD_REQUEST.
     */
    @Test
    @DisplayName("createEvent - User not found")
    void testCreateEvent_UserNotFound() {
        when(userService.getUserInformation(999))
                .thenThrow(new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));

        CustomException ex = assertThrows(CustomException.class,
                () -> eventService.createEvent(999, new CreateUpdateEventDTO()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifies that an unexpected error during event creation results in
     * INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("createEvent - Unexpected error throws INTERNAL_SERVER_ERROR")
    void testCreateEvent_UnexpectedError() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1);
        when(userService.getUserInformation(1)).thenReturn(userDTO);
        when(eventRepository.existsByEventCode(anyString())).thenReturn(false);

        CreateUpdateEventDTO createDTO = new CreateUpdateEventDTO();
        createDTO.setEventName("New Event");
        when(eventMapper.convertCreateUpdateEventDTOToEvent(createDTO)).thenReturn(testEvent);
        when(eventRepository.save(any(Event.class))).thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class, () -> eventService.createEvent(1, createDTO));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals(Constantes.MESSAGE_INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Verifies that updating an existing event completes correctly.
     */
    @Test
    @DisplayName("updateEvent - Successful update")
    void testUpdateEvent_Success() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        doNothing().when(accessControlUtils).validateUserIsHost(1);

        CreateUpdateEventDTO updateDTO = new CreateUpdateEventDTO();
        updateDTO.setEventName("Updated Event");
        Event updatedEvent = Event.builder().eventId(1).eventName("Updated Event").build();
        when(eventMapper.convertCreateUpdateEventDTOToEvent(updateDTO)).thenReturn(updatedEvent);
        when(eventRepository.save(updatedEvent)).thenReturn(updatedEvent);

        EventDTO updatedDTO = new EventDTO();
        updatedDTO.setEventName("Updated Event");
        when(eventMapper.convertEventToEventDTO(updatedEvent)).thenReturn(updatedDTO);

        EventDTO result = eventService.updateEvent("ABC123", updateDTO);

        assertNotNull(result);
        assertEquals("Updated Event", result.getEventName());
    }

    /**
     * Verifies that attempting to update a non-existent event throws NOT_FOUND.
     */
    @Test
    @DisplayName("updateEvent - Event not found")
    void testUpdateEvent_NotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> eventService.updateEvent("NOTFND", new CreateUpdateEventDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that a user without the HOST role cannot update the event.
     */
    @Test
    @DisplayName("updateEvent - Not HOST, throws FORBIDDEN")
    void testUpdateEvent_NotHost() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_HOST)).when(accessControlUtils)
                .validateUserIsHost(1);

        CustomException ex = assertThrows(CustomException.class,
                () -> eventService.updateEvent("ABC123", new CreateUpdateEventDTO()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
