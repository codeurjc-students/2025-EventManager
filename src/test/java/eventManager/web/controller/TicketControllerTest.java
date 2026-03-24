package eventManager.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventManager.dto.*;
import eventManager.security.jwt.JwtTokenProvider;
import eventManager.service.TicketService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del controlador de tickets. Cubre obtencion de informacion del evento, listado de tickets, inscripcion y actualizacion.
 */
@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TicketController Tests")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private TicketService ticketService;

        @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

        @MockitoBean
    private UserDetailsService userDetailsService;

    private EventTicketDTO eventTicketDTO;
    private TicketDTO ticketDTO;
    private EnrollUserDTO enrollUserDTO;
    private UpdateTicketDTO updateTicketDTO;
    private ResultPaginationDTO paginationDTO;

    @BeforeEach
    void setUp() {
        // EventTicketDTO
        eventTicketDTO = new EventTicketDTO();
                EventDTO eventDTO = new EventDTO();
                eventDTO.setEventCode("MADRID");
                eventDTO.setEventName("Cena de Navidad");
                eventDTO.setDate(LocalDateTime.now().plusDays(7));
                TicketDTO eventTicket = new TicketDTO();
                eventTicket.setTicketId(1);
                eventTicket.setRole("GUEST");
                eventTicket.setGuestNumber(2);
                eventTicket.setAssistConfirmation(false);
                eventTicketDTO.setEvent(eventDTO);
                eventTicketDTO.setTicket(eventTicket);

        // TicketDTO
        ticketDTO = new TicketDTO();
        ticketDTO.setTicketId(1);
        ticketDTO.setRole("GUEST");
        ticketDTO.setGuestNumber(2);
        ticketDTO.setAssistConfirmation(false);
        ticketDTO.setNotes("Test notes");

        // EnrollUserDTO
        enrollUserDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("MADRID")
                .role("GUEST")
                .guestNumber(2)
                .notes("Asistencia con familia")
                .build();

        // UpdateTicketDTO
        updateTicketDTO = UpdateTicketDTO.builder()
                .role("HOST")
                .guestNumber(0)
                .assistConfirmation(true)
                .notes("Updated notes")
                .build();

        // Resultado paginado
        paginationDTO = new ResultPaginationDTO();
        paginationDTO.setData(new ArrayList<>());
        PaginationDTO pageDTO = new PaginationDTO();
        pageDTO.setTotalPages(1);
        pageDTO.setTotalElements(0L);
        pageDTO.setNumber(0);
        pageDTO.setSize(10);
        paginationDTO.setPage(pageDTO);
    }

    /**
     * Verifica que se obtiene la informacion del evento y ticket correctamente.
     */
    @Test
    @DisplayName("Get Event Information - Exitoso")
    @WithMockUser
    void testGetEventInformation_Success() throws Exception {
        when(ticketService.getEventInformation("MADRID", 1, 1)).thenReturn(eventTicketDTO);

        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "MADRID", 1)
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.eventCode").value("MADRID"))
                .andExpect(jsonPath("$.ticket.ticketId").value(1))
                .andExpect(jsonPath("$.ticket.role").value("GUEST"));

        verify(ticketService, times(1)).getEventInformation("MADRID", 1, 1);
    }

    /**
     * Verifica que la consulta de informacion falla cuando el evento no existe.
     */
    @Test
    @DisplayName("Get Event Information - Evento no encontrado")
    @WithMockUser
    void testGetEventInformation_EventNotFound() throws Exception {
        when(ticketService.getEventInformation("NOTFOUND", 1, 1)).thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "NOTFOUND", 1)
                        .param("userId", "1"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventInformation("NOTFOUND", 1, 1);
    }

    /**
     * Verifica que la consulta de informacion falla cuando el ticket no existe.
     */
    @Test
    @DisplayName("Get Event Information - Ticket no encontrado")
    @WithMockUser
    void testGetEventInformation_TicketNotFound() throws Exception {
        when(ticketService.getEventInformation("MADRID", 999, 1)).thenThrow(new RuntimeException("Ticket not found"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "MADRID", 999)
                        .param("userId", "1"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventInformation("MADRID", 999, 1);
    }

    /**
     * Verifica que la consulta de informacion falla cuando el usuario no esta autorizado.
     */
    @Test
    @DisplayName("Get Event Information - Usuario no autorizado")
    @WithMockUser
    void testGetEventInformation_Unauthorized() throws Exception {
        when(ticketService.getEventInformation("MADRID", 1, 999)).thenThrow(new RuntimeException("User not authorized"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "MADRID", 1)
                        .param("userId", "999"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventInformation("MADRID", 1, 999);
    }

    /**
     * Verifica que la consulta de informacion falla cuando no se proporciona el userId.
     */
    @Test
    @DisplayName("Get Event Information - UserId null")
    @WithMockUser
    void testGetEventInformation_NullUserId() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "MADRID", 1))
                                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que se listan los tickets de un evento correctamente con paginacion.
     */
    @Test
    @DisplayName("Get Event Tickets - Listar tickets exitosamente")
    @WithMockUser
    void testGetEventTickets_Success() throws Exception {
        when(ticketService.getEventTickets(eq("MADRID"), anyInt(), anyInt(), anyString(), anyString(), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "MADRID")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("sortBy", "ticketId")
                        .param("sortDir", "ASC")
                        .param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10));

        verify(ticketService, times(1)).getEventTickets(eq("MADRID"), anyInt(), anyInt(), anyString(), anyString(), anyString());
    }

    /**
     * Verifica que el listado de tickets funciona correctamente con filtro de busqueda.
     */
    @Test
    @DisplayName("Get Event Tickets - Con búsqueda")
    @WithMockUser
    void testGetEventTickets_WithSearch() throws Exception {
                when(ticketService.getEventTickets(eq("MADRID"), anyInt(), nullable(Integer.class), anyString(), anyString(), eq("Carlos"))).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "MADRID")
                        .param("page", "1")
                        .param("search", "Carlos"))
                .andExpect(status().isOk());

        verify(ticketService, times(1)).getEventTickets(eq("MADRID"), anyInt(), nullable(Integer.class), anyString(), anyString(), eq("Carlos"));
    }

    /**
     * Verifica que el listado de tickets falla cuando el evento no existe.
     */
    @Test
    @DisplayName("Get Event Tickets - Evento no encontrado")
    @WithMockUser
    void testGetEventTickets_EventNotFound() throws Exception {
                when(ticketService.getEventTickets(eq("NOTFND"), anyInt(), nullable(Integer.class), anyString(), anyString(), nullable(String.class))).thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "NOTFND")
                        .param("page", "1"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventTickets(eq("NOTFND"), anyInt(), nullable(Integer.class), anyString(), anyString(), nullable(String.class));
    }

    /**
     * Verifica el comportamiento del listado de tickets cuando se proporciona una pagina invalida.
     */
    @Test
    @DisplayName("Get Event Tickets - Página inválida")
    @WithMockUser
    void testGetEventTickets_InvalidPage() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}/tickets", "ABC123")
                        .param("page", "-1"))
                                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que el listado de tickets funciona correctamente al ordenar por rol de forma descendente.
     */
    @Test
    @DisplayName("Get Event Tickets - Ordenar por rol")
    @WithMockUser
    void testGetEventTickets_SortByRole() throws Exception {
                when(ticketService.getEventTickets(eq("ABC123"), anyInt(), nullable(Integer.class), eq("role"), eq("DESC"), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "ABC123")
                        .param("page", "1")
                        .param("sortBy", "role")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk());

        verify(ticketService, times(1)).getEventTickets(eq("ABC123"), anyInt(), nullable(Integer.class), eq("role"), eq("DESC"), nullable(String.class));
    }

    /**
     * Verifica que la inscripcion de un usuario en un evento funciona correctamente.
     */
    @Test
    @DisplayName("Enroll User - Inscripción exitosa")
    @WithMockUser
    void testEnrollUser_Success() throws Exception {
        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class))).thenReturn(ticketDTO);

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollUserDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(1))
                .andExpect(jsonPath("$.role").value("GUEST"));

        verify(ticketService, times(1)).enrollUserInEvent(any(EnrollUserDTO.class));
    }

    /**
     * Verifica que la inscripcion falla cuando el usuario ya esta inscrito en el evento.
     */
    @Test
    @DisplayName("Enroll User - Usuario ya inscrito")
    @WithMockUser
    void testEnrollUser_AlreadyEnrolled() throws Exception {
        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class))).thenThrow(new RuntimeException("User already enrolled in this event"));

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollUserDTO)))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).enrollUserInEvent(any(EnrollUserDTO.class));
    }

    /**
     * Verifica que la inscripcion falla cuando el evento esta lleno.
     */
    @Test
    @DisplayName("Enroll User - Evento lleno")
    @WithMockUser
    void testEnrollUser_EventFull() throws Exception {
        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class))).thenThrow(new RuntimeException("Event is at full capacity"));

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollUserDTO)))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).enrollUserInEvent(any(EnrollUserDTO.class));
    }

    /**
     * Verifica que la inscripcion se rechaza cuando el codigo de evento es invalido.
     */
    @Test
    @DisplayName("Enroll User - Código de evento inválido")
    @WithMockUser
    void testEnrollUser_InvalidEventCode() throws Exception {
        EnrollUserDTO invalidDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("abc")  // Código inválido (debe ser 6 caracteres mayúsculas)
                .role("GUEST")
                .guestNumber(2)
                .build();

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que la inscripcion se rechaza cuando el formato del codigo de evento es incorrecto.
     */
    @Test
    @DisplayName("Enroll User - Código de evento formato incorrecto")
    @WithMockUser
    void testEnrollUser_WrongFormatEventCode() throws Exception {
        EnrollUserDTO wrongFormatDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("abc123")  // No es mayúsculas
                .role("GUEST")
                .guestNumber(2)
                .build();

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongFormatDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica el comportamiento de la inscripcion cuando se proporciona un numero de invitados negativo.
     */
    @Test
    @DisplayName("Enroll User - Número de invitados negativo")
    @WithMockUser
    void testEnrollUser_NegativeGuestNumber() throws Exception {
        EnrollUserDTO negativeGuestsDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("ABC123")
                .role("GUEST")
                .guestNumber(-1)  // Número negativo
                .build();

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeGuestsDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifica que la inscripcion falla cuando el usuario no existe.
     */
    @Test
    @DisplayName("Enroll User - Usuario no encontrado")
    @WithMockUser
    void testEnrollUser_UserNotFound() throws Exception {
        EnrollUserDTO invalidUserDTO = EnrollUserDTO.builder()
                .userId(999)
                .eventCode("ABC123")
                .role("GUEST")
                .guestNumber(2)
                .build();

        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDTO)))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).enrollUserInEvent(any(EnrollUserDTO.class));
    }

    /**
     * Verifica que la inscripcion falla cuando el evento no existe.
     */
    @Test
    @DisplayName("Enroll User - Evento no encontrado")
    @WithMockUser
    void testEnrollUser_EventNotFound() throws Exception {
        EnrollUserDTO invalidEventDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("NOTFND")
                .role("GUEST")
                .guestNumber(2)
                .build();

        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class)))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEventDTO)))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).enrollUserInEvent(any(EnrollUserDTO.class));
    }

    /**
     * Verifica que la inscripcion falla cuando se proporciona un rol invalido.
     */
    @Test
    @DisplayName("Enroll User - Rol inválido")
    @WithMockUser
    void testEnrollUser_InvalidRole() throws Exception {
        EnrollUserDTO invalidRoleDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("ABC123")
                .role("INVALID_ROLE")
                .guestNumber(2)
                .build();

        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class)))
                .thenThrow(new RuntimeException("Invalid role"));

        mockMvc.perform(post("/api/events/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoleDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que la actualizacion de un ticket funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Update Ticket - Actualización exitosa")
    @WithMockUser
    void testUpdateTicket_Success() throws Exception {
        when(ticketService.updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class)))
                .thenReturn(eventTicketDTO);

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTicketDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.ticketId").value(1));

        verify(ticketService, times(1)).updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class));
    }

    /**
     * Verifica que la actualizacion falla cuando el ticket no existe.
     */
    @Test
    @DisplayName("Update Ticket - Ticket no encontrado")
    @WithMockUser
    void testUpdateTicket_NotFound() throws Exception {
        when(ticketService.updateTicket(eq("ABC123"), eq(999), any(UpdateTicketDTO.class)))
                .thenThrow(new RuntimeException("Ticket not found"));

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTicketDTO)))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).updateTicket(eq("ABC123"), eq(999), any(UpdateTicketDTO.class));
    }

    /**
     * Verifica que se puede cambiar el rol de un ticket a HOST correctamente.
     */
    @Test
    @DisplayName("Update Ticket - Cambiar rol a HOST")
    @WithMockUser
    void testUpdateTicket_ChangeToHost() throws Exception {
        UpdateTicketDTO hostDTO = UpdateTicketDTO.builder()
                .role("HOST")
                .guestNumber(0)
                .assistConfirmation(true)
                .build();

        when(ticketService.updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class)))
                .thenReturn(eventTicketDTO);

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostDTO)))
                .andExpect(status().isOk());

        verify(ticketService, times(1)).updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class));
    }

    /**
     * Verifica que se puede marcar la confirmacion de asistencia en un ticket.
     */
    @Test
    @DisplayName("Update Ticket - Marcar asistencia")
    @WithMockUser
    void testUpdateTicket_MarkAttendance() throws Exception {
        UpdateTicketDTO attendanceDTO = UpdateTicketDTO.builder()
                .role("GUEST")
                .guestNumber(2)
                .assistConfirmation(true)
                .notes("Confirmed attendance")
                .build();

        when(ticketService.updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class)))
                .thenReturn(eventTicketDTO);

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendanceDTO)))
                .andExpect(status().isOk());

        verify(ticketService, times(1)).updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class));
    }

    /**
     * Verifica que se pueden actualizar las notas de un ticket correctamente.
     */
    @Test
    @DisplayName("Update Ticket - Actualizar notas")
    @WithMockUser
    void testUpdateTicket_UpdateNotes() throws Exception {
        UpdateTicketDTO notesDTO = UpdateTicketDTO.builder()
                .role("GUEST")
                .guestNumber(2)
                .assistConfirmation(false)
                .notes("Updated with new information")
                .build();

        when(ticketService.updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class)))
                .thenReturn(eventTicketDTO);

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notesDTO)))
                .andExpect(status().isOk());

        verify(ticketService, times(1)).updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class));
    }

    /**
     * Verifica que la actualizacion se rechaza cuando las notas exceden los 500 caracteres.
     */
    @Test
    @DisplayName("Update Ticket - Notas demasiado largas")
    @WithMockUser
    void testUpdateTicket_NotesTooLong() throws Exception {
        UpdateTicketDTO longNotesDTO = UpdateTicketDTO.builder()
                .role("GUEST")
                .guestNumber(2)
                .assistConfirmation(false)
                .notes("A".repeat(505))  // > 500 caracteres
                .build();

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longNotesDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica el comportamiento de la actualizacion cuando el numero de invitados es negativo.
     */
    @Test
    @DisplayName("Update Ticket - Número de invitados negativo")
    @WithMockUser
    void testUpdateTicket_NegativeGuestNumber() throws Exception {
        UpdateTicketDTO negativeDTO = UpdateTicketDTO.builder()
                .role("GUEST")
                .guestNumber(-5)
                .assistConfirmation(false)
                .build();

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeDTO)))
                .andExpect(status().isOk());
    }

    /**
     * Verifica que la actualizacion falla cuando se proporciona un rol invalido.
     */
    @Test
    @DisplayName("Update Ticket - Rol inválido")
    @WithMockUser
    void testUpdateTicket_InvalidRole() throws Exception {
        UpdateTicketDTO invalidRoleDTO = UpdateTicketDTO.builder()
                .role("INVALID_ROLE")
                .guestNumber(2)
                .assistConfirmation(false)
                .build();

        when(ticketService.updateTicket(eq("ABC123"), eq(1), any(UpdateTicketDTO.class)))
                .thenThrow(new RuntimeException("Invalid role"));

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoleDTO)))
                .andExpect(status().is5xxServerError());
    }
}
