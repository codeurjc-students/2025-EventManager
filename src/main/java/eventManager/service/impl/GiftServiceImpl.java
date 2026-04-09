package eventManager.service.impl;

import eventManager.dto.EventDTO;
import eventManager.dto.GiftCreateDTO;
import eventManager.dto.GiftDTO;
import eventManager.dto.GiftExtendedDTO;
import eventManager.dto.GiftUpdateDTO;
import eventManager.dto.PaginationDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.dto.TicketDTO;
import eventManager.dto.UserDTO;
import eventManager.dto.UserGiftContributionDTO;
import eventManager.dto.UserGiftDTO;
import eventManager.constant.Constantes;
import eventManager.entity.Event;
import eventManager.entity.Gift;
import eventManager.entity.GiftContribution;
import eventManager.entity.User;
import eventManager.exception.CustomException;
import eventManager.mapper.GiftContributionMapper;
import eventManager.mapper.GiftMapper;
import eventManager.repository.GiftContributionRepository;
import eventManager.repository.GiftRepository;
import eventManager.service.EventService;
import eventManager.service.GiftService;
import eventManager.service.S3Service;
import eventManager.service.TicketService;
import eventManager.service.UserService;
import eventManager.search.GiftSpecificationsBuilder;
import eventManager.search.SearchCriteria;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.model.S3Object;

@Service
@Transactional
@Slf4j
public class GiftServiceImpl implements GiftService{
	
	@Autowired
	private GiftRepository giftRepository;

	@Autowired
	private GiftContributionRepository giftContributionRepository;
	
	@Autowired
	private GiftMapper giftMapper;

	@Autowired
	private GiftContributionMapper giftContributionMapper;

	@Autowired
	private GiftSpecificationsBuilder giftSpecificationBuilder;

	@Autowired
	private EventService eventService;

	@Autowired
	private UserService userService;

	@Autowired
	private TicketService ticketService;

	// S3Service está disponible en perfil 'dev' (MinIO) y 'aws' (AWS S3)
	@Autowired(required = false)
	private S3Service s3Service;

	@Autowired
	private AccessControlUtils accessControlUtils;

	@Value("${app.images.max-size-bytes:" + Constantes.GIFT_IMAGES_MAX_SIZE_BYTES + "}")
	private long maxImageSizeBytes;

	@Override
	public GiftDTO createGift(String eventCode, GiftCreateDTO giftCreateDTO) {
		try{
			// Verificamos que el evento existe
			EventDTO eventDTO = eventService.getEvent(eventCode);
			Event event = eventService.getEventByEventCode(eventCode);
			
			// Validar que el usuario autenticado está registrado en el evento
			accessControlUtils.validateUserRegisteredInEvent(eventDTO.getEventId());
			
			// Validamos que el regalo no esté repetido
			if (giftRepository.existsByNameAndEvent_EventId(giftCreateDTO.getName(), eventDTO.getEventId())) {
				throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_GIFT_ALREADY_EXISTS);
			}

			// Determinamos si el usuario que crea el regalo es HOST del evento
			UserDTO userDTO = userService.getUserInformationByUsername(giftCreateDTO.getCreationUser());
			TicketDTO userTicket = ticketService.getTicketByEventAndUser(eventDTO.getEventId(), userDTO.getUserId());
			boolean createdByHost = "HOST".equalsIgnoreCase(userTicket.getRole());

			String imageS3Key = null;
			if (s3Service != null && giftCreateDTO.getImage() != null && !giftCreateDTO.getImage().toString().isEmpty()) {
				try {
					// Decodificar base64 recibido del frontend
					String originalImage = giftCreateDTO.getImage().toString();
					byte[] imageBytes = decodeBase64Image(originalImage);
					validateImageSize(imageBytes);
					
					// Determinar tipo de contenido y extensión
					String contentType = "image/jpeg";
					String extension = ".jpg";
					
					if (originalImage.contains("data:image/png")) {
						contentType = "image/png";
						extension = ".png";
					} else if (originalImage.contains("data:image/gif")) {
						contentType = "image/gif";
						extension = ".gif";
					}
					
					// Generar nombre de archivo seguro
					String fileName = giftCreateDTO.getName().replaceAll("[^a-zA-Z0-9]", "_") + extension;
					
					imageS3Key = s3Service.uploadImage(imageBytes, fileName, contentType);
					log.info("Imagen subida exitosamente: {}", imageS3Key);
					
				} catch (CustomException e) {
					throw e;
				} catch (Exception e) {
					log.error("Error al subir imagen: {}", e.getMessage());
					// No fallar la creación del regalo si falla la subida de imagen
					// La imagen simplemente no se guardará
				}
			}

			Gift gift = Gift.builder()
					.event(event)
					.name(giftCreateDTO.getName())
					.details(giftCreateDTO.getDetails())
					.price(giftCreateDTO.getPrice())
					.url(giftCreateDTO.getUrl())
					.image(imageS3Key) // Guardará la S3 key (o null en dev)
					.collected(0.0)
					.creationUser(giftCreateDTO.getCreationUser())
					.createdByHost(createdByHost)
					.paidInFull(false)
					.build();
			
			return giftMapper.convertGiftToGiftDTO(giftRepository.save(gift), eventDTO.getEventId());
		} catch (CustomException e) {
			log.error("Error al crear el regalo: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al crear el regalo: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public GiftExtendedDTO getGiftInformation(String eventCode, Integer giftId) {
		try {
			// Verificamos que el evento existe
			EventDTO event = eventService.getEvent(eventCode);

			// Verificamos que el regalo existe
			Gift gift = giftRepository.findByGiftId(giftId).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_GIFT_DOES_NOT_EXIST));
			GiftExtendedDTO giftDto = giftMapper.convertGiftToGiftExtendedDTO(gift, event.getEventId());

			// Generar URL firmada para la imagen si existe
			if (s3Service != null && giftDto.getImage() != null && !giftDto.getImage().isEmpty()) {
				String presignedUrl = s3Service.generatePresignedUrl(giftDto.getImage());
				if (presignedUrl != null) {
					giftDto.setImage(presignedUrl);
					log.debug("URL firmada generada para imagen del regalo {}", giftId);
				}
			}

			// Obtenemos las contribuciones del regalo
			giftDto.setUserContributionList(getGiftContributions(giftId));
			return giftDto;
		} catch (CustomException e) {
			log.error("Error al obtener la información del regalo: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al obtener la información del regalo: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResultPaginationDTO getGifts(String eventCode, Integer page, Integer pageSize, String sortBy, String sortDir, String search) {
		// Verificamos que el evento existe
		EventDTO event = eventService.getEvent(eventCode);

		List<Object> giftsList = new ArrayList<>();
		ResultPaginationDTO resultPagination = new ResultPaginationDTO();
		PaginationDTO pagination;
		try {
			// Generamos nuestro motor de búsqueda a partir de los parámetros de paginación, ordenación y filtros de búsqueda
			Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending(): Sort.by(sortBy).descending();
			Pageable pageable = page <= 0 ? PageRequest.of(0, Integer.MAX_VALUE, sort) : PageRequest.of(page-1, pageSize, sort);
			
			// Añadimos el evento al criterio de búsqueda
			List<SearchCriteria> params = new ArrayList<>();
			params.add(new SearchCriteria("event.eventId", "=", event.getEventId().toString()));
			
			// Agregamos criterios de búsqueda adicionales si existen
			if(search != null) {
				Pattern pattern = Pattern.compile(Constantes.PATTERN_SEARCH);
				Matcher matcher = pattern.matcher(search + ",");
				while(matcher.find()) {
					params.add(new SearchCriteria(matcher.group(1), matcher.group(2), matcher.group(3)));	
				}
			}
			
			// Construimos la especificación con todos los criterios (siempre incluye el filtro por evento)
			Specification<Gift> specification = giftSpecificationBuilder.build(params);

			// Búsqueda y conversión de los eventos
			Page<Gift> giftsEntity = giftRepository.findAll(specification, pageable);
			giftsEntity.forEach(gift -> giftsList.add(giftMapper.convertGiftToGiftDTO(gift, event.getEventId())));

			// Generamos la paginación y agregamos los datos
			pagination = PaginationDTO.builder()
					.size(page <= 0 ? (int) giftsEntity.getTotalElements(): giftsEntity.getSize())
					.totalElements(giftsEntity.getTotalElements())
					.totalPages(giftsEntity.getTotalPages())
					.number(giftsEntity.getNumber() + 1)
					.build();
			resultPagination.setData(giftsList);
			resultPagination.setPage(pagination);
		} catch(Exception e) {
			log.error("Error inesperado al obtener regalos: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
		
		return resultPagination;
	}

	@Override
	public GiftExtendedDTO updateGift(String eventCode, Integer giftId, GiftUpdateDTO giftDTO) {
		try {
			// Verificamos que el evento existe
			EventDTO event = eventService.getEvent(eventCode);

			// Verificamos que el regalo existe
			Gift gift = giftRepository.findByGiftId(giftId).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_GIFT_DOES_NOT_EXIST));
			
			// Validar que el usuario autenticado es HOST del evento O creador del regalo
			accessControlUtils.validateHostOrGiftCreator(event.getEventId(), gift.getCreationUser());

			// Actualizamos los campos del regalo
			gift.setName(giftDTO.getName());
			gift.setDetails(giftDTO.getDetails());
			gift.setUrl(giftDTO.getUrl());
			
			if (s3Service != null && giftDTO.getImage() != null && !giftDTO.getImage().isEmpty()) {
				try {
					// Si el DTO contiene una imagen nueva (base64), procesarla
					// Si ya es una URL de S3, no hacer nada
					if (giftDTO.getImage().startsWith("data:") || !giftDTO.getImage().startsWith("https://")) {
						// Eliminar imagen anterior si existe
						if (gift.getImage() != null && !gift.getImage().isEmpty()) {
							s3Service.deleteImage(gift.getImage());
							log.info("Imagen anterior eliminada: {}", gift.getImage());
						}
						
						// Decodificar base64
						String originalImage = giftDTO.getImage();
						byte[] imageBytes = decodeBase64Image(originalImage);
						validateImageSize(imageBytes);
						
						// Determinar tipo de contenido
						String contentType = "image/jpeg";
						String extension = ".jpg";
						if (originalImage.contains("data:image/png")) {
							contentType = "image/png";
							extension = ".png";
						}
						
						String fileName = giftDTO.getName().replaceAll("[^a-zA-Z0-9]", "_") + extension;
						String newImageS3Key = s3Service.uploadImage(imageBytes, fileName, contentType);
						gift.setImage(newImageS3Key);
						log.info("Nueva imagen subida: {}", newImageS3Key);
					}
				} catch (CustomException e) {
					throw e;
				} catch (Exception e) {
					log.error("Error al actualizar imagen: {}", e.getMessage());
					// No fallar la actualización del regalo
				}
			}
			
			gift.setPrice(giftDTO.getPrice());
			
			// Recalcular el collected desde las contribuciones existentes
			Double collectedAmount = calculateCollectedAmount(giftId);
			gift.setCollected(collectedAmount);
			
			// Actualizamos el estado de paidInFull cuando se actualiza el regalo
			gift.setPaidInFull(gift.getCollected() >= gift.getPrice());
			
			GiftExtendedDTO giftDtoUpdated = giftMapper.convertGiftToGiftExtendedDTO(giftRepository.save(gift), event.getEventId());
			
			giftDtoUpdated.setUserContributionList(getGiftContributions(giftId));
			return giftDtoUpdated;
		} catch (CustomException e) {
			log.error("Error al actualizar el regalo: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al actualizar el regalo: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public GiftDTO deleteGift(String eventCode, Integer giftId) {
		try {
			// Verificamos que el evento existe
			EventDTO event = eventService.getEvent(eventCode);

			// Verificamos que el regalo existe
			Gift gift = giftRepository.findByGiftId(giftId).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_GIFT_DOES_NOT_EXIST));
			
			// Validar que el usuario autenticado es HOST del evento o creador del regalo
			accessControlUtils.validateHostOrGiftCreator(event.getEventId(), gift.getCreationUser());
			
			// Eliminamos la imagen del regalo de S3 si existe (solo en perfil AWS)
			if (s3Service != null && gift.getImage() != null && !gift.getImage().isEmpty()) {
				try {
					s3Service.deleteImage(gift.getImage());
					log.info("Imagen eliminada de S3: {}", gift.getImage());
				} catch (Exception e) {
					log.error("Error al eliminar imagen de S3: {}", e.getMessage());
					// No fallar la eliminación del regalo si falla la eliminación de la imagen
				}
			}

			// Eliminamos las contribuciones del regalo
			giftContributionRepository.deleteByGiftId_GiftId(giftId);

			// Eliminamos el regalo
			giftRepository.delete(gift);
			return giftMapper.convertGiftToGiftDTO(gift, event.getEventId());
		} catch (CustomException e) {
			log.error("Error al eliminar el regalo: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al eliminar el regalo: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public GiftExtendedDTO createUpdateGiftContribution(String eventCode, Integer giftId, UserGiftDTO userGiftDTO) {
		try {
			// Verificamos que el evento existe
			EventDTO event = eventService.getEvent(eventCode);
			
			// Validar que el usuario autenticado está registrado en el evento
			accessControlUtils.validateUserRegisteredInEvent(event.getEventId());
			
			// Verificamos que el regalo existe
			Gift gift = giftRepository.findByGiftId(giftId).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_GIFT_DOES_NOT_EXIST));

			// Validamos que la cantidad introducida sea positiva
			if (userGiftDTO.getAmount() <= 0) {
				throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_GIFT_CONTRIBUTION_POSITIVE);
			}

			User user = userService.getUser(userGiftDTO.getUserId());

			// Verificamos si el usuario ya ha contribuido al regalo
			GiftContribution giftContribution = giftContributionRepository.findByGiftId_GiftIdAndUserId_UserId(giftId, user.getUserId()).orElse(null);
			if (giftContribution != null) {
				// Si el usuario ya ha contribuido, actualizamos su contribución y el total del regalo
				gift.setCollected(gift.getCollected() - giftContribution.getContribution() + userGiftDTO.getAmount());
				giftContribution.setContribution(userGiftDTO.getAmount());
			} else {
				// Si el usuario no ha contribuido, creamos una nueva contribución y sumamos su contribución al total del regalo
				giftContribution = GiftContribution.builder()
					.giftId(gift)
					.userId(user)
					.contribution(userGiftDTO.getAmount())
					.build();
				gift.setCollected(gift.getCollected() + userGiftDTO.getAmount());
			}

			// Actualizamos el estado de paidInFull
			gift.setPaidInFull(gift.getCollected() >= gift.getPrice());

			// Actualizamos el regalo y guardamos la contribución
			giftContributionRepository.save(giftContribution);
			GiftExtendedDTO giftDtoUpdated = giftMapper.convertGiftToGiftExtendedDTO(giftRepository.save(gift), event.getEventId());
			giftDtoUpdated.setUserContributionList(getGiftContributions(giftId));
			return giftDtoUpdated;
		} catch (CustomException e) {
			log.error("Error al crear la contribución: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error al crear contribución: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	private List<UserGiftContributionDTO> getGiftContributions(Integer giftId) {
		List<UserGiftContributionDTO> userContributions = new ArrayList<>();
		try {
			// Obtenemos las contribuciones del regalo
			List<GiftContribution> contributions = giftContributionRepository.findByGiftId_GiftId(giftId);
			if (!contributions.isEmpty()) {
				for (GiftContribution contribution : contributions) {
					userContributions.add(giftContributionMapper.convertGiftContributionToUserGiftContributionDTO(contribution));
				}
			}
		} catch (Exception e) {
			log.error("Error al obtener contribuciones: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
		return userContributions;
	}

	private byte[] decodeBase64Image(String rawImageData) {
		String base64Image = rawImageData;
		if (base64Image.startsWith("data:")) {
			base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
		}
		return java.util.Base64.getDecoder().decode(base64Image);
	}

	private void validateImageSize(byte[] imageBytes) {
		if (imageBytes != null && imageBytes.length > maxImageSizeBytes) {
			throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_IMAGE_TOO_LARGE);
		}
	}

	/**
	 * Calcula el monto total recaudado de un regalo sumando todas las contribuciones
	 * @param giftId ID del regalo
	 * @return monto total recaudado
	 */
	private Double calculateCollectedAmount(Integer giftId) {
		try {
			List<GiftContribution> contributions = giftContributionRepository.findByGiftId_GiftId(giftId);
			return contributions.stream()
					.mapToDouble(GiftContribution::getContribution)
					.sum();
		} catch (Exception e) {
			log.error("Error al calcular el monto recaudado: {}", e.getMessage());
			return 0.0;
		}
	}

}
