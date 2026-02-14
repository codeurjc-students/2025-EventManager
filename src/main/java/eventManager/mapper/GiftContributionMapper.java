package eventManager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import eventManager.dto.UserGiftContributionDTO;
import eventManager.entity.GiftContribution;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GiftContributionMapper {

    @Mapping(target="userId", expression="java(giftContribution.getUserId().getUserId())")
    @Mapping(target="username", expression="java(giftContribution.getUserId().getUsername())")
    @Mapping(target="email", expression="java(giftContribution.getUserId().getEmail())")
    @Mapping(target="phoneNumber", expression="java(giftContribution.getUserId().getPhoneNumber())")
    @Mapping(target="amount", expression="java(giftContribution.getContribution())")
    UserGiftContributionDTO convertGiftContributionToUserGiftContributionDTO(GiftContribution giftContribution);

}
