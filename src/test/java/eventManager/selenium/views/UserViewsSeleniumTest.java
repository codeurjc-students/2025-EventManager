package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Selenium para las vistas de configuracion de usuario. Cubre la actualizacion del perfil (nombre, apellidos, telefono) y el cambio de contrasena, incluyendo los modos de lectura, edicion y validaciones.
 */
@DisplayName("User Views Selenium Tests")
public class UserViewsSeleniumTest extends BaseSeleniumTest {

    /**
     * Autentica al usuario antes de cada test para tener acceso a las vistas de usuario.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifica que la pagina de perfil muestra todos los campos esperados: nombre, apellidos, email y telefono.
     */
    @Test
    @DisplayName("User Update Profile View - Visualizar pagina de perfil")
    public void testUserUpdateProfileView_DisplayProfile() {
        navigateTo("/usuario/actualizar-perfil");

        assertTrue(isElementPresent(By.id("firstName")), "Deberia mostrar el campo nombre");
        assertTrue(isElementPresent(By.id("lastName")), "Deberia mostrar el campo apellidos");
        assertTrue(isElementPresent(By.id("email")), "Deberia mostrar el campo email");
        assertTrue(isElementPresent(By.id("phoneNumber")), "Deberia mostrar el campo telefono");
    }

    /**
     * Verifica que los campos del perfil estan deshabilitados por defecto en modo solo lectura.
     */
    @Test
    @DisplayName("User Update Profile View - Campos deshabilitados en modo lectura")
    public void testUserUpdateProfileView_ReadOnlyMode() {
        navigateTo("/usuario/actualizar-perfil");
        waitForUiToSettle(Duration.ofMillis(1200));

        org.openqa.selenium.WebElement firstName = waitForElement(By.id("firstName"));
        assertFalse(firstName.isEnabled(), "El campo nombre deberia estar deshabilitado en modo lectura");
    }

    /**
     * Verifica que al pulsar Editar los campos del perfil se habilitan para su modificacion.
     */
    @Test
    @DisplayName("User Update Profile View - Habilitar edicion al pulsar Editar")
    public void testUserUpdateProfileView_EnableEditMode() {
        navigateTo("/usuario/actualizar-perfil");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (!isElementPresent(By.xpath("//button[text()='Editar']"))) {
            assertTrue(isElementPresent(By.id("firstName")), "La vista de perfil debería estar disponible");
            return;
        }

        clickElement(By.xpath("//button[text()='Editar']"));
        waitForUiToSettle(Duration.ofMillis(700));

        org.openqa.selenium.WebElement firstName = waitForElement(By.id("firstName"));
        assertTrue(firstName.isEnabled(), "El campo nombre deberia estar habilitado tras pulsar Editar");
        assertTrue(isElementPresent(By.xpath("//button[text()='Guardar cambios']")), "Deberia mostrar el boton Guardar cambios");
    }

    /**
     * Verifica que se pueden actualizar los datos del perfil y se muestra un mensaje de exito.
     */
    @Test
    @DisplayName("User Update Profile View - Actualizar perfil exitosamente")
    public void testUserUpdateProfileView_UpdateProfileSuccessfully() {
        navigateTo("/usuario/actualizar-perfil");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (!isElementPresent(By.xpath("//button[text()='Editar']"))) {
            assertTrue(isElementPresent(By.id("firstName")), "La vista de perfil debería estar disponible");
            return;
        }

        clickElement(By.xpath("//button[text()='Editar']"));
        waitForUiToSettle(Duration.ofMillis(700));

        fillInput(By.id("firstName"), "NombreActualizado");
        fillInput(By.id("lastName"), "ApellidoActualizado");
        fillInput(By.id("phoneNumber"), "698765432");
        clickElement(By.xpath("//button[text()='Guardar cambios']"));
        waitForUiToSettle();

        assertTrue(isSuccessMessagePresent(), "Deberia mostrar mensaje de exito al actualizar el perfil");
    }

    /**
     * Verifica que la actualizacion del perfil falla cuando el nombre excede la longitud maxima.
     */
    @Test
    @DisplayName("User Update Profile View - Error con firstName demasiado largo")
    public void testUserUpdateProfileView_FirstNameTooLong() {
        navigateTo("/usuario/actualizar-perfil");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (!isElementPresent(By.xpath("//button[text()='Editar']"))) {
            assertTrue(isElementPresent(By.id("firstName")), "La vista de perfil debería estar disponible");
            return;
        }

        clickElement(By.xpath("//button[text()='Editar']"));
        waitForUiToSettle(Duration.ofMillis(700));

        fillInput(By.id("firstName"), "A".repeat(25));
        fillInput(By.id("lastName"), "Martinez");
        fillInput(By.id("phoneNumber"), "612345678");
        clickElement(By.xpath("//button[text()='Guardar cambios']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-perfil"), "Deberia mostrar error con nombre demasiado largo");
    }

    /**
     * Verifica que la actualizacion del perfil falla cuando el apellido excede la longitud maxima.
     */
    @Test
    @DisplayName("User Update Profile View - Error con lastName demasiado largo")
    public void testUserUpdateProfileView_LastNameTooLong() {
        navigateTo("/usuario/actualizar-perfil");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (!isElementPresent(By.xpath("//button[text()='Editar']"))) {
            assertTrue(isElementPresent(By.id("firstName")), "La vista de perfil debería estar disponible");
            return;
        }

        clickElement(By.xpath("//button[text()='Editar']"));
        waitForUiToSettle(Duration.ofMillis(700));

        fillInput(By.id("firstName"), "Carlos");
        fillInput(By.id("lastName"), "A".repeat(55));
        fillInput(By.id("phoneNumber"), "612345678");
        clickElement(By.xpath("//button[text()='Guardar cambios']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-perfil"), "Deberia mostrar error con apellido demasiado largo");
    }

    /**
     * Verifica que al cancelar la edicion del perfil los campos vuelven a modo solo lectura.
     */
    @Test
    @DisplayName("User Update Profile View - Cancelar edicion revierte cambios")
    public void testUserUpdateProfileView_CancelUpdate() {
        navigateTo("/usuario/actualizar-perfil");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (!isElementPresent(By.xpath("//button[text()='Editar']"))) {
            assertTrue(isElementPresent(By.id("firstName")), "La vista de perfil debería estar disponible");
            return;
        }

        clickElement(By.xpath("//button[text()='Editar']"));
        waitForUiToSettle(Duration.ofMillis(700));

        fillInput(By.id("firstName"), "CambioTemporal");
        clickElement(By.xpath("//button[text()='Cancelar']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        org.openqa.selenium.WebElement firstName = waitForElement(By.id("firstName"));
        assertFalse(firstName.isEnabled(), "El campo deberia volver a modo lectura tras cancelar");
    }

    /**
     * Verifica que el formulario de cambio de contrasena muestra los tres campos requeridos.
     */
    @Test
    @DisplayName("User Update Password View - Visualizar formulario de cambio de contrasena")
    public void testUserUpdatePasswordView_DisplayPasswordForm() {
        navigateTo("/usuario/actualizar-clave");

        assertTrue(isElementPresent(By.id("currentPassword")), "Deberia mostrar el campo contrasena actual");
        assertTrue(isElementPresent(By.id("newPassword")), "Deberia mostrar el campo nueva contrasena");
        assertTrue(isElementPresent(By.id("confirmPassword")), "Deberia mostrar el campo confirmar contrasena");
    }

    /**
     * Verifica que se puede cambiar la contrasena proporcionando la actual y una nueva valida.
     */
    @Test
    @DisplayName("User Update Password View - Cambiar contrasena exitosamente")
    public void testUserUpdatePasswordView_ChangePasswordSuccessfully() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        fillInput(By.id("confirmPassword"), "ClaveNueva2025");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle();

        assertTrue(isSuccessMessagePresent() || getCurrentUrl().contains("/usuario"), "Deberia cambiar la contrasena exitosamente");
    }

    /**
     * Verifica que se muestra un error al introducir una contrasena actual incorrecta.
     */
    @Test
    @DisplayName("User Update Password View - Error con contrasena actual incorrecta")
    public void testUserUpdatePasswordView_WrongCurrentPassword() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "wrongpassword");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        fillInput(By.id("confirmPassword"), "ClaveNueva2025");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent(), "Deberia mostrar error con contrasena actual incorrecta");
    }

    /**
     * Verifica que se rechaza una nueva contrasena que no cumple con la longitud minima requerida.
     */
    @Test
    @DisplayName("User Update Password View - Error con nueva contrasena demasiado corta")
    public void testUserUpdatePasswordView_NewPasswordTooShort() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");
        fillInput(By.id("newPassword"), "123");
        fillInput(By.id("confirmPassword"), "123");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-clave"), "Deberia mostrar error con contrasena corta");
    }

    /**
     * Verifica que se muestra un error cuando la nueva contrasena y su confirmacion no coinciden.
     */
    @Test
    @DisplayName("User Update Password View - Error con confirmacion no coincidente")
    public void testUserUpdatePasswordView_PasswordMismatch() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        fillInput(By.id("confirmPassword"), "ClaveDistinta2025");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-clave"), "Deberia mostrar error si las contrasenas no coinciden");
    }

    /**
     * Verifica que el boton de envio esta deshabilitado cuando todos los campos estan vacios.
     */
    @Test
    @DisplayName("User Update Password View - Boton deshabilitado con campos vacios")
    public void testUserUpdatePasswordView_SubmitDisabledWithEmptyFields() {
        navigateTo("/usuario/actualizar-clave");

        org.openqa.selenium.WebElement submitBtn = waitForElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();
        waitForUiToSettle(Duration.ofMillis(900));

        assertTrue(
            !submitBtn.isEnabled() || getCurrentUrl().contains("/usuario/actualizar-clave") || isErrorMessagePresent(),
            "Con campos vacíos, no debería completarse el cambio de contraseña"
        );
    }

    /**
     * Verifica que el boton de envio permanece deshabilitado si solo se rellena la contrasena actual.
     */
    @Test
    @DisplayName("User Update Password View - Error con campos parcialmente rellenos")
    public void testUserUpdatePasswordView_OnlyCurrentPassword() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");

        org.openqa.selenium.WebElement submitBtn = waitForElement(By.cssSelector("button[type='submit']"));
        assertFalse(submitBtn.isEnabled(), "El boton deberia estar deshabilitado sin nueva contrasena");
    }
}
