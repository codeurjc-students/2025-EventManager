package eventManager.service.impl;

import eventManager.constant.Constantes;
import eventManager.dto.UserDTO;
import eventManager.dto.UserPasswordDTO;
import eventManager.dto.UserUpdateDTO;
import eventManager.entity.User;
import eventManager.exception.CustomException;
import eventManager.mapper.UserMapper;
import eventManager.repository.UserRepository;
import eventManager.service.UserService;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
//import jakarta.servlet.http.HttpServletResponse;

@Service
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
	private UserMapper userMapper;

    @Autowired
    private AccessControlUtils accessControlUtils;

    //@Autowired
    //private UserLoginService userLoginService;

    @Override
    public UserDTO getUserInformationByUsername(String username) {
        try{
            // Validamos que el usuario esté registrado y obtenemos su información
            User user = userRepository.findByUsername(username).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));
            return userMapper.convertUserToUserDTO(user);
        } 
        catch (CustomException e) {
            log.error("Error al obtener la información mediante el nombre de usuario del usuario: {}", e.getMessage());
            throw new CustomException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al buscar usuario por username: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public UserDTO getUserInformation(Integer userId) {
        try{
            // Validar que el usuario solo puede acceder a su propia información
            accessControlUtils.validateUserOwnership(userId);
            
            // Validamos que el usuario esté registrado y obtenemos su información
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));
            return userMapper.convertUserToUserDTO(user);
        } 
        catch (CustomException e) {
            log.error("Error al obtener la información del usuario: {}", e.getMessage());
            throw new CustomException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al buscar usuario: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public User getUser(Integer userId) {
        try{
            // Validamos que el usuario esté registrado y obtenemos su información
           return userRepository.findById(userId).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));
        } 
        catch (CustomException e) {
            log.error("Error al obtener la información del usuario: {}", e.getMessage());
            throw new CustomException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al buscar usuario: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public UserDTO updateUser(Integer userId, UserUpdateDTO updateUserDTO) {
        try{
            // Validar que el usuario solo puede actualizar su propia información
            accessControlUtils.validateUserOwnership(userId);
            
            // Validamos que el usuario esté registrado y obtenemos su información
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));
            
            // Actualizamos la información del usuario y la guardamos en BBDD
            user.setFirstName(updateUserDTO.getFirstName());
            user.setLastName(updateUserDTO.getLastName());
            user.setPhoneNumber(updateUserDTO.getPhoneNumber());
            userRepository.save(user);

            return userMapper.convertUserToUserDTO(user);
        } 
        catch (CustomException e) {
            log.error("Error al actualizar la información del usuario: {}", e.getMessage());
            throw new CustomException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado durante la actualización del usuario: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public UserDTO deleteUser(Integer userId) {
        try{
            // Validamos que el usuario esté registrado y obtenemos su información
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));

            // Invalidate user session by logging out
            // HttpServletResponse response = new HttpServletResponse() {
                // Mock implementation or use a real response object in a real application
            //};
            //userLoginService.logoutUser(response);

            userRepository.delete(user);
            return userMapper.convertUserToUserDTO(user);
        } 
        catch (CustomException e) {
            log.error("Error al eliminar al usuario: {}", e.getMessage());
            throw new CustomException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al eliminar el usuario: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public UserDTO updateUserPassword(Integer userId, UserPasswordDTO userPasswordDTO) {
        try{
            // Validar que el usuario solo puede cambiar su propia contraseña
            accessControlUtils.validateUserOwnership(userId);
            
            // Validamos que el usuario esté registrado y obtenemos su información
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_USER_NOT_REGISTERED));
            
            // Validamos que la contraseña actual sea correcta
            if (!passwordEncoder.matches(userPasswordDTO.getPassword(), user.getPassword())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_INCORRECT_PASSWORD);
            }

            // Validamos que las contraseñas nuevas coincidan
            if (!userPasswordDTO.getNewPassword().equals(userPasswordDTO.getNewPasswordConfirm())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, Constantes.MESSAGE_PASSWORDS_DO_NOT_MATCH);
            }

            // Actualizamos la contraseña y la guardamos en BBDD
            user.setPassword(passwordEncoder.encode(userPasswordDTO.getNewPassword()));
            userRepository.save(user);

            return userMapper.convertUserToUserDTO(user);
        } 
        catch (CustomException e) {
            log.error("Error al actualizar la información del usuario: {}", e.getMessage());
            throw new CustomException(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al cambiar la contraseña: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
