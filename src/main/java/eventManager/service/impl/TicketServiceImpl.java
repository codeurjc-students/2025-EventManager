package eventManager.service.impl;

import eventManager.dto.EnrollUserDTO;
import eventManager.dto.EventDTO;
import eventManager.dto.EventTicketDTO;
import eventManager.dto.PaginationDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.dto.TicketDTO;
import eventManager.dto.UpdateTicketDTO;
import eventManager.constant.Constantes;
import eventManager.entity.Event;
import eventManager.entity.Ticket;
import eventManager.entity.User;
import eventManager.exception.CustomException;
import eventManager.mapper.TicketMapper;
import eventManager.mapper.EventMapper;
import eventManager.repository.TicketRepository;
import eventManager.repository.EventRepository;
import eventManager.service.TicketService;
import eventManager.service.UserService;
import eventManager.search.SearchCriteria;
import eventManager.search.TicketSpecificationsBuilder;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@Slf4j
public class TicketServiceImpl implements TicketService{
	
	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TicketMapper ticketMapper;

	@Autowired
	private TicketSpecificationsBuilder ticketSpecificationBuilder;

	@Autowired
	private UserService userService;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private EventMapper eventMapper;

	@Autowired
	private AccessControlUtils accessControlUtils;
	
	@Override
	public EventTicketDTO getEventInformation(String eventCode, Integer ticketId, Integer userId) {
		try  {
			// Verificamos si existe la entrada y si el usuario está registrado en el evento
			Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_TICKET_DOES_NOT_EXIST));
			
			if (!ticket.getUserId().getUserId().equals(userId)) {
				throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT);
			} 
			else {
				Event event = eventRepository.findByEventCode(eventCode).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));
				return ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(ticketMapper.convertTicketToTicketDTO(ticket), eventMapper.convertEventToEventDTO(event));
			}
		} catch (CustomException e) {
			log.error("Error al obtener la información del evento: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al obtener la información del evento: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}	

	@Override
	public ResultPaginationDTO getEventTickets(String eventCode, Integer page, Integer pageSize, String sortBy, String sortDir, String search) {
		List<Object> ticketsList = new ArrayList<>();
		ResultPaginationDTO resultPagination = new ResultPaginationDTO();
		PaginationDTO pagination;
		try {
			// Verificamos que el evento existe
			Event event = eventRepository.findByEventCode(eventCode)
				.orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));
			
			// Validar que el usuario autenticado es HOST del evento
			accessControlUtils.validateUserIsHost(event.getEventId());
			
			// Generamos nuestro motor de búsqueda a partir de los parámetros de paginación, ordenación y filtros de búsqueda
			Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending(): Sort.by(sortBy).descending();
			Pageable pageable = page <= 0 ? PageRequest.of(0, Integer.MAX_VALUE, sort) : PageRequest.of(page-1, pageSize, sort);
			
			// Construimos la especificación base: filtrar por eventId
			List<SearchCriteria> params = new ArrayList<>();
			params.add(new SearchCriteria("eventId.eventCode", "=", eventCode));
			
			// Añadimos los filtros adicionales del search si existen
			if(search != null && !search.isEmpty()) {
				Pattern pattern = Pattern.compile(Constantes.PATTERN_SEARCH);
				Matcher matcher = pattern.matcher(search + ",");
				while(matcher.find()) {
					params.add(new SearchCriteria(matcher.group(1), matcher.group(2), matcher.group(3)));	
				}
			}
			
			Specification<Ticket> specification = ticketSpecificationBuilder.build(params);

			// Búsqueda y conversión de los tickets
			Page<Ticket> ticketsEntity = ticketRepository.findAll(specification, pageable);
			ticketsEntity.forEach(ticket-> ticketsList.add(ticketMapper.convertTicketToTicketDTO(ticket)));

			// Generamos la paginación y agregamos los datos
			pagination = PaginationDTO.builder()
					.size(page <= 0 ? (int) ticketsEntity.getTotalElements(): ticketsEntity.getSize())
					.totalElements(ticketsEntity.getTotalElements())
					.totalPages(ticketsEntity.getTotalPages())
					.number(ticketsEntity.getNumber() + 1)
					.build();
			resultPagination.setData(ticketsList);
			resultPagination.setPage(pagination);
		} catch(CustomException e) {
			log.error("No se pudieron obtener las entradas: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch(Exception e) {
			log.error("Error inesperado al obtener las entradas: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
		
		return resultPagination;
	}

	@Override
	public TicketDTO enrollUserInEvent(EnrollUserDTO enrollUserDTO) {
		try {			
			// Validamos que el evento y el usuario existen, y que el usuario no está ya registrado en el evento
			Event event = eventRepository.findByEventCode(enrollUserDTO.getEventCode()).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));
			User user = userService.getUser(enrollUserDTO.getUserId());
			if (ticketRepository.existsByEventId_EventIdAndUserId_UserId(event.getEventId(), enrollUserDTO.getUserId())) {
				throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_ALREADY_REGISTERED_IN_EVENT);
			}

			// Creamos la entrada para el usuario en el evento
			Ticket ticket = Ticket.builder()
					.eventId(event)
					.userId(user)
					.role(enrollUserDTO.getRole())
					.guestNumber(enrollUserDTO.getGuestNumber() != null && enrollUserDTO.getGuestNumber() > 1 ? enrollUserDTO.getGuestNumber() : 1)
					.notes(enrollUserDTO.getNotes())
					.invitationConfirmation(enrollUserDTO.getRole().equals("HOST") ? true : null) // Pendiente de confirmar para los invitados por defecto
					.assistConfirmation(enrollUserDTO.getRole().equals("HOST") ? true : null) // Pendiente de confirmar para los invitados por defecto
					.build();
			
			Ticket savedTicket = ticketRepository.save(ticket);
			return ticketMapper.convertTicketToTicketDTO(savedTicket);
		} catch (CustomException e) {
			log.error("Error al inscribir al usuario en el evento: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al inscribir al usuario en el evento: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public EventTicketDTO updateTicket(String eventCode, Integer ticketId, UpdateTicketDTO updateTicketDTO) {
		try {
			// Validar que el usuario puede acceder a este ticket (propietario o HOST)
			accessControlUtils.validateTicketAccess(ticketId);
			
			// Verificamos si existe la entrada
			Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_TICKET_DOES_NOT_EXIST));

			// Actualizamos los datos de la entrada solo si se proporcionan en el DTO
			if (updateTicketDTO.getRole() != null) {
				ticket.setRole(updateTicketDTO.getRole());
			}
			if (updateTicketDTO.getGuestNumber() != null) {
				ticket.setGuestNumber(updateTicketDTO.getGuestNumber() > 1 ? updateTicketDTO.getGuestNumber() : 1);
			}
			if (updateTicketDTO.getInvitationConfirmation() != null) {
				ticket.setInvitationConfirmation(updateTicketDTO.getInvitationConfirmation());
			}
			if (updateTicketDTO.getAssistConfirmation() != null) {
				ticket.setAssistConfirmation(updateTicketDTO.getAssistConfirmation());
			}
			if (updateTicketDTO.getNotes() != null) {
				ticket.setNotes(updateTicketDTO.getNotes());
			}
			Ticket updatedTicket = ticketRepository.save(ticket);

			Event event = eventRepository.findByEventCode(eventCode).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_EVENT_DOES_NOT_EXIST));
			EventDTO eventDto = eventMapper.convertEventToEventDTO(event);
			return ticketMapper.convertTicketDTOAndEventDTOToEventTicketDTO(ticketMapper.convertTicketToTicketDTO(updatedTicket), eventDto);
		} catch (CustomException e) {
			log.error("Error al actualizar la entrada: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al actualizar la entrada: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Map<Integer, Integer> getTicketsByUserAndRole(Integer userId, String role) {
		try {
			// Validamos que el usuario existe y obtenemos su información
			User user = userService.getUser(userId);

			// Obtenemos todas las entradas del usuario según el rol solicitado
			List<Ticket> userTickets = ticketRepository.findByUserId_UserIdAndRole(user.getUserId(), role);
			Map<Integer, Integer> eventTicketMap = new HashMap<>();
			userTickets.forEach(ticket -> eventTicketMap.put(ticket.getEventId().getEventId(), ticket.getTicketId()));
			return eventTicketMap;
		} catch (CustomException e) {
			log.error("Error al obtener las entradas del usuario: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al obtener las entradas del usuario: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public TicketDTO getTicketByEventAndUser(Integer eventId, Integer userId) {
		try {
			// Validamos que el usuario existe
			User user = userService.getUser(userId);

			// Obtenemos el ticket del usuario en el evento
			Ticket ticket = ticketRepository.findByEventId_EventIdAndUserId_UserId(eventId, user.getUserId())
				.orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT));
			
			return ticketMapper.convertTicketToTicketDTO(ticket);
		} catch (CustomException e) {
			log.error("Error al obtener la entrada: {}", e.getMessage());
			throw new CustomException(e.getStatus(), e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al obtener la entrada del usuario: {}", e.getMessage());
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, Constantes.MESSAGE_INTERNAL_SERVER_ERROR);
		}
	}

}
