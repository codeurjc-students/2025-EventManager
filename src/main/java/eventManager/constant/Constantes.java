package eventManager.constant;

public class Constantes {

	private Constantes() { }

	//Authorization Header
	public static final String AUTHORIZATION_HEADER = "Authorization";

	//Path Actuator
	public static final String PATH_ACTUATOR = "/actuator/";
	
	// Mensajes de error
	public static final String MESSAGE_INTERNAL_SERVER_ERROR = "Internal Server Error";
	public static final String MESSAGE_INCORRECT_USER_OR_PASSWORD = "El usuario o la contraseña son incorrectos";
	public static final String MESSAGE_USER_NOT_REGISTERED = "El usuario no está registrado en la aplicación";
	public static final String MESSAGE_USER_ALREADY_REGISTERED = "El email o el nombre de usuario introducidos ya están registrado en la aplicación";
	public static final String MESSAGE_USERNAME_DOES_NOT_MATCH = "El nombre de usuario proporcionado no coincide con el registrado en la aplicación";
	public static final String MESSAGE_INCORRECT_PASSWORD = "La contraseña actual proporcionada es incorrecta";
	public static final String MESSAGE_PASSWORDS_DO_NOT_MATCH = "Las contraseñas nuevas no coinciden";
	public static final String MESSAGE_EVENT_DOES_NOT_EXIST = "El evento no existe";
	public static final String MESSAGE_USER_NOT_REGISTERED_IN_EVENT = "El usuario no está registrado en el evento";
	public static final String MESSAGE_EVENT_CODE_ALREADY_EXISTS = "El código del evento ya existe";
	public static final String MESSAGE_USER_ALREADY_REGISTERED_IN_EVENT = "El usuario ya está registrado en el evento";
	public static final String MESSAGE_TICKET_DOES_NOT_EXIST = "La entrada no existe";
	public static final String MESSAGE_GIFT_ALREADY_EXISTS = "Un regalo con  ese nombre ya existe en el evento";
	public static final String MESSAGE_GIFT_DOES_NOT_EXIST = "El regalo no existe";
	public static final String MESSAGE_GIFT_CONTRIBUTION_POSITIVE = "La aportación al regalo debe ser positiva";
	public static final String MESSAGE_FORBIDDEN_ACCESS = "No tienes permisos para acceder a este recurso";
	public static final String MESSAGE_USER_NOT_HOST = "El usuario no es anfitrión del evento";
	public static final String MESSAGE_GIFT_UPDATE_FORBIDDEN = "Solo el anfitrión del evento o el creador del regalo pueden actualizarlo";

	public static final String PATTERN_YYYY_MM_DD = "yyyy-MM-dd";

	// Filtros de búsqueda
	public static final String LIKE = "like";
	public static final String EQUAL = "=";
	public static final String GREATER_THAN = ">";
	public static final String GREATER_EQUAL = ">=";
	public static final String LESS_THAN = "<";
	public static final String LESS_EQUAL = "<=";
	// Patrón actualizado para soportar LocalDateTime (ej: 2025-11-27T23:59:59) con coma como separador
	public static final String PATTERN_SEARCH ="([A-Za-z.]+?)(=|<|>|<=|>=|like)([A-Za-z0-9ñÑáéíóúÁÉÍÓÚüÜ_;.\\-\\s\\(\\):T]+?),";

}
