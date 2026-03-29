package com.phoenix.ui;

import com.phoenix.pages.LoginPage;
import com.phoenix.utils.ConfigReader;
import com.phoenix.utils.DriverManager;
import com.aventstack.extentreports.Status;
import com.phoenix.utils.ExtentReportManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ============================================================================
 * LoginTest  –  UI Tests for the Login Flow
 * ============================================================================
 * Covers: happy path, error messages, empty fields, self-healing demo,
 *         accessibility, and page title.
 *
 * All tests extend BaseTest which handles driver init/quit and Extent logging.
 * ============================================================================
 */
public class LoginTest extends BaseTest {

    private static final String VALID_USER = ConfigReader.get("validUsername");
    private static final String VALID_PASS = ConfigReader.get("validPassword");

    // ── Happy Path ────────────────────────────────────────────────────────────

    @Test(
        groups        = {"ui", "login", "smoke"},
        description   = "Valid credentials should redirect to secure area with success flash",
        priority      = 1
    )
    public void testSuccessfulLogin() {
        logStep("Opening login page");
        LoginPage loginPage = new LoginPage().open();

        logStep("Entering valid credentials: " + VALID_USER);
        loginPage.login(VALID_USER, VALID_PASS);

        logStep("Asserting redirect to /secure and success message");
        loginPage
            .assertRedirectedToSecureArea()
            .assertLoginSuccess();

        logPass("Login successful — redirected and flash message confirmed");
    }

    @Test(
        groups      = {"ui", "login", "smoke"},
        description = "Success flash message text and CSS class must match expected values",
        priority    = 2
    )
    public void testSuccessFlashMessageContent() {
        logStep("Performing login");
        LoginPage loginPage = new LoginPage().open().login(VALID_USER, VALID_PASS);

        logStep("Reading flash message text");
        String flashText = loginPage.getFlashMessageText();
        ExtentReportManager.getTest().log(Status.INFO, "Flash message: " + flashText);

        Assert.assertTrue(
            flashText.contains("You logged into a secure area!"),
            "Unexpected flash text: " + flashText
        );
        logPass("Flash message content verified");
    }

    // ── Negative Paths ────────────────────────────────────────────────────────

    @Test(
        groups      = {"ui", "login", "regression"},
        description = "Invalid username should show error flash",
        priority    = 3
    )
    public void testInvalidUsername() {
        logStep("Attempting login with invalid username");
        new LoginPage()
            .open()
            .login("invalid_user_xyz", VALID_PASS)
            .assertLoginError("Your username is invalid!");
        logPass("Invalid username error message confirmed");
    }

    @Test(
        groups      = {"ui", "login", "regression"},
        description = "Invalid password should show error flash",
        priority    = 4
    )
    public void testInvalidPassword() {
        logStep("Attempting login with invalid password");
        new LoginPage()
            .open()
            .login(VALID_USER, "wrong_password_123")
            .assertLoginError("Your password is invalid!");
        logPass("Invalid password error message confirmed");
    }

    @Test(
        groups      = {"ui", "login", "regression"},
        description = "Empty username field should produce an error",
        priority    = 5
    )
    public void testEmptyUsername() {
        logStep("Submitting form with empty username");
        LoginPage loginPage = new LoginPage().open();
        loginPage.enterPassword(VALID_PASS).clickLogin();
        Assert.assertTrue(loginPage.isFlashMessageDisplayed(),
            "Flash message should appear for empty username");
        logPass("Empty username validation confirmed");
    }

    @Test(
        groups      = {"ui", "login", "regression"},
        description = "Empty password field should produce an error",
        priority    = 6
    )
    public void testEmptyPassword() {
        logStep("Submitting form with empty password");
        LoginPage loginPage = new LoginPage().open();
        loginPage.enterUsername(VALID_USER).clickLogin();
        Assert.assertTrue(loginPage.isFlashMessageDisplayed(),
            "Flash message should appear for empty password");
        logPass("Empty password validation confirmed");
    }

    @Test(
        groups      = {"ui", "login", "regression"},
        description = "SQL injection attempt should be rejected safely",
        priority    = 7
    )
    public void testSqlInjectionAttempt() {
        logStep("Attempting SQL injection in username field");
        LoginPage loginPage = new LoginPage()
            .open()
            .login("' OR '1'='1", "' OR '1'='1");

        Assert.assertFalse(
            loginPage.getCurrentUrl().contains("/secure"),
            "SQL injection should NOT result in successful login"
        );
        logPass("SQL injection attempt correctly rejected");
    }

    // ── Self-Healing Demo ─────────────────────────────────────────────────────

    @Test(
        groups      = {"ui", "login", "healing"},
        description = "SelfHealingLocator must fall back gracefully when the primary " +
                      "selector is removed from the DOM at runtime",
        priority    = 8
    )
    public void testSelfHealingLocatorFallback() {
        logStep("Opening login page");
        LoginPage loginPage = new LoginPage().open();

        logStep("Simulating selector decay: removing #username id from the DOM");
        ExtentReportManager.getTest().log(Status.WARNING,
            "⚠ Simulating UI change: removing id='username' to trigger self-healing");

        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
        js.executeScript(
            "var el = document.getElementById('username'); " +
            "if(el){ el.removeAttribute('id'); el.removeAttribute('class'); }"
        );

        logStep("Attempting to type in the username field – healer should find it via [name='username']");
        // This must NOT throw — the healer falls back to By.name("username")
        loginPage.enterUsername(VALID_USER);

        logStep("Restoring the DOM and completing login");
        js.executeScript(
            "var el = document.querySelector('[name=\"username\"]'); " +
            "if(el){ el.id = 'username'; }"
        );
        loginPage.enterPassword(VALID_PASS).clickLogin();
        loginPage.assertLoginSuccess();

        ExtentReportManager.getTest().log(Status.INFO,
            "Self-healing used: " + loginPage.wasHealingTriggered());
        logPass("Self-healing fallback worked correctly — login completed after selector decay");
    }

    // ── Accessibility & Metadata ──────────────────────────────────────────────

    @Test(
        groups      = {"ui", "login", "accessibility"},
        description = "Login page must have the expected page title",
        priority    = 9
    )
    public void testPageTitle() {
        logStep("Checking page title");
        LoginPage loginPage = new LoginPage().open();
        loginPage.assertPageTitle();
        logPass("Page title verified: " + loginPage.getPageTitle());
    }

    @Test(
        groups      = {"ui", "login", "accessibility"},
        description = "Username and password inputs must be interactable",
        priority    = 10
    )
    public void testInputFieldsAreInteractable() {
        logStep("Verifying input fields are present and editable");
        LoginPage loginPage = new LoginPage().open();

        Assert.assertTrue(
            DriverManager.getDriver().findElement(By.id("username")).isEnabled(),
            "Username field should be enabled"
        );
        Assert.assertTrue(
            DriverManager.getDriver().findElement(By.id("password")).isEnabled(),
            "Password field should be enabled"
        );
        logPass("Both input fields are enabled and interactable");
    }
}
