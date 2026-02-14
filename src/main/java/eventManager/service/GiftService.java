package eventManager.service;

import eventManager.dto.GiftCreateDTO;
import eventManager.dto.GiftDTO;
import eventManager.dto.GiftExtendedDTO;
import eventManager.dto.GiftUpdateDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.dto.UserGiftDTO;

public interface GiftService {

	/**
	 * Crea un nuevo regalo en un evento.
	 *
	 * @param eventCode código único del evento
	 * @param giftCreateDTO los detalles del regalo a crear
	 * @return ResponseEntity con GiftDTO que contiene el regalo creado
	 */
	GiftDTO createGift(String eventCode, GiftCreateDTO giftCreateDTO);

	/**
	 * Obtiene la información de un regalo específico en un evento.
	 *
	 * @param eventCode código único del evento
	 * @param giftId el ID del regalo
	 * @return ResponseEntity con GiftExtendedDTO que contiene la información del regalo y las aportaciones
	 */
	GiftExtendedDTO getGiftInformation(String eventCode, Integer giftId);

	/**
	 * Obtiene una lista paginada de regalos en un evento.
	 *
	 * @param eventCode código único del evento
	 * @param page el número de página
	 * @param pageSize el tamaño de la página
	 * @param sortBy el campo por el que se ordena
	 * @param sortDir la dirección de la ordenación (asc o desc)
	 * @param search texto a buscar en los nombres de los regalos
	 * @return ResponseEntity con ResultPaginationDTO que contiene la lista de regalos paginada
	 */
	ResultPaginationDTO getGifts(String eventCode, Integer page, Integer pageSize, String sortBy, String sortDir, String search);

	/**
	 * Actualiza la información de un regalo en un evento.
	 *
	 * @param eventCod código único del evento
	 * @param giftId el ID del regalo a actualizar
	 * @param giftUpdateDTO los nuevos detalles del regalo
	 * @return ResponseEntity con GiftExtendedDTO que contiene el regalo actualizado y las aportaciones
	 */
	GiftExtendedDTO updateGift(String eventCode, Integer giftId, GiftUpdateDTO giftUpdateDTO);

	/**
	 * Elimina un regalo de un evento.
	 *
	 * @param eventCode código único del evento
	 * @param giftId el ID del regalo a eliminar
	 * @return ResponseEntity con GiftDTO que contiene el regalo eliminado
	 */
	GiftDTO deleteGift(String eventCode, Integer giftId);

	/**
	 * Añade una aportación monetaria a un regalo en un evento o actualiza una realizada anteriormente.
	 *
	 * @param eventCode código único del evento
	 * @param giftId el ID del regalo
	 * @param userGiftDTO los detalles del regalo del usuario
	 * @return ResponseEntity con GiftExtendedDTO que contiene el regalo actualizado con la aportación
	 */
	GiftExtendedDTO createUpdateGiftContribution(String eventCode, Integer giftId, UserGiftDTO userGiftDTO);

}
