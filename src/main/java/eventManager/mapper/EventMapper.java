package eventManager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import eventManager.dto.CreateUpdateEventDTO;
import eventManager.dto.EventDTO;
import eventManager.dto.EventWithTicketDTO;
import eventManager.entity.Event;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

    EventDTO convertEventToEventDTO(Event event);

    @Mapping(target = "ticketId", ignore = true)
    EventWithTicketDTO convertEventToEventWithTicketDTO(Event event);

    @Mapping(target = "eventId", ignore = true)
    Event convertCreateUpdateEventDTOToEvent(CreateUpdateEventDTO eventDTO);

}
