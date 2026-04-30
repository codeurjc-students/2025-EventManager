package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium tests for the main view (Home/Dashboard). Verifies that the
 * dashboard is shown after login, navigation links work, and protected routes
 * redirect to login when there is no active session.
 */
@DisplayName("Home View Selenium Tests")
public class HomeViewSeleniumTest extends BaseSeleniumTest {

    /**
     * Authenticates the user before each test to access protected views.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifies that after login the dashboard is shown with the welcome message.
     */
    @Test
    @DisplayName("Home View - Shows dashboard after login")
    public void testHomeView_DisplayAfterLogin() {
        navigateTo("/");

        assertTrue(isElementPresent(By.cssSelector(".welcome-box")), "Deberia mostrar el cuadro de bienvenida");
        String welcomeText = getElementText(By.cssSelector(".welcome-box h1"));
        assertTrue(welcomeText.contains("Bienvenido") || welcomeText.contains("EventManager"),
                "Deberia mostrar el mensaje de bienvenida a EventManager");
    }

    /**
     * Verifies that the navigation menu and its sections are shown on the home
     * page.
     */
    @Test
    @DisplayName("Home View - Shows navigation menu")
    public void testHomeView_DisplayNavigationMenu() {
        navigateTo("/");

        assertTrue(isElementPresent(By.cssSelector(".menu-box")), "Deberia mostrar el cuadro de menu");
        assertTrue(isElementPresent(By.cssSelector(".menu-section")), "Deberia mostrar al menos una seccion de menu");
    }

    /**
     * Verifies that the menu link navigates to the event list.
     */
    @Test
    @DisplayName("Home View - Navigate to event list")
    public void testHomeView_NavigateToEventList() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/eventos']"));

        waitForUrlContains("/eventos");
        assertTrue(getCurrentUrl().contains("/eventos"), "Deberia navegar a la lista de eventos");
    }

    /**
     * Verifies that the menu link navigates to the event creation page.
     */
    @Test
    @DisplayName("Home View - Navigate to create event")
    public void testHomeView_NavigateToCreateEvent() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/crear-evento']"));

        waitForUrlContains("/crear-evento");
        assertTrue(getCurrentUrl().contains("/crear-evento"), "Deberia navegar a crear evento");
    }

    /**
     * Verifies that the menu link navigates to the event enrollment page.
     */
    @Test
    @DisplayName("Home View - Navigate to join event")
    public void testHomeView_NavigateToJoinEvent() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/inscribirse-evento']"));

        waitForUrlContains("/inscribirse-evento");
        assertTrue(getCurrentUrl().contains("/inscribirse-evento"), "Deberia navegar a inscribirse en un evento");
    }

    /**
     * Verifies that the menu link navigates to the profile update page.
     */
    @Test
    @DisplayName("Home View - Navigate to update profile")
    public void testHomeView_NavigateToUpdateProfile() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/usuario/actualizar-perfil']"));

        waitForUrlContains("/usuario/actualizar-perfil");
        assertTrue(getCurrentUrl().contains("/usuario/actualizar-perfil"), "Deberia navegar a actualizar perfil");
    }

    /**
     * Verifies that the menu link navigates to the password change page.
     */
    @Test
    @DisplayName("Home View - Navigate to update password")
    public void testHomeView_NavigateToUpdatePassword() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/usuario/actualizar-clave']"));

        waitForUrlContains("/usuario/actualizar-clave");
        assertTrue(getCurrentUrl().contains("/usuario/actualizar-clave"), "Deberia navegar a actualizar contrasena");
    }

    /**
     * Verifies that a user without an active session is redirected to login when
     * accessing a protected route.
     */
    @Test
    @DisplayName("Home View - Redirects to login when unauthenticated")
    public void testHomeView_RedirectsUnauthenticated() {
        driver.manage().deleteAllCookies();
        ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("window.localStorage.clear();");

        navigateTo("/eventos");
        sleep(2000);

        assertTrue(getCurrentUrl().contains("/iniciar-sesion"),
                "Deberia redirigir a la pagina de login si no esta autenticado");
    }
}
