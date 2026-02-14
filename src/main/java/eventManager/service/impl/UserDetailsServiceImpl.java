package eventManager.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import eventManager.constant.Constantes;
import eventManager.entity.User;
import eventManager.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(Constantes.MESSAGE_USER_NOT_REGISTERED));

		List<GrantedAuthority> roles = new ArrayList<>();
		/* for (String role : user.getRoles()) {
			roles.add(new SimpleGrantedAuthority("ROLE_" + role));
		} */
		roles.add(new SimpleGrantedAuthority("ROLE_USER")); // Assuming all users have the USER role

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), roles);
	}
	
}
