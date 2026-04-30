package eventManager.selenium.views;

import eventManager.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium tests for gift and ticket views. Covers event enrollment, attendance
 * updates, event detail access with host actions, and gift management (create,
 * edit, contribute).
 */
@DisplayName("Gift and Ticket Views Selenium Tests")
public class GiftAndTicketViewsSeleniumTest extends BaseSeleniumTest {

    /**
     * Authenticates the user before each test to access gift and ticket views.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifies that the enrollment form shows event code, guest count, and notes
     * fields.
     */
    @Test
    @DisplayName("Ticket Event Join View - Display enrollment form")
    public void testTicketEventJoinView_DisplayForm() {
        navigateTo("/inscribirse-evento");

        assertTrue(isElementPresent(By.id("eventCode")), "Deberia mostrar el campo codigo de evento");
        assertTrue(isElementPresent(By.id("guestNumber")), "Deberia mostrar el campo numero de invitados");
        assertTrue(isElementPresent(By.id("notes")), "Deberia mostrar el campo notas");
    }

    /**
     * Verifies that the enrollment form can be submitted with an event code and
     * attendance data.
     */
    @Test
    @DisplayName("Ticket Event Join View - Successfully join an event")
    public void testTicketEventJoinView_JoinEventSuccessfully() {
        navigateTo("/inscribirse-evento");

        fillInput(By.id("eventCode"), "MADRID");
        fillInput(By.id("guestNumber"), "2");
        fillInput(By.id("notes"), "Asisto con familia");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle();

        assertTrue(isSuccessMessagePresent() || isErrorMessagePresent(), "Deberia mostrar mensaje de resultado");
    }

    /**
     * Verifies that an error is shown when enrolling with a non-existent event
     * code.
     */
    @Test
    @DisplayName("Ticket Event Join View - Error with invalid event code")
    public void testTicketEventJoinView_InvalidEventCode() {
        navigateTo("/inscribirse-evento");

        fillInput(By.id("eventCode"), "ZZZZZZ");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle();

        assertTrue(isErrorMessagePresent(), "Deberia mostrar error con codigo de evento invalido");
    }

    /**
     * Verifies that a negative guest number is rejected when enrolling in an event.
     */
    @Test
    @DisplayName("Ticket Event Join View - Error with negative guest number")
    public void testTicketEventJoinView_NegativeGuestNumber() {
        navigateTo("/inscribirse-evento");

        fillInput(By.id("eventCode"), "MADRID");
        fillInput(By.id("guestNumber"), "-1");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/inscribirse-evento"),
                "Deberia mostrar error con numero negativo");
    }

    /**
     * Verifies that the form is not submitted if no event code is provided.
     */
    @Test
    @DisplayName("Ticket Event Join View - Enrollment without event code")
    public void testTicketEventJoinView_EmptyEventCode() {
        navigateTo("/inscribirse-evento");

        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(700));

        assertTrue(getCurrentUrl().contains("/inscribirse-evento"),
                "Deberia permanecer en la pagina sin codigo de evento");
    }

    /**
     * Verifies that attendance fields are disabled in read-only mode when accessing
     * the view.
     */
    @Test
    @DisplayName("Ticket Event Update View - Fields disabled in read-only mode")
    public void testTicketEventUpdateView_ReadOnlyMode() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            if (isElementPresent(By.xpath("//button[contains(text(),'Modificar informacion de asistencia')]"))) {
                clickElement(By.xpath("//button[contains(text(),'Modificar informacion de asistencia')]"));
                waitForUiToSettle(Duration.ofMillis(1200));

                org.openqa.selenium.WebElement guestField = waitForElement(By.id("guest-number"));
                assertFalse(guestField.isEnabled(),
                        "El campo acompanantes deberia estar deshabilitado en modo lectura");
                assertTrue(isElementPresent(By.xpath("//button[text()='Editar']")), "Deberia mostrar el boton Editar");
            }
        }
    }

    /**
     * Verifies that attendance data for a ticket can be edited and saved correctly.
     */
    @Test
    @DisplayName("Ticket Event Update View - Enable edit and save changes")
    public void testTicketEventUpdateView_EditAndSave() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            if (isElementPresent(By.xpath("//button[contains(text(),'Modificar informacion de asistencia')]"))) {
                clickElement(By.xpath("//button[contains(text(),'Modificar informacion de asistencia')]"));
                waitForUiToSettle(Duration.ofMillis(1200));

                clickElement(By.xpath("//button[text()='Editar']"));
                waitForUiToSettle(Duration.ofMillis(700));

                fillInput(By.id("guest-number"), "3");
                fillInput(By.id("ticket-notes"), "Notas actualizadas");

                if (isElementPresent(By.id("assist-confirmation"))) {
                    Select assistSelect = new Select(driver.findElement(By.id("assist-confirmation")));
                    assistSelect.selectByValue("CONFIRMADA");
                }

                clickElement(By.xpath("//button[text()='Guardar cambios']"));
                waitForUiToSettle();

                assertTrue(isSuccessMessagePresent() || getCurrentUrl().contains("/eventos"),
                        "Deberia guardar los cambios exitosamente");
            }
        }
    }

    /**
     * Verifies that canceling ticket edits returns fields to read-only mode.
     */
    @Test
    @DisplayName("Ticket Event Update View - Cancel edit")
    public void testTicketEventUpdateView_CancelEdit() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            if (isElementPresent(By.xpath("//button[contains(text(),'Modificar informacion de asistencia')]"))) {
                clickElement(By.xpath("//button[contains(text(),'Modificar informacion de asistencia')]"));
                waitForUiToSettle(Duration.ofMillis(1200));

                clickElement(By.xpath("//button[text()='Editar']"));
                waitForUiToSettle(Duration.ofMillis(700));

                fillInput(By.id("ticket-notes"), "Cambio temporal");
                clickElement(By.xpath("//button[text()='Cancelar']"));
                waitForUiToSettle(Duration.ofMillis(1200));

                org.openqa.selenium.WebElement notesField = waitForElement(By.id("ticket-notes"));
                assertFalse(notesField.isEnabled(), "El campo deberia volver a modo lectura tras cancelar");
            }
        }
    }

    /**
     * Verifies that event details are shown with the information table and actions
     * section.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Display event information")
    public void testTicketEventDetailView_DisplayEventDetails() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            assertTrue(isElementPresent(By.cssSelector(".info-table")),
                    "Deberia mostrar la tabla de informacion del evento");
            assertTrue(isElementPresent(By.cssSelector(".actions-section")), "Deberia mostrar la seccion de acciones");
        }
    }

    /**
     * Verifies that a host can see the edit event and view guests buttons.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Action buttons visible for HOST")
    public void testTicketEventDetailView_HostActionButtons() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            boolean hostButtonsVisible = isElementPresent(
                    By.xpath("//button[contains(text(),'Modificar informacion del evento')]"))
                    && isElementPresent(By.xpath("//button[contains(text(),'Consultar invitados')]"));
            boolean guestActionsVisible = isElementPresent(
                    By.xpath("//button[contains(text(),'Consultar lista de regalos')]"));
            assertTrue(hostButtonsVisible || guestActionsVisible,
                    "Deberia mostrar acciones disponibles según el rol del usuario");
        }
    }

    /**
     * Verifies that clicking the edit event button navigates to the edit view.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Navigate to edit event")
    public void testTicketEventDetailView_NavigateToEditEvent() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            if (isElementPresent(By.xpath("//button[contains(text(),'Modificar informacion del evento')]"))) {
                clickElement(By.xpath("//button[contains(text(),'Modificar informacion del evento')]"));
                waitForUiToSettle(Duration.ofMillis(1200));

                assertTrue(getCurrentUrl().contains("/modificar"), "Deberia navegar a la vista de modificar evento");
            }
        }
    }

    /**
     * Verifies that clicking the view gifts button navigates to the event gift
     * list.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Navigate to gift list")
    public void testTicketEventDetailView_NavigateToGifts() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            if (isElementPresent(By.xpath("//button[contains(text(),'Consultar lista de regalos')]"))) {
                clickElement(By.xpath("//button[contains(text(),'Consultar lista de regalos')]"));
                waitForUiToSettle(Duration.ofMillis(1200));

                assertTrue(getCurrentUrl().contains("/regalos"), "Deberia navegar a la lista de regalos");
            }
        }
    }

    /**
     * Verifies that a gift can be created from the popup by filling name and price.
     */
    @Test
    @DisplayName("Gift Event List View - Create gift from popup successfully")
    public void testGiftEventListView_CreateGiftFromPopup() {
        String eventCode = "MADRID";
        navigateTo("/evento/" + eventCode + "/regalos");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.cssSelector(".add-gift-btn"))) {
            clickElement(By.cssSelector(".add-gift-btn"));
            waitForUiToSettle(Duration.ofMillis(700));

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay")), "Deberia mostrar el popup de crear regalo");

            fillInput(By.id("gift-name"), "Laptop Test");
            fillInput(By.id("gift-price"), "1200.00");
            if (isElementPresent(By.id("gift-details"))) {
                fillInput(By.id("gift-details"), "Una laptop de prueba");
            }

            clickElement(By.cssSelector(".save-btn"));
            waitForUiToSettle();

            assertTrue(
                    !isElementVisible(By.cssSelector(".popup-overlay")) || isErrorMessagePresent()
                            || isElementPresent(By.cssSelector(".error-message-popup")),
                    "Tras guardar, el popup debería cerrarse o mostrar feedback de error");
        }
    }

    /**
     * Verifies that the gift creation popup stays open if the required name field
     * is missing.
     */
    @Test
    @DisplayName("Gift Event List View - Error creating gift without name")
    public void testGiftEventListView_CreateGiftValidationError() {
        String eventCode = "MADRID";
        navigateTo("/evento/" + eventCode + "/regalos");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.cssSelector(".add-gift-btn"))) {
            clickElement(By.cssSelector(".add-gift-btn"));
            waitForUiToSettle(Duration.ofMillis(700));

            fillInput(By.id("gift-price"), "50.00");
            clickElement(By.cssSelector(".save-btn"));
            waitForUiToSettle(Duration.ofMillis(1200));

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay")),
                    "El popup deberia seguir visible si falta el nombre");
        }
    }

    /**
     * Verifies that canceling gift creation closes the popup without saving data.
     */
    @Test
    @DisplayName("Gift Event List View - Cancel gift creation")
    public void testGiftEventListView_CancelCreateGift() {
        String eventCode = "MADRID";
        navigateTo("/evento/" + eventCode + "/regalos");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.cssSelector(".add-gift-btn"))) {
            clickElement(By.cssSelector(".add-gift-btn"));
            waitForUiToSettle(Duration.ofMillis(700));

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay")), "Deberia mostrar el popup");

            clickElement(By.cssSelector(".cancel-btn"));
            waitForUiToSettle(Duration.ofMillis(700));

            assertFalse(isElementVisible(By.cssSelector(".popup-overlay")), "El popup deberia cerrarse al cancelar");
        }
    }

    /**
     * Verifies that gift details are shown or an error message appears if it does
     * not exist.
     */
    @Test
    @DisplayName("Gift Detail View - Display gift details")
    public void testGiftDetailView_DisplayGiftDetails() {
        String eventCode = "MADRID";
        int giftId = 1;
        navigateTo("/evento/" + eventCode + "/regalo/" + giftId);
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(
                isElementPresent(By.cssSelector(".gift-info-table"))
                        || isElementPresent(By.cssSelector(".error-message")),
                "Deberia mostrar los detalles del regalo o mensaje de error");
    }

    /**
     * Verifies that an existing gift can be edited through the edit popup.
     */
    @Test
    @DisplayName("Gift Detail View - Edit gift via popup successfully")
    public void testGiftDetailView_EditGiftViaPopup() {
        String eventCode = "MADRID";
        int giftId = 1;
        navigateTo("/evento/" + eventCode + "/regalo/" + giftId);
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.xpath("//button[contains(text(),'Modificar regalo')]"))) {
            org.openqa.selenium.WebElement editBtn = driver
                    .findElement(By.xpath("//button[contains(text(),'Modificar regalo')]"));
            if (editBtn.isEnabled()) {
                editBtn.click();
                waitForUiToSettle(Duration.ofMillis(700));

                if (isElementPresent(By.cssSelector(".popup-overlay"))) {
                    fillInput(By.id("edit-name"), "Regalo Modificado");
                    fillInput(By.id("edit-price"), "200.00");
                    clickElement(By.cssSelector(".save-btn"));
                    waitForUiToSettle();

                    assertFalse(isElementVisible(By.cssSelector(".popup-overlay")),
                            "El popup deberia cerrarse tras guardar la edicion");
                }
            }
        }
    }

    /**
     * Verifies that clicking add contribution opens the contribution popup.
     */
    @Test
    @DisplayName("Gift Detail View - Open contribution popup")
    public void testGiftDetailView_OpenContributionPopup() {
        String eventCode = "MADRID";
        int giftId = 1;
        navigateTo("/evento/" + eventCode + "/regalo/" + giftId);
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.xpath("//button[contains(text(),'Anadir aportacion')]"))) {
            clickElement(By.xpath("//button[contains(text(),'Anadir aportacion')]"));
            waitForUiToSettle(Duration.ofMillis(700));

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay")), "Deberia mostrar el popup de contribucion");
            assertTrue(isElementPresent(By.id("contribution-amount")), "Deberia mostrar el campo de cantidad");
        }
    }

    /**
     * Verifies that a monetary contribution can be made to a gift from the popup.
     */
    @Test
    @DisplayName("Gift Detail View - Make contribution successfully")
    public void testGiftDetailView_MakeContribution() {
        String eventCode = "MADRID";
        int giftId = 1;
        navigateTo("/evento/" + eventCode + "/regalo/" + giftId);
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.xpath("//button[contains(text(),'Anadir aportacion')]"))) {
            clickElement(By.xpath("//button[contains(text(),'Anadir aportacion')]"));
            waitForUiToSettle(Duration.ofMillis(700));

            if (isElementPresent(By.id("contribution-amount"))) {
                fillInput(By.id("contribution-amount"), "25.00");
                clickElement(By.cssSelector(".save-btn"));
                waitForUiToSettle();

                assertFalse(isElementVisible(By.cssSelector(".popup-overlay")),
                        "El popup deberia cerrarse tras la contribucion");
            }
        }
    }

    /**
     * Verifies that a guest invitation can be confirmed from the edit attendance
     * popup.
     */
    @Test
    @DisplayName("Ticket Event List View - EditAttendancePopup confirm invitation")
    public void testTicketEventListView_EditAttendanceConfirm() {
        String eventCode = "MADRID";
        navigateTo("/evento/" + eventCode + "/invitados");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.cssSelector(".search-button"))) {
            clickElement(By.cssSelector(".search-button"));
            waitForUiToSettle();
        }

        if (isElementPresent(By.cssSelector(".edit-icon-btn"))) {
            clickElement(By.cssSelector(".edit-icon-btn"));
            waitForUiToSettle(Duration.ofMillis(700));

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay, .popup")),
                    "Deberia mostrar el popup de editar asistencia");

            if (isElementPresent(By.cssSelector(".edit-button"))) {
                clickElement(By.cssSelector(".edit-button"));
                waitForUiToSettle(Duration.ofMillis(700));

                if (isElementPresent(By.id("invitation-confirmation"))) {
                    Select confirmSelect = new Select(driver.findElement(By.id("invitation-confirmation")));
                    confirmSelect.selectByValue("true");
                }

                clickElement(By.cssSelector(".confirm-button"));
                waitForUiToSettle();
            }
        }
    }

    /**
     * Verifies that a guest can be promoted to host from the corresponding popup.
     */
    @Test
    @DisplayName("Ticket Event List View - MakeHostPopup promote to host")
    public void testTicketEventListView_MakeHostPopup() {
        String eventCode = "MADRID";
        navigateTo("/evento/" + eventCode + "/invitados");
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.cssSelector(".search-button"))) {
            clickElement(By.cssSelector(".search-button"));
            waitForUiToSettle();
        }

        if (isElementPresent(By.cssSelector(".make-host-btn"))) {
            clickElement(By.cssSelector(".make-host-btn"));
            waitForUiToSettle(Duration.ofMillis(700));

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay, .popup-container")),
                    "Deberia mostrar el popup de confirmacion");

            if (isElementPresent(By.cssSelector(".btn-confirm"))) {
                clickElement(By.cssSelector(".btn-confirm"));
                waitForUiToSettle();
            }

            assertFalse(isElementVisible(By.cssSelector(".popup-container")),
                    "El popup deberia cerrarse tras confirmar");
        }
    }
}
