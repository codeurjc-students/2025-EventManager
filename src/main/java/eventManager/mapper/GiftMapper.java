package eventManager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import eventManager.dto.GiftDTO;
import eventManager.dto.GiftExtendedDTO;
import eventManager.entity.Gift;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GiftMapper {

    @Mapping(target = "eventId", expression = "java(eventId)")
    GiftDTO convertGiftToGiftDTO(Gift gift, Integer eventId);

    @Mapping(target = "eventId", expression = "java(eventId)")
    @Mapping(target = "userContributionList", ignore = true)
    GiftExtendedDTO convertGiftToGiftExtendedDTO(Gift gift, Integer eventId);

}
