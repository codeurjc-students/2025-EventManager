package eventManager.service;

import java.util.Map;

import eventManager.dto.EnrollUserDTO;
import eventManager.dto.EventTicketDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.dto.TicketDTO;
import eventManager.dto.UpdateTicketDTO;
import eventManager.exception.CustomException;

public interface TicketService {

	/**
	 * Obtiene la información de un evento y de la entrada del usuario.
	 * 
	 * @param eventCode - Código único del evento
	 * @param ticketId - Código único de la entrada
	 * @param userId - Código único del usuario
	 * @return - Información del evento y de la entrada del usuario
	 * @throws Exception - Excepción en caso de que el usuario no esté registrado en la aplicación
	 * @throws Exception - Excepción en caso de que el evento no exista
	 * @throws Exception - Excepción en caso de que el usuario no esté registrado en ese evento
	 */
	EventTicketDTO getEventInformation(String eventCode, Integer ticketId, Integer userId);

	
	/**
	 * Obtiene la lista de entradas de un evento paginada.
	 *
	 * @param eventCode - Código único del evento
	 * @param page - Número de página
	 * @param pageSize - Tamaño de la página
	 * @param sortBy - Campo por el que se ordena
	 * @param sortDir - Dirección de la ordenación (asc o desc)
	 * @param search - Texto a buscar en el nombre del usuario o en el código de la entrada
	 * @return - Lista de entradas del evento paginada
	 */
	ResultPaginationDTO getEventTickets(String eventCode, Integer page, Integer pageSize, String sortBy, String sortDir, String search);

	/**
	 * Crea una entrada a un evento para un usuario.
	 *
	 * @param createUpdateEventDTO - DTO con los datos actualizados del evento
	 * @return - DTO de la entrada generada
	 */
	TicketDTO enrollUserInEvent(EnrollUserDTO enrollUserDTO);

	/**
	 * Actualiza la información de una entrada de un evento.
	 *
	 * @param eventCode - Código único del evento
	 * @param ticketId - Código único de la entrada
	 * @param updateTicketDTO - DTO con los datos actualizados de la entrada
	 * @return - DTO de la entrada actualizada
	 */
	EventTicketDTO updateTicket(String eventCode, Integer ticketId, UpdateTicketDTO updateTicketDTO);

	/**
	 * Obtiene una lista de identificadores de eventos para los que el usuario tiene entradas según el rol solicitado.
	 * @param userId - Código único del usuario
	 * @param role - Rol del usuario en el evento
	 * @throws CustomException - Excepción en caso de que el usuario no exista
	 * @return - Un mapa con la lista de identificadores de eventos y la entrada asociada a cada evento
	 */
	Map<Integer, Integer> getTicketsByUserAndRole(Integer userId, String role);

	/**
	 * Obtiene el ticket de un usuario en un evento específico.
	 * @param eventId - Código único del evento
	 * @param userId - Código único del usuario
	 * @throws CustomException - Excepción en caso de que el usuario no tenga entrada en ese evento
	 * @return - TicketDTO con la información del ticket
	 */
	TicketDTO getTicketByEventAndUser(Integer eventId, Integer userId);

}
