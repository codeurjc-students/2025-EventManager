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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the gift service, including creation, retrieval, listing,
 * updates, deletion, and contribution management.
 */
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
        ReflectionTestUtils.setField(giftService, "maxImageSizeBytes", 5_242_880L);

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
     * Verifies that gift creation completes correctly and is persisted in the
     * database.
     */
    @Test
    @DisplayName("createGift - Successful creation")
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
     * Verifies that creating a gift with a name already in the event throws
     * BAD_REQUEST.
     */
    @Test
    @DisplayName("createGift - Duplicate name")
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
     * Verifies that creating a gift with an image larger than the max size throws
     * BAD_REQUEST.
     */
    @Test
    @DisplayName("createGift - Image too large")
    void testCreateGift_ImageTooLarge() {
        GiftCreateDTO createDTO = new GiftCreateDTO();
        createDTO.setName("New Gift");
        createDTO.setPrice(50.0);
        createDTO.setCreationUser("testuser");

        byte[] largeImage = new byte[10];
        String base64 = java.util.Base64.getEncoder().encodeToString(largeImage);
        createDTO.setImage("data:image/png;base64," + base64);

        ReflectionTestUtils.setField(giftService, "maxImageSizeBytes", 5L);

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

        CustomException ex = assertThrows(CustomException.class, () -> giftService.createGift("ABC123", createDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_IMAGE_TOO_LARGE, ex.getMessage());
        verify(s3Service, never()).uploadImage(any(), anyString(), anyString());
    }

    /**
     * Verifies that a user not registered in the event cannot create gifts.
     */
    @Test
    @DisplayName("createGift - User not registered in event")
    void testCreateGift_UserNotRegistered() {
        GiftCreateDTO createDTO = new GiftCreateDTO();
        createDTO.setName("Gift");

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(eventService.getEventByEventCode("ABC123")).thenReturn(testEvent);
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT))
                .when(accessControlUtils).validateUserRegisteredInEvent(1);

        CustomException ex = assertThrows(CustomException.class, () -> giftService.createGift("ABC123", createDTO));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that detailed gift information is retrieved correctly.
     */
    @Test
    @DisplayName("getGiftInformation - Success")
    void testGetGiftInfo_Success() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());

        GiftExtendedDTO result = giftService.getGiftInformation("ABC123", 1);

        assertNotNull(result);
    }

    /**
     * Verifies that querying a non-existent gift throws NOT_FOUND.
     */
    @Test
    @DisplayName("getGiftInformation - Gift not found")
    void testGetGiftInfo_NotFound() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> giftService.getGiftInformation("ABC123", 999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that gift listing returns paginated results correctly.
     */
    @Test
    @DisplayName("getGifts - Success with results")
    void testGetGifts_Success() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);

        Page<Gift> giftPage = new PageImpl<>(List.of(testGift));
        Specification<Gift> specification = (root, query, builder) -> builder.conjunction();
        when(giftRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Gift>>any(), any(Pageable.class)))
                .thenReturn(giftPage);
        when(giftSpecificationBuilder.build(anyList())).thenReturn(specification);
        when(giftMapper.convertGiftToGiftDTO(testGift, 1)).thenReturn(testGiftDTO);

        ResultPaginationDTO result = giftService.getGifts("ABC123", 1, 10, "name", "ASC", null);

        assertNotNull(result);
        assertFalse(result.getData().isEmpty());
    }

    /**
     * Verifies that listing gifts for a non-existent event throws NOT_FOUND.
     */
    @Test
    @DisplayName("getGifts - Event not found")
    void testGetGifts_EventNotFound() {
        when(eventService.getEvent("NOTFND"))
                .thenThrow(new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));

        CustomException ex = assertThrows(CustomException.class,
                () -> giftService.getGifts("NOTFND", 1, 10, "name", "ASC", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that an unexpected error while listing gifts results in
     * INTERNAL_SERVER_ERROR.
     */
    @Test
    @DisplayName("getGifts - Unexpected error throws INTERNAL_SERVER_ERROR")
    void testGetGifts_InternalError() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        Specification<Gift> specification = (root, query, builder) -> builder.conjunction();
        when(giftSpecificationBuilder.build(anyList())).thenReturn(specification);
        when(giftRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Gift>>any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class,
                () -> giftService.getGifts("ABC123", 1, 10, "name", "ASC", null));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    /**
     * Verifies that updating an existing gift completes correctly.
     */
    @Test
    @DisplayName("updateGift - Successful update")
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
     * Verifies that updating a gift with an image that is too large throws
     * BAD_REQUEST.
     */
    @Test
    @DisplayName("updateGift - Image too large")
    void testUpdateGift_ImageTooLarge() {
        GiftUpdateDTO updateDTO = new GiftUpdateDTO();
        updateDTO.setName("Updated Gift");
        updateDTO.setPrice(150.0);

        byte[] largeImage = new byte[10];
        String base64 = java.util.Base64.getEncoder().encodeToString(largeImage);
        updateDTO.setImage("data:image/png;base64," + base64);

        ReflectionTestUtils.setField(giftService, "maxImageSizeBytes", 5L);

        testGift.setImage(null);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        doNothing().when(accessControlUtils).validateHostOrGiftCreator(1, "testuser");

        CustomException ex = assertThrows(CustomException.class, () -> giftService.updateGift("ABC123", 1, updateDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_IMAGE_TOO_LARGE, ex.getMessage());
        verify(s3Service, never()).uploadImage(any(), anyString(), anyString());
    }

    /**
     * Verifies that attempting to update a non-existent gift throws NOT_FOUND.
     */
    @Test
    @DisplayName("updateGift - Gift not found")
    void testUpdateGift_NotFound() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> giftService.updateGift("ABC123", 999, new GiftUpdateDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that a user without permissions cannot update the gift.
     */
    @Test
    @DisplayName("updateGift - Not authorized")
    void testUpdateGift_Unauthorized() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_GIFT_UPDATE_FORBIDDEN))
                .when(accessControlUtils).validateHostOrGiftCreator(1, "testuser");

        CustomException ex = assertThrows(CustomException.class,
                () -> giftService.updateGift("ABC123", 1, new GiftUpdateDTO()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that updating a gift recalculates the collected amount from
     * contributions.
     */
    @Test
    @DisplayName("updateGift - Recalculates collected from contributions")
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
     * Verifies that deleting a gift completes correctly along with its
     * contributions.
     */
    @Test
    @DisplayName("deleteGift - Successful deletion")
    void testDeleteGift_Success() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        doNothing().when(accessControlUtils).validateHostOrGiftCreator(1, "testuser");
        when(giftMapper.convertGiftToGiftDTO(testGift, 1)).thenReturn(testGiftDTO);

        GiftDTO result = giftService.deleteGift("ABC123", 1);

        assertNotNull(result);
        verify(giftContributionRepository).deleteByGiftId_GiftId(1);
        verify(giftRepository).delete(testGift);
    }

    /**
     * Verifies that a user without the HOST role cannot delete a gift from the
     * event.
     */
    @Test
    @DisplayName("deleteGift - Not HOST")
    void testDeleteGift_NotHost() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        doThrow(new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_GIFT_UPDATE_FORBIDDEN))
                .when(accessControlUtils).validateHostOrGiftCreator(1, "testuser");

        CustomException ex = assertThrows(CustomException.class, () -> giftService.deleteGift("ABC123", 1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /**
     * Verifies that attempting to delete a non-existent gift throws NOT_FOUND.
     */
    @Test
    @DisplayName("deleteGift - Gift not found")
    void testDeleteGift_GiftNotFound() {
        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        when(giftRepository.findByGiftId(999)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> giftService.deleteGift("ABC123", 999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    /**
     * Verifies that a new contribution to a gift is created correctly and the
     * collected amount is updated.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - New contribution")
    void testContribution_New() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(25.0);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));
        when(userService.getUser(1)).thenReturn(testUser);
        when(giftContributionRepository.findByGiftId_GiftIdAndUserId_UserId(1, 1)).thenReturn(Optional.empty());
        when(giftContributionRepository.save(any(GiftContribution.class)))
                .thenReturn(GiftContribution.builder().build());
        when(giftRepository.save(testGift)).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());

        giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO);

        assertEquals(25.0, testGift.getCollected());
        verify(giftContributionRepository).save(any(GiftContribution.class));
    }

    /**
     * Verifies that updating an existing contribution recalculates the collected
     * amount correctly.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - Update existing contribution")
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
        when(giftContributionRepository.findByGiftId_GiftIdAndUserId_UserId(1, 1))
                .thenReturn(Optional.of(existingContrib));
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
     * Verifies that a contribution with a negative amount throws BAD_REQUEST.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - Negative amount")
    void testContribution_NegativeAmount() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(-10.0);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));

        CustomException ex = assertThrows(CustomException.class,
                () -> giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(Constantes.MESSAGE_GIFT_CONTRIBUTION_POSITIVE, ex.getMessage());
    }

    /**
     * Verifies that a contribution with a zero amount throws BAD_REQUEST.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - Zero amount")
    void testContribution_ZeroAmount() {
        UserGiftDTO userGiftDTO = new UserGiftDTO();
        userGiftDTO.setUserId(1);
        userGiftDTO.setAmount(0.0);

        when(eventService.getEvent("ABC123")).thenReturn(testEventDTO);
        doNothing().when(accessControlUtils).validateUserRegisteredInEvent(1);
        when(giftRepository.findByGiftId(1)).thenReturn(Optional.of(testGift));

        CustomException ex = assertThrows(CustomException.class,
                () -> giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    /**
     * Verifies that reaching the gift's total price marks it as paid in full.
     */
    @Test
    @DisplayName("createUpdateGiftContribution - PaidInFull set when completed")
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
        when(giftContributionRepository.save(any(GiftContribution.class)))
                .thenReturn(GiftContribution.builder().build());
        when(giftRepository.save(testGift)).thenReturn(testGift);
        when(giftMapper.convertGiftToGiftExtendedDTO(testGift, 1)).thenReturn(testGiftExtendedDTO);
        when(giftContributionRepository.findByGiftId_GiftId(1)).thenReturn(Collections.emptyList());

        giftService.createUpdateGiftContribution("ABC123", 1, userGiftDTO);

        assertTrue(testGift.getPaidInFull());
        assertEquals(100.0, testGift.getCollected());
    }
}
