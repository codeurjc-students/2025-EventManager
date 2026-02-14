package eventManager.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eventManager.dto.UserCreateDTO;
import eventManager.dto.UserDTO;
import eventManager.dto.UserForgottenPassword;
import eventManager.security.jwt.AuthResponse;
import eventManager.service.AuthService;
import eventManager.security.jwt.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {
	
	@Autowired
	private AuthService authService;
	
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody UserCreateDTO userCreateDTO, HttpServletResponse response) {
		log.debug("process=register-user");
		return new ResponseEntity<>(authService.registerUser(userCreateDTO, response), HttpStatus.OK);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
		log.debug("process=login-user");
		return new ResponseEntity<>(authService.login(loginRequest, response), HttpStatus.OK);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refreshToken(@CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response) {
		log.debug("process=refresh-token");
		return new ResponseEntity<>(authService.refreshToken(refreshToken, response), HttpStatus.OK);
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logout(HttpServletResponse response) {
		log.debug("process=logut-user");
		return new ResponseEntity<>(authService.logout(response), HttpStatus.OK);
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<UserDTO> changeForgottenPassword(@Valid UserForgottenPassword userForgotenPassword) {
		log.debug("process=change-forgotten-password");
		return new ResponseEntity<>(authService.changeForgottenPassword(userForgotenPassword), HttpStatus.OK);
	}

}
