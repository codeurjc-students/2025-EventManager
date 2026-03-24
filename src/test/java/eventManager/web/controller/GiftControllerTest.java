package eventManager.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventManager.dto.*;
import eventManager.security.jwt.JwtTokenProvider;
import eventManager.service.GiftService;
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

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del controlador de regalos. Cubre creacion, obtencion, listado, actualizacion, eliminacion y contribuciones.
 */
@WebMvcTest(GiftController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GiftController Tests")
class GiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private GiftService giftService;

        @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

        @MockitoBean
    private UserDetailsService userDetailsService;

    private GiftDTO giftDTO;
    private GiftExtendedDTO giftExtendedDTO;
    private GiftCreateDTO giftCreateDTO;
    private GiftUpdateDTO giftUpdateDTO;
    private UserGiftDTO userGiftDTO;
    private ResultPaginationDTO paginationDTO;

    @BeforeEach
    void setUp() {
        // GiftDTO básico
        giftDTO = new GiftDTO();
        giftDTO.setGiftId(1);
        giftDTO.setName("Juego de mesa cooperativo");
        giftDTO.setPrice(100.00);
        giftDTO.setCollected(50.00);

        // GiftExtendedDTO con información adicional
        giftExtendedDTO = new GiftExtendedDTO();
        giftExtendedDTO.setGiftId(1);
        giftExtendedDTO.setName("Juego de mesa cooperativo");
        giftExtendedDTO.setDetails("Regalo para la quedada de fin de semana");
        giftExtendedDTO.setPrice(100.00);
        giftExtendedDTO.setCollected(50.00);
        giftExtendedDTO.setUserContributionList(new ArrayList<>());

        // DTO para crear regalo
        giftCreateDTO = GiftCreateDTO.builder()
                .name("Juego de mesa cooperativo")
                .details("Regalo para la quedada de fin de semana")
                .price(100.00)
                .url("https://tienda-regalos.es/productos/juego-mesa-cooperativo")
                .image("https://imagenes.tienda-regalos.es/juego-mesa-cooperativo.jpg")
                .build();

        // DTO para actualizar regalo
        giftUpdateDTO = GiftUpdateDTO.builder()
                .name("Juego de mesa premium")
                .details("Edicion coleccionista para regalo grupal")
                .price(150.00)
                .url("https://tienda-regalos.es/productos/juego-mesa-premium")
                .image("https://imagenes.tienda-regalos.es/juego-mesa-premium.jpg")
                .build();

        // DTO para contribución
        userGiftDTO = UserGiftDTO.builder()
                .userId(1)
                .amount(25.00)
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
     * Verifica que la creacion de un regalo funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Create Gift - Creación exitosa")
    @WithMockUser
    void testCreateGift_Success() throws Exception {
        when(giftService.createGift(eq("MADRID"), any(GiftCreateDTO.class)))
                .thenReturn(giftDTO);

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "MADRID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(giftCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.giftId").value(1))
                .andExpect(jsonPath("$.name").value("Juego de mesa cooperativo"));

        verify(giftService, times(1)).createGift(eq("MADRID"), any(GiftCreateDTO.class));
    }

    /**
     * Verifica que la creacion de un regalo falla cuando el evento no existe.
     */
    @Test
    @DisplayName("Create Gift - Evento no encontrado")
    @WithMockUser
    void testCreateGift_EventNotFound() throws Exception {
        when(giftService.createGift(eq("NOTFOUND"), any(GiftCreateDTO.class)))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "NOTFOUND")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(giftCreateDTO)))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).createGift(eq("NOTFOUND"), any(GiftCreateDTO.class));
    }

    /**
     * Verifica que la creacion se rechaza cuando el nombre del regalo excede los 100 caracteres.
     */
    @Test
    @DisplayName("Create Gift - Nombre demasiado largo")
    @WithMockUser
    void testCreateGift_NameTooLong() throws Exception {
        GiftCreateDTO invalidDTO = GiftCreateDTO.builder()
                .name("A".repeat(105))  // > 100 caracteres
                .details("Regalo")
                .price(100.00)
                .url("https://tienda-regalos.es")
                .build();

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "MADRID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica el comportamiento de la creacion cuando se proporciona un precio objetivo negativo.
     */
    @Test
    @DisplayName("Create Gift - Precio objetivo negativo")
    @WithMockUser
    void testCreateGift_NegativeTargetPrice() throws Exception {
        GiftCreateDTO invalidDTO = GiftCreateDTO.builder()
                .name("Juego de mesa cooperativo")
                .details("Regalo para evento familiar")
                .price(-50.00)  // Precio negativo
                .url("https://tienda-regalos.es")
                .build();

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "MADRID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifica que la creacion falla cuando el precio objetivo es cero.
     */
    @Test
    @DisplayName("Create Gift - Precio objetivo cero")
    @WithMockUser
    void testCreateGift_ZeroTargetPrice() throws Exception {
        GiftCreateDTO zeroDTO = GiftCreateDTO.builder()
                .name("Juego de mesa cooperativo")
                .details("Regalo para evento familiar")
                .price(0.00)
                .url("https://tienda-regalos.es")
                .build();

        when(giftService.createGift(eq("MADRID"), any(GiftCreateDTO.class)))
                .thenThrow(new RuntimeException("Target price must be positive"));

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "MADRID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica el comportamiento de la creacion cuando los campos obligatorios son null.
     */
    @Test
    @DisplayName("Create Gift - Campos obligatorios null")
    @WithMockUser
    void testCreateGift_RequiredFieldsNull() throws Exception {
        GiftCreateDTO nullDTO = GiftCreateDTO.builder()
                .name(null)
                .details(null)
                .price(null)
                .build();

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "ABC123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifica el comportamiento de la creacion cuando se proporciona una URL mal formada.
     */
    @Test
    @DisplayName("Create Gift - URL inválida")
    @WithMockUser
    void testCreateGift_InvalidUrl() throws Exception {
        GiftCreateDTO invalidUrlDTO = GiftCreateDTO.builder()
                .name("Test Gift")
                .details("Test Description")
                .price(100.00)
                .url("invalid-url")  // URL mal formada
                .build();

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "ABC123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUrlDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifica que se obtiene la informacion de un regalo correctamente.
     */
    @Test
    @DisplayName("Get Gift - Obtener regalo exitosamente")
    @WithMockUser
    void testGetGift_Success() throws Exception {
        when(giftService.getGiftInformation("ABC123", 1))
                .thenReturn(giftExtendedDTO);

        mockMvc.perform(get("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giftId").value(1))
                .andExpect(jsonPath("$.name").value("Juego de mesa cooperativo"));

        verify(giftService, times(1)).getGiftInformation("ABC123", 1);
    }

    /**
     * Verifica que la consulta de un regalo falla cuando no existe.
     */
    @Test
    @DisplayName("Get Gift - Regalo no encontrado")
    @WithMockUser
    void testGetGift_NotFound() throws Exception {
        when(giftService.getGiftInformation("ABC123", 999))
                .thenThrow(new RuntimeException("Gift not found"));

        mockMvc.perform(get("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 999))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).getGiftInformation("ABC123", 999);
    }

    /**
     * Verifica que la consulta de un regalo falla cuando el evento asociado no existe.
     */
    @Test
    @DisplayName("Get Gift - Evento no encontrado")
    @WithMockUser
    void testGetGift_EventNotFound() throws Exception {
        when(giftService.getGiftInformation("NOTFOUND", 1))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}/gifts/{giftId}", "NOTFOUND", 1))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).getGiftInformation("NOTFOUND", 1);
    }

    /**
     * Verifica que se listan los regalos de un evento correctamente con paginacion.
     */
    @Test
    @DisplayName("Get Gifts - Listar regalos exitosamente")
    @WithMockUser
    void testGetGifts_Success() throws Exception {
        when(giftService.getGifts(eq("ABC123"), anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/gifts", "ABC123")
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("sortBy", "name")
                        .param("sortDir", "ASC")
                        .param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10));

        verify(giftService, times(1)).getGifts(eq("ABC123"), anyInt(), anyInt(), anyString(), anyString(), anyString());
    }

    /**
     * Verifica que la busqueda de regalos por nombre funciona correctamente.
     */
    @Test
    @DisplayName("Get Gifts - Con búsqueda por nombre")
    @WithMockUser
    void testGetGifts_WithSearch() throws Exception {
                when(giftService.getGifts(eq("ABC123"), anyInt(), nullable(Integer.class), anyString(), anyString(), eq("Laptop")))
                .thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/gifts", "ABC123")
                        .param("page", "0")
                        .param("search", "Laptop"))
                .andExpect(status().isOk());

        verify(giftService, times(1)).getGifts(eq("ABC123"), anyInt(), nullable(Integer.class), anyString(), anyString(), eq("Laptop"));
    }

    /**
     * Verifica el comportamiento del listado de regalos cuando se proporciona una pagina invalida.
     */
    @Test
    @DisplayName("Get Gifts - Página inválida")
    @WithMockUser
    void testGetGifts_InvalidPage() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}/gifts", "ABC123")
                        .param("page", "-1"))
                                .andExpect(status().isOk());
    }

    /**
     * Verifica que la actualizacion de un regalo funciona correctamente con datos validos.
     */
    @Test
    @DisplayName("Update Gift - Actualización exitosa")
    @WithMockUser
    void testUpdateGift_Success() throws Exception {
        when(giftService.updateGift(eq("ABC123"), eq(1), any(GiftUpdateDTO.class)))
                .thenReturn(giftExtendedDTO);

        mockMvc.perform(put("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(giftUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giftId").value(1));

        verify(giftService, times(1)).updateGift(eq("ABC123"), eq(1), any(GiftUpdateDTO.class));
    }

    /**
     * Verifica que la actualizacion falla cuando el regalo no existe.
     */
    @Test
    @DisplayName("Update Gift - Regalo no encontrado")
    @WithMockUser
    void testUpdateGift_NotFound() throws Exception {
        when(giftService.updateGift(eq("ABC123"), eq(999), any(GiftUpdateDTO.class)))
                .thenThrow(new RuntimeException("Gift not found"));

        mockMvc.perform(put("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(giftUpdateDTO)))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).updateGift(eq("ABC123"), eq(999), any(GiftUpdateDTO.class));
    }

    /**
     * Verifica que la actualizacion se rechaza cuando los datos proporcionados son invalidos.
     */
    @Test
    @DisplayName("Update Gift - Datos inválidos")
    @WithMockUser
    void testUpdateGift_InvalidData() throws Exception {
        GiftUpdateDTO invalidDTO = GiftUpdateDTO.builder()
                .name("A".repeat(105))  // Demasiado largo
                .details("Test")
                .price(100.00)
                .build();

        mockMvc.perform(put("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que la eliminacion de un regalo funciona correctamente.
     */
    @Test
    @DisplayName("Delete Gift - Eliminación exitosa")
    @WithMockUser
    void testDeleteGift_Success() throws Exception {
        when(giftService.deleteGift("ABC123", 1))
                .thenReturn(giftDTO);

        mockMvc.perform(delete("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giftId").value(1));

        verify(giftService, times(1)).deleteGift("ABC123", 1);
    }

    /**
     * Verifica que la eliminacion falla cuando el regalo no existe.
     */
    @Test
    @DisplayName("Delete Gift - Regalo no encontrado")
    @WithMockUser
    void testDeleteGift_NotFound() throws Exception {
        when(giftService.deleteGift("ABC123", 999))
                .thenThrow(new RuntimeException("Gift not found"));

        mockMvc.perform(delete("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 999))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).deleteGift("ABC123", 999);
    }

    /**
     * Verifica que la eliminacion falla cuando el regalo tiene contribuciones asociadas.
     */
    @Test
    @DisplayName("Delete Gift - Regalo con contribuciones")
    @WithMockUser
    void testDeleteGift_WithContributions() throws Exception {
        when(giftService.deleteGift("ABC123", 1))
                .thenThrow(new RuntimeException("Cannot delete gift with existing contributions"));

        mockMvc.perform(delete("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).deleteGift("ABC123", 1);
    }

    /**
     * Verifica que la creacion de una contribucion a un regalo funciona correctamente.
     */
    @Test
    @DisplayName("Create Contribution - Contribución exitosa")
    @WithMockUser
    void testCreateContribution_Success() throws Exception {
        when(giftService.createUpdateGiftContribution(eq("ABC123"), eq(1), any(UserGiftDTO.class)))
                .thenReturn(giftExtendedDTO);

        mockMvc.perform(post("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userGiftDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.giftId").value(1));

        verify(giftService, times(1)).createUpdateGiftContribution(eq("ABC123"), eq(1), any(UserGiftDTO.class));
    }

    /**
     * Verifica el comportamiento de la contribucion cuando el monto es negativo.
     */
    @Test
    @DisplayName("Create Contribution - Monto negativo")
    @WithMockUser
    void testCreateContribution_NegativeAmount() throws Exception {
        UserGiftDTO negativeDTO = UserGiftDTO.builder()
                .userId(1)
                .amount(-10.00)
                .build();

        mockMvc.perform(post("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifica que la contribucion falla cuando el monto excede el precio objetivo del regalo.
     */
    @Test
    @DisplayName("Create Contribution - Monto excede precio objetivo")
    @WithMockUser
    void testCreateContribution_ExceedsTargetPrice() throws Exception {
        UserGiftDTO excessDTO = UserGiftDTO.builder()
                .userId(1)
                .amount(200.00)  // > targetPrice
                .build();

        when(giftService.createUpdateGiftContribution(eq("ABC123"), eq(1), any(UserGiftDTO.class)))
                .thenThrow(new RuntimeException("Contribution exceeds remaining amount"));

        mockMvc.perform(post("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que la contribucion falla cuando el usuario no existe.
     */
    @Test
    @DisplayName("Create Contribution - Usuario no encontrado")
    @WithMockUser
    void testCreateContribution_UserNotFound() throws Exception {
        UserGiftDTO invalidUserDTO = UserGiftDTO.builder()
                .userId(999)
                .amount(25.00)
                .build();

        when(giftService.createUpdateGiftContribution(eq("ABC123"), eq(1), any(UserGiftDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifica que la contribucion falla cuando el monto es cero.
     */
    @Test
    @DisplayName("Create Contribution - Monto cero")
    @WithMockUser
    void testCreateContribution_ZeroAmount() throws Exception {
        UserGiftDTO zeroDTO = UserGiftDTO.builder()
                .userId(1)
                .amount(0.00)
                .build();

        when(giftService.createUpdateGiftContribution(eq("ABC123"), eq(1), any(UserGiftDTO.class)))
                .thenThrow(new RuntimeException("Contribution amount must be positive"));

        mockMvc.perform(post("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroDTO)))
                .andExpect(status().is5xxServerError());
    }
}
