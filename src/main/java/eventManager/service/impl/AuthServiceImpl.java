package eventManager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import eventManager.constant.Constantes;
import eventManager.dto.UserCreateDTO;
import eventManager.dto.UserDTO;
import eventManager.dto.UserForgottenPassword;
import eventManager.entity.User;
import eventManager.entity.UserRole;
import eventManager.exception.CustomException;
import eventManager.mapper.UserMapper;
import eventManager.repository.UserRepository;
import eventManager.security.jwt.AuthResponse;
import eventManager.security.jwt.JwtTokenProvider;
import eventManager.security.jwt.LoginRequest;
import eventManager.security.jwt.TokenType;
import eventManager.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtTokenProvider jwtTokenProvider;
	
	@Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

	@Autowired
    private UserMapper userMapper;

	public AuthServiceImpl(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

    public AuthResponse registerUser(UserCreateDTO userCreateDTO, HttpServletResponse response) {
        try{
            // Validamos que el usuario no esté ya registrado
            if (Boolean.TRUE.equals(userRepository.existsByEmailOrUsername(userCreateDTO.getEmail(), userCreateDTO.getUsername()))) {
                throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_ALREADY_REGISTERED);
            }
			
            // Creamos y guardamos en BBDD el nuevo usuario
            User newUser = User.builder()
                    .email(userCreateDTO.getEmail())
                    .username(userCreateDTO.getUsername())
                    .password(passwordEncoder.encode(userCreateDTO.getPassword()))
                    .firstName(userCreateDTO.getFirstName())
                    .lastName(userCreateDTO.getLastName())
                    .phoneNumber(userCreateDTO.getPhoneNumber())
                    .role(UserRole.USER)
                    .build();
            User userCreated = userRepository.save(newUser);
			
			UserDetails userDetails = userDetailsService.loadUserByUsername(userCreated.getUsername());
			
			var newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
			var newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

			response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));
			response.addCookie(buildTokenCookie(TokenType.REFRESH, newRefreshToken));

			return new AuthResponse(AuthResponse.Status.SUCCESS, "Register successful. Tokens are created in cookie.");
        }
        catch (CustomException e) {
            log.error("Error en el registro de usuario: {}", e.getMessage());
            throw new CustomException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado durante el registro: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

	public AuthResponse login(LoginRequest loginRequest, HttpServletResponse response) {
		try {
			Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			String username = loginRequest.getUsername();
			UserDetails user = userDetailsService.loadUserByUsername(username);
			var newAccessToken = jwtTokenProvider.generateAccessToken(user);
			var newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

			response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));
			response.addCookie(buildTokenCookie(TokenType.REFRESH, newRefreshToken));

			return new AuthResponse(AuthResponse.Status.SUCCESS, "Auth successful. Tokens are created in cookie.");
		} catch (Exception e) {
			log.error("Error en la autenticación: {}", e.getMessage());
			throw new CustomException(HttpStatus.UNAUTHORIZED, Constantes.MESSAGE_INCORRECT_USER_OR_PASSWORD);
		}
	}

	public AuthResponse refreshToken(String refreshToken, HttpServletResponse response) {
		try {
			var claims = jwtTokenProvider.validateToken(refreshToken);
			UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());

			var newAccessToken = jwtTokenProvider.generateAccessToken(user);
			response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));

			return new AuthResponse(AuthResponse.Status.SUCCESS, "Auth successful. Tokens are created in cookie.");
		} catch (Exception e) {
			log.error("Error al renovar token de sesión: {}", e.getMessage());
			return new AuthResponse(AuthResponse.Status.FAILURE, "Failure while processing refresh token");
		}
	}

	private Cookie buildTokenCookie(TokenType type, String token) {
		Cookie cookie = new Cookie(type.cookieName, token);
		cookie.setMaxAge((int) type.duration.getSeconds());
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setSecure(false); // En producción debería ser true (HTTPS)
		// Para desarrollo local con CORS, usamos SameSite=Lax
		cookie.setAttribute("SameSite", "Lax");
		return cookie;
	}

	public AuthResponse logout(HttpServletResponse response) {
		SecurityContextHolder.clearContext();
		response.addCookie(removeTokenCookie(TokenType.ACCESS));
		response.addCookie(removeTokenCookie(TokenType.REFRESH));

		return new AuthResponse(AuthResponse.Status.SUCCESS, "LogOut successful. Tokens removed from cookie.");
	}

	private Cookie removeTokenCookie(TokenType type){
		Cookie cookie = new Cookie(type.cookieName, "");
		cookie.setMaxAge(0);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setSecure(false); // En producción debería ser true (HTTPS)
		cookie.setAttribute("SameSite", "Lax");
		return cookie;
	}

	@Override
	public UserDTO changeForgottenPassword(UserForgottenPassword userForgotenPassword) {
		// Validamos que el usuario esté registrado, que los nombres de usuario coincidan y que las contraseñas proprocionadas coincidan
		User user = userRepository.findByEmail(userForgotenPassword.getEmail())
				.orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, Constantes.MESSAGE_USER_NOT_REGISTERED));

		if (!String.valueOf(user.getUsername()).equals(String.valueOf(userForgotenPassword.getUsername()))) {
			throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USERNAME_DOES_NOT_MATCH);
		}

		// Validamos que las contraseñas nuevas coincidan
        if (!userForgotenPassword.getNewPassword().equals(userForgotenPassword.getNewPasswordConfirm())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_PASSWORDS_DO_NOT_MATCH);
        }

		user.setPassword(passwordEncoder.encode(userForgotenPassword.getNewPassword()));
		return userMapper.convertUserToUserDTO(userRepository.save(user));
	}
	
}
