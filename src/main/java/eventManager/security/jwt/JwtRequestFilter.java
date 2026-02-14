package eventManager.security.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
	
	private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

	private final UserDetailsService userDetailsService;

	private final JwtTokenProvider jwtTokenProvider;

	public JwtRequestFilter(UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider) {
		this.userDetailsService = userDetailsService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String requestPath = request.getRequestURI();
		
		// Skip JWT validation for public endpoints
		if (isPublicEndpoint(requestPath)) {
			if (!requestPath.startsWith("/assets/") && !requestPath.startsWith("/static/")) {
				log.info("Skipping JWT validation for public endpoint: {}", requestPath);
			}
			filterChain.doFilter(request, response);
			return;
		}
		
		try {
			var claims = jwtTokenProvider.validateToken(request, true);
			log.info("JWT Token validated successfully for user: {}", claims.getSubject());
			var userDetails = userDetailsService.loadUserByUsername(claims.getSubject());

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (IllegalArgumentException ex) {
			log.error("JWT Token validation failed for path {}: {}", requestPath, ex.getMessage());
			SecurityContextHolder.clearContext();
		} catch (Exception ex) {
			log.error("Exception processing JWT Token for path {}: ", requestPath, ex);
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}
	
	private boolean isPublicEndpoint(String requestPath) {
		// Public static resources
		if (requestPath.equals("/") ||
			requestPath.equals("/index.html") ||
			requestPath.equals("/test") ||
			requestPath.equals("/test.html") ||
			requestPath.equals("/api/health") ||
			requestPath.startsWith("/static/") ||
			requestPath.startsWith("/assets/") ||
			requestPath.startsWith("/css/") ||
			requestPath.startsWith("/js/") ||
			requestPath.startsWith("/img/") ||
			requestPath.startsWith("/images/") ||
			requestPath.startsWith("/fonts/") ||
			requestPath.startsWith("/swagger-ui/") ||
			requestPath.equals("/swagger-ui.html") ||
			requestPath.startsWith("/v3/api-docs/") ||
			requestPath.equals("/favicon.ico")) {
			return true;
		}
		
		// Public authentication API endpoints
		if (requestPath.equals("/api/auth/register") ||
			requestPath.equals("/api/auth/login") ||
			requestPath.equals("/api/auth/forgot-password")) {
			return true;
		}
		
		// Frontend SPA routes (not API) - these are handled by HomeController
		// Any path that doesn't start with /api is a frontend SPA route
		if (!requestPath.startsWith("/api")) {
			return true;
		}
		
		// All other /api/** routes require JWT authentication
		return false;
	}

}
