package eventManager.web.controller;

import eventManager.service.GiftService;
import eventManager.api.GiftApi;
import eventManager.dto.GiftCreateDTO;
import eventManager.dto.GiftDTO;
import eventManager.dto.GiftExtendedDTO;
import eventManager.dto.GiftUpdateDTO;
import eventManager.dto.ResultPaginationDTO;
import eventManager.dto.UserGiftDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@Slf4j
public class GiftController implements GiftApi{

	@Autowired
	private GiftService giftService;

	@Override
	public ResponseEntity<GiftDTO> createGift(String eventCode, @Valid GiftCreateDTO giftCreateDTO) {
		log.info("process=create-gift");
		return new ResponseEntity<>(giftService.createGift(eventCode, giftCreateDTO), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<GiftExtendedDTO> getGiftInformation(String eventCode, Integer giftId) {
		log.info("process=get-gift-information");
		return new ResponseEntity<>(giftService.getGiftInformation(eventCode, giftId), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ResultPaginationDTO> getGifts(String eventCode, @NotNull @Valid Integer page, @Valid Integer pageSize, @Valid String sortBy, @Valid String sortDir, @Valid String search) {
		log.info("process=get-gifts");
		return new ResponseEntity<>(giftService.getGifts(eventCode, page, pageSize, sortBy, sortDir, search), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<GiftExtendedDTO> updateGift(String eventCode, Integer giftId, @Valid GiftUpdateDTO giftUpdateDTO) {
		log.info("process=update-gift");
		return new ResponseEntity<>(giftService.updateGift(eventCode, giftId, giftUpdateDTO), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<GiftDTO> deleteGift(String eventCode, Integer giftId) {
		log.info("process=delete-gift");
		return new ResponseEntity<>(giftService.deleteGift(eventCode, giftId), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<GiftExtendedDTO> createUpdateGiftContribution(String eventCode, Integer giftId, @Valid UserGiftDTO userGiftDTO) {
		log.info("process=create-update-gift-contribution");
		return new ResponseEntity<>(giftService.createUpdateGiftContribution(eventCode, giftId, userGiftDTO), HttpStatus.CREATED);
	}
	
}