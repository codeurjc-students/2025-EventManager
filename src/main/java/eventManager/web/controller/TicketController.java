package eventManager.web.controller;

import eventManager.service.TicketService;
import eventManager.api.TicketApi;
import eventManager.dto.EnrollUserDTO;
import eventManager.dto.EventTicketDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.dto.TicketDTO;
import eventManager.dto.UpdateTicketDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@Slf4j
public class TicketController implements TicketApi{

	@Autowired
	private TicketService ticketService;

	@Override
	public ResponseEntity<EventTicketDTO> getEventInformation(String eventCode, Integer ticketId, Integer userId) {
		log.debug("process=get-event-information");
		return new ResponseEntity<>(ticketService.getEventInformation(eventCode, ticketId, userId), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ResultPaginationDTO> getEventTickets(String eventCode, Integer page, Integer pageSize, String sortBy, String sortDir, String search) {
		log.debug("process=get-event-tickets");
		return new ResponseEntity<>(ticketService.getEventTickets(eventCode, page, pageSize, sortBy, sortDir, search), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<TicketDTO> enrollUserInEvent(@Valid EnrollUserDTO enrollUserInEvent) {
		log.debug("process=enroll-user-in-event");
		return new ResponseEntity<>(ticketService.enrollUserInEvent(enrollUserInEvent), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<EventTicketDTO> updateTicket(String eventCode, Integer ticketId, @Valid UpdateTicketDTO updateTicketDTO) {
		log.debug("process=update-ticket-information");
		return new ResponseEntity<>(ticketService.updateTicket(eventCode, ticketId, updateTicketDTO), HttpStatus.OK);
	}

}
