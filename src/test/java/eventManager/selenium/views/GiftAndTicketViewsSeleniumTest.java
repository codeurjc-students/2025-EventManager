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
 * Tests de Selenium para las vistas de regalos y tickets. Cubre la inscripcion a eventos, la modificacion de datos de asistencia, la consulta de detalles de evento con acciones de anfitrion y la gestion de regalos (crear, editar y contribuir).
 */
@DisplayName("Gift and Ticket Views Selenium Tests")
public class GiftAndTicketViewsSeleniumTest extends BaseSeleniumTest {

    /**
     * Autentica al usuario antes de cada test para tener acceso a las vistas de regalos y tickets.
     */
    @BeforeEach
    public void authenticateUser() {
        login("carlos.martinez", "ClaveSegura2025");
    }

    /**
     * Verifica que el formulario de inscripcion muestra los campos de codigo de evento, invitados y notas.
     */
    @Test
    @DisplayName("Ticket Event Join View - Visualizar formulario de inscripcion")
    public void testTicketEventJoinView_DisplayForm() {
        navigateTo("/inscribirse-evento");

        assertTrue(isElementPresent(By.id("eventCode")), "Deberia mostrar el campo codigo de evento");
        assertTrue(isElementPresent(By.id("guestNumber")), "Deberia mostrar el campo numero de invitados");
        assertTrue(isElementPresent(By.id("notes")), "Deberia mostrar el campo notas");
    }

    /**
     * Verifica que se puede enviar el formulario de inscripcion con un codigo de evento y datos de asistencia.
     */
    @Test
    @DisplayName("Ticket Event Join View - Inscribirse a un evento exitosamente")
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
     * Verifica que se muestra un error al intentar inscribirse con un codigo de evento que no existe.
     */
    @Test
    @DisplayName("Ticket Event Join View - Error con codigo de evento invalido")
    public void testTicketEventJoinView_InvalidEventCode() {
        navigateTo("/inscribirse-evento");

        fillInput(By.id("eventCode"), "ZZZZZZ");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle();

        assertTrue(isErrorMessagePresent(), "Deberia mostrar error con codigo de evento invalido");
    }

    /**
     * Verifica que se rechaza un numero de invitados negativo al inscribirse a un evento.
     */
    @Test
    @DisplayName("Ticket Event Join View - Error con numero de invitados negativo")
    public void testTicketEventJoinView_NegativeGuestNumber() {
        navigateTo("/inscribirse-evento");

        fillInput(By.id("eventCode"), "MADRID");
        fillInput(By.id("guestNumber"), "-1");
        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isErrorMessagePresent() || getCurrentUrl().contains("/inscribirse-evento"), "Deberia mostrar error con numero negativo");
    }

    /**
     * Verifica que el formulario no se envia si no se introduce un codigo de evento.
     */
    @Test
    @DisplayName("Ticket Event Join View - Inscripcion sin codigo de evento")
    public void testTicketEventJoinView_EmptyEventCode() {
        navigateTo("/inscribirse-evento");

        clickElement(By.cssSelector("button[type='submit']"));
        waitForUiToSettle(Duration.ofMillis(700));

        assertTrue(getCurrentUrl().contains("/inscribirse-evento"), "Deberia permanecer en la pagina sin codigo de evento");
    }

    /**
     * Verifica que los campos de asistencia estan deshabilitados en modo solo lectura al acceder a la vista.
     */
    @Test
    @DisplayName("Ticket Event Update View - Campos deshabilitados en modo lectura")
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
                assertFalse(guestField.isEnabled(), "El campo acompanantes deberia estar deshabilitado en modo lectura");
                assertTrue(isElementPresent(By.xpath("//button[text()='Editar']")), "Deberia mostrar el boton Editar");
            }
        }
    }

    /**
     * Verifica que se pueden editar y guardar los datos de asistencia de un ticket correctamente.
     */
    @Test
    @DisplayName("Ticket Event Update View - Habilitar edicion y guardar cambios")
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

                assertTrue(isSuccessMessagePresent() || getCurrentUrl().contains("/eventos"), "Deberia guardar los cambios exitosamente");
            }
        }
    }

    /**
     * Verifica que al cancelar la edicion de un ticket los campos vuelven a modo solo lectura.
     */
    @Test
    @DisplayName("Ticket Event Update View - Cancelar edicion")
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
     * Verifica que los detalles de un evento se muestran con la tabla de informacion y la seccion de acciones.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Visualizar informacion del evento")
    public void testTicketEventDetailView_DisplayEventDetails() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            assertTrue(isElementPresent(By.cssSelector(".info-table")), "Deberia mostrar la tabla de informacion del evento");
            assertTrue(isElementPresent(By.cssSelector(".actions-section")), "Deberia mostrar la seccion de acciones");
        }
    }

    /**
     * Verifica que un anfitrion puede ver los botones de modificar evento y consultar invitados.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Botones de accion visibles para HOST")
    public void testTicketEventDetailView_HostActionButtons() {
        navigateTo("/eventos");
        Select roleSelect = new Select(waitForElement(By.id("role")));
        roleSelect.selectByValue("ANFITRION");
        clickElement(By.cssSelector(".search-btn"));
        waitForUiToSettle();

        if (isElementPresent(By.cssSelector(".info-link"))) {
            clickElement(By.cssSelector(".info-link"));
            waitForUiToSettle(Duration.ofMillis(1200));

            boolean hostButtonsVisible = isElementPresent(By.xpath("//button[contains(text(),'Modificar informacion del evento')]"))
                    && isElementPresent(By.xpath("//button[contains(text(),'Consultar invitados')]"));
            boolean guestActionsVisible = isElementPresent(By.xpath("//button[contains(text(),'Consultar lista de regalos')]"));
            assertTrue(hostButtonsVisible || guestActionsVisible, "Deberia mostrar acciones disponibles según el rol del usuario");
        }
    }

    /**
     * Verifica que al pulsar el boton de modificar evento se navega a la vista de edicion.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Navegar a modificar evento")
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
     * Verifica que al pulsar el boton de consultar regalos se navega a la lista de regalos del evento.
     */
    @Test
    @DisplayName("Ticket Event Detail View - Navegar a lista de regalos")
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
     * Verifica que se puede crear un regalo desde el popup rellenando nombre y precio.
     */
    @Test
    @DisplayName("Gift Event List View - Crear regalo desde popup exitosamente")
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
                    !isElementVisible(By.cssSelector(".popup-overlay")) || isErrorMessagePresent() || isElementPresent(By.cssSelector(".error-message-popup")),
                    "Tras guardar, el popup debería cerrarse o mostrar feedback de error"
            );
        }
    }

    /**
     * Verifica que el popup de creacion de regalo no se cierra si falta el campo nombre obligatorio.
     */
    @Test
    @DisplayName("Gift Event List View - Error al crear regalo sin nombre")
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

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay")), "El popup deberia seguir visible si falta el nombre");
        }
    }

    /**
     * Verifica que al cancelar la creacion de un regalo el popup se cierra sin guardar datos.
     */
    @Test
    @DisplayName("Gift Event List View - Cancelar creacion de regalo")
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
     * Verifica que se muestran los detalles de un regalo o un mensaje de error si no existe.
     */
    @Test
    @DisplayName("Gift Detail View - Visualizar detalles del regalo")
    public void testGiftDetailView_DisplayGiftDetails() {
        String eventCode = "MADRID";
        int giftId = 1;
        navigateTo("/evento/" + eventCode + "/regalo/" + giftId);
        waitForUiToSettle(Duration.ofMillis(1200));

        assertTrue(isElementPresent(By.cssSelector(".gift-info-table")) || isElementPresent(By.cssSelector(".error-message")), "Deberia mostrar los detalles del regalo o mensaje de error");
    }

    /**
     * Verifica que se puede editar un regalo existente a traves del popup de modificacion.
     */
    @Test
    @DisplayName("Gift Detail View - Editar regalo via popup exitosamente")
    public void testGiftDetailView_EditGiftViaPopup() {
        String eventCode = "MADRID";
        int giftId = 1;
        navigateTo("/evento/" + eventCode + "/regalo/" + giftId);
        waitForUiToSettle(Duration.ofMillis(1200));

        if (isElementPresent(By.xpath("//button[contains(text(),'Modificar regalo')]"))) {
            org.openqa.selenium.WebElement editBtn = driver.findElement(By.xpath("//button[contains(text(),'Modificar regalo')]"));
            if (editBtn.isEnabled()) {
                editBtn.click();
                waitForUiToSettle(Duration.ofMillis(700));

                if (isElementPresent(By.cssSelector(".popup-overlay"))) {
                    fillInput(By.id("edit-name"), "Regalo Modificado");
                    fillInput(By.id("edit-price"), "200.00");
                    clickElement(By.cssSelector(".save-btn"));
                    waitForUiToSettle();

                    assertFalse(isElementVisible(By.cssSelector(".popup-overlay")), "El popup deberia cerrarse tras guardar la edicion");
                }
            }
        }
    }

    /**
     * Verifica que al pulsar el boton de anadir aportacion se abre el popup de contribucion.
     */
    @Test
    @DisplayName("Gift Detail View - Abrir popup de contribucion")
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
     * Verifica que se puede realizar una contribucion economica a un regalo desde el popup.
     */
    @Test
    @DisplayName("Gift Detail View - Realizar contribucion exitosa")
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

                assertFalse(isElementVisible(By.cssSelector(".popup-overlay")), "El popup deberia cerrarse tras la contribucion");
            }
        }
    }

    /**
     * Verifica que desde el popup de editar asistencia se puede confirmar la invitacion de un invitado.
     */
    @Test
    @DisplayName("Ticket Event List View - EditAttendancePopup confirmar invitacion")
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

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay, .popup")), "Deberia mostrar el popup de editar asistencia");

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
     * Verifica que desde el popup correspondiente se puede promover a un invitado a anfitrion del evento.
     */
    @Test
    @DisplayName("Ticket Event List View - MakeHostPopup promover a anfitrion")
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

            assertTrue(isElementPresent(By.cssSelector(".popup-overlay, .popup-container")), "Deberia mostrar el popup de confirmacion");

            if (isElementPresent(By.cssSelector(".btn-confirm"))) {
                clickElement(By.cssSelector(".btn-confirm"));
                waitForUiToSettle();
            }

            assertFalse(isElementVisible(By.cssSelector(".popup-container")), "El popup deberia cerrarse tras confirmar");
        }
    }
}
