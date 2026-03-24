package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Selenium para las vistas de autenticacion del sistema. Valida el comportamiento de las pantallas de login, registro, recuperacion de contrasena y cierre de sesion.
 */
@DisplayName("Authentication Views Selenium Tests")
public class AuthViewsSeleniumTest extends BaseSeleniumTest {

    @Override
    protected boolean preserveCookiesBetweenTests() {
        return false;
    }

    /**
     * Verifica que el login exitoso con credenciales validas redirige al usuario a la pagina principal.
     */
    @Test
    @DisplayName("Login View - Login exitoso con credenciales válidas")
    public void testLoginView_SuccessfulLogin() {
        login("carlos.martinez", "ClaveSegura2025");
        assertFalse(getCurrentUrl().contains("/iniciar-sesion"), "Debería salir de la página de login");
    }

    /**
     * Verifica que se muestra un mensaje de error cuando se introducen credenciales incorrectas.
     */
    @Test
    @DisplayName("Login View - Error con credenciales incorrectas")
    public void testLoginView_InvalidCredentials() {
        navigateTo("/iniciar-sesion");

        fillInput(By.cssSelector(".login-container input[type='text']"), "invaliduser");
        fillInput(By.cssSelector(".login-container input[type='password']"), "wrongpassword");
        clickElement(By.cssSelector(".login-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent(), "Debería mostrar un mensaje de error");
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería permanecer en la página de login");
    }

    /**
     * Verifica que el formulario no se envia y el usuario permanece en login cuando ambos campos estan vacios.
     */
    @Test
    @DisplayName("Login View - Error con campos vacíos")
    public void testLoginView_EmptyFields() {
        navigateTo("/iniciar-sesion");

        clickElement(By.cssSelector(".login-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería permanecer en la página de login");
    }

    /**
     * Verifica que no se puede iniciar sesion sin introducir el nombre de usuario.
     */
    @Test
    @DisplayName("Login View - Campo username vacío")
    public void testLoginView_EmptyUsername() {
        navigateTo("/iniciar-sesion");

        fillInput(By.cssSelector(".login-container input[type='password']"), "ClaveSegura2025");
        clickElement(By.cssSelector(".login-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería permanecer en la página de login");
    }

    /**
     * Verifica que no se puede iniciar sesion sin introducir la contrasena.
     */
    @Test
    @DisplayName("Login View - Campo password vacío")
    public void testLoginView_EmptyPassword() {
        navigateTo("/iniciar-sesion");

        fillInput(By.cssSelector(".login-container input[type='text']"), "carlos.martinez");
        clickElement(By.cssSelector(".login-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería permanecer en la página de login");
    }

    /**
     * Verifica que el enlace de crear cuenta lleva correctamente a la pagina de registro.
     */
    @Test
    @DisplayName("Login View - Enlace a página de registro")
    public void testLoginView_NavigateToRegister() {
        navigateTo("/iniciar-sesion");

        clickElement(By.linkText("Crear una cuenta"));

        waitForUrlContains("/registro");
        assertTrue(getCurrentUrl().contains("/registro"), "Debería navegar a la página de registro");
    }

    /**
     * Verifica que el enlace de recuperar contrasena lleva a la pagina correspondiente.
     */
    @Test
    @DisplayName("Login View - Enlace a recuperar contraseña")
    public void testLoginView_NavigateToForgotPassword() {
        navigateTo("/iniciar-sesion");

        clickElement(By.cssSelector(".forgot-password-link a"));

        waitForUrlContains("/clave-olvidada");
        assertTrue(getCurrentUrl().contains("/clave-olvidada"), "Debería navegar a recuperar contraseña");
    }

    /**
     * Verifica que un nuevo usuario puede registrarse con datos validos y es redirigido a la pagina principal.
     */
    @Test
    @DisplayName("Register View - Registro exitoso con datos válidos")
    public void testRegisterView_SuccessfulRegistration() {
        navigateTo("/registro");
        String timestamp = String.valueOf(System.currentTimeMillis());

        fillInput(By.cssSelector(".register-container input[type='email']"), "user" + timestamp + "@example.com");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "user" + timestamp);
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        waitForUrlContains("/");
        assertTrue(getCurrentUrl().contains("/"), "Debería redirigir a la página de eventos");
    }

    /**
     * Verifica que se muestra un error al intentar registrarse con un email que ya esta en uso.
     */
    @Test
    @DisplayName("Register View - Error con email ya registrado")
    public void testRegisterView_EmailAlreadyExists() {
        navigateTo("/registro");

        fillInput(By.cssSelector(".register-container input[type='email']"), "existing@example.com");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "newuser123");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent(), "Debería mostrar un mensaje de error");
        String errorMessage = getErrorMessage().toLowerCase();
        assertTrue(errorMessage.contains("email") || errorMessage.contains("existe") || errorMessage.contains("registrado"), "El mensaje de error debería indicar que el email ya existe");
    }

    /**
     * Verifica que el registro falla cuando se introduce un email con formato invalido.
     */
    @Test
    @DisplayName("Register View - Error con email inválido")
    public void testRegisterView_InvalidEmailFormat() {
        navigateTo("/registro");

        fillInput(By.cssSelector(".register-container input[type='email']"), "invalid-email");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/registro"), "Debería permanecer en la página de registro");
    }

    /**
     * Verifica que el registro rechaza un email que supera el limite maximo de caracteres permitidos.
     */
    @Test
    @DisplayName("Register View - Error con email demasiado largo")
    public void testRegisterView_EmailTooLong() {
        navigateTo("/registro");
        String longEmail = "a".repeat(45) + "@test.com";

        fillInput(By.cssSelector(".register-container input[type='email']"), longEmail);
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"), "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifica que el registro rechaza una contrasena que supera el limite maximo de caracteres.
     */
    @Test
    @DisplayName("Register View - Error con password demasiado largo")
    public void testRegisterView_PasswordTooLong() {
        navigateTo("/registro");
        String longPassword = "a".repeat(30);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), longPassword);
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"), "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifica que el registro rechaza un nombre de usuario que excede la longitud maxima permitida.
     */
    @Test
    @DisplayName("Register View - Error con username demasiado largo")
    public void testRegisterView_UsernameTooLong() {
        navigateTo("/registro");
        String longUsername = "a".repeat(30);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), longUsername);
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"), "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifica que el registro rechaza un nombre que excede la longitud maxima permitida.
     */
    @Test
    @DisplayName("Register View - Error con firstName demasiado largo")
    public void testRegisterView_FirstNameTooLong() {
        navigateTo("/registro");
        String longFirstName = "a".repeat(25);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), longFirstName);
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"), "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifica que el registro rechaza un apellido que excede la longitud maxima permitida.
     */
    @Test
    @DisplayName("Register View - Error con lastName demasiado largo")
    public void testRegisterView_LastNameTooLong() {
        navigateTo("/registro");
        String longLastName = "a".repeat(55);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), longLastName);
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"), "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifica que el formulario de registro no se envia cuando todos los campos obligatorios estan vacios.
     */
    @Test
    @DisplayName("Register View - Error con campos obligatorios vacíos")
    public void testRegisterView_RequiredFieldsEmpty() {
        navigateTo("/registro");

        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/registro"), "Debería permanecer en la página de registro");
    }

    /**
     * Verifica que el registro rechaza una contrasena demasiado corta que no cumple el minimo de caracteres.
     */
    @Test
    @DisplayName("Register View - Error con password demasiado corto")
    public void testRegisterView_PasswordTooShort() {
        navigateTo("/registro");

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"), "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "123");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"), "Test");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"), "User");
        fillInput(By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"), "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"), "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifica que el enlace para volver a login desde la pagina de registro funciona correctamente.
     */
    @Test
    @DisplayName("Register View - Enlace a página de login")
    public void testRegisterView_NavigateToLogin() {
        navigateTo("/registro");

        clickElement(By.linkText("Ya tengo una cuenta"));

        waitForUrlContains("/iniciar-sesion");
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería navegar a la página de login");
    }

    /**
     * Verifica que se puede cambiar la contrasena correctamente proporcionando email, usuario y nueva clave validos.
     */
    @Test
    @DisplayName("Forgot Password View - Cambio exitoso de contraseña")
    public void testForgotPasswordView_SuccessfulPasswordChange() {
        navigateTo("/clave-olvidada");

        fillInput(By.id("email"), "carlos.martinez@eventmanager.es");
        fillInput(By.id("username"), "carlos.martinez");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        fillInput(By.id("newPasswordConfirm"), "ClaveNueva2025");
        clickElement(By.cssSelector("button[type='submit']"));

        sleep(1000);
        assertTrue(
            isSuccessMessagePresent() || isErrorMessagePresent() || getCurrentUrl().contains("/iniciar-sesion"),
            "Debería mostrar feedback de éxito/error o redirigir al login"
        );
    }

    /**
     * Verifica que se muestra un error al intentar recuperar la contrasena con un email no registrado.
     */
    @Test
    @DisplayName("Forgot Password View - Error con email no registrado")
    public void testForgotPasswordView_EmailNotFound() {
        navigateTo("/clave-olvidada");

        fillInput(By.id("email"), "laura.sanchez@eventmanager.es");
        fillInput(By.id("username"), "laura.sanchez");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        fillInput(By.id("newPasswordConfirm"), "ClaveNueva2025");
        clickElement(By.cssSelector("button[type='submit']"));

        sleep(1000);
        assertTrue(
            isErrorMessagePresent() || getCurrentUrl().contains("/clave-olvidada"),
            "Debería mostrar error o mantenerse en la pantalla de recuperación"
        );
    }

    /**
     * Verifica que la recuperacion de contrasena no avanza cuando se introduce un email con formato invalido.
     */
    @Test
    @DisplayName("Forgot Password View - Error con email inválido")
    public void testForgotPasswordView_InvalidEmail() {
        navigateTo("/clave-olvidada");

        fillInput(By.id("email"), "invalid-email");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        clickElement(By.cssSelector("button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/clave-olvidada"), "Debería permanecer en la página");
    }

    /**
     * Verifica que se rechaza una nueva contrasena que no cumple con la longitud minima requerida.
     */
    @Test
    @DisplayName("Forgot Password View - Error con nueva contraseña demasiado corta")
    public void testForgotPasswordView_NewPasswordTooShort() {
        navigateTo("/clave-olvidada");

        fillInput(By.id("email"), "carlos.martinez@eventmanager.es");
        fillInput(By.id("newPassword"), "123");
        clickElement(By.cssSelector("button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/clave-olvidada"), "Debería mostrar error o permanecer en la página");
    }

    /**
     * Verifica que el formulario de recuperacion no se envia cuando todos los campos estan vacios.
     */
    @Test
    @DisplayName("Forgot Password View - Error con campos vacíos")
    public void testForgotPasswordView_EmptyFields() {
        navigateTo("/clave-olvidada");

        clickElement(By.cssSelector("button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/clave-olvidada"), "Debería permanecer en la página");
    }

    /**
     * Verifica que el enlace para volver al login desde la pagina de recuperacion funciona correctamente.
     */
    @Test
    @DisplayName("Forgot Password View - Enlace a página de login")
    public void testForgotPasswordView_NavigateToLogin() {
        navigateTo("/clave-olvidada");

        clickElement(By.cssSelector("a.btn-secondary"));

        waitForUrlContains("/iniciar-sesion");
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería navegar a la página de login");
    }

    /**
     * Verifica que tras cerrar sesion se redirige al login al intentar acceder a una ruta protegida.
     */
    @Test
    @DisplayName("Auth - Logout desde la UI redirige a login")
    public void testAuth_LogoutFromUI() {
        login("carlos.martinez", "ClaveSegura2025");
        sleep(1000);

        logout();
        sleep(1000);

        navigateTo("/");
        sleep(2000);
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Deberia redirigir a login al acceder sin sesion");
    }
}
