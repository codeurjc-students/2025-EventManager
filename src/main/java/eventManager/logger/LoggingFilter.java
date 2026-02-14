package eventManager.logger;

import eventManager.constant.Constantes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;

@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

	private static final int MAX_PAYLOAD_LENGTH = 1000; // Límite de caracteres para el log
	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String requestURI = request.getRequestURI();
		
		// Excluir actuator y archivos estáticos
		if(shouldNotFilter(requestURI)) {
			filterChain.doFilter(request, response);
			return;
		}
		
		if(log.isDebugEnabled()) {
			log.debug("Request URL::" + request.getRequestURL().toString() + ":: Start Time=" + Instant.now());

			ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
			ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

			String requestBody = getRequestData(wrappedRequest);
			if(requestBody != null && !requestBody.isEmpty()) {
				log.debug("Request Body: " + truncateIfNeeded(requestBody));
			}

			filterChain.doFilter(wrappedRequest, wrappedResponse);

			String responseBody = getResponseData(wrappedResponse);
			if(responseBody != null && !responseBody.isEmpty() && !isStaticContent(response)) {
				log.debug("Response Body: " + truncateIfNeeded(responseBody));
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}
	
	/**
	 * Determina si la petición debe ser excluida del logging
	 */
	private boolean shouldNotFilter(String requestURI) {
		return requestURI.startsWith(Constantes.PATH_ACTUATOR) ||
			   requestURI.endsWith(".css") ||
			   requestURI.endsWith(".js") ||
			   requestURI.endsWith(".map") ||
			   requestURI.endsWith(".html") ||
			   requestURI.endsWith(".ico") ||
			   requestURI.endsWith(".png") ||
			   requestURI.endsWith(".jpg") ||
			   requestURI.endsWith(".jpeg") ||
			   requestURI.endsWith(".gif") ||
			   requestURI.endsWith(".svg") ||
			   requestURI.endsWith(".woff") ||
			   requestURI.endsWith(".woff2") ||
			   requestURI.endsWith(".ttf") ||
			   requestURI.endsWith(".eot") ||
			   requestURI.contains("/assets/") ||
			   requestURI.contains("/static/");
	}
	
	/**
	 * Verifica si el response es contenido estático
	 */
	private boolean isStaticContent(HttpServletResponse response) {
		String contentType = response.getContentType();
		if(contentType == null) return false;
		
		return contentType.contains("text/css") ||
			   contentType.contains("text/javascript") ||
			   contentType.contains("application/javascript") ||
			   contentType.contains("text/html") ||
			   contentType.contains("image/");
	}
	
	/**
	 * Trunca el contenido si excede el límite
	 */
	private String truncateIfNeeded(String content) {
		if(content.length() > MAX_PAYLOAD_LENGTH) {
			return content.substring(0, MAX_PAYLOAD_LENGTH) + "... [truncated]";
		}
		return content;
	}

	/**
	 * Método que obtiene el payload de la petición.
	 *
	 * @param request
	 * @return
	 * @throws IOException
	 */
	private String getRequestData(ContentCachingRequestWrapper request) {
		String payload = null;
		ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				try {
					payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
				} catch (UnsupportedEncodingException ex) {
					payload = "[unknown]";
				}
			}
		}
		return payload;
	}

	/**
	 * Método que obtiene el payload de la respuesta.
	 *
	 * @param response
	 * @return
	 * @throws IOException
	 */
	private String getResponseData(ContentCachingResponseWrapper response) throws IOException {
		String payload = null;
		ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response,
				ContentCachingResponseWrapper.class);
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
				wrapper.copyBodyToResponse();
			}
		}
		return payload;
	}

}
