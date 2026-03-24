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

/** Pruebas unitarias del servicio de tickets, incluyendo consulta de informacion de evento, listado de tickets, inscripcion de usuarios y actualizacion de tickets. */
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
     * Verifica que se obtiene correctamente la informacion de un evento a partir del ticket.
     */
    @Test
    @DisplayName("getEventInformation - Exitoso")
    void testGetEventInfo_Success() {
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);
        when(ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(testTicketDTO, testEventDTO)).thenReturn(testEventTicketDTO);

        EventTicketDTO result = ticketService.getEventInformation("ABC123", 10, 1);

        assertNotNull(result);
    }

    /**
     * Verifica que consultar informacion con un ticket inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getEventInformation - Ticket no encontrado")
    void testGetEventInfo_TicketNotFound() {
        when(ticketRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.getEventInformation("ABC123", 999, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que un usuario no registrado en el evento no puede consultar su informacion.
     */
    @Test
    @DisplayName("getEventInformation - Usuario no registrado en evento")
    void testGetEventInfo_UserNotRegistered() {
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.getEventInformation("ABC123", 10, 999));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT, ex.getMessage());
    }

    /**
     * Verifica que consultar informacion de un evento inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getEventInformation - Evento no encontrado")
    void testGetEventInfo_EventNotFound() {
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.getEventInformation("NOTFND", 10, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que el listado de tickets de un evento se obtiene correctamente con paginacion.
     */
    @Test
    @DisplayName("getEventTickets - Exitoso")
    void testGetEventTickets_Success() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        doNothing().when(accessControlUtils).validateUserIsHost(1);

        Page<Ticket> ticketPage = new PageImpl<>(List.of(testTicket));
        Specification<Ticket> specification = (root, query, builder) -> builder.conjunction();
        when(ticketRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Ticket>>any(), any(Pageable.class))).thenReturn(ticketPage);
        when(ticketSpecificationBuilder.build(anyList())).thenReturn(specification);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);

        ResultPaginationDTO result = ticketService.getEventTickets("ABC123", 1, 10, "ticketId", "ASC", null);

        assertNotNull(result);
        assertFalse(result.getData().isEmpty());
    }

    /**
     * Verifica que listar tickets de un evento inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getEventTickets - Evento no encontrado")
    void testGetEventTickets_EventNotFound() {
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.getEventTickets("NOTFND", 1, 10, "ticketId", "ASC", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que un usuario sin rol HOST no puede listar los tickets del evento.
     */
    @Test
    @DisplayName("getEventTickets - No es HOST")
    void testGetEventTickets_NotHost() {
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_HOST)).when(accessControlUtils).validateUserIsHost(1);

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.getEventTickets("ABC123", 1, 10, "ticketId", "ASC", null));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que la inscripcion de un usuario como GUEST se realiza sin confirmaciones automaticas.
     */
    @Test
    @DisplayName("enrollUserInEvent - Inscripcion exitosa como GUEST")
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
        verify(ticketRepository).save(argThat(t -> t.getInvitationConfirmation() == null && t.getAssistConfirmation() == null));
    }

    /**
     * Verifica que la inscripcion como HOST auto-confirma tanto la invitacion como la asistencia.
     */
    @Test
    @DisplayName("enrollUserInEvent - Inscripcion como HOST auto-confirma")
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

        verify(ticketRepository).save(argThat(t -> Boolean.TRUE.equals(t.getInvitationConfirmation()) && Boolean.TRUE.equals(t.getAssistConfirmation())));
    }

    /**
     * Verifica que inscribirse en un evento inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("enrollUserInEvent - Evento no encontrado")
    void testEnroll_EventNotFound() {
        EnrollUserDTO enrollDTO = EnrollUserDTO.builder().eventCode("NOTFND").userId(1).role("GUEST").build();
        when(eventRepository.findByEventCode("NOTFND")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.enrollUserInEvent(enrollDTO));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que un usuario ya registrado en el evento no puede inscribirse de nuevo.
     */
    @Test
    @DisplayName("enrollUserInEvent - Ya registrado")
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
     * Verifica que si no se indica el numero de invitados se establece por defecto a 1.
     */
    @Test
    @DisplayName("enrollUserInEvent - guestNumber nulo se establece a 1")
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
     * Verifica que la actualizacion de un ticket existente se realiza correctamente.
     */
    @Test
    @DisplayName("updateTicket - Actualizacion exitosa")
    void testUpdateTicket_Success() {
        UpdateTicketDTO updateDTO = new UpdateTicketDTO();
        updateDTO.setNotes("Updated notes");

        doNothing().when(accessControlUtils).validateTicketAccess(10);
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(testTicket)).thenReturn(testTicket);
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);
        when(ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(testTicketDTO, testEventDTO)).thenReturn(testEventTicketDTO);

        EventTicketDTO result = ticketService.updateTicket("ABC123", 10, updateDTO);

        assertNotNull(result);
        assertEquals("Updated notes", testTicket.getNotes());
    }

    /**
     * Verifica que un usuario sin autorizacion no puede actualizar un ticket.
     */
    @Test
    @DisplayName("updateTicket - No autorizado")
    void testUpdateTicket_NotAuthorized() {
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS)).when(accessControlUtils).validateTicketAccess(10);

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.updateTicket("ABC123", 10, new UpdateTicketDTO()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que intentar actualizar un ticket inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("updateTicket - Ticket no encontrado")
    void testUpdateTicket_TicketNotFound() {
        doNothing().when(accessControlUtils).validateTicketAccess(999);
        when(ticketRepository.findById(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.updateTicket("ABC123", 999, new UpdateTicketDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que la actualizacion parcial solo modifica los campos proporcionados, sin alterar los demas.
     */
    @Test
    @DisplayName("updateTicket - Actualizacion parcial (solo campos no null)")
    void testUpdateTicket_PartialUpdate() {
        UpdateTicketDTO updateDTO = new UpdateTicketDTO();
        updateDTO.setGuestNumber(5);

        doNothing().when(accessControlUtils).validateTicketAccess(10);
        when(ticketRepository.findById(10)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(testTicket)).thenReturn(testTicket);
        when(eventRepository.findByEventCode("ABC123")).thenReturn(Optional.of(testEvent));
        when(eventMapper.convertEventToEventDTO(testEvent)).thenReturn(testEventDTO);
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);
        when(ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(testTicketDTO, testEventDTO)).thenReturn(testEventTicketDTO);

        ticketService.updateTicket("ABC123", 10, updateDTO);

        assertEquals(5, testTicket.getGuestNumber());
        assertEquals("GUEST", testTicket.getRole());
        assertEquals("Test notes", testTicket.getNotes());
    }

    /**
     * Verifica que se obtiene correctamente el mapa de eventId a ticketId para un usuario y rol dados.
     */
    @Test
    @DisplayName("getTicketsByUserAndRole - Retorna mapa eventId->ticketId")
    void testGetTicketsByUserAndRole_Success() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByUserId_UserIdAndRole(1, "HOST")).thenReturn(List.of(testTicket));

        Map<Integer, Integer> result = ticketService.getTicketsByUserAndRole(1, "HOST");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(1));
    }

    /**
     * Verifica que si no existen tickets para el usuario y rol se devuelve un mapa vacio.
     */
    @Test
    @DisplayName("getTicketsByUserAndRole - Sin tickets, retorna mapa vacio")
    void testGetTicketsByUserAndRole_NoTickets() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByUserId_UserIdAndRole(1, "HOST")).thenReturn(Collections.emptyList());

        Map<Integer, Integer> result = ticketService.getTicketsByUserAndRole(1, "HOST");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Verifica que se obtiene correctamente el ticket de un usuario en un evento especifico.
     */
    @Test
    @DisplayName("getTicketByEventAndUser - Exitoso")
    void testGetTicketByEventAndUser_Success() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByEventId_EventIdAndUserId_UserId(1, 1)).thenReturn(Optional.of(testTicket));
        when(ticketMapper.convertTicketToTicketDTO(testTicket)).thenReturn(testTicketDTO);

        TicketDTO result = ticketService.getTicketByEventAndUser(1, 1);

        assertNotNull(result);
    }

    /**
     * Verifica que buscar un ticket inexistente para un evento y usuario lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getTicketByEventAndUser - No encontrado")
    void testGetTicketByEventAndUser_NotFound() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ticketRepository.findByEventId_EventIdAndUserId_UserId(99, 1)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> ticketService.getTicketByEventAndUser(99, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
