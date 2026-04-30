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
 * Unit tests for the ticket controller. Covers event information retrieval,
 * ticket listing, enrollment, and updates.
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

        // Paginated result
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
     * Verifies that event and ticket information is retrieved correctly.
     */
    @Test
    @DisplayName("Get Event Information - Success")
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
     * Verifies that the information query fails when the event does not exist.
     */
    @Test
    @DisplayName("Get Event Information - Event not found")
    @WithMockUser
    void testGetEventInformation_EventNotFound() throws Exception {
        when(ticketService.getEventInformation("NOTFOUND", 1, 1))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "NOTFOUND", 1)
                .param("userId", "1"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventInformation("NOTFOUND", 1, 1);
    }

    /**
     * Verifies that the information query fails when the ticket does not exist.
     */
    @Test
    @DisplayName("Get Event Information - Ticket not found")
    @WithMockUser
    void testGetEventInformation_TicketNotFound() throws Exception {
        when(ticketService.getEventInformation("MADRID", 999, 1))
                .thenThrow(new RuntimeException("Ticket not found"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "MADRID", 999)
                .param("userId", "1"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventInformation("MADRID", 999, 1);
    }

    /**
     * Verifies that the information query fails when the user is not authorized.
     */
    @Test
    @DisplayName("Get Event Information - User not authorized")
    @WithMockUser
    void testGetEventInformation_Unauthorized() throws Exception {
        when(ticketService.getEventInformation("MADRID", 1, 999))
                .thenThrow(new RuntimeException("User not authorized"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "MADRID", 1)
                .param("userId", "999"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventInformation("MADRID", 1, 999);
    }

    /**
     * Verifies that the information query fails when userId is not provided.
     */
    @Test
    @DisplayName("Get Event Information - UserId is null")
    @WithMockUser
    void testGetEventInformation_NullUserId() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}/tickets/{ticketId}", "MADRID", 1))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that tickets for an event are listed correctly with pagination.
     */
    @Test
    @DisplayName("Get Event Tickets - Successfully list tickets")
    @WithMockUser
    void testGetEventTickets_Success() throws Exception {
        when(ticketService.getEventTickets(eq("MADRID"), anyInt(), anyInt(), anyString(), anyString(),
                anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "MADRID")
                .param("page", "1")
                .param("pageSize", "10")
                .param("sortBy", "ticketId")
                .param("sortDir", "ASC")
                .param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10));

        verify(ticketService, times(1)).getEventTickets(eq("MADRID"), anyInt(), anyInt(), anyString(),
                anyString(), anyString());
    }

    /**
     * Verifies that ticket listing works correctly with a search filter.
     */
    @Test
    @DisplayName("Get Event Tickets - With search")
    @WithMockUser
    void testGetEventTickets_WithSearch() throws Exception {
        when(ticketService.getEventTickets(eq("MADRID"), anyInt(), nullable(Integer.class), anyString(),
                anyString(), eq("Carlos"))).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "MADRID")
                .param("page", "1")
                .param("search", "Carlos"))
                .andExpect(status().isOk());

        verify(ticketService, times(1)).getEventTickets(eq("MADRID"), anyInt(), nullable(Integer.class),
                anyString(), anyString(), eq("Carlos"));
    }

    /**
     * Verifies that ticket listing fails when the event does not exist.
     */
    @Test
    @DisplayName("Get Event Tickets - Event not found")
    @WithMockUser
    void testGetEventTickets_EventNotFound() throws Exception {
        when(ticketService.getEventTickets(eq("NOTFND"), anyInt(), nullable(Integer.class), anyString(),
                anyString(), nullable(String.class)))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "NOTFND")
                .param("page", "1"))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).getEventTickets(eq("NOTFND"), anyInt(), nullable(Integer.class),
                anyString(), anyString(), nullable(String.class));
    }

    /**
     * Verifies ticket listing behavior when an invalid page is provided.
     */
    @Test
    @DisplayName("Get Event Tickets - Invalid page")
    @WithMockUser
    void testGetEventTickets_InvalidPage() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}/tickets", "ABC123")
                .param("page", "-1"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that ticket listing works correctly when sorting by role in
     * descending order.
     */
    @Test
    @DisplayName("Get Event Tickets - Sort by role")
    @WithMockUser
    void testGetEventTickets_SortByRole() throws Exception {
        when(ticketService.getEventTickets(eq("ABC123"), anyInt(), nullable(Integer.class), eq("role"),
                eq("DESC"), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/tickets", "ABC123")
                .param("page", "1")
                .param("sortBy", "role")
                .param("sortDir", "DESC"))
                .andExpect(status().isOk());

        verify(ticketService, times(1)).getEventTickets(eq("ABC123"), anyInt(), nullable(Integer.class),
                eq("role"), eq("DESC"), nullable(String.class));
    }

    /**
     * Verifies that enrolling a user in an event works correctly.
     */
    @Test
    @DisplayName("Enroll User - Successful enrollment")
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
     * Verifies that enrollment fails when the user is already enrolled in the
     * event.
     */
    @Test
    @DisplayName("Enroll User - User already enrolled")
    @WithMockUser
    void testEnrollUser_AlreadyEnrolled() throws Exception {
        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class)))
                .thenThrow(new RuntimeException("User already enrolled in this event"));

        mockMvc.perform(post("/api/events/enrollment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enrollUserDTO)))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).enrollUserInEvent(any(EnrollUserDTO.class));
    }

    /**
     * Verifies that enrollment fails when the event is full.
     */
    @Test
    @DisplayName("Enroll User - Event full")
    @WithMockUser
    void testEnrollUser_EventFull() throws Exception {
        when(ticketService.enrollUserInEvent(any(EnrollUserDTO.class)))
                .thenThrow(new RuntimeException("Event is at full capacity"));

        mockMvc.perform(post("/api/events/enrollment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enrollUserDTO)))
                .andExpect(status().is5xxServerError());

        verify(ticketService, times(1)).enrollUserInEvent(any(EnrollUserDTO.class));
    }

    /**
     * Verifies that enrollment is rejected when the event code is invalid.
     */
    @Test
    @DisplayName("Enroll User - Invalid event code")
    @WithMockUser
    void testEnrollUser_InvalidEventCode() throws Exception {
        EnrollUserDTO invalidDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("abc") // Invalid code (must be 6 uppercase characters)
                .role("GUEST")
                .guestNumber(2)
                .build();

        mockMvc.perform(post("/api/events/enrollment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that enrollment is rejected when the event code format is incorrect.
     */
    @Test
    @DisplayName("Enroll User - Event code wrong format")
    @WithMockUser
    void testEnrollUser_WrongFormatEventCode() throws Exception {
        EnrollUserDTO wrongFormatDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("abc123") // Not uppercase
                .role("GUEST")
                .guestNumber(2)
                .build();

        mockMvc.perform(post("/api/events/enrollment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongFormatDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies enrollment behavior when a negative guest count is provided.
     */
    @Test
    @DisplayName("Enroll User - Negative guest number")
    @WithMockUser
    void testEnrollUser_NegativeGuestNumber() throws Exception {
        EnrollUserDTO negativeGuestsDTO = EnrollUserDTO.builder()
                .userId(1)
                .eventCode("ABC123")
                .role("GUEST")
                .guestNumber(-1) // Negative number
                .build();

        mockMvc.perform(post("/api/events/enrollment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(negativeGuestsDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifies that enrollment fails when the user does not exist.
     */
    @Test
    @DisplayName("Enroll User - User not found")
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
     * Verifies that enrollment fails when the event does not exist.
     */
    @Test
    @DisplayName("Enroll User - Event not found")
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
     * Verifies that enrollment fails when an invalid role is provided.
     */
    @Test
    @DisplayName("Enroll User - Invalid role")
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
     * Verifies that ticket update works correctly with valid data.
     */
    @Test
    @DisplayName("Update Ticket - Successful update")
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
     * Verifies that update fails when the ticket does not exist.
     */
    @Test
    @DisplayName("Update Ticket - Ticket not found")
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
     * Verifies that a ticket role can be changed to HOST correctly.
     */
    @Test
    @DisplayName("Update Ticket - Change role to HOST")
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
     * Verifies that attendance confirmation can be marked on a ticket.
     */
    @Test
    @DisplayName("Update Ticket - Mark attendance")
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
     * Verifies that ticket notes can be updated correctly.
     */
    @Test
    @DisplayName("Update Ticket - Update notes")
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
     * Verifies that update is rejected when notes exceed 500 characters.
     */
    @Test
    @DisplayName("Update Ticket - Notes too long")
    @WithMockUser
    void testUpdateTicket_NotesTooLong() throws Exception {
        UpdateTicketDTO longNotesDTO = UpdateTicketDTO.builder()
                .role("GUEST")
                .guestNumber(2)
                .assistConfirmation(false)
                .notes("A".repeat(505)) // > 500 characters
                .build();

        mockMvc.perform(put("/api/events/{eventCode}/tickets/{ticketId}", "ABC123", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longNotesDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies update behavior when the guest number is negative.
     */
    @Test
    @DisplayName("Update Ticket - Negative guest number")
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
     * Verifies that update fails when an invalid role is provided.
     */
    @Test
    @DisplayName("Update Ticket - Invalid role")
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
