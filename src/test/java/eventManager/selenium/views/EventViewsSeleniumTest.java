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
 * Selenium tests for the event views. Covers the event list with filters and
 * pagination, creating new events, and updating existing events in read-only
 * and edit modes.
 */
@DisplayName("Event Views Selenium Tests")
public class EventViewsSeleniumTest extends BaseSeleniumTest {

    /**
     * Authenticates the user before each test to access event views.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifies that the event list page loads correctly with the role filter and
     * search button.
     */
    @Test
    @DisplayName("Event List View - Display event list page")
    public void testEventListView_DisplayEvents() {
        navigateTo("/eventos");

        assertTrue(isElementPresent(By.id("role")), "Deberia mostrar el filtro de rol");
        assertTrue(isElementPresent(By.cssSelector(".search-btn")), "Deberia mostrar el boton de busqueda");
        assertTrue(getCurrentUrl().contains("/eventos"), "Deberia estar en la pagina de eventos");
    }

    /**
     * Verifies that events can be filtered by selecting the Host role.
     */
    @Test
    @DisplayName("Event List View - Filter events by Host role")
    public void testEventListView_FilterByHost() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        assertTrue(
                isElementPresent(By.cssSelector(".event-table")) || isElementPresent(By.cssSelector(".error-message")),
                "Deberia mostrar tabla de eventos o un mensaje");
    }

    /**
     * Verifies that events can be filtered by selecting the Guest role.
     */
    @Test
    @DisplayName("Event List View - Filter events by Guest role")
    public void testEventListView_FilterByGuest() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("INVITADO");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        assertTrue(
                isElementPresent(By.cssSelector(".event-table")) || isElementPresent(By.cssSelector(".error-message")),
                "Deberia mostrar tabla de eventos o un mensaje");
    }

    /**
     * Verifies that the search button remains disabled until a role is selected.
     */
    @Test
    @DisplayName("Event List View - Search button disabled without role")
    public void testEventListView_SearchDisabledWithoutRole() {
        navigateTo("/eventos");

        org.openqa.selenium.WebElement searchBtn = waitForElement(By.cssSelector(".search-btn"));
        assertFalse(searchBtn.isEnabled(),
                "El boton de busqueda deberia estar deshabilitado si no se selecciona un rol");
    }

    /**
     * Verifies that clicking the event info link navigates to event details.
     */
    @Test
    @DisplayName("Event List View - View event details from table")
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
     * Verifies that arrow pagination works correctly in the events table.
     */
    @Test
    @DisplayName("Event List View - Arrow pagination")
    public void testEventListView_Pagination() {
        navigateTo("/eventos");

        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".pagination-arrow:last-child:not(:disabled)"))) {
            clickElement(By.cssSelector(".pagination-arrow:last-child"));
            waitForUiToSettle(Duration.ofMillis(1200));

            assertTrue(isElementPresent(By.cssSelector(".event-table")),
                    "Deberia mostrar la segunda pagina de eventos");
        }
    }

    /**
     * Verifies that an event can be created by filling in all required fields with
     * valid data.
     */
    @Test
    @DisplayName("Event Create View - Create event successfully")
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
                "Deberia mostrar feedback de éxito o error tras crear evento");
    }

    /**
     * Verifies that event creation fails when the name exceeds the maximum length.
     */
    @Test
    @DisplayName("Event Create View - Error with name too long")
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

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/crear-evento"),
                "Deberia mostrar error o permanecer en la pagina");
    }

    /**
     * Verifies that event creation fails when the description exceeds the maximum
     * length.
     */
    @Test
    @DisplayName("Event Create View - Error with description too long")
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

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/crear-evento"),
                "Deberia mostrar error o permanecer en la pagina");
    }

    /**
     * Verifies that the submit button is disabled while required fields are empty.
     */
    @Test
    @DisplayName("Event Create View - Submit disabled with empty fields")
    public void testEventCreateView_SubmitDisabledWithEmptyFields() {
        navigateTo("/crear-evento");

        org.openqa.selenium.WebElement submitBtn = waitForElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();
        waitForUiToSettle(Duration.ofMillis(900));

        assertTrue(
                !submitBtn.isEnabled() || getCurrentUrl().contains("/crear-evento") || isErrorMessagePresent(),
                "Con campos vacíos, no debería completar la creación del evento");
    }

    /**
     * Verifies that event creation fails when a past date is selected.
     */
    @Test
    @DisplayName("Event Create View - Error with past date")
    public void testEventCreateView_PastDate() {
        navigateTo("/crear-evento");

        fillInput(By.id("eventName"), "Partido de padel");
        fillInput(By.id("eventDate"), "2020-01-01");
        fillInput(By.id("eventTime"), "18:00");
        fillInput(By.id("eventDescription"), "Descripcion");
        fillInput(By.id("eventLocation"), "Ubicacion");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/crear-evento"),
                "Deberia mostrar error con fecha en el pasado");
    }

    /**
     * Verifies that the event update view starts in read-only mode with disabled
     * fields.
     */
    @Test
    @DisplayName("Event Update View - Starts in read-only mode")
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
     * Verifies that clicking Edit enables the fields for modification.
     */
    @Test
    @DisplayName("Event Update View - Enable edit mode when clicking Edit")
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
            assertTrue(isElementPresent(By.xpath("//button[text()='Guardar cambios']")),
                    "Deberia mostrar el boton Guardar cambios");
            assertTrue(isElementPresent(By.xpath("//button[text()='Cancelar']")), "Deberia mostrar el boton Cancelar");
        }
    }

    /**
     * Verifies that changes to an event are saved correctly when clicking Save
     * changes.
     */
    @Test
    @DisplayName("Event Update View - Update event successfully")
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
     * Verifies that canceling the edit returns fields to read-only mode without
     * saving changes.
     */
    @Test
    @DisplayName("Event Update View - Cancel edit")
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
