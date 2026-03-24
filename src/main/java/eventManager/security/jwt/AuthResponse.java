package eventManager.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

	private Status status;
	private String message;
	private String error;

	public AuthResponse(Status status, String message) {
		this.status = status;
		this.message = message;
	}

	public enum Status {
		SUCCESS, FAILURE
	}

}
