package eventManager.security;

import eventManager.constant.Constantes;
import eventManager.entity.User;
import eventManager.entity.UserRole;
import eventManager.repository.UserRepository;
import eventManager.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UserDetailsServiceImpl, que carga los datos del usuario desde el repositorio para la autenticacion de Spring Security.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Tests")
class UserDetailsServiceImplTest {

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .email("carlos.martinez@eventmanager.es")
                .username("carlos.martinez")
                .password("encodedPassword")
                .firstName("Carlos")
                .lastName("Martinez")
                .phoneNumber("612345678")
                .role(UserRole.USER)
                .build();
    }

    /**
     * Verifica que se retorna un UserDetails con el rol correcto cuando el usuario existe.
     */
    @Test
    @DisplayName("loadUserByUsername - Usuario encontrado retorna UserDetails con ROLE_USER")
    void testLoadUserByUsername_Success() {
        when(userRepository.findByUsername("carlos.martinez")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("carlos.martinez");

        assertNotNull(userDetails);
        assertEquals("carlos.martinez", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(userRepository).findByUsername("carlos.martinez");
    }

    /**
     * Verifica que se lanza UsernameNotFoundException cuando el usuario no existe en el repositorio.
     */
    @Test
    @DisplayName("loadUserByUsername - Usuario no encontrado lanza UsernameNotFoundException")
    void testLoadUserByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("nonexistent"));

        assertEquals(Constantes.MESSAGE_USER_NOT_REGISTERED, exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }
}
