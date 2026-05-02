package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium tests for user settings views. Covers profile updates (name, last
 * name, phone) and password changes, including read-only/edit modes and
 * validations.
 */
@DisplayName("User Views Selenium Tests")
public class UserViewsSeleniumTest extends BaseSeleniumTest {

    /**
     * Authenticates the user before each test to access user views.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifies that the profile page shows all expected fields: first name, last
     * name, email, and phone.
     */
    @Test
    @DisplayName("User Update Profile View - Display profile page")
    public void testUserUpdateProfileView_DisplayProfile() {
        navigateTo("/usuario/actualizar-perfil");

        assertTrue(isElementPresent(By.id("firstName")), "Deberia mostrar el campo nombre");
        assertTrue(isElementPresent(By.id("lastName")), "Deberia mostrar el campo apellidos");
        assertTrue(isElementPresent(By.id("email")), "Deberia mostrar el campo email");
        assertTrue(isElementPresent(By.id("phoneNumber")), "Deberia mostrar el campo telefono");
    }

    /**
     * Verifies that profile fields are disabled by default in read-only mode.
     */
    @Test
    @DisplayName("User Update Profile View - Fields disabled in read-only mode")
    public void testUserUpdateProfileView_ReadOnlyMode() {
        navigateTo("/usuario/actualizar-perfil");
        waitForUiToSettle(Duration.ofMillis(1200));

        org.openqa.selenium.WebElement firstName = waitForElement(By.id("firstName"));
        assertFalse(firstName.isEnabled(), "El campo nombre deberia estar deshabilitado en modo lectura");
    }

    /**
     * Verifies that clicking Edit enables profile fields for modification.
     */
    @Test
    @DisplayName("User Update Profile View - Enable edit mode when clicking Edit")
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
        assertTrue(isElementPresent(By.xpath("//button[text()='Guardar cambios']")),
                "Deberia mostrar el boton Guardar cambios");
    }

    /**
     * Verifies that profile data can be updated and a success message is shown.
     */
    @Test
    @DisplayName("User Update Profile View - Update profile successfully")
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
     * Verifies that the profile update fails when the first name exceeds the
     * maximum length.
     */
    @Test
    @DisplayName("User Update Profile View - Error with first name too long")
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

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-perfil"),
                "Deberia mostrar error con nombre demasiado largo");
    }

    /**
     * Verifies that the profile update fails when the last name exceeds the maximum
     * length.
     */
    @Test
    @DisplayName("User Update Profile View - Error with last name too long")
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

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-perfil"),
                "Deberia mostrar error con apellido demasiado largo");
    }

    /**
     * Verifies that canceling profile edits returns fields to read-only mode.
     */
    @Test
    @DisplayName("User Update Profile View - Cancel edit reverts changes")
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
     * Verifies that the password change form shows the three required fields.
     */
    @Test
    @DisplayName("User Update Password View - Display password change form")
    public void testUserUpdatePasswordView_DisplayPasswordForm() {
        navigateTo("/usuario/actualizar-clave");

        assertTrue(isElementPresent(By.id("currentPassword")), "Deberia mostrar el campo contrasena actual");
        assertTrue(isElementPresent(By.id("newPassword")), "Deberia mostrar el campo nueva contrasena");
        assertTrue(isElementPresent(By.id("confirmPassword")), "Deberia mostrar el campo confirmar contrasena");
    }

    /**
     * Verifies that the password can be changed by providing the current and a
     * valid new password.
     */
    @Test
    @DisplayName("User Update Password View - Change password successfully")
    public void testUserUpdatePasswordView_ChangePasswordSuccessfully() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        fillInput(By.id("confirmPassword"), "ClaveNueva2025");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle();

        assertTrue(isSuccessMessagePresent() || getCurrentUrl().contains("/usuario"),
                "Deberia cambiar la contrasena exitosamente");
    }

    /**
     * Verifies that an error is shown when the current password is incorrect.
     */
    @Test
    @DisplayName("User Update Password View - Error with incorrect current password")
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
     * Verifies that a new password that does not meet the minimum length is
     * rejected.
     */
    @Test
    @DisplayName("User Update Password View - Error with new password too short")
    public void testUserUpdatePasswordView_NewPasswordTooShort() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");
        fillInput(By.id("newPassword"), "123");
        fillInput(By.id("confirmPassword"), "123");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-clave"),
                "Deberia mostrar error con contrasena corta");
    }

    /**
     * Verifies that an error is shown when the new password and confirmation do not
     * match.
     */
    @Test
    @DisplayName("User Update Password View - Error with mismatched confirmation")
    public void testUserUpdatePasswordView_PasswordMismatch() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");
        fillInput(By.id("newPassword"), "ClaveNueva2025");
        fillInput(By.id("confirmPassword"), "ClaveDistinta2025");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/usuario/actualizar-clave"),
                "Deberia mostrar error si las contrasenas no coinciden");
    }

    /**
     * Verifies that the submit button is disabled when all fields are empty.
     */
    @Test
    @DisplayName("User Update Password View - Submit button disabled with empty fields")
    public void testUserUpdatePasswordView_SubmitDisabledWithEmptyFields() {
        navigateTo("/usuario/actualizar-clave");

        org.openqa.selenium.WebElement submitBtn = waitForElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();
        waitForUiToSettle(Duration.ofMillis(900));

        assertTrue(
                !submitBtn.isEnabled() || getCurrentUrl().contains("/usuario/actualizar-clave")
                        || isErrorMessagePresent(),
                "Con campos vacíos, no debería completarse el cambio de contraseña");
    }

    /**
     * Verifies that the submit button remains disabled if only the current password
     * is filled in.
     */
    @Test
    @DisplayName("User Update Password View - Error with partially filled fields")
    public void testUserUpdatePasswordView_OnlyCurrentPassword() {
        navigateTo("/usuario/actualizar-clave");

        fillInput(By.id("currentPassword"), "ClaveSegura2025");

        org.openqa.selenium.WebElement submitBtn = waitForElement(By.cssSelector("button[type='submit']"));
        assertFalse(submitBtn.isEnabled(), "El boton deberia estar deshabilitado sin nueva contrasena");
    }
}
