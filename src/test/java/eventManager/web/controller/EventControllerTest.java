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
 * Pruebas unitarias del controlador de eventos. Cubre obtencion de eventos, creacion, actualizacion y consulta por codigo.
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
        // EventDTO de respuesta
        eventDTO = new EventDTO();
        eventDTO.setEventCode("ABC123");
        eventDTO.setEventName("Test Event");
        eventDTO.setDescription("Test Description");
        eventDTO.setPlace("Test Location");
        eventDTO.setDate(LocalDateTime.now().plusDays(7));
        eventDTO.setStatus("ACTIVO");

        // DTO para crear/actualizar evento
        createEventDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
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
     * Verifica que se obtienen los eventos correctamente con paginacion y todos los parametros.
     */
    @Test
    @DisplayName("Get Events - Obtener eventos exitosamente con paginación")
    @WithMockUser
    void testGetEvents_Success() throws Exception {
        when(eventService.getEvents(anyInt(), anyInt(), anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(paginationDTO);

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

        verify(eventService, times(1)).getEvents(anyInt(), anyInt(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    /**
     * Verifica que se obtienen los eventos proporcionando solo los parametros minimos requeridos.
     */
    @Test
    @DisplayName("Get Events - Parámetros mínimos requeridos")
    @WithMockUser
    void testGetEvents_MinimalParams() throws Exception {
                when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(), nullable(String.class), anyInt(), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("userId", "1")
                        .param("role", "GUEST"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(), nullable(String.class), anyInt(), anyString());
    }

    /**
     * Verifica el comportamiento de la consulta de eventos cuando se proporciona una pagina negativa.
     */
    @Test
    @DisplayName("Get Events - Página inválida (negativa)")
    @WithMockUser
    void testGetEvents_InvalidPage() throws Exception {
        mockMvc.perform(get("/api/events")
                        .param("page", "-1")
                        .param("userId", "1")
                        .param("role", "HOST"))
                                .andExpect(status().isOk());
    }

    /**
     * Verifica que la consulta de eventos falla cuando no se proporciona el userId.
     */
    @Test
    @DisplayName("Get Events - UserId faltante")
    @WithMockUser
    void testGetEvents_MissingUserId() throws Exception {
        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("role", "HOST"))
                                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que la consulta de eventos falla cuando se proporciona un rol invalido.
     */
    @Test
    @DisplayName("Get Events - Role inválido")
    @WithMockUser
    void testGetEvents_InvalidRole() throws Exception {
                when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(), nullable(String.class), anyInt(), anyString())).thenThrow(new RuntimeException("Invalid role"));

        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("userId", "1")
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que la busqueda de eventos por nombre funciona correctamente.
     */
    @Test
    @DisplayName("Get Events - Con búsqueda por nombre")
    @WithMockUser
    void testGetEvents_WithSearch() throws Exception {
                when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("userId", "1")
                        .param("role", "HOST")
                        .param("search", "Birthday"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(), eq("Birthday"), anyInt(), anyString());
    }

    /**
     * Verifica que la consulta de eventos funciona correctamente con ordenacion descendente.
     */
    @Test
    @DisplayName("Get Events - Ordenar descendente")
    @WithMockUser
    void testGetEvents_SortDescending() throws Exception {
                when(eventService.getEvents(anyInt(), nullable(Integer.class), anyString(), anyString(), nullable(String.class), anyInt(), anyString())).thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("userId", "1")
                        .param("role", "HOST")
                        .param("sortBy", "name")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEvents(anyInt(), nullable(Integer.class), eq("name"), eq("DESC"), nullable(String.class), anyInt(), anyString());
    }

    /**
     * Verifica que se obtiene un evento correctamente a partir de su codigo.
     */
    @Test
    @DisplayName("Get Event By Code - Exitoso")
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
     * Verifica que la consulta por codigo falla cuando el evento no existe.
     */
    @Test
    @DisplayName("Get Event By Code - Evento no encontrado")
    @WithMockUser
    void testGetEventByCode_NotFound() throws Exception {
        when(eventService.getEvent("NOTFOUND")).thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}", "NOTFOUND"))
                .andExpect(status().is5xxServerError());

        verify(eventService, times(1)).getEvent("NOTFOUND");
    }

    /**
     * Verifica que la consulta por codigo falla cuando el formato del codigo es incorrecto.
     */
    @Test
    @DisplayName("Get Event By Code - Código inválido (formato incorrecto)")
    @WithMockUser
    void testGetEventByCode_InvalidFormat() throws Exception {
        when(eventService.getEvent("abc")).thenThrow(new RuntimeException("Invalid event code format"));

        mockMvc.perform(get("/api/events/{eventCode}", "abc"))
                .andExpect(status().is5xxServerError());

        verify(eventService, times(1)).getEvent("abc");
    }

    /**
     * Verifica que la consulta por codigo falla cuando se proporciona un codigo null.
     */
    @Test
    @DisplayName("Get Event By Code - Código null")
    @WithMockUser
    void testGetEventByCode_NullCode() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}", (Object) null))
                                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que la creacion de un evento funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Create Event - Creación exitosa")
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
     * Verifica que la creacion se rechaza cuando el nombre del evento excede los 100 caracteres.
     */
    @Test
    @DisplayName("Create Event - Nombre demasiado largo")
    @WithMockUser
    void testCreateEvent_NameTooLong() throws Exception {
        CreateUpdateEventDTO invalidDTO = CreateUpdateEventDTO.builder()
                .eventName("A".repeat(105))  // > 100 caracteres
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
     * Verifica que la creacion se rechaza cuando la descripcion excede los 500 caracteres.
     */
    @Test
    @DisplayName("Create Event - Descripción demasiado larga")
    @WithMockUser
    void testCreateEvent_DescriptionTooLong() throws Exception {
        CreateUpdateEventDTO invalidDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("A".repeat(505))  // > 500 caracteres
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
     * Verifica que la creacion falla cuando se proporciona una fecha en el pasado.
     */
    @Test
    @DisplayName("Create Event - Fecha en el pasado")
    @WithMockUser
    void testCreateEvent_PastDate() throws Exception {
        CreateUpdateEventDTO pastDateDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().minusDays(1))  // Fecha pasada
                .status("ACTIVO")
                .build();

        when(eventService.createEvent(eq(1), any(CreateUpdateEventDTO.class))).thenThrow(new RuntimeException("Event date cannot be in the past"));

        mockMvc.perform(post("/api/events")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastDateDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica el comportamiento de la creacion cuando se proporciona una capacidad maxima invalida.
     */
    @Test
    @DisplayName("Create Event - Capacidad máxima inválida (negativa)")
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
     * Verifica que la creacion falla cuando la capacidad maxima es cero.
     */
    @Test
    @DisplayName("Create Event - Capacidad máxima cero")
    @WithMockUser
    void testCreateEvent_ZeroMaxCapacity() throws Exception {
        CreateUpdateEventDTO zeroCapacityDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        when(eventService.createEvent(eq(1), any(CreateUpdateEventDTO.class))).thenThrow(new RuntimeException("Max capacity must be positive"));

        mockMvc.perform(post("/api/events")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroCapacityDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica el comportamiento de la creacion cuando los campos obligatorios son null.
     */
    @Test
    @DisplayName("Create Event - Campos obligatorios null")
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
     * Verifica que la creacion falla cuando el usuario asociado no existe.
     */
    @Test
    @DisplayName("Create Event - Usuario no encontrado")
    @WithMockUser
    void testCreateEvent_UserNotFound() throws Exception {
        when(eventService.createEvent(eq(999), any(CreateUpdateEventDTO.class))).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/events")
                        .param("userId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que la actualizacion de un evento funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Update Event - Actualización exitosa")
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
     * Verifica que la actualizacion falla cuando el evento no existe.
     */
    @Test
    @DisplayName("Update Event - Evento no encontrado")
    @WithMockUser
    void testUpdateEvent_NotFound() throws Exception {
        when(eventService.updateEvent(eq("NOTFOUND"), any(CreateUpdateEventDTO.class))).thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(put("/api/events/{eventCode}", "NOTFOUND")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDTO)))
                .andExpect(status().is5xxServerError());

        verify(eventService, times(1)).updateEvent(eq("NOTFOUND"), any(CreateUpdateEventDTO.class));
    }

    /**
     * Verifica que la actualizacion se rechaza cuando los datos proporcionados son invalidos.
     */
    @Test
    @DisplayName("Update Event - Datos inválidos")
    @WithMockUser
    void testUpdateEvent_InvalidData() throws Exception {
        CreateUpdateEventDTO invalidDTO = CreateUpdateEventDTO.builder()
                .eventName("A".repeat(105))  // Nombre demasiado largo
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
     * Verifica que la actualizacion falla cuando se reduce la capacidad por debajo de los asistentes actuales.
     */
    @Test
    @DisplayName("Update Event - Reducir capacidad por debajo de asistentes actuales")
    @WithMockUser
    void testUpdateEvent_CapacityBelowCurrentAttendees() throws Exception {
        CreateUpdateEventDTO reducedCapacityDTO = CreateUpdateEventDTO.builder()
                .eventName("Test Event")
                .description("Test Description")
                .place("Test Location")
                .date(LocalDateTime.now().plusDays(7))
                .status("ACTIVO")
                .build();

        when(eventService.updateEvent(eq("ABC123"), any(CreateUpdateEventDTO.class))).thenThrow(new RuntimeException("Cannot reduce capacity below current attendees"));

        mockMvc.perform(put("/api/events/{eventCode}", "ABC123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reducedCapacityDTO)))
                .andExpect(status().is5xxServerError());
    }
}
