package eventManager.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventManager.dto.CreateUpdateEventDTO;
import eventManager.dto.EventDTO;
import eventManager.dto.PaginationDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.security.jwt.JwtTokenProvider;
import eventManager.service.EventService;
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
 * Unit tests for the event controller. Covers event retrieval, creation,
 * updates, and lookup by code.
 */
@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EventController Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private EventDTO eventDTO;
    private CreateUpdateEventDTO createEventDTO;
    private ResultPaginationDTO paginationDTO;

    @BeforeEach
    void setUp() {
        // Response EventDTO
        eventDTO = new EventDTO();
        eventDTO.setEventCode("ABC123");
        eventDTO.setEventName("Test Event");
        eventDTO.setDescription("Test Description");
        eventDTO.setPlace("Test Location");
        eventDTO.setDate(LocalDateTime.now().plusDays(7));
        eventDTO.setStatus("ACTIVO");

        // DTO for create/update event
        createEventDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
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
     * Verifies that events are retrieved correctly with pagination and all
     * parameters.
     */
    @Test
    @DisplayName("Get Events - Successfully retrieve events with pagination")
    @WithMockUser
    void testGetEvents_Success() throws Exception {
        when(eventService.getEvents(anyInt(), anyInt(), anyString(), anyString(), anyString(), anyInt(),
                anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events")
                .param("page", "0")
                .param("userId", "1")
                .param("role", "HOST")
                .param("pageSize", "10")
                .param("sortBy", "eventDate")
                .param("sortDir", "ASC")
                .param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10));

        verify(eventService, times(1)).getEvents(anyInt(), anyInt(), anyString(), anyString(), anyString(),
                anyInt(), anyString());
    }

    /**
     * Verifies that events are retrieved when only the minimum required parameters
     * are provided.
     */
    @Test
    @DisplayName("Get Events - Minimum required parameters")
    @WithMockUser
    void testGetEvents_MinimalParams() throws Exception {
        when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(),
                nullable(String.class), anyInt(), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events")
                .param("page", "0")
                .param("userId", "1")
                .param("role", "GUEST"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(),
                nullable(String.class), anyInt(), anyString());
    }

    /**
     * Verifies the behavior of event queries when a negative page is provided.
     */
    @Test
    @DisplayName("Get Events - Invalid page (negative)")
    @WithMockUser
    void testGetEvents_InvalidPage() throws Exception {
        mockMvc.perform(get("/api/events")
                .param("page", "-1")
                .param("userId", "1")
                .param("role", "HOST"))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that the event query fails when userId is not provided.
     */
    @Test
    @DisplayName("Get Events - Missing userId")
    @WithMockUser
    void testGetEvents_MissingUserId() throws Exception {
        mockMvc.perform(get("/api/events")
                .param("page", "0")
                .param("role", "HOST"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that the event query fails when an invalid role is provided.
     */
    @Test
    @DisplayName("Get Events - Invalid role")
    @WithMockUser
    void testGetEvents_InvalidRole() throws Exception {
        when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(),
                nullable(String.class), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Invalid role"));

        mockMvc.perform(get("/api/events")
                .param("page", "0")
                .param("userId", "1")
                .param("role", "INVALID_ROLE"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that searching events by name works correctly.
     */
    @Test
    @DisplayName("Get Events - With name search")
    @WithMockUser
    void testGetEvents_WithSearch() throws Exception {
        when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(), anyString(),
                anyInt(), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events")
                .param("page", "0")
                .param("userId", "1")
                .param("role", "HOST")
                .param("search", "Birthday"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(),
                eq("Birthday"), anyInt(), anyString());
    }

    /**
     * Verifies that event queries work correctly with descending sort order.
     */
    @Test
    @DisplayName("Get Events - Sort descending")
    @WithMockUser
    void testGetEvents_SortDescending() throws Exception {
        when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(),
                nullable(String.class), anyInt(), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events")
                .param("page", "0")
                .param("userId", "1")
                .param("role", "HOST")
                .param("sortBy", "name")
                .param("sortDir", "DESC"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEvents(anyInt(), nullable(Integer.class), eq("name"), eq("DESC"),
                nullable(String.class), anyInt(), anyString());
    }

    /**
     * Verifies that an event is retrieved correctly by its code.
     */
    @Test
    @DisplayName("Get Event By Code - Success")
    @WithMockUser
    void testGetEventByCode_Success() throws Exception {
        when(eventService.getEvent("ABC123")).thenReturn(eventDTO);

        mockMvc.perform(get("/api/events/{eventCode}", "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("ABC123"))
                .andExpect(jsonPath("$.eventName").value("Test Event"));

        verify(eventService, times(1)).getEvent("ABC123");
    }

    /**
     * Verifies that lookup by code fails when the event does not exist.
     */
    @Test
    @DisplayName("Get Event By Code - Event not found")
    @WithMockUser
    void testGetEventByCode_NotFound() throws Exception {
        when(eventService.getEvent("NOTFOUND")).thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}", "NOTFOUND"))
                .andExpect(status().is5xxServerError());

        verify(eventService, times(1)).getEvent("NOTFOUND");
    }

    /**
     * Verifies that lookup by code fails when the code format is incorrect.
     */
    @Test
    @DisplayName("Get Event By Code - Invalid code (incorrect format)")
    @WithMockUser
    void testGetEventByCode_InvalidFormat() throws Exception {
        when(eventService.getEvent("abc")).thenThrow(new RuntimeException("Invalid event code format"));

        mockMvc.perform(get("/api/events/{eventCode}", "abc"))
                .andExpect(status().is5xxServerError());

        verify(eventService, times(1)).getEvent("abc");
    }

    /**
     * Verifies that lookup by code fails when a null code is provided.
     */
    @Test
    @DisplayName("Get Event By Code - Null code")
    @WithMockUser
    void testGetEventByCode_NullCode() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}", (Object) null))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that event creation works correctly with valid data.
     */
    @Test
    @DisplayName("Create Event - Successful creation")
    @WithMockUser
    void testCreateEvent_Success() throws Exception {
        when(eventService.createEvent(eq(1), any(CreateUpdateEventDTO.class))).thenReturn(eventDTO);

        mockMvc.perform(post("/api/events")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createEventDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventCode").value("ABC123"))
                .andExpect(jsonPath("$.eventName").value("Test Event"));

        verify(eventService, times(1)).createEvent(eq(1), any(CreateUpdateEventDTO.class));
    }

    /**
     * Verifies that creation is rejected when the event name exceeds 100
     * characters.
     */
    @Test
    @DisplayName("Create Event - Name too long")
    @WithMockUser
    void testCreateEvent_NameTooLong() throws Exception {
        CreateUpdateEventDTO invalidDTO = CreateUpdateEventDTO.builder()
                .eventName("A".repeat(105)) // > 100 characters
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        mockMvc.perform(post("/api/events")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that creation is rejected when the description exceeds 500
     * characters.
     */
    @Test
    @DisplayName("Create Event - Description too long")
    @WithMockUser
    void testCreateEvent_DescriptionTooLong() throws Exception {
        CreateUpdateEventDTO invalidDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("A".repeat(505)) // > 500 characters
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        mockMvc.perform(post("/api/events")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that creation fails when a date in the past is provided.
     */
    @Test
    @DisplayName("Create Event - Date in the past")
    @WithMockUser
    void testCreateEvent_PastDate() throws Exception {
        CreateUpdateEventDTO pastDateDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().minusDays(1)) // Past date
                .status("ACTIVO")
                .build();

        when(eventService.createEvent(eq(1), any(CreateUpdateEventDTO.class)))
                .thenThrow(new RuntimeException("Event date cannot be in the past"));

        mockMvc.perform(post("/api/events")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pastDateDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies creation behavior when an invalid maximum capacity is provided.
     */
    @Test
    @DisplayName("Create Event - Invalid max capacity (negative)")
    @WithMockUser
    void testCreateEvent_InvalidMaxCapacity() throws Exception {
        CreateUpdateEventDTO invalidCapacityDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        mockMvc.perform(post("/api/events")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCapacityDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifies that creation fails when the maximum capacity is zero.
     */
    @Test
    @DisplayName("Create Event - Max capacity zero")
    @WithMockUser
    void testCreateEvent_ZeroMaxCapacity() throws Exception {
        CreateUpdateEventDTO zeroCapacityDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        when(eventService.createEvent(eq(1), any(CreateUpdateEventDTO.class)))
                .thenThrow(new RuntimeException("Max capacity must be positive"));

        mockMvc.perform(post("/api/events")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zeroCapacityDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies creation behavior when required fields are null.
     */
    @Test
    @DisplayName("Create Event - Required fields are null")
    @WithMockUser
    void testCreateEvent_RequiredFieldsNull() throws Exception {
        CreateUpdateEventDTO nullFieldsDTO = CreateUpdateEventDTO.builder()
                .eventName(null)
                .description(null)
                .place(null)
                .date(null)
                .status(null)
                .build();

        mockMvc.perform(post("/api/events")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullFieldsDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifies that creation fails when the associated user does not exist.
     */
    @Test
    @DisplayName("Create Event - User not found")
    @WithMockUser
    void testCreateEvent_UserNotFound() throws Exception {
        when(eventService.createEvent(eq(999), any(CreateUpdateEventDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/events")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createEventDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that updating an event works correctly with valid data.
     */
    @Test
    @DisplayName("Update Event - Successful update")
    @WithMockUser
    void testUpdateEvent_Success() throws Exception {
        EventDTO updatedEvent = new EventDTO();
        updatedEvent.setEventCode("ABC123");
        updatedEvent.setEventName("Updated Event");
        updatedEvent.setDescription("Updated Description");

        when(eventService.updateEvent(eq("ABC123"), any(CreateUpdateEventDTO.class))).thenReturn(updatedEvent);

        mockMvc.perform(put("/api/events/{eventCode}", "ABC123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createEventDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("ABC123"));

        verify(eventService, times(1)).updateEvent(eq("ABC123"), any(CreateUpdateEventDTO.class));
    }

    /**
     * Verifies that update fails when the event does not exist.
     */
    @Test
    @DisplayName("Update Event - Event not found")
    @WithMockUser
    void testUpdateEvent_NotFound() throws Exception {
        when(eventService.updateEvent(eq("NOTFOUND"), any(CreateUpdateEventDTO.class)))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(put("/api/events/{eventCode}", "NOTFOUND")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createEventDTO)))
                .andExpect(status().is5xxServerError());

        verify(eventService, times(1)).updateEvent(eq("NOTFOUND"), any(CreateUpdateEventDTO.class));
    }

    /**
     * Verifies that update is rejected when the provided data is invalid.
     */
    @Test
    @DisplayName("Update Event - Invalid data")
    @WithMockUser
    void testUpdateEvent_InvalidData() throws Exception {
        CreateUpdateEventDTO invalidDTO = CreateUpdateEventDTO.builder()
                .eventName("A".repeat(105)) // Name too long
                .description("Test")
                .place("Test")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        mockMvc.perform(put("/api/events/{eventCode}", "ABC123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that update fails when reducing capacity below current attendees.
     */
    @Test
    @DisplayName("Update Event - Reduce capacity below current attendees")
    @WithMockUser
    void testUpdateEvent_CapacityBelowCurrentAttendees() throws Exception {
        CreateUpdateEventDTO reducedCapacityDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        when(eventService.updateEvent(eq("ABC123"), any(CreateUpdateEventDTO.class)))
                .thenThrow(new RuntimeException("Cannot reduce capacity below current attendees"));

        mockMvc.perform(put("/api/events/{eventCode}", "ABC123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reducedCapacityDTO)))
                .andExpect(status().is5xxServerError());
    }
}
