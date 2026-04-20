package eventManager.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import eventManager.security.jwt.JwtRequestFilter;
import eventManager.service.impl.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class RestSecurityConfig {

	@Autowired
	public UserDetailsServiceImpl userDetailService;

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

		authProvider.setUserDetailsService(userDetailService);
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.authenticationProvider(authenticationProvider());

		http
				.authorizeHttpRequests(authorize -> authorize
						// PUBLIC ENDPOINTS - Frontend static files and SPA routes
						.requestMatchers("/", "/index.html", "/static/**", "/assets/**").permitAll()
						.requestMatchers("/registro", "/iniciar-sesion", "/clave-olvidada").permitAll()
						.requestMatchers("/usuario/**", "/eventos/**", "/evento/**", "/crear-evento", "/inscribirse-evento", "/regalo/**").permitAll() // SPA routes
						// PUBLIC ENDPOINTS - Test and diagnostic pages
						.requestMatchers("/test", "/test.html", "/api/health").permitAll()
						// PUBLIC ENDPOINTS - Authentication API endpoints (no JWT required)
						.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/refresh").permitAll()
						// PUBLIC ENDPOINTS - API documentation
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
						// PRIVATE ENDPOINTS - All API routes require authentication (JWT in cookies)
						.requestMatchers("/api/**").authenticated()
						// All other requests require authentication
						.anyRequest().authenticated());

		// Enable CORS
		http.cors(cors -> cors.configurationSource(request -> {
			var corsConfig = new org.springframework.web.cors.CorsConfiguration();
			corsConfig.setAllowedOriginPatterns(java.util.List.of("*"));
			corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
			corsConfig.setAllowedHeaders(java.util.List.of("*"));
			corsConfig.setAllowCredentials(true);
			return corsConfig;
		}));

		// Disable Form login Authentication
		http.formLogin(formLogin -> formLogin.disable());

		// Disable CSRF protection (it is difficult to implement in REST APIs)
		http.csrf(csrf -> csrf.disable());

		// Disable Basic Authentication
		http.httpBasic(httpBasic -> httpBasic.disable());

		// Stateless session
		http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// Add JWT Token filter
		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
