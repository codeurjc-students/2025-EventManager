package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Selenium para la vista principal (Home/Dashboard). Comprueba que el dashboard se muestra correctamente tras el login, que los enlaces de navegacion funcionan y que las rutas protegidas redirigen al login cuando no hay sesion activa.
 */
@DisplayName("Home View Selenium Tests")
public class HomeViewSeleniumTest extends BaseSeleniumTest {

    /**
     * Autentica al usuario antes de cada test para tener acceso a las vistas protegidas.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifica que tras el login se muestra el dashboard con el mensaje de bienvenida.
     */
    @Test
    @DisplayName("Home View - Muestra dashboard tras login")
    public void testHomeView_DisplayAfterLogin() {
        navigateTo("/");

        assertTrue(isElementPresent(By.cssSelector(".welcome-box")), "Deberia mostrar el cuadro de bienvenida");
        String welcomeText = getElementText(By.cssSelector(".welcome-box h1"));
        assertTrue(welcomeText.contains("Bienvenido") || welcomeText.contains("EventManager"), "Deberia mostrar el mensaje de bienvenida a EventManager");
    }

    /**
     * Verifica que el menu de navegacion con sus secciones se muestra en la pagina principal.
     */
    @Test
    @DisplayName("Home View - Muestra menu de navegacion")
    public void testHomeView_DisplayNavigationMenu() {
        navigateTo("/");

        assertTrue(isElementPresent(By.cssSelector(".menu-box")), "Deberia mostrar el cuadro de menu");
        assertTrue(isElementPresent(By.cssSelector(".menu-section")), "Deberia mostrar al menos una seccion de menu");
    }

    /**
     * Verifica que el enlace del menu lleva correctamente a la lista de eventos.
     */
    @Test
    @DisplayName("Home View - Navegar a lista de eventos")
    public void testHomeView_NavigateToEventList() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/eventos']"));

        waitForUrlContains("/eventos");
        assertTrue(getCurrentUrl().contains("/eventos"), "Deberia navegar a la lista de eventos");
    }

    /**
     * Verifica que el enlace del menu lleva correctamente a la pagina de creacion de evento.
     */
    @Test
    @DisplayName("Home View - Navegar a crear evento")
    public void testHomeView_NavigateToCreateEvent() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/crear-evento']"));

        waitForUrlContains("/crear-evento");
        assertTrue(getCurrentUrl().contains("/crear-evento"), "Deberia navegar a crear evento");
    }

    /**
     * Verifica que el enlace del menu lleva correctamente a la pagina de inscripcion en evento.
     */
    @Test
    @DisplayName("Home View - Navegar a inscribirse en evento")
    public void testHomeView_NavigateToJoinEvent() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/inscribirse-evento']"));

        waitForUrlContains("/inscribirse-evento");
        assertTrue(getCurrentUrl().contains("/inscribirse-evento"), "Deberia navegar a inscribirse en un evento");
    }

    /**
     * Verifica que el enlace del menu lleva correctamente a la pagina de actualizacion de perfil.
     */
    @Test
    @DisplayName("Home View - Navegar a actualizar perfil")
    public void testHomeView_NavigateToUpdateProfile() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/usuario/actualizar-perfil']"));

        waitForUrlContains("/usuario/actualizar-perfil");
        assertTrue(getCurrentUrl().contains("/usuario/actualizar-perfil"), "Deberia navegar a actualizar perfil");
    }

    /**
     * Verifica que el enlace del menu lleva correctamente a la pagina de cambio de contrasena.
     */
    @Test
    @DisplayName("Home View - Navegar a actualizar contrasena")
    public void testHomeView_NavigateToUpdatePassword() {
        navigateTo("/");

        clickElement(By.cssSelector("a.menu-link[href='/usuario/actualizar-clave']"));

        waitForUrlContains("/usuario/actualizar-clave");
        assertTrue(getCurrentUrl().contains("/usuario/actualizar-clave"), "Deberia navegar a actualizar contrasena");
    }

    /**
     * Verifica que un usuario sin sesion activa es redirigido al login al intentar acceder a una ruta protegida.
     */
    @Test
    @DisplayName("Home View - Redirige a login si no autenticado")
    public void testHomeView_RedirectsUnauthenticated() {
        driver.manage().deleteAllCookies();
        ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("window.localStorage.clear();");

        navigateTo("/eventos");
        sleep(2000);

        assertTrue(getCurrentUrl().contains("/iniciar-sesion"), "Deberia redirigir a la pagina de login si no esta autenticado");
    }
}
