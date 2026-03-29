package com.phoenix.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

/**
 * ============================================================================
 * LoginPage  –  Page Object Model
 * ============================================================================
 * Encapsulates all interactions with /login.
 *
 * Every element uses SelfHealingLocator (via BasePage.healer) with a
 * prioritised strategy chain. If the primary locator fails due to a UI
 * refactor, a fallback is used automatically and a warning is logged.
 * ============================================================================
 */
public class LoginPage extends BasePage {

    private static final String PATH = "/login";

    // ── Locator strategy chains ───────────────────────────────────────────────

    // Username field: id → name → css → xpath
    private static final By[] USERNAME_STRATEGIES = {
        By.id("username"),
        By.name("username"),
        By.cssSelector("[name='username']"),
        By.cssSelector("input[type='text']"),
        By.xpath("//input[@id='username']")
    };

    // Password field: id → name → css → xpath
    private static final By[] PASSWORD_STRATEGIES = {
        By.id("password"),
        By.name("password"),
        By.cssSelector("[name='password']"),
        By.cssSelector("input[type='password']"),
        By.xpath("//input[@id='password']")
    };

    // Submit button: css class → type → text → xpath
    private static final By[] SUBMIT_STRATEGIES = {
        By.cssSelector("button.radius[type='submit']"),
        By.cssSelector("button[type='submit']"),
        By.xpath("//button[@type='submit']"),
        By.xpath("//button[contains(text(),'Login')]"),
        By.xpath("//input[@type='submit']")
    };

    // Flash message: id → class → role
    private static final By[] FLASH_STRATEGIES = {
        By.id("flash"),
        By.cssSelector("#flash"),
        By.cssSelector(".flash"),
        By.cssSelector("[role='alert']"),
        By.xpath("//*[@id='flash']")
    };

    // Secure area heading (post-login verification)
    private static final By SECURE_HEADING = By.cssSelector("h2");

    // ── Navigation ────────────────────────────────────────────────────────────

    public LoginPage open() {
        navigateTo(PATH);
        log.info("Login page opened");
        return this;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public LoginPage enterUsername(String username) {
        WebElement field = healer.find("Username field", USERNAME_STRATEGIES);
        field.clear();
        field.sendKeys(username);
        log.info("Username entered: {}", username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        WebElement field = healer.find("Password field", PASSWORD_STRATEGIES);
        field.clear();
        field.sendKeys(password);
        log.info("Password entered: [REDACTED]");
        return this;
    }

    public LoginPage clickLogin() {
        WebElement btn = healer.find("Login button", SUBMIT_STRATEGIES);
        btn.click();
        log.info("Login button clicked");
        return this;
    }

    /**
     * Composite action – enters credentials and submits the form.
     * Uses values from config.properties by default.
     */
    public LoginPage login(String username, String password) {
        return enterUsername(username)
                .enterPassword(password)
                .clickLogin();
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    public LoginPage assertLoginSuccess() {
        WebElement flash = healer.find("Flash message", FLASH_STRATEGIES);
        String text = flash.getText();
        log.info("Flash message: {}", text);
        Assert.assertTrue(
            text.contains("You logged into a secure area!"),
            "Expected success flash message, got: " + text
        );
        Assert.assertTrue(
            flash.getAttribute("class").contains("success"),
            "Flash message did not have 'success' CSS class"
        );
        return this;
    }

    public LoginPage assertLoginError(String expectedFragment) {
        WebElement flash = healer.find("Flash message", FLASH_STRATEGIES);
        String text = flash.getText();
        log.info("Flash error message: {}", text);
        Assert.assertTrue(
            text.contains(expectedFragment),
            "Expected error containing '" + expectedFragment + "', got: " + text
        );
        Assert.assertTrue(
            flash.getAttribute("class").contains("error"),
            "Flash message did not have 'error' CSS class"
        );
        return this;
    }

    public LoginPage assertRedirectedToSecureArea() {
        waitForUrl("/secure");
        Assert.assertTrue(
            getCurrentUrl().contains("/secure"),
            "Expected redirect to /secure, but URL is: " + getCurrentUrl()
        );
        log.info("Redirected to secure area: {}", getCurrentUrl());
        return this;
    }

    public LoginPage assertPageTitle() {
        String title = getPageTitle();
        Assert.assertTrue(
            title.contains("The Internet"),
            "Unexpected page title: " + title
        );
        return this;
    }

    // ── Getters for test-level assertions ─────────────────────────────────────

    public String getFlashMessageText() {
        return healer.find("Flash message", FLASH_STRATEGIES).getText().trim();
    }

    public boolean isFlashMessageDisplayed() {
        try {
            return healer.find("Flash message", FLASH_STRATEGIES).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean wasHealingTriggered() {
        return healer.wasHealingRequired();
    }
}
