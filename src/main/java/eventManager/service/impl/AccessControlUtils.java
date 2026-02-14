package eventManager.service.impl;

import eventManager.constant.Constantes;
import eventManager.dto.UserDTO;
import eventManager.entity.Ticket;
import eventManager.entity.User;
import eventManager.exception.CustomException;
import eventManager.repository.TicketRepository;
import eventManager.repository.UserRepository;
import eventManager.service.TicketService;
import eventManager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utilidad centralizada para el control de acceso (Access Control)
 * Proporciona métodos reutilizables para validar permisos y propiedad de recursos
 */
@Component
@Slf4j
public class AccessControlUtils {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Lazy
    @Autowired
    private UserService userService;

    @Lazy
    @Autowired
    private TicketService ticketService;

    /**
     * Obtiene el username del usuario autenticado actualmente desde el contexto de seguridad
     * @return username del usuario autenticado
     */
    public String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * Obtiene el usuario autenticado actualmente
     * @return UserDTO del usuario autenticado
     * @throws CustomException si el usuario no se encuentra
     */
    public UserDTO getAuthenticatedUser() {
        String authenticatedUsername = getAuthenticatedUsername();
        return userService.getUserInformationByUsername(authenticatedUsername);
    }

    /**
     * Verifica si el userId proporcionado corresponde al usuario autenticado actualmente
     * @param userId ID del usuario a verificar
     * @throws CustomException con código 403 si el usuario no tiene permiso
     */
    public void validateUserOwnership(Integer userId) {
        String authenticatedUsername = getAuthenticatedUsername();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_USER_NOT_REGISTERED));
        
        if (!user.getUsername().equals(authenticatedUsername)) {
            log.warn("User {} attempted to access resources of user {}", authenticatedUsername, user.getUsername());
            throw new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS);
        }
    }

    /**
     * Verifica si el usuario autenticado es HOST del evento especificado
     * @param eventId ID del evento
     * @return true si el usuario es HOST, false en caso contrario
     */
    public boolean isUserHostOfEvent(Integer eventId) {
        String authenticatedUsername = getAuthenticatedUsername();
        User authenticatedUser = userRepository.findByUsername(authenticatedUsername)
            .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_USER_NOT_REGISTERED));
        
        // Verificar si el usuario tiene un ticket de HOST para este evento
        return ticketRepository.existsByEventId_EventIdAndUserId_UserIdAndRole(
            eventId, authenticatedUser.getUserId(), "HOST");
    }

    /**
     * Verifica si el usuario autenticado es HOST del evento especificado
     * @param eventId ID del evento
     * @throws CustomException con código 403 si el usuario no es HOST
     */
    public void validateUserIsHost(Integer eventId) {
        if (!isUserHostOfEvent(eventId)) {
            String authenticatedUsername = getAuthenticatedUsername();
            log.warn("User {} attempted to modify event {} without being HOST", authenticatedUsername, eventId);
            throw new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_HOST);
        }
    }

    /**
     * Verifica si el usuario autenticado está registrado en el evento especificado
     * @param eventId ID del evento
     * @throws CustomException con código 403 si el usuario no está registrado
     */
    public void validateUserRegisteredInEvent(Integer eventId) {
        String authenticatedUsername = getAuthenticatedUsername();
        UserDTO authenticatedUser = userService.getUserInformationByUsername(authenticatedUsername);
        
        try {
            // Intentar obtener el ticket del usuario para este evento
            ticketService.getTicketByEventAndUser(eventId, authenticatedUser.getUserId());
        } catch (CustomException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                log.warn("User {} attempted to access event {} without being registered", authenticatedUsername, eventId);
                throw new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_USER_NOT_REGISTERED_IN_EVENT);
            }
            throw e;
        }
    }

    /**
     * Verifica si el ticket pertenece al usuario autenticado o si el usuario es HOST del evento
     * @param ticketId ID del ticket
     * @throws CustomException con código 403 si el usuario no tiene permiso
     */
    public void validateTicketAccess(Integer ticketId) {
        String authenticatedUsername = getAuthenticatedUsername();
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_TICKET_DOES_NOT_EXIST));
        
        User authenticatedUser = userRepository.findByUsername(authenticatedUsername)
            .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_USER_NOT_REGISTERED));
        
        // Permitir acceso si el ticket pertenece al usuario o si el usuario es HOST del evento
        boolean isOwner = ticket.getUserId().getUserId().equals(authenticatedUser.getUserId());
        boolean isHost = isUserHostOfEvent(ticket.getEventId().getEventId());
        
        if (!isOwner && !isHost) {
            log.warn("User {} attempted to access ticket {} without permission", authenticatedUsername, ticketId);
            throw new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_FORBIDDEN_ACCESS);
        }
    }

    /**
     * Verifica si el usuario autenticado es HOST del evento (versión alternativa usando eventCode)
     * @param eventCode Código del evento
     * @throws CustomException con código 403 si el usuario no es HOST
     */
    public void validateUserIsHostByEventCode(String eventCode, Integer eventId) {
        validateUserIsHost(eventId);
    }

    /**
     * Verifica si el usuario autenticado es HOST del evento O es el creador del regalo
     * @param eventId ID del evento
     * @param giftCreatorUsername Username del creador del regalo
     * @throws CustomException con código 403 si el usuario no es HOST ni creador
     */
    public void validateHostOrGiftCreator(Integer eventId, String giftCreatorUsername) {
        String authenticatedUsername = getAuthenticatedUsername();
        
        // Verificar si el usuario es HOST del evento
        boolean isHost = isUserHostOfEvent(eventId);
        
        // Verificar si el usuario es el creador del regalo
        boolean isCreator = authenticatedUsername.equals(giftCreatorUsername);
        
        if (!isHost && !isCreator) {
            log.warn("User {} attempted to update gift created by {} without being HOST of event {}", 
                authenticatedUsername, giftCreatorUsername, eventId);
            throw new CustomException(HttpStatus.FORBIDDEN, Constantes.MESSAGE_GIFT_UPDATE_FORBIDDEN);
        }
    }
}
