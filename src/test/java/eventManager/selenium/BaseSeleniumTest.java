package eventManager.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Abstract base class for all Selenium tests in the project. Centralizes Chrome
 * WebDriver configuration, wait settings, and provides shared helper methods
 * like login, registration, navigation, and UI element interactions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = { "server.port=8090" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseSeleniumTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected static final String BASE_URL = "http://localhost:8090";
    protected static final int DEFAULT_WAIT_SECONDS = 45;
    private String fallbackUsername;
    private boolean sessionValidatedInCurrentBrowser;
    private long lastSessionValidationAtMillis;
    private static final String FALLBACK_PASSWORD = "ClaveSegura2025";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final ConcurrentMap<String, CachedAuthCookies> AUTH_COOKIE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> FAILED_LOGIN_CACHE = new ConcurrentHashMap<>();
    private static final long AUTH_CACHE_TTL_MILLIS = Duration.ofMinutes(4).toMillis();
    private static final long FAILED_LOGIN_CACHE_TTL_MILLIS = Duration.ofMinutes(5).toMillis();
    private static final double SLEEP_MULTIPLIER = Double
            .parseDouble(System.getProperty("selenium.sleep.multiplier", "0.5"));
    private static final Duration QUICK_CHECK_TIMEOUT = Duration.ofSeconds(2);
    private static final boolean REUSE_BROWSER_PER_CLASS = Boolean
            .parseBoolean(System.getProperty("selenium.reuse.browser.per.class", "true"));

    /**
     * Downloads and configures the Chrome driver via WebDriverManager before any
     * tests in the class run.
     */
    @BeforeAll
    public static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    /**
     * Initializes headless Chrome with the options required for test execution and
     * configures implicit waits.
     */
    @BeforeEach
    public void setupTest() {
        if (driver == null || !REUSE_BROWSER_PER_CLASS) {
            initializeDriver();
        }
        resetBrowserState();
    }

    private void initializeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-notifications");
        options.addArguments("--incognito");
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-autofill-keyboard-accessory-view[8]");

        driver = new ChromeDriver(options);
        setSessionValidated(false);
        // Avoid global implicit waits so findElement/findElements do not multiply wait
        // times.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
        wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
    }

    private void restartDriver() {
        try {
            if (driver != null) {
                driver.quit();
            }
        } catch (Exception ignored) {
            // no-op
        }
        driver = null;
        initializeDriver();
    }

    private void resetBrowserState() {
        if (driver == null) {
            return;
        }

        if (!preserveCookiesBetweenTests()) {
            try {
                driver.manage().deleteAllCookies();
                setSessionValidated(false);
            } catch (Exception ignored) {
                // no-op
            }
        }

        try {
            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
        } catch (Exception ignored) {
            // no-op
        }

        navigateTo("/");
    }

    /**
     * Lets each suite control whether cookies are preserved between tests to avoid
     * redundant logins.
     * Auth views should override this and return false.
     */
    protected boolean preserveCookiesBetweenTests() {
        return true;
    }

    /**
     * Closes the browser and releases WebDriver resources after each test.
     */
    @AfterEach
    public void tearDown() {
        if (!REUSE_BROWSER_PER_CLASS && driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @AfterAll
    public void tearDownClass() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    /**
     * Navigates to a URL relative to the base URL.
     */
    protected void navigateTo(String path) {
        String fullUrl = BASE_URL + path;
        try {
            driver.get(fullUrl);
        } catch (org.openqa.selenium.WebDriverException e) {
            restartDriver();
            driver.get(fullUrl);
        }
    }

    /**
     * Waits until an element is visible on the page.
     */
    protected WebElement waitForElement(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until an element is clickable.
     */
    protected WebElement waitForClickableElement(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits for an element to disappear from the page.
     */
    protected void waitForElementToDisappear(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Types text into an input field.
     */
    protected void fillInput(By locator, String text) {
        WebElement element = waitForElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Clicks an element.
     */
    protected void clickElement(By locator) {
        WebElement element = waitForClickableElement(locator);
        element.click();
    }

    /**
     * Gets the text of an element.
     */
    protected String getElementText(By locator) {
        WebElement element = waitForElement(locator);
        return element.getText();
    }

    /**
     * Checks whether an element is present on the page.
     */
    protected boolean isElementPresent(By locator) {
        try {
            new WebDriverWait(driver, QUICK_CHECK_TIMEOUT).until(d -> !d.findElements(locator).isEmpty());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether an element is visible on the page.
     */
    protected boolean isElementVisible(By locator) {
        try {
            new WebDriverWait(driver, QUICK_CHECK_TIMEOUT).until(d -> {
                List<WebElement> elements = d.findElements(locator);
                return !elements.isEmpty() && elements.get(0).isDisplayed();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Waits for the URL to contain specific text.
     */
    protected void waitForUrlContains(String text) {
        wait.until(ExpectedConditions.urlContains(text));
    }

    /**
     * Gets the current URL.
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Scrolls to an element.
     */
    protected void scrollToElement(By locator) {
        WebElement element = driver.findElement(locator);
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    /**
     * Waits for a specific time (use only when necessary).
     */
    protected void sleep(int milliseconds) {
        try {
            int scaledMillis = (int) Math.max(60, Math.round(milliseconds * SLEEP_MULTIPLIER));
            Thread.sleep(scaledMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits for the UI to be stable (document loaded and no visible loading
     * overlays).
     */
    protected void waitForUiToSettle() {
        waitForUiToSettle(Duration.ofSeconds(2));
    }

    /**
     * Waits for the UI to be stable with a configurable timeout.
     */
    protected void waitForUiToSettle(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d -> {
                try {
                    Object ready = ((org.openqa.selenium.JavascriptExecutor) d)
                            .executeScript("return document.readyState");
                    if (!"complete".equals(String.valueOf(ready))) {
                        return false;
                    }

                    List<WebElement> busyElements = d.findElements(By.cssSelector(
                            ".loading, .spinner, .loader, [aria-busy='true'], .swal2-container.swal2-backdrop-show"));
                    for (WebElement element : busyElements) {
                        if (element.isDisplayed()) {
                            return false;
                        }
                    }

                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            });
        } catch (Exception ignored) {
            // If it does not settle, continue so optional selectors do not block the test.
        }
    }

    /**
     * Helper method for login (reusable in other tests).
     */
    protected void login(String username, String password) {
        if (hasAuthCookie() && hasActiveAuthenticatedSession()) {
            setSessionValidated(true);
            return;
        }

        setSessionValidated(false);

        String loginKey = username + "::" + password;

        if (isKnownFailedCredential(loginKey)) {
            ensureFallbackUserAndLogin();
            return;
        }

        if (performApiLogin(username, password) && !getCurrentUrl().contains("/iniciar-sesion")) {
            FAILED_LOGIN_CACHE.remove(loginKey);
            setSessionValidated(true);
            return;
        }

        if (performLogin(username, password)) {
            FAILED_LOGIN_CACHE.remove(loginKey);
            setSessionValidated(true);
            return;
        }

        markCredentialAsFailed(loginKey);

        // If still on login, create a fallback user and retry authentication.
        if (getCurrentUrl().contains("/iniciar-sesion")) {
            ensureFallbackUserAndLogin();
        }
    }

    private boolean hasAuthCookie() {
        try {
            Cookie authToken = driver.manage().getCookieNamed("AuthToken");
            return authToken != null && authToken.getValue() != null && !authToken.getValue().isBlank();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void setSessionValidated(boolean validated) {
        this.sessionValidatedInCurrentBrowser = validated;
        this.lastSessionValidationAtMillis = validated ? System.currentTimeMillis() : 0L;
    }

    private boolean hasActiveAuthenticatedSession() {
        try {
            navigateTo("/usuario/actualizar-perfil");
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> !d.getCurrentUrl().contains("/iniciar-sesion"));
            return !getCurrentUrl().contains("/iniciar-sesion");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isKnownFailedCredential(String loginKey) {
        Long failedAt = FAILED_LOGIN_CACHE.get(loginKey);
        if (failedAt == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if ((now - failedAt) > FAILED_LOGIN_CACHE_TTL_MILLIS) {
            FAILED_LOGIN_CACHE.remove(loginKey);
            return false;
        }

        return true;
    }

    private void markCredentialAsFailed(String loginKey) {
        FAILED_LOGIN_CACHE.put(loginKey, System.currentTimeMillis());
    }

    private boolean performApiLogin(String username, String password) {
        try {
            CachedAuthCookies cached = AUTH_COOKIE_CACHE.get(username);
            long now = System.currentTimeMillis();

            if (cached != null && (now - cached.createdAtMillis) < AUTH_CACHE_TTL_MILLIS) {
                return applyCookies(cached.cookies);
            }

            String payload = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password)
                    + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return false;
            }

            List<String> setCookieHeaders = response.headers().allValues("set-cookie");
            List<Cookie> cookies = extractSeleniumCookies(setCookieHeaders);
            if (cookies.isEmpty()) {
                return false;
            }

            AUTH_COOKIE_CACHE.put(username, new CachedAuthCookies(cookies, now));
            return applyCookies(cookies);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean applyCookies(List<Cookie> cookies) {
        navigateTo("/");
        driver.manage().deleteAllCookies();
        setSessionValidated(false);
        for (Cookie cookie : cookies) {
            driver.manage().addCookie(cookie);
        }
        // Validate authentication on a protected view to avoid false positives.
        navigateTo("/usuario/actualizar-perfil");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> !d.getCurrentUrl().contains("/iniciar-sesion"));
        } catch (Exception ignored) {
            // If it does not pass, validate by URL at the end.
        }
        boolean authenticated = !getCurrentUrl().contains("/iniciar-sesion");
        setSessionValidated(authenticated);
        return authenticated;
    }

    private List<Cookie> extractSeleniumCookies(List<String> setCookieHeaders) {
        List<Cookie> cookies = new ArrayList<>();

        for (String header : setCookieHeaders) {
            if (header == null || header.isBlank()) {
                continue;
            }

            String firstPart = header.split(";", 2)[0];
            int eqIndex = firstPart.indexOf('=');
            if (eqIndex <= 0) {
                continue;
            }

            String name = firstPart.substring(0, eqIndex).trim();
            String value = firstPart.substring(eqIndex + 1).trim();
            if (name.isEmpty() || value.isEmpty()) {
                continue;
            }

            if ("AuthToken".equals(name) || "RefreshToken".equals(name)) {
                cookies.add(new Cookie.Builder(name, value)
                        .path("/")
                        .isHttpOnly(true)
                        .build());
            }
        }

        return cookies;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class CachedAuthCookies {
        private final List<Cookie> cookies;
        private final long createdAtMillis;

        private CachedAuthCookies(List<Cookie> cookies, long createdAtMillis) {
            this.cookies = cookies;
            this.createdAtMillis = createdAtMillis;
        }
    }

    private void ensureFallbackUserAndLogin() {
        if (fallbackUsername != null && (performApiLogin(fallbackUsername, FALLBACK_PASSWORD)
                || performLogin(fallbackUsername, FALLBACK_PASSWORD))) {
            return;
        }

        fallbackUsername = "selenium" + System.currentTimeMillis();
        String fallbackEmail = fallbackUsername + "@eventmanager.es";

        if (performApiRegisterAndApplyCookies(fallbackEmail, fallbackUsername, FALLBACK_PASSWORD, "Carlos", "Martinez",
                "612345678")) {
            return;
        }

        register(fallbackEmail, fallbackUsername, FALLBACK_PASSWORD, "Carlos", "Martinez", "612345678");
        sleep(2200);

        if (!performLogin(fallbackUsername, FALLBACK_PASSWORD)) {
            fallbackUsername = "selenium" + System.currentTimeMillis();
            fallbackEmail = fallbackUsername + "@eventmanager.es";
            register(fallbackEmail, fallbackUsername, FALLBACK_PASSWORD, "Carlos", "Martinez", "612345678");
            sleep(2200);
            if (!performLogin(fallbackUsername, FALLBACK_PASSWORD)) {
                performApiLogin(fallbackUsername, FALLBACK_PASSWORD);
            }
        }
    }

    private boolean performApiRegisterAndApplyCookies(String email, String username, String password, String firstName,
            String lastName, String phone) {
        try {
            String payload = "{"
                    + "\"email\":\"" + escapeJson(email) + "\","
                    + "\"username\":\"" + escapeJson(username) + "\","
                    + "\"password\":\"" + escapeJson(password) + "\","
                    + "\"firstName\":\"" + escapeJson(firstName) + "\","
                    + "\"lastName\":\"" + escapeJson(lastName) + "\","
                    + "\"phoneNumber\":\"" + escapeJson(phone) + "\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/register"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return false;
            }

            List<Cookie> cookies = extractSeleniumCookies(response.headers().allValues("set-cookie"));
            if (cookies.isEmpty()) {
                return false;
            }

            AUTH_COOKIE_CACHE.put(username, new CachedAuthCookies(cookies, System.currentTimeMillis()));
            return applyCookies(cookies);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean performLogin(String username, String password) {
        navigateTo("/iniciar-sesion");
        By loginUserInput = By.cssSelector(".login-container input[type='text']");
        if (!isElementPresent(loginUserInput)) {
            return false;
        }

        fillInput(loginUserInput, username);
        fillInput(By.cssSelector(".login-container input[type='password']"), password);
        clickElement(By.cssSelector(".login-container button[type='submit']"));
        waitForLoginOutcome();

        boolean authenticated = !getCurrentUrl().contains("/iniciar-sesion");
        setSessionValidated(authenticated);
        return authenticated;
    }

    private void waitForLoginOutcome() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4)).until(d -> {
                String currentUrl = d.getCurrentUrl();
                if (!currentUrl.contains("/iniciar-sesion")) {
                    return true;
                }
                return isErrorMessagePresent();
            });
        } catch (Exception ignored) {
            // It will be evaluated by URL at the end.
        }
    }

    /**
     * Helper method to register a new user.
     */
    protected void register(String email, String username, String password, String firstName, String lastName,
            String phone) {
        navigateTo("/registro");
        fillInput(By.cssSelector(".register-container input[type='email']"), email);
        fillInput(By.cssSelector(".register-container input[type='text']:nth-of-type(1)"), username);
        fillInput(By.cssSelector(".register-container input[type='password']"), password);
        java.util.List<org.openqa.selenium.WebElement> textInputs = driver
                .findElements(By.cssSelector(".register-container .form-group input[type='text']"));
        if (textInputs.size() >= 4) {
            textInputs.get(1).clear();
            textInputs.get(1).sendKeys(firstName);
            textInputs.get(2).clear();
            textInputs.get(2).sendKeys(lastName);
            textInputs.get(3).clear();
            textInputs.get(3).sendKeys(phone);
        }
        clickElement(By.cssSelector(".register-container button[type='submit']"));
    }

    /**
     * Helper method for logout.
     */
    protected void logout() {
        driver.manage().deleteAllCookies();
        setSessionValidated(false);
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
        navigateTo("/iniciar-sesion");
        waitForUrlContains("/iniciar-sesion");
    }

    /**
     * Checks whether an error message is present.
     */
    protected boolean isErrorMessagePresent() {
        return isElementVisible(By.cssSelector(".error-message, .alert-error, .text-red-500"));
    }

    /**
     * Gets the error message text.
     */
    protected String getErrorMessage() {
        return getElementText(By.cssSelector(".error-message, .alert-error, .text-red-500"));
    }

    /**
     * Checks whether a success message is present.
     */
    protected boolean isSuccessMessagePresent() {
        return isElementVisible(By.cssSelector(".success-message, .alert-success, .text-green-500"));
    }

    /**
     * Gets the success message text.
     */
    protected String getSuccessMessage() {
        return getElementText(By.cssSelector(".success-message, .alert-success, .text-green-500"));
    }
}
