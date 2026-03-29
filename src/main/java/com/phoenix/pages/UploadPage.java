package com.phoenix.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import java.io.File;

/**
 * ============================================================================
 * UploadPage  –  Page Object Model
 * ============================================================================
 * Covers the /upload route – the Document Upload step of the Merchant KYC
 * onboarding flow. Uses SelfHealingLocator for all element interactions.
 * ============================================================================
 */
public class UploadPage extends BasePage {

    private static final String PATH        = "/upload";
    private static final String DYNAMIC_PATH = "/dynamic_loading/1";

    // ── Locator strategy chains ───────────────────────────────────────────────

    private static final By[] FILE_INPUT_STRATEGIES = {
        By.id("file-upload"),
        By.name("file"),
        By.cssSelector("input[type='file']"),
        By.xpath("//input[@id='file-upload']"),
        By.xpath("//input[@type='file']")
    };

    private static final By[] SUBMIT_STRATEGIES = {
        By.id("file-submit"),
        By.cssSelector("input[type='submit']"),
        By.cssSelector("#file-submit"),
        By.xpath("//input[@id='file-submit']"),
        By.xpath("//input[@type='submit']")
    };

    private static final By[] UPLOADED_NAME_STRATEGIES = {
        By.id("uploaded-files"),
        By.cssSelector("#uploaded-files"),
        By.xpath("//*[@id='uploaded-files']"),
        By.cssSelector(".example h3 + div"),
        By.xpath("//div[@class='example']//h3[text()='File Uploaded!']/following-sibling::*")
    };

    // Dynamic loading locators
    private static final By START_BUTTON   = By.cssSelector("#start button");
    private static final By FINISH_ELEMENT = By.id("finish");

    // ── Navigation ────────────────────────────────────────────────────────────

    public UploadPage open() {
        navigateTo(PATH);
        log.info("Upload page opened");
        return this;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Selects a file for upload using the hidden file input.
     * Uses WebElement.sendKeys() — works headlessly, no OS dialog needed.
     */
    public UploadPage selectFile(String absoluteFilePath) {
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            throw new RuntimeException("Test file not found: " + absoluteFilePath);
        }
        WebElement input = healer.find("File input", FILE_INPUT_STRATEGIES);
        input.sendKeys(absoluteFilePath);
        log.info("File selected: {}", file.getName());
        return this;
    }

    public UploadPage clickUpload() {
        WebElement btn = healer.find("Upload button", SUBMIT_STRATEGIES);
        btn.click();
        log.info("Upload button clicked");
        waitForPageLoad();
        return this;
    }

    /**
     * Composite action – selects and uploads a file.
     */
    public UploadPage uploadFile(String absoluteFilePath) {
        return selectFile(absoluteFilePath).clickUpload();
    }

    // ── Dynamic Content Interaction ───────────────────────────────────────────

    /**
     * Navigates to the dynamic loading example and triggers a lazy element.
     * Simulates an async KYC document status update.
     */
    public String triggerAndWaitForDynamicContent() {
        navigateTo(DYNAMIC_PATH);
        log.info("Dynamic loading page opened");
        clickElement(START_BUTTON);
        log.info("Start button clicked – waiting for hidden element...");
        WebElement finish = waitForVisible(FINISH_ELEMENT);
        String text = finish.getText().trim();
        log.info("Dynamic element appeared: '{}'", text);
        return text;
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    public UploadPage assertUploadSuccess(String expectedFilename) {
        WebElement uploaded = healer.find("Uploaded filename", UPLOADED_NAME_STRATEGIES);
        String actualText   = uploaded.getText().trim();
        log.info("Uploaded file confirmation text: '{}'", actualText);
        Assert.assertTrue(
            actualText.contains(expectedFilename),
            "Expected filename '" + expectedFilename + "' in confirmation, got: " + actualText
        );
        return this;
    }

    public UploadPage assertOnUploadPage() {
        Assert.assertTrue(
            getCurrentUrl().contains("/upload"),
            "Not on upload page. Current URL: " + getCurrentUrl()
        );
        return this;
    }

    public String getUploadedFilename() {
        return healer.find("Uploaded filename", UPLOADED_NAME_STRATEGIES).getText().trim();
    }
}
