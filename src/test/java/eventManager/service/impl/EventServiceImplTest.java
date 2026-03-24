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

/** Pruebas unitarias del servicio de eventos, incluyendo consulta, listado, creacion y actualizacion de eventos. */
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
     * Verifica que se obtiene correctamente un evento existente a partir de su codigo.
     */
    @Test
    @DisplayName("getEvent - Exitoso")
    void testGetEvent_Success() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);

        EventDTO result = eventService.getEvent("ABC123");

        assertNotNull(result);
        assertEquals("ABC123", result.getEventCode());
    }

    /**
     * Verifica que buscar un evento con un codigo inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getEvent - No encontrado")
    void testGetEvent_NotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> eventService.getEvent("NOTFND"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(Constantes.MESSAGE_EVENT_DOES_NOT_EXIST, ex.getMessage());
    }

    /**
     * Verifica que se recupera correctamente la entidad Event a partir de su codigo de evento.
     */
    @Test
    @DisplayName("getEventByEventCode - Exitoso")
    void testGetEventByCode_Success() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));

        Event result = eventService.getEventByEventCode("ABC123");

        assertNotNull(result);
        assertEquals("ABC123", result.getEventCode());
    }

    /**
     * Verifica que buscar una entidad Event con un codigo inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getEventByEventCode - No encontrado")
    void testGetEventByCode_NotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> eventService.getEventByEventCode("NOTFND"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que el listado de eventos devuelve resultados paginados correctamente.
     */
    @Test
    @DisplayName("getEvents - Con resultados")
    void testGetEvents_WithResults() {
        Map<Integer, Integer> ticketMap = new HashMap<>();
        ticketMap.put(1, 10);
        when(ticketService.getTicketsByUserAndRole(1, "HOST")).thenReturn(ticketMap);

        EventWithTicketDTO eventWithTicket = new EventWithTicketDTO();
        eventWithTicket.setEventId(1);
        when(eventMapper.convertEventToEventWithTicketDTO(testEvent)).thenReturn(eventWithTicket);

        Page<Event> eventPage = new PageImpl<>(List.of(testEvent));
        Specification<Event> specification = (root, query, builder) -> builder.conjunction();
        when(eventRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class))).thenReturn(eventPage);
        when(eventSpecificationBuilder.build(anyList())).thenReturn(specification);

        ResultPaginationDTO result = eventService.getEvents(1, 10, "eventName", "ASC", null, 1, "HOST");

        assertNotNull(result);
        assertNotNull(result.getData());
    }

    /**
     * Verifica que si el usuario no tiene tickets se devuelve una lista vacia de eventos.
     */
    @Test
    @DisplayName("getEvents - Sin tickets, retorna lista vacia")
    void testGetEvents_EmptyTickets() {
        when(ticketService.getTicketsByUserAndRole(1, "HOST")).thenReturn(new HashMap<>());

        ResultPaginationDTO result = eventService.getEvents(1, 10, "eventName", "ASC", null, 1, "HOST");

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getPage().getTotalElements());
    }

    /**
     * Verifica que un error inesperado al listar eventos produce INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("getEvents - Error inesperado lanza INTERNAL_SERVER_ERROR")
    void testGetEvents_InternalError() {
        when(ticketService.getTicketsByUserAndRole(1, "HOST")).thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class, () -> eventService.getEvents(1, 10, "eventName", "ASC", null, 1, "HOST"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals(Constantes.MESSAGE_INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Verifica que la creacion de un evento se realiza correctamente y se asigna el ticket de HOST al creador.
     */
    @Test
    @DisplayName("createEvent - Creacion exitosa")
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
     * Verifica que crear un evento con un usuario inexistente lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("createEvent - Usuario no encontrado")
    void testCreateEvent_UserNotFound() {
        when(userService.getUserInformation(999)).thenThrow(new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));

        CustomException ex = assertThrows(CustomException.class, () -> eventService.createEvent(999, new CreateUpdateEventDTO()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifica que un error inesperado durante la creacion de un evento produce INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("createEvent - Error inesperado lanza INTERNAL_SERVER_ERROR")
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
     * Verifica que la actualizacion de un evento existente se completa correctamente.
     */
    @Test
    @DisplayName("updateEvent - Actualizacion exitosa")
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
     * Verifica que intentar actualizar un evento inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("updateEvent - Evento no encontrado")
    void testUpdateEvent_NotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> eventService.updateEvent("NOTFND", new CreateUpdateEventDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que un usuario sin rol HOST no puede actualizar el evento.
     */
    @Test
    @DisplayName("updateEvent - No es HOST, lanza FORBIDDEN")
    void testUpdateEvent_NotHost() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_HOST)).when(accessControlUtils).validateUserIsHost(1);

        CustomException ex = assertThrows(CustomException.class, () -> eventService.updateEvent("ABC123", new CreateUpdateEventDTO()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
