package eventManager.web.controller;

import eventManager.service.UserService;
import eventManager.api.UserApi;
import eventManager.dto.UserDTO;
import eventManager.dto.UserPasswordDTO;
import eventManager.dto.UserUpdateDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@Slf4j
public class UserController implements UserApi{
	
	@Autowired
	private UserService userService;

	@Override
	public ResponseEntity<UserDTO> getAuthenticatedUserProfile() {
		log.info("process=get-authenticated-user-profile");
		
		// Obtener el usuario autenticado desde el SecurityContext
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		
		return new ResponseEntity<>(userService.getUserInformationByUsername(username), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<UserDTO> getUserInformationByUsername(@NotNull @Valid String username) {
		log.info("process=get-user-information");
		return new ResponseEntity<>(userService.getUserInformationByUsername(username), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<UserDTO> getUserInformation(Integer userId) {
		log.info("process=get-user-information");
		return new ResponseEntity<>(userService.getUserInformation(userId), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<UserDTO> updateUser(Integer userId, @Valid UserUpdateDTO updateUserDTO) {
		log.info("process=update-user");
		return new ResponseEntity<>(userService.updateUser(userId, updateUserDTO), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<UserDTO> updateUserPassword(Integer userId, @Valid UserPasswordDTO userPasswordDTO) {
		log.info("process=update-user-password");
		return new ResponseEntity<>(userService.updateUserPassword(userId, userPasswordDTO), HttpStatus.OK);
	}
	
}
