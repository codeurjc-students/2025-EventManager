package eventManager.web.controller;

import eventManager.service.EventService;
import eventManager.api.EventApi;
import eventManager.dto.CreateUpdateEventDTO;
import eventManager.dto.EventDTO;
import eventManager.dto.ResultPaginationDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@Slf4j
public class EventController implements EventApi{

	@Autowired
	private EventService eventService;

	@Override
	public ResponseEntity<ResultPaginationDTO> getEvents(@NotNull @Valid Integer page, @NotNull @Valid Integer userId, @NotNull @Valid String role, @Valid Integer pageSize, @Valid String sortBy, @Valid String sortDir, @Valid String search) {
		log.debug("process=get-events");
		return new ResponseEntity<>(eventService.getEvents(page, pageSize, sortBy, sortDir, search, userId, role), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<EventDTO> getEventByCode(String eventCode) {
		log.debug("process=get-event-by-code eventCode={}", eventCode);
		return new ResponseEntity<>(eventService.getEvent(eventCode), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<EventDTO> createEvent(@NotNull @Valid Integer userId, @Valid CreateUpdateEventDTO createUpdateEventDTO) {
		log.debug("process=create-event");
		return new ResponseEntity<>(eventService.createEvent(userId, createUpdateEventDTO), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<EventDTO> updateEvent(String eventCode, @Valid CreateUpdateEventDTO createUpdateEventDTO) {
		log.debug("process=update-event eventCode={}", eventCode);
		return new ResponseEntity<>(eventService.updateEvent(eventCode, createUpdateEventDTO), HttpStatus.OK);
	}
		
}