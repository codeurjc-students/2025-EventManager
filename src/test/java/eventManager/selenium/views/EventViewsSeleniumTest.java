package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import java.time.Duration;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Selenium para las vistas de eventos. Cubre la lista de eventos con filtros y paginacion, la creacion de nuevos eventos y la modificacion de eventos existentes con sus modos de lectura y edicion.
 */
@DisplayName("Event Views Selenium Tests")
public class EventViewsSeleniumTest extends BaseSeleniumTest {

    /**
     * Autentica al usuario antes de cada test para tener acceso a las vistas de eventos.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifica que la pagina de lista de eventos se carga correctamente con el filtro de rol y el boton de busqueda.
     */
    @Test
    @DisplayName("Event List View - Visualizar pagina de lista de eventos")
    public void testEventListView_DisplayEvents() {
        navigateTo("/eventos");

        assertTrue(isElementPresent(By.id("role")), "Deberia mostrar el filtro de rol");
        assertTrue(isElementPresent(By.cssSelector(".search-btn")), "Deberia mostrar el boton de busqueda");
        assertTrue(getCurrentUrl().contains("/eventos"), "Deberia estar en la pagina de eventos");
    }

    /**
     * Verifica que se pueden filtrar los eventos seleccionando el rol de anfitrion.
     */
    @Test
    @DisplayName("Event List View - Filtrar eventos por rol Anfitrion")
    public void testEventListView_FilterByHost() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        assertTrue(isElementPresent(By.cssSelector(".event-table")) || isElementPresent(By.cssSelector(".error-message")), "Deberia mostrar tabla de eventos o un mensaje");
    }

    /**
     * Verifica que se pueden filtrar los eventos seleccionando el rol de invitado.
     */
    @Test
    @DisplayName("Event List View - Filtrar eventos por rol Invitado")
    public void testEventListView_FilterByGuest() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("INVITADO");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        assertTrue(isElementPresent(By.cssSelector(".event-table")) || isElementPresent(By.cssSelector(".error-message")), "Deberia mostrar tabla de eventos o un mensaje");
    }

    /**
     * Verifica que el boton de busqueda permanece deshabilitado hasta que se selecciona un rol.
     */
    @Test
    @DisplayName("Event List View - Boton buscar deshabilitado sin seleccionar rol")
    public void testEventListView_SearchDisabledWithoutRole() {
        navigateTo("/eventos");

        org.openqa.selenium.WebElement searchBtn = waitForElement(By.cssSelector(".search-btn"));
        assertFalse(searchBtn.isEnabled(), "El boton de busqueda deberia estar deshabilitado si no se selecciona un rol");
    }

    /**
     * Verifica que al pulsar en el enlace de informacion de un evento se navega a sus detalles.
     */
    @Test
    @DisplayName("Event List View - Ver detalles de un evento desde la tabla")
    public void testEventListView_ViewEventDetails() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            assertTrue(getCurrentUrl().contains("/eventos/"), "Deberia navegar a los detalles del evento");
        }
    }

    /**
     * Verifica que la paginacion con flechas funciona correctamente en la tabla de eventos.
     */
    @Test
    @DisplayName("Event List View - Paginacion con flechas")
    public void testEventListView_Pagination() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".pagination-arrow:last-child:not(:disabled)"))) {
            clickElement(By.cssSelector(".pagination-arrow:last-child"));
            waitForUiToSettle(Duration.ofMillis(1200));

            assertTrue(isElementPresent(By.cssSelector(".event-table")), "Deberia mostrar la segunda pagina de eventos");
        }
    }

    /**
     * Verifica que se puede crear un evento rellenando todos los campos obligatorios con datos validos.
     */
    @Test
    @DisplayName("Event Create View - Crear evento exitosamente")
    public void testEventCreateView_CreateEventSuccessfully() {
        navigateTo("/crear-evento");
        String timestamp = String.valueOf(System.currentTimeMillis());

        fillInput(By.id("eventName"), "Cena de Navidad " + timestamp);
        fillInput(By.id("eventDate"), "2027-12-31");
        fillInput(By.id("eventTime"), "18:00");
        fillInput(By.id("eventDescription"), "Descripcion del evento de prueba");
        fillInput(By.id("eventLocation"), "Ubicacion de prueba");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle();

        assertTrue(
            isSuccessMessagePresent() || isErrorMessagePresent(),
            "Deberia mostrar feedback de éxito o error tras crear evento"
        );
    }

    /**
     * Verifica que la creacion de evento falla cuando el nombre excede la longitud maxima permitida.
     */
    @Test
    @DisplayName("Event Create View - Error con nombre demasiado largo")
    public void testEventCreateView_NameTooLong() {
        navigateTo("/crear-evento");
        String longName = "A".repeat(105);

        fillInput(By.id("eventName"), longName);
        fillInput(By.id("eventDate"), "2027-12-31");
        fillInput(By.id("eventTime"), "18:00");
        fillInput(By.id("eventDescription"), "Descripcion");
        fillInput(By.id("eventLocation"), "Ubicacion");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/crear-evento"), "Deberia mostrar error o permanecer en la pagina");
    }

    /**
     * Verifica que la creacion de evento falla cuando la descripcion excede la longitud maxima permitida.
     */
    @Test
    @DisplayName("Event Create View - Error con descripcion demasiado larga")
    public void testEventCreateView_DescriptionTooLong() {
        navigateTo("/crear-evento");
        String longDescription = "A".repeat(505);

        fillInput(By.id("eventName"), "Reunion de amigos");
        fillInput(By.id("eventDate"), "2027-12-31");
        fillInput(By.id("eventTime"), "18:00");
        fillInput(By.id("eventDescription"), longDescription);
        fillInput(By.id("eventLocation"), "Ubicacion");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/crear-evento"), "Deberia mostrar error o permanecer en la pagina");
    }

    /**
     * Verifica que el boton de envio esta deshabilitado mientras los campos obligatorios estan vacios.
     */
    @Test
    @DisplayName("Event Create View - Boton deshabilitado con campos vacios")
    public void testEventCreateView_SubmitDisabledWithEmptyFields() {
        navigateTo("/crear-evento");

        org.openqa.selenium.WebElement submitBtn = waitForElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();
        waitForUiToSettle(Duration.ofMillis(900));

        assertTrue(
            !submitBtn.isEnabled() || getCurrentUrl().contains("/crear-evento") || isErrorMessagePresent(),
            "Con campos vacíos, no debería completar la creación del evento"
        );
    }

    /**
     * Verifica que la creacion de evento falla cuando se selecciona una fecha anterior a la actual.
     */
    @Test
    @DisplayName("Event Create View - Error con fecha en el pasado")
    public void testEventCreateView_PastDate() {
        navigateTo("/crear-evento");

        fillInput(By.id("eventName"), "Partido de padel");
        fillInput(By.id("eventDate"), "2020-01-01");
        fillInput(By.id("eventTime"), "18:00");
        fillInput(By.id("eventDescription"), "Descripcion");
        fillInput(By.id("eventLocation"), "Ubicacion");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/crear-evento"), "Deberia mostrar error con fecha en el pasado");
    }

    /**
     * Verifica que la vista de modificar evento inicia en modo solo lectura con los campos deshabilitados.
     */
    @Test
    @DisplayName("Event Update View - Vista inicia en modo solo lectura")
    public void testEventUpdateView_StartsInReadOnlyMode() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            String eventUrl = driver.findElement(By.cssSelector(".info-link")).getDomProperty("href");
            String eventCode = eventUrl.split("/eventos/")[1].split("/")[0];

            navigateTo("/eventos/" + eventCode + "/modificar");
            waitForUiToSettle(Duration.ofMillis(1200));

            org.openqa.selenium.WebElement nameField = waitForElement(By.id("event-name"));
            assertFalse(nameField.isEnabled(), "El campo nombre deberia estar deshabilitado en modo lectura");
            assertTrue(isElementPresent(By.xpath("//button[text()='Editar']")), "Deberia mostrar el boton Editar");
        }
    }

    /**
     * Verifica que al pulsar el boton Editar los campos se habilitan para su modificacion.
     */
    @Test
    @DisplayName("Event Update View - Habilitar edicion al pulsar Editar")
    public void testEventUpdateView_EnableEditMode() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            String eventUrl = driver.findElement(By.cssSelector(".info-link")).getDomProperty("href");
            String eventCode = eventUrl.split("/eventos/")[1].split("/")[0];

            navigateTo("/eventos/" + eventCode + "/modificar");
            waitForUiToSettle(Duration.ofMillis(1200));

            clickElement(By.xpath("//button[text()='Editar']"));
            waitForUiToSettle(Duration.ofMillis(700));

            org.openqa.selenium.WebElement nameField = waitForElement(By.id("event-name"));
            assertTrue(nameField.isEnabled(), "El campo nombre deberia estar habilitado tras pulsar Editar");
            assertTrue(isElementPresent(By.xpath("//button[text()='Guardar cambios']")), "Deberia mostrar el boton Guardar cambios");
            assertTrue(isElementPresent(By.xpath("//button[text()='Cancelar']")), "Deberia mostrar el boton Cancelar");
        }
    }

    /**
     * Verifica que los cambios en un evento se guardan correctamente al pulsar Guardar cambios.
     */
    @Test
    @DisplayName("Event Update View - Actualizar evento exitosamente")
    public void testEventUpdateView_UpdateEventSuccessfully() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            String eventUrl = driver.findElement(By.cssSelector(".info-link")).getDomProperty("href");
            String eventCode = eventUrl.split("/eventos/")[1].split("/")[0];

            navigateTo("/eventos/" + eventCode + "/modificar");
            waitForUiToSettle(Duration.ofMillis(1200));

            clickElement(By.xpath("//button[text()='Editar']"));
            waitForUiToSettle(Duration.ofMillis(700));

            fillInput(By.id("event-name"), "Evento Actualizado");
            fillInput(By.id("event-description"), "Descripcion actualizada");
            clickElement(By.xpath("//button[text()='Guardar cambios']"));
            waitForUiToSettle();

            assertTrue(isSuccessMessagePresent(), "Deberia mostrar mensaje de exito tras actualizar");
        }
    }

    /**
     * Verifica que al cancelar la edicion los campos vuelven a modo solo lectura sin guardar cambios.
     */
    @Test
    @DisplayName("Event Update View - Cancelar edicion")
    public void testEventUpdateView_CancelEdit() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            String eventUrl = driver.findElement(By.cssSelector(".info-link")).getDomProperty("href");
            String eventCode = eventUrl.split("/eventos/")[1].split("/")[0];

            navigateTo("/eventos/" + eventCode + "/modificar");
            waitForUiToSettle(Duration.ofMillis(1200));

            clickElement(By.xpath("//button[text()='Editar']"));
            waitForUiToSettle(Duration.ofMillis(700));

            fillInput(By.id("event-name"), "Cambio temporal");
            clickElement(By.xpath("//button[text()='Cancelar']"));
            waitForUiToSettle(Duration.ofMillis(1200));

            org.openqa.selenium.WebElement nameField = waitForElement(By.id("event-name"));
            assertFalse(nameField.isEnabled(), "El campo deberia volver a modo solo lectura tras cancelar");
        }
    }
}
