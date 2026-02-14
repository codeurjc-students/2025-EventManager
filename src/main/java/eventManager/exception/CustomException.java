package eventManager.exception;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

@Data
@Builder
@lombok.EqualsAndHashCode(callSuper = true)
public class CustomException extends RuntimeException {

	/*
	 * Clase para el manejo de las excepciones personalizadas
	 */

	private static final long serialVersionUID = 1L;

	private final HttpStatus status;

	@Nullable
	private final String message;

	public CustomException(HttpStatus status, String message) {
		super();
		this.status = status;
		this.message = message;
	}

}
