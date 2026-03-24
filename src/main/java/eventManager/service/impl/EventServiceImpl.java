package eventManager.service.impl;

import eventManager.dto.CreateUpdateEventDTO;
import eventManager.dto.EnrollUserDTO;
import eventManager.dto.EventDTO;
import eventManager.dto.EventWithTicketDTO;
import eventManager.dto.PaginationDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.constant.Constantes;
import eventManager.entity.Event;
import eventManager.exception.CustomException;
import eventManager.mapper.EventMapper;
import eventManager.repository.EventRepository;
import eventManager.service.EventService;
import eventManager.service.TicketService;
import eventManager.service.UserService;
import eventManager.search.EventSpecificationsBuilder;
import eventManager.search.SearchCriteria;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

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
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class EventServiceImpl implements EventService{
	
	private static final Random RANDOM = new Random();
	
	@Autowired
	private EventRepository eventRepository;
	
	@Autowired
	private EventMapper eventMapper;

	@Autowired
	private EventSpecificationsBuilder eventSpecificationBuilder;

	@Autowired
	private UserService userService;

	@Autowired
	@Lazy
	private TicketService ticketService;

	@Autowired
	private AccessControlUtils accessControlUtils;

	private String generateEventCode() {
		// Genera un código de evento aleatorio de 6 caracteres. Usa números del 1-9 y letras mayúsculas A-Z (excluyendo Ñ)
		String chars = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder code = new StringBuilder(6);
		for (int i = 0; i < 6; i++) {
			code.append(chars.charAt(RANDOM.nextInt(chars.length())));
		}
		
		return code.toString();
	}

	private String generateUniqueEventCode() {
		// Generamos un código de evento único de 6 caracteres verificando que no exista en la base de datos
		String eventCode;
		int maxAttempts = 50; // Límite de intentos para evitar bucle infinito
		int attempts = 0;
		
		do {
			eventCode = generateEventCode();
			attempts++;
			if (attempts >= maxAttempts) {
				log.error("No se pudo generar un código único después de {} intentos", maxAttempts);
				throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar un código único para el evento");
			}
		} while (eventRepository.existsByEventCode(eventCode));
		
		return eventCode;
	}

	@Override
	public EventDTO getEvent(String eventCode) throws CustomException {
		Event event = eventRepository.findByEventCode(eventCode).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));
		return eventMapper.convertEventToEventDTO(event);
	}

	@Override
	public Event getEventByEventCode(String eventCode) throws CustomException {
		return eventRepository.findByEventCode(eventCode).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));
	}

	@Override
	public ResultPaginationDTO getEvents(Integer page, Integer pageSize, String sortBy, String sortDir, String search, Integer userId, String role) {
		List<Object> eventsList = new ArrayList<>();
		ResultPaginationDTO resultPagination = new ResultPaginationDTO();
		PaginationDTO pagination;
		try {			
			// Obtenemos el identificador de los eventos para los que el usuario tiene entrada según el rol solicitado
			Map<Integer, Integer> userEventTicketsMap = ticketService.getTicketsByUserAndRole(userId, role);
			ArrayList<Integer> eventIdList = new ArrayList<>(userEventTicketsMap.keySet());
			
			if(eventIdList.isEmpty()) {
				// Si el usuario no tiene entradas devolvemos una lista vacía
				pagination = PaginationDTO.builder().size(0).totalElements((long) 0).totalPages(0).number(0).build();
				resultPagination.setPage(pagination);
				resultPagination.setData(eventsList);
				return resultPagination;
			}
			
			// Concatenamos a los filtros el filtro de búsqueda para que solo devuelva los eventos a los que el usuario tiene acceso
			search = search == null ? "eventId=(" + eventIdList.stream().map(String::valueOf).collect(Collectors.joining(";")) + ")" : search.concat(",eventId=(" + eventIdList.stream().map(String::valueOf).collect(Collectors.joining(";")) + ")");
			
			Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending(): Sort.by(sortBy).descending();
			Pageable pageable = page <= 0 ? PageRequest.of(0, Integer.MAX_VALUE, sort) : PageRequest.of(page-1, pageSize, sort);
			Specification<Event> specification = null;
			if(search != null) {
				List<SearchCriteria> params = new ArrayList<>();
				Pattern pattern = Pattern.compile(Constantes.PATTERN_SEARCH);
				Matcher matcher = pattern.matcher(search + ",");
				
				while(matcher.find()) {
					String key = matcher.group(1);
					String operation = matcher.group(2);
					String value = matcher.group(3);
					params.add(new SearchCriteria(key, operation, value));	
				}
				
				specification = eventSpecificationBuilder.build(params);
			}

			// Búsqueda y conversión de los eventos
			Page<Event> eventsEntity = eventRepository.findAll(specification, pageable);
			
			eventsEntity.forEach(event-> {
				EventWithTicketDTO dto = eventMapper.convertEventToEventWithTicketDTO(event);
				eventsList.add(dto);
			});

			// Añadimos la información de la entrada que tiene el usuario para cada evento
			for (Object obj : eventsList) {
				EventWithTicketDTO event = (EventWithTicketDTO) obj;
				event.setTicketId(userEventTicketsMap.get(event.getEventId()));
			}

			// Generamos la paginación y agregamos los datos
			pagination = PaginationDTO.builder()
					.size(page <= 0 ? (int) eventsEntity.getTotalElements(): eventsEntity.getSize())
					.totalElements(eventsEntity.getTotalElements())
					.totalPages(eventsEntity.getTotalPages())
					.number(eventsEntity.getNumber() + 1)
					.build();
			resultPagination.setData(eventsList);
			resultPagination.setPage(pagination);
			
		} catch(Exception e) {
			log.error("Error al obtener eventos: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
		
		return resultPagination;
	}

	@Override
	public EventDTO createEvent(Integer userId, CreateUpdateEventDTO createUpdateEventDTO) {
		try {			
			// Validamos que el usuario existe
			userService.getUserInformation(userId);
			
			// Generamos un código único para el evento
			String uniqueEventCode = generateUniqueEventCode();
			createUpdateEventDTO.setEventCode(uniqueEventCode);

			// Guardamos el evento
			Event newEvent = eventRepository.save(eventMapper.convertCreateUpdateEventDTOToEvent(createUpdateEventDTO));

			// Creamos una entrada para el anfitrión del evento (el usuario que lo crea)
			EnrollUserDTO enrollUserDTO = EnrollUserDTO.builder()
					.eventCode(newEvent.getEventCode())
					.userId(userId)
					.role("HOST")
					.guestNumber(1) // Por defecto no se añaden acompañantes al anfitrión
					.notes("Ticket de anfitrión del evento")
					.build();
			
			ticketService.enrollUserInEvent(enrollUserDTO);

			return eventMapper.convertEventToEventDTO(newEvent);
		} catch (CustomException e) {
			log.error("Error al crear el evento: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al crear el evento: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public EventDTO updateEvent(String eventCode, CreateUpdateEventDTO createUpdateEventDTO) {
		try {
			// Validamos que el evento existe
			Event existingEvent = eventRepository.findByEventCode(eventCode)
				.orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));
			
			// Validar que el usuario autenticado es HOST del evento
			accessControlUtils.validateUserIsHost(existingEvent.getEventId());

			// Guardamos el evento actualizado con el ID existente
			Event eventMapped = eventMapper.convertCreateUpdateEventDTOToEvent(createUpdateEventDTO);
			eventMapped.setEventId(existingEvent.getEventId());
			
			Event updatedEvent = eventRepository.save(eventMapped);
			return eventMapper.convertEventToEventDTO(updatedEvent);
		} catch (CustomException e) {
			log.error("Error al actualizar el evento: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al actualizar el evento: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar el evento: " + e.getMessage());
		}
	}

}
