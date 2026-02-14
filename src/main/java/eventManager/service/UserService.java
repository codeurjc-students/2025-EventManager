package eventManager.service;

import eventManager.dto.UserDTO;
import eventManager.dto.UserPasswordDTO;
import eventManager.dto.UserUpdateDTO;
import eventManager.entity.User;

public interface UserService {
	/**
	 * Retrieves the user information.
	 *
	 * @param username the username of the user
	 * @return the user data transfer object
	 */
	UserDTO getUserInformationByUsername(String username);

	/**
	 * Retrieves the user information.
	 *
	 * @param userId the ID of the user
	 * @return the user data transfer object
	 */
	UserDTO getUserInformation(Integer userId);

	/**
	 * Retrieves the user entity.
	 *
	 * @param userId the ID of the user
	 * @return the user entity
	 */
	User getUser(Integer userId);

	/**
	 * Deletes a user.
	 *
	 * @param userId the ID of the user
	 * @return the deleted user data transfer object
	 */
	UserDTO deleteUser(Integer userId);

	/**
	 * Updates the user information.
	 *
	 * @param userId          the ID of the user
	 * @param userDTO the user data transfer object
	 * @return the updated user data transfer object
	 */
	UserDTO updateUser(Integer userId, UserUpdateDTO updateUserDTO);

	/**
	 * Updates the password of a user.
	 *
	 * @param userId          the ID of the user
	 * @param userPasswordDTO the user password data transfer object
	 * @return the updated user data transfer object
	 */
	UserDTO updateUserPassword(Integer userId, UserPasswordDTO userPasswordDTO);

}
