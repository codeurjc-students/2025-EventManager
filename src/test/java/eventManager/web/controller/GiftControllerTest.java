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
 * Unit tests for the gift controller. Covers creation, retrieval, listing,
 * updates, deletion, and contributions.
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
        // Basic GiftDTO
        giftDTO = new GiftDTO();
        giftDTO.setGiftId(1);
        giftDTO.setName("Juego de mesa cooperativo");
        giftDTO.setPrice(100.00);
        giftDTO.setCollected(50.00);

        // GiftExtendedDTO with additional information
        giftExtendedDTO = new GiftExtendedDTO();
        giftExtendedDTO.setGiftId(1);
        giftExtendedDTO.setName("Juego de mesa cooperativo");
        giftExtendedDTO.setDetails("Regalo para la quedada de fin de semana");
        giftExtendedDTO.setPrice(100.00);
        giftExtendedDTO.setCollected(50.00);
        giftExtendedDTO.setUserContributionList(new ArrayList<>());

        // DTO for gift creation
        giftCreateDTO = GiftCreateDTO.builder()
                .name("Juego de mesa cooperativo")
                .details("Regalo para la quedada de fin de semana")
                .price(100.00)
                .url("https://tienda-regalos.es/productos/juego-mesa-cooperativo")
                .image("https://imagenes.tienda-regalos.es/juego-mesa-cooperativo.jpg")
                .build();

        // DTO for gift update
        giftUpdateDTO = GiftUpdateDTO.builder()
                .name("Juego de mesa premium")
                .details("Edicion coleccionista para regalo grupal")
                .price(150.00)
                .url("https://tienda-regalos.es/productos/juego-mesa-premium")
                .image("https://imagenes.tienda-regalos.es/juego-mesa-premium.jpg")
                .build();

        // DTO for contribution
        userGiftDTO = UserGiftDTO.builder()
                .userId(1)
                .amount(25.00)
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
     * Verifies that gift creation works correctly with valid data.
     */
    @Test
    @DisplayName("Create Gift - Successful creation")
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
     * Verifies that gift creation fails when the event does not exist.
     */
    @Test
    @DisplayName("Create Gift - Event not found")
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
     * Verifies that creation is rejected when the gift name exceeds 100 characters.
     */
    @Test
    @DisplayName("Create Gift - Name too long")
    @WithMockUser
    void testCreateGift_NameTooLong() throws Exception {
        GiftCreateDTO invalidDTO = GiftCreateDTO.builder()
                .name("A".repeat(105)) // > 100 characters
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
     * Verifies creation behavior when a negative target price is provided.
     */
    @Test
    @DisplayName("Create Gift - Negative target price")
    @WithMockUser
    void testCreateGift_NegativeTargetPrice() throws Exception {
        GiftCreateDTO invalidDTO = GiftCreateDTO.builder()
                .name("Juego de mesa cooperativo")
                .details("Regalo para evento familiar")
                .price(-50.00) // Negative price
                .url("https://tienda-regalos.es")
                .build();

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "MADRID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifies that creation fails when the target price is zero.
     */
    @Test
    @DisplayName("Create Gift - Target price zero")
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
     * Verifies creation behavior when required fields are null.
     */
    @Test
    @DisplayName("Create Gift - Required fields are null")
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
     * Verifies creation behavior when a malformed URL is provided.
     */
    @Test
    @DisplayName("Create Gift - Invalid URL")
    @WithMockUser
    void testCreateGift_InvalidUrl() throws Exception {
        GiftCreateDTO invalidUrlDTO = GiftCreateDTO.builder()
                .name("Test Gift")
                .details("Test Description")
                .price(100.00)
                .url("invalid-url") // Malformed URL
                .build();

        mockMvc.perform(post("/api/events/{eventCode}/gifts", "ABC123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUrlDTO)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifies that gift information is retrieved correctly.
     */
    @Test
    @DisplayName("Get Gift - Successfully retrieve gift")
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
     * Verifies that retrieving a gift fails when it does not exist.
     */
    @Test
    @DisplayName("Get Gift - Gift not found")
    @WithMockUser
    void testGetGift_NotFound() throws Exception {
        when(giftService.getGiftInformation("ABC123", 999))
                .thenThrow(new RuntimeException("Gift not found"));

        mockMvc.perform(get("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 999))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).getGiftInformation("ABC123", 999);
    }

    /**
     * Verifies that retrieving a gift fails when the associated event does not
     * exist.
     */
    @Test
    @DisplayName("Get Gift - Event not found")
    @WithMockUser
    void testGetGift_EventNotFound() throws Exception {
        when(giftService.getGiftInformation("NOTFOUND", 1))
                .thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/events/{eventCode}/gifts/{giftId}", "NOTFOUND", 1))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).getGiftInformation("NOTFOUND", 1);
    }

    /**
     * Verifies that gifts are listed correctly for an event with pagination.
     */
    @Test
    @DisplayName("Get Gifts - Successfully list gifts")
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

        verify(giftService, times(1)).getGifts(eq("ABC123"), anyInt(), anyInt(), anyString(), anyString(),
                anyString());
    }

    /**
     * Verifies that searching gifts by name works correctly.
     */
    @Test
    @DisplayName("Get Gifts - With name search")
    @WithMockUser
    void testGetGifts_WithSearch() throws Exception {
        when(giftService.getGifts(eq("ABC123"), anyInt(), nullable(Integer.class), anyString(), anyString(),
                eq("Laptop")))
                .thenReturn(paginationDTO);

        mockMvc.perform(get("/api/events/{eventCode}/gifts", "ABC123")
                .param("page", "0")
                .param("search", "Laptop"))
                .andExpect(status().isOk());

        verify(giftService, times(1)).getGifts(eq("ABC123"), anyInt(), nullable(Integer.class), anyString(),
                anyString(), eq("Laptop"));
    }

    /**
     * Verifies gift listing behavior when an invalid page is provided.
     */
    @Test
    @DisplayName("Get Gifts - Invalid page")
    @WithMockUser
    void testGetGifts_InvalidPage() throws Exception {
        mockMvc.perform(get("/api/events/{eventCode}/gifts", "ABC123")
                .param("page", "-1"))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that a gift update works correctly with valid data.
     */
    @Test
    @DisplayName("Update Gift - Successful update")
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
     * Verifies that update fails when the gift does not exist.
     */
    @Test
    @DisplayName("Update Gift - Gift not found")
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
     * Verifies that update is rejected when the provided data is invalid.
     */
    @Test
    @DisplayName("Update Gift - Invalid data")
    @WithMockUser
    void testUpdateGift_InvalidData() throws Exception {
        GiftUpdateDTO invalidDTO = GiftUpdateDTO.builder()
                .name("A".repeat(105)) // Too long
                .details("Test")
                .price(100.00)
                .build();

        mockMvc.perform(put("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that deleting a gift works correctly.
     */
    @Test
    @DisplayName("Delete Gift - Successful deletion")
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
     * Verifies that deletion fails when the gift does not exist.
     */
    @Test
    @DisplayName("Delete Gift - Gift not found")
    @WithMockUser
    void testDeleteGift_NotFound() throws Exception {
        when(giftService.deleteGift("ABC123", 999))
                .thenThrow(new RuntimeException("Gift not found"));

        mockMvc.perform(delete("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 999))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).deleteGift("ABC123", 999);
    }

    /**
     * Verifies that deletion fails when the gift has associated contributions.
     */
    @Test
    @DisplayName("Delete Gift - Gift with contributions")
    @WithMockUser
    void testDeleteGift_WithContributions() throws Exception {
        when(giftService.deleteGift("ABC123", 1))
                .thenThrow(new RuntimeException("Cannot delete gift with existing contributions"));

        mockMvc.perform(delete("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1))
                .andExpect(status().is5xxServerError());

        verify(giftService, times(1)).deleteGift("ABC123", 1);
    }

    /**
     * Verifies that creating a contribution to a gift works correctly.
     */
    @Test
    @DisplayName("Create Contribution - Successful contribution")
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
     * Verifies contribution behavior when the amount is negative.
     */
    @Test
    @DisplayName("Create Contribution - Negative amount")
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
     * Verifies that the contribution fails when the amount exceeds the gift target
     * price.
     */
    @Test
    @DisplayName("Create Contribution - Amount exceeds target price")
    @WithMockUser
    void testCreateContribution_ExceedsTargetPrice() throws Exception {
        UserGiftDTO excessDTO = UserGiftDTO.builder()
                .userId(1)
                .amount(200.00) // > targetPrice
                .build();

        when(giftService.createUpdateGiftContribution(eq("ABC123"), eq(1), any(UserGiftDTO.class)))
                .thenThrow(new RuntimeException("Contribution exceeds remaining amount"));

        mockMvc.perform(post("/api/events/{eventCode}/gifts/{giftId}", "ABC123", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(excessDTO)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Verifies that the contribution fails when the user does not exist.
     */
    @Test
    @DisplayName("Create Contribution - User not found")
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
     * Verifies that the contribution fails when the amount is zero.
     */
    @Test
    @DisplayName("Create Contribution - Zero amount")
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
