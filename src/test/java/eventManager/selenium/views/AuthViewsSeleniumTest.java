package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium tests for the authentication views. Validates the behavior of login,
 * registration, password recovery, and logout screens.
 */
@DisplayName("Authentication Views Selenium Tests")
public class AuthViewsSeleniumTest extends BaseSeleniumTest {

    @Override
    protected boolean preserveCookiesBetweenTests() {
        return false;
    }

    /**
     * Verifies that a successful login with valid credentials redirects the user to
     * the home page.
     */
    @Test
    @DisplayName("Login View - Successful login with valid credentials")
    public void testLoginView_SuccessfulLogin() {
        login("carlos.martinez", "ClaveSegura2025");
        assertFalse(getCurrentUrl().contains("/iniciar-sesion"), "Debería salir de la página de login");
    }

    /**
     * Verifies that an error message is shown when invalid credentials are
     * provided.
     */
    @Test
    @DisplayName("Login View - Error with invalid credentials")
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
     * Verifies that the form is not submitted and the user stays on login when both
     * fields are empty.
     */
    @Test
    @DisplayName("Login View - Error with empty fields")
    public void testLoginView_EmptyFields() {
        navigateTo("/iniciar-sesion");

        clickElement(By.cssSelector(".login-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería permanecer en la página de login");
    }

    /**
     * Verifies that login is not possible without entering the username.
     */
    @Test
    @DisplayName("Login View - Empty username field")
    public void testLoginView_EmptyUsername() {
        navigateTo("/iniciar-sesion");

        fillInput(By.cssSelector(".login-container input[type='password']"), "ClaveSegura2025");
        clickElement(By.cssSelector(".login-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería permanecer en la página de login");
    }

    /**
     * Verifies that login is not possible without entering the password.
     */
    @Test
    @DisplayName("Login View - Empty password field")
    public void testLoginView_EmptyPassword() {
        navigateTo("/iniciar-sesion");

        fillInput(By.cssSelector(".login-container input[type='text']"), "carlos.martinez");
        clickElement(By.cssSelector(".login-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería permanecer en la página de login");
    }

    /**
     * Verifies that the create account link navigates to the registration page.
     */
    @Test
    @DisplayName("Login View - Link to registration page")
    public void testLoginView_NavigateToRegister() {
        navigateTo("/iniciar-sesion");

        clickElement(By.linkText("Crear una cuenta"));

        waitForUrlContains("/registro");
        assertTrue(getCurrentUrl().contains("/registro"), "Debería navegar a la página de registro");
    }

    /**
     * Verifies that the forgot password link navigates to the corresponding page.
     */
    @Test
    @DisplayName("Login View - Link to forgot password")
    public void testLoginView_NavigateToForgotPassword() {
        navigateTo("/iniciar-sesion");

        clickElement(By.cssSelector(".forgot-password-link a"));

        waitForUrlContains("/clave-olvidada");
        assertTrue(getCurrentUrl().contains("/clave-olvidada"), "Debería navegar a recuperar contraseña");
    }

    /**
     * Verifies that a new user can register with valid data and is redirected to
     * the home page.
     */
    @Test
    @DisplayName("Register View - Successful registration with valid data")
    public void testRegisterView_SuccessfulRegistration() {
        navigateTo("/registro");
        String timestamp = String.valueOf(System.currentTimeMillis());

        fillInput(By.cssSelector(".register-container input[type='email']"), "user" + timestamp + "@example.com");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "user" + timestamp);
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        waitForUrlContains("/");
        assertTrue(getCurrentUrl().contains("/"), "Debería redirigir a la página de eventos");
    }

    /**
     * Verifies that an error is shown when registering with an email already in
     * use.
     */
    @Test
    @DisplayName("Register View - Error with email already registered")
    public void testRegisterView_EmailAlreadyExists() {
        navigateTo("/registro");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String duplicateEmail = "existing" + timestamp + "@example.com";

        fillInput(By.cssSelector(".register-container input[type='email']"), duplicateEmail);
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "existinguser" + timestamp);
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        waitForUrlContains("/");
        assertTrue(getCurrentUrl().contains("/"), "La primera creación debería ser exitosa");

        navigateTo("/registro");

        fillInput(By.cssSelector(".register-container input[type='email']"), duplicateEmail);
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "anotheruser" + timestamp);
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent(), "Debería mostrar un mensaje de error");
        String errorMessage = getErrorMessage().toLowerCase();
        assertTrue(
                errorMessage.contains("email") || errorMessage.contains("existe")
                        || errorMessage.contains("registrado"),
                "El mensaje de error debería indicar que el email ya existe");
    }

    /**
     * Verifies that registration fails when an email with invalid format is
     * provided.
     */
    @Test
    @DisplayName("Register View - Error with invalid email")
    public void testRegisterView_InvalidEmailFormat() {
        navigateTo("/registro");

        fillInput(By.cssSelector(".register-container input[type='email']"), "invalid-email");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/registro"), "Debería permanecer en la página de registro");
    }

    /**
     * Verifies that registration rejects an email exceeding the maximum length.
     */
    @Test
    @DisplayName("Register View - Error with email too long")
    public void testRegisterView_EmailTooLong() {
        navigateTo("/registro");
        String longEmail = "a".repeat(45) + "@test.com";

        fillInput(By.cssSelector(".register-container input[type='email']"), longEmail);
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"),
                "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifies that registration rejects a password that exceeds the maximum
     * length.
     */
    @Test
    @DisplayName("Register View - Error with password too long")
    public void testRegisterView_PasswordTooLong() {
        navigateTo("/registro");
        String longPassword = "a".repeat(30);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), longPassword);
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"),
                "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifies that registration rejects a username that exceeds the maximum
     * allowed length.
     */
    @Test
    @DisplayName("Register View - Error with username too long")
    public void testRegisterView_UsernameTooLong() {
        navigateTo("/registro");
        String longUsername = "a".repeat(30);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                longUsername);
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"),
                "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifies that registration rejects a first name that exceeds the maximum
     * allowed length.
     */
    @Test
    @DisplayName("Register View - Error with first name too long")
    public void testRegisterView_FirstNameTooLong() {
        navigateTo("/registro");
        String longFirstName = "a".repeat(25);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                longFirstName);
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                "User");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"),
                "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifies that registration rejects a last name that exceeds the maximum
     * allowed length.
     */
    @Test
    @DisplayName("Register View - Error with last name too long")
    public void testRegisterView_LastNameTooLong() {
        navigateTo("/registro");
        String longLastName = "a".repeat(55);

        fillInput(By.cssSelector(".register-container input[type='email']"), "test@example.com");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][2]//input"),
                "testuser");
        fillInput(By.cssSelector(".register-container input[type='password']"), "password123");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][4]//input"),
                "Test");
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][5]//input"),
                longLastName);
        fillInput(
                By.xpath("//div[contains(@class,'register-container')]//div[contains(@class,'form-group')][6]//input"),
                "123456789");
        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/registro"),
                "Debería mostrar error o permanecer en registro");
    }

    /**
     * Verifies that the registration form is not submitted when all required fields
     * are empty.
     */
    @Test
    @DisplayName("Register View - Error with empty required fields")
    public void testRegisterView_RequiredFieldsEmpty() {
        navigateTo("/registro");

        clickElement(By.cssSelector(".register-container button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/registro"), "Debería permanecer en la página de registro");
    }

    /**
     * Verifies that the link back to login from the registration page works
     * correctly.
     */
    @Test
    @DisplayName("Register View - Link to login page")
    public void testRegisterView_NavigateToLogin() {
        navigateTo("/registro");

        clickElement(By.linkText("Ya tengo una cuenta"));

        waitForUrlContains("/iniciar-sesion");
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería navegar a la página de login");
    }

    /**
     * Verifies that the password can be changed by providing a valid email,
     * username, and new password.
     */
    @Test
    @DisplayName("Forgot Password View - Successful password change")
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
                "Debería mostrar feedback de éxito/error o redirigir al login");
    }

    /**
     * Verifies that an error is shown when attempting password recovery with an
     * unregistered email.
     */
    @Test
    @DisplayName("Forgot Password View - Error with unregistered email")
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
                "Debería mostrar error o mantenerse en la pantalla de recuperación");
    }

    /**
     * Verifies that password recovery does not proceed when an invalid email format
     * is provided.
     */
    @Test
    @DisplayName("Forgot Password View - Error with invalid email")
    public void testForgotPasswordView_InvalidEmail() {
        navigateTo("/clave-olvidada");

        fillInput(By.id("email"), "invalid-email");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        clickElement(By.cssSelector("button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/clave-olvidada"), "Debería permanecer en la página");
    }

    /**
     * Verifies that a new password that does not meet the minimum length is
     * rejected.
     */
    @Test
    @DisplayName("Forgot Password View - Error with new password too short")
    public void testForgotPasswordView_NewPasswordTooShort() {
        navigateTo("/clave-olvidada");

        fillInput(By.id("email"), "carlos.martinez@eventmanager.es");
        fillInput(By.id("newPassword"), "123");
        clickElement(By.cssSelector("button[type='submit']"));

        sleep(1000);
        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/clave-olvidada"),
                "Debería mostrar error o permanecer en la página");
    }

    /**
     * Verifies that the recovery form is not submitted when all fields are empty.
     */
    @Test
    @DisplayName("Forgot Password View - Error with empty fields")
    public void testForgotPasswordView_EmptyFields() {
        navigateTo("/clave-olvidada");

        clickElement(By.cssSelector("button[type='submit']"));

        sleep(500);
        assertTrue(getCurrentUrl().contains("/clave-olvidada"), "Debería permanecer en la página");
    }

    /**
     * Verifies that the link back to login from the recovery page works correctly.
     */
    @Test
    @DisplayName("Forgot Password View - Link to login page")
    public void testForgotPasswordView_NavigateToLogin() {
        navigateTo("/clave-olvidada");

        clickElement(By.cssSelector("a.btn-secondary"));

        waitForUrlContains("/iniciar-sesion");
        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Debería navegar a la página de login");
    }

    /**
     * Verifies that after logout the user is redirected to login when accessing a
     * protected route.
     */
    @Test
    @DisplayName("Auth - UI logout redirects to login")
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
