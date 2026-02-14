package eventManager.service;

import eventManager.dto.CreateUpdateEventDTO;
import eventManager.dto.EventDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.entity.Event;
import jakarta.validation.Valid;

public interface EventService {
	
	/**
	 * Obtiene un evento por su código único.
	 *
	 * @param eventCode - Código único del evento
	 * @return - DTO del evento
	 */
	EventDTO getEvent(String eventCode);

	/**
	 * Obtiene un evento por su código único.
	 *
	 * @param eventCode - Código único del evento
	 * @return - Event entity
	 */
	Event getEventByEventCode(String eventCode);
	
	/**
	 * Obtiene la lista de eventos en los que está registrado un usuario paginada.
	 * 
	 * @param page - Número de página
	 * @param pageSize - Tamaño de la página
	 * @param sortBy - Campo por el que se ordena
	 * @param sortDir - Dirección de la ordenación (asc o desc)
	 * @param search - Texto a buscar en el nombre del evento
	 * @param userId - Identificador del usuario
	 * @param role - Rol del usuario en el evento
	 * @return - Lista de eventos paginada
	 */
	ResultPaginationDTO getEvents(Integer page, Integer pageSize, String sortBy, String sortDir, String search, Integer userId, String role);

	/**
	 * Crea un nuevo evento y una entrada de anfitrión para el usuario que lo crea.
	 *
	 * @param userId - Código único del usuario que crea el evento
	 * @param createUpdateEventDTO - DTO con los datos del evento a crear
	 * @return - DTO del evento creado
	 */
	EventDTO createEvent(Integer userId, CreateUpdateEventDTO createUpdateEventDTO);

	/**
	 * Actualiza un la informeción de un evento.
	 *
	 * @param eventCode - Código único del evento a actualizar
	 * @param createUpdateEventDTO - DTO con los datos actualizados del evento
	 * @return - DTO del evento actualizado
	 * @throws Exception - Excepción en caso de que el evento no exista
	 */
	EventDTO updateEvent(String eventCode, @Valid CreateUpdateEventDTO createUpdateEventDTO);

}
