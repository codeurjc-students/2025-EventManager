package eventManager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import eventManager.dto.EventDTO;
import eventManager.dto.EventTicketDTO;
import eventManager.dto.TicketDTO;
import eventManager.entity.Ticket;


@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TicketMapper {

    TicketDTO convertTicketToTicketDTO(Ticket ticket);

    @Mapping(target = "event", source = "event")
	@Mapping(target = "ticket", source = "ticket")
	EventTicketDTO convertTicketDTOAndEventDTOToEventTicketDTO(TicketDTO ticket, EventDTO event);

}
