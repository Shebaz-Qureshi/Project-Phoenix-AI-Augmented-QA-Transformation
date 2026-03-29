package com.phoenix.pages;

import com.phoenix.helpers.SelfHealingLocator;
import com.phoenix.utils.ConfigReader;
import com.phoenix.utils.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * ============================================================================
 * BasePage  –  Abstract Parent for All Page Objects
 * ============================================================================
 * Provides shared utilities: explicit waits, JS execution, navigation,
 * and direct access to the SelfHealingLocator.
 *
 * All Page Objects extend this class.
 * ============================================================================
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final SelfHealingLocator healer;
    protected final String baseUrl;

    protected BasePage() {
        this.driver  = DriverManager.getDriver();
        this.baseUrl = ConfigReader.get("baseUrl");
        int timeout  = ConfigReader.getInt("explicitWait");
        this.wait    = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        this.healer  = new SelfHealingLocator(driver, timeout);
        PageFactory.initElements(driver, this);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    protected void navigateTo(String path) {
        String url = baseUrl + path;
        log.info("Navigating to: {}", url);
        driver.get(url);
        waitForPageLoad();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    // ── Waits ─────────────────────────────────────────────────────────────────

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected boolean waitForUrl(String urlFragment) {
        return wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    protected void waitForPageLoad() {
        wait.until(driver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState")
                .equals("complete"));
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    protected void clickElement(By locator) {
        waitForClickable(locator).click();
    }

    protected void typeInto(By locator, String text) {
        WebElement el = waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    protected String getTextOf(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ── JavaScript helpers ────────────────────────────────────────────────────

    protected void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    protected void highlightElement(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String originalStyle = element.getAttribute("style");
        js.executeScript(
            "arguments[0].setAttribute('style','border:3px solid red; background:yellow');",
            element);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        js.executeScript(
            "arguments[0].setAttribute('style', arguments[1]);",
            element, originalStyle);
    }
}
