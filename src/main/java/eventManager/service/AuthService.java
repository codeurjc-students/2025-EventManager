package eventManager.service;

import org.springframework.web.bind.annotation.CookieValue;

import eventManager.dto.UserCreateDTO;
import eventManager.dto.UserDTO;
import eventManager.dto.UserForgottenPassword;
import eventManager.security.jwt.AuthResponse;
import eventManager.security.jwt.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
	
	/**
	 * Registers a new user.
	 * @param userCreateDTO the user creation data transfer object
	 * @param response the HTTP response to set cookies
	 * @return a response entity containing the authentication response
	 */
	AuthResponse registerUser(UserCreateDTO userCreateDTO, HttpServletResponse response);

    /**
	 * Logs in a user.
	 * @param loginRequest the login request containing username and password
     * @param response the HTTP response to set cookies
	 * @return a response entity containing the authentication response
	 */
	AuthResponse login(LoginRequest loginRequest, HttpServletResponse response);

    /**
     * Refreshes the authentication token using the refresh token stored in a cookie.
	 * * @param refreshToken the refresh token from the cookie
	 * @param response the HTTP response to set cookies
	 * @return a response entity containing the new authentication token
     */
	AuthResponse refreshToken(@CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response);

	/**
	 * Logs out the user by invalidating the session and clearing cookies.
	 * @param response the HTTP response to clear cookies
	 * @return a response entity indicating the logout status
	 */
	AuthResponse logout(HttpServletResponse response);

	/**
	 * Changes the user's forgotten password.
	 * @param userForgotenPassword the user forgotten password data transfer object
	 * @return a response entity containing the updated user information
	 */
	UserDTO changeForgottenPassword(UserForgottenPassword userForgotenPassword);

}
