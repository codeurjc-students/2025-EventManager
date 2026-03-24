package eventManager.service.impl;

import eventManager.constant.Constantes;
import eventManager.dto.*;
import eventManager.entity.*;
import eventManager.exception.CustomException;
import eventManager.mapper.GiftContributionMapper;
import eventManager.mapper.GiftMapper;
import eventManager.repository.GiftContributionRepository;
import eventManager.repository.GiftRepository;
import eventManager.search.GiftSpecificationsBuilder;
import eventManager.service.EventService;
import eventManager.service.S3Service;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pruebas unitarias del servicio de regalos, incluyendo creacion, consulta, listado, actualizacion, eliminacion y gestion de contribuciones. */
@ExtendWith(MockitoExtension.class)
@DisplayName("GiftServiceImpl Tests")
class GiftServiceImplTest {

    @InjectMocks
    private GiftServiceImpl giftService;

    @Mock
    private GiftRepository giftRepository;

    @Mock
    private GiftContributionRepository giftContributionRepository;

    @Mock
    private GiftMapper giftMapper;

    @Mock
    private GiftContributionMapper giftContributionMapper;

    @Mock
    private GiftSpecificationsBuilder giftSpecificationBuilder;

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @Mock
    private TicketService ticketService;

    @Mock
    private S3Service s3Service;

    @Mock
    private AccessControlUtils accessControlUtils;

    private Event testEvent;
    private Gift testGift;
    private User testUser;
    private EventDTO testEventDTO;
    private GiftDTO testGiftDTO;
    private GiftExtendedDTO testGiftExtendedDTO;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder().eventId(1).eventCode("ABC123").build();

        testGift = Gift.builder()
                .giftId(1)
                .event(testEvent)
                .name("Test Gift")
                .price(100.0)
                .details("Test details")
                .collected(0.0)
                .creationUser("testuser")
                .createdByHost(true)
                .paidInFull(false)
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

        testEventDTO = new EventDTO();
        testEventDTO.setEventId(1);
        testEventDTO.setEventCode("ABC123");

        testGiftDTO = new GiftDTO();
        testGiftDTO.setGiftId(1);
        testGiftDTO.setName("Test Gift");

        testGiftExtendedDTO = new GiftExtendedDTO();
        testGiftExtendedDTO.setGiftId(1);
        testGiftExtendedDTO.setName("Test Gift");
        testGiftExtendedDTO.setPrice(100.0);
        testGiftExtendedDTO.setCollected(0.0);
    }

    /**
     * Verifica que la creacion de un regalo se realiza correctamente y se persiste en base de datos.
     */
    @Test
    @DisplayName("createGift - Creacion exitosa")
    void testCreateGift_Success() {
        GiftCreateDTO createDTO = new GiftCreateDTO();
        createDTO.setName("New Gift");
        createDTO.setPrice(50.0);
        createDTO.setCreationUser("testuser");

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(eventService.getEventByEventCode("ABC123")).thenReturn(testEvent);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.existsByNameAndEvent_EventId("New Gift", 1)).thenReturn(false);

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1);
        when(userService.getUserInformationByUsername("testuser")).thenReturn(userDTO);
        TicketDTO ticketDTO = new TicketDTO();
        ticketDTO.setRole("HOST");
        when(ticketService.getTicketByEventAndUser(1, 1)).thenReturn(ticketDTO);
        when(giftRepository.save(any(Gift.class))).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftDTO(testGift, 1)).thenReturn(testGiftDTO);

        GiftDTO result = giftService.createGift("ABC123", createDTO);

        assertNotNull(result);
        verify(giftRepository).save(any(Gift.class));
    }

    /**
     * Verifica que crear un regalo con un nombre ya existente en el evento lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("createGift - Nombre duplicado")
    void testCreateGift_DuplicateName() {
        GiftCreateDTO createDTO = new GiftCreateDTO();
        createDTO.setName("Existing Gift");
        createDTO.setCreationUser("testuser");

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(eventService.getEventByEventCode("ABC123")).thenReturn(testEvent);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.existsByNameAndEvent_EventId("Existing Gift", 1)).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> giftService.createGift("ABC123", createDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_GIFT_ALREADY_EXISTS, ex.getMessage());
    }

    /**
     * Verifica que un usuario no registrado en el evento no puede crear regalos.
     */
    @Test
    @DisplayName("createGift - Usuario no registrado en evento")
    void testCreateGift_UserNotRegistered() {
        GiftCreateDTO createDTO = new GiftCreateDTO();
        createDTO.setName("Gift");

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(eventService.getEventByEventCode("ABC123")).thenReturn(testEvent);
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT)).when(accessControlUtils).validateUserRegisteredInEvent(1);

        CustomException ex = assertThrows(CustomException.class, () -> giftService.createGift("ABC123", createDTO));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que se obtiene correctamente la informacion detallada de un regalo.
     */
    @Test
    @DisplayName("getGiftInformation - Exitoso")
    void testGetGiftInfo_Success() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());

        GiftExtendedDTO result = giftService.getGiftInformation("ABC123", 1);

        assertNotNull(result);
    }

    /**
     * Verifica que consultar un regalo inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getGiftInformation - Regalo no encontrado")
    void testGetGiftInfo_NotFound() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> giftService.getGiftInformation("ABC123", 999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que el listado de regalos devuelve resultados paginados correctamente.
     */
    @Test
    @DisplayName("getGifts - Exitoso con resultados")
    void testGetGifts_Success() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);

        Page<Gift> giftPage = new PageImpl<>(List.of(testGift));
        Specification<Gift> specification = (root, query, builder) -> builder.conjunction();
        when(giftRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Gift>>any(), any(Pageable.class))).thenReturn(giftPage);
        when(giftSpecificationBuilder.build(anyList())).thenReturn(specification);
        when(giftMapper.convertGiftToGiftDTO(testGift, 1)).thenReturn(testGiftDTO);

        ResultPaginationDTO result = giftService.getGifts("ABC123", 1, 10, "name", "ASC", null);

        assertNotNull(result);
        assertFalse(result.getData().isEmpty());
    }

    /**
     * Verifica que listar regalos de un evento inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("getGifts - Evento no encontrado")
    void testGetGifts_EventNotFound() {
        when(eventService.getEvent("NOTFND")).thenThrow(new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));

        CustomException ex = assertThrows(CustomException.class, () -> giftService.getGifts("NOTFND", 1, 10, "name", "ASC", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que un error inesperado al listar regalos produce INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("getGifts - Error inesperado lanza INTERNAL_SERVER_ERROR")
    void testGetGifts_InternalError() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        Specification<Gift> specification = (root, query, builder) -> builder.conjunction();
        when(giftSpecificationBuilder.build(anyList())).thenReturn(specification);
        when(giftRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Gift>>any(), any(Pageable.class))).thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class, () -> giftService.getGifts("ABC123", 1, 10, "name", "ASC", null));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    /**
     * Verifica que la actualizacion de un regalo existente se completa correctamente.
     */
    @Test
    @DisplayName("updateGift - Actualizacion exitosa")
    void testUpdateGift_Success() {
        GiftUpdateDTO updateDTO = new GiftUpdateDTO();
        updateDTO.setName("Updated Gift");
        updateDTO.setPrice(150.0);
        updateDTO.setDetails("Updated details");

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        doNothing().when(accessControlUtils).validateHostOrGiftCreator(1, "testuser");
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());
        when(giftRepository.save(testGift)).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);

        GiftExtendedDTO result = giftService.updateGift("ABC123", 1, updateDTO);

        assertNotNull(result);
        assertEquals("Updated Gift", testGift.getName());
        assertEquals(150.0, testGift.getPrice());
    }

    /**
     * Verifica que intentar actualizar un regalo inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("updateGift - Regalo no encontrado")
    void testUpdateGift_NotFound() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> giftService.updateGift("ABC123", 999, new GiftUpdateDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que un usuario sin permisos no puede actualizar el regalo.
     */
    @Test
    @DisplayName("updateGift - No autorizado")
    void testUpdateGift_Unauthorized() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_GIFT_UPDATE_FORBIDDEN)).when(accessControlUtils).validateHostOrGiftCreator(1, "testuser");

        CustomException ex = assertThrows(CustomException.class, () -> giftService.updateGift("ABC123", 1, new GiftUpdateDTO()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que al actualizar un regalo se recalcula el monto recaudado a partir de las contribuciones.
     */
    @Test
    @DisplayName("updateGift - Recalcula collected desde contribuciones")
    void testUpdateGift_RecalculateCollected() {
        GiftUpdateDTO updateDTO = new GiftUpdateDTO();
        updateDTO.setName("Gift");
        updateDTO.setPrice(100.0);

        GiftContribution contrib1 = GiftContribution.builder().contribution(30.0).build();
        GiftContribution contrib2 = GiftContribution.builder().contribution(20.0).build();

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        doNothing().when(accessControlUtils).validateHostOrGiftCreator(1, "testuser");
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(List.of(contrib1, contrib2));
        when(giftRepository.save(testGift)).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);

        giftService.updateGift("ABC123", 1, updateDTO);

        assertEquals(50.0, testGift.getCollected());
        assertFalse(testGift.getPaidInFull());
    }

    /**
     * Verifica que la eliminacion de un regalo se completa correctamente junto con sus contribuciones.
     */
    @Test
    @DisplayName("deleteGift - Eliminacion exitosa")
    void testDeleteGift_Success() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserIsHost(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        when(giftMapper.convertGiftToGiftDTO(testGift, 1)).thenReturn(testGiftDTO);

        GiftDTO result = giftService.deleteGift("ABC123", 1);

        assertNotNull(result);
        verify(giftContributionRepository).deleteByGiftId_GiftId(1);
        verify(giftRepository).delete(testGift);
    }

    /**
     * Verifica que un usuario sin rol HOST no puede eliminar un regalo del evento.
     */
    @Test
    @DisplayName("deleteGift - No es HOST")
    void testDeleteGift_NotHost() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_HOST)).when(accessControlUtils).validateUserIsHost(1);

        CustomException ex = assertThrows(CustomException.class, () -> giftService.deleteGift("ABC123", 1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifica que intentar eliminar un regalo inexistente lanza NOT_FOUND.
     */
    @Test
    @DisplayName("deleteGift - Regalo no encontrado")
    void testDeleteGift_GiftNotFound() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserIsHost(1);
        when(giftRepository.findByGiftId(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> giftService.deleteGift("ABC123", 999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifica que se crea correctamente una nueva contribucion a un regalo y se actualiza el monto recaudado.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - Nueva contribucion")
    void testContribution_New() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(25.0);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        when(userService.getUser(1)).thenReturn(testUser);
        when(giftContributionRepository.findByGiftId_GiftIdAndUserId_UserId(1, 1)).thenReturn(Optional.empty());
        when(giftContributionRepository.save(any(GiftContribution.class))).thenReturn(GiftContribution.builder().build());
        when(giftRepository.save(testGift)).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());

        giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO);

        assertEquals(25.0, testGift.getCollected());
        verify(giftContributionRepository).save(any(GiftContribution.class));
    }

    /**
     * Verifica que al actualizar una contribucion existente se recalcula correctamente el monto recaudado.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - Actualizar contribucion existente")
    void testContribution_UpdateExisting() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(50.0);

        testGift.setCollected(30.0);
        GiftContribution existingContrib = GiftContribution.builder()
                .giftContributionId(1)
                .giftId(testGift)
                .userId(testUser)
                .contribution(30.0)
                .build();

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        when(userService.getUser(1)).thenReturn(testUser);
        when(giftContributionRepository.findByGiftId_GiftIdAndUserId_UserId(1, 1)).thenReturn(Optional.of(existingContrib));
        when(giftContributionRepository.save(existingContrib)).thenReturn(existingContrib);
        when(giftRepository.save(testGift)).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());

        giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO);

        // collected = 30.0 - 30.0 + 50.0 = 50.0
        assertEquals(50.0, testGift.getCollected());
        assertEquals(50.0, existingContrib.getContribution());
    }

    /**
     * Verifica que una contribucion con monto negativo lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - Monto negativo")
    void testContribution_NegativeAmount() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(-10.0);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));

        CustomException ex = assertThrows(CustomException.class, () -> giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_GIFT_CONTRIBUTION_POSITIVE, ex.getMessage());
    }

    /**
     * Verifica que una contribucion con monto cero lanza BAD_REQUEST.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - Monto cero")
    void testContribution_ZeroAmount() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(0.0);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));

        CustomException ex = assertThrows(CustomException.class, () -> giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifica que al alcanzar el precio total del regalo se marca como completamente pagado.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - paidInFull se activa al completar")
    void testContribution_PaidInFull() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(100.0);

        testGift.setPrice(100.0);
        testGift.setCollected(0.0);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        when(userService.getUser(1)).thenReturn(testUser);
        when(giftContributionRepository.findByGiftId_GiftIdAndUserId_UserId(1, 1)).thenReturn(Optional.empty());
        when(giftContributionRepository.save(any(GiftContribution.class))).thenReturn(GiftContribution.builder().build());
        when(giftRepository.save(testGift)).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());

        giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO);

        assertTrue(testGift.getPaidInFull());
        assertEquals(100.0, testGift.getCollected());
    }
}
