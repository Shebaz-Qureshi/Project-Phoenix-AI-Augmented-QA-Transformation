package com.phoenix.helpers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * SelfHealingLocator  –  Project Phoenix Core Component
 * ============================================================================
 *
 * Solves the "brittle selector" problem by accepting a PRIORITISED LIST of
 * By locators. It tries each strategy in order and returns the first visible
 * element found within the timeout.
 *
 * When a fallback selector is used (i.e., the primary one failed), a
 * structured WARNING is logged so engineers know exactly which selector
 * decayed — feeding a future AI healing loop via webhook.
 *
 * Strategy Priority (recommended):
 *   1. By.id()              – Most stable; tied to backend templates
 *   2. By.name()            – Usually stable; bound to form fields
 *   3. By.cssSelector()     – Stable if using data-testid or semantic attrs
 *   4. By.linkText()        – Good for anchors and nav links
 *   5. By.xpath()           – Most flexible, most brittle; last resort
 *
 * AI Healing Loop (production extension):
 *   When a fallback fires in CI, call an AI agent webhook with the page's
 *   current DOM. The agent suggests a new primary selector and opens a PR.
 *
 * Usage:
 *   SelfHealingLocator locator = new SelfHealingLocator(driver);
 *   WebElement usernameField = locator.find(
 *       "Username field",
 *       By.id("username"),
 *       By.name("username"),
 *       By.cssSelector(".username"),
 *       By.xpath("//input[@id='username']")
 *   );
 * ============================================================================
 */
public class SelfHealingLocator {

    private static final Logger log = LogManager.getLogger(SelfHealingLocator.class);

    private final WebDriver driver;
    private final int       timeoutSeconds;

    // Result of the last resolution – for inspection and reporting
    private String lastUsedStrategy;
    private boolean lastHealingRequired;

    public SelfHealingLocator(WebDriver driver) {
        this.driver         = driver;
        this.timeoutSeconds = 5;
    }

    public SelfHealingLocator(WebDriver driver, int timeoutSeconds) {
        this.driver         = driver;
        this.timeoutSeconds = timeoutSeconds;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves the first visible element from the provided By strategies.
     *
     * @param elementName  Human-readable label for logging and error messages.
     * @param strategies   Ordered strategies – index 0 is the "golden" locator.
     * @return             The resolved WebElement.
     * @throws NoSuchElementException if no strategy locates a visible element.
     */
    public WebElement find(String elementName, By... strategies) {
        List<String> attempted = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        for (int i = 0; i < strategies.length; i++) {
            By strategy = strategies[i];
            attempted.add(strategy.toString());

            try {
                WebElement element = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(strategy));

                lastUsedStrategy    = strategy.toString();
                lastHealingRequired = (i > 0);

                if (i > 0) {
                    emitHealingWarning(elementName, strategies[0], strategy, attempted);
                } else {
                    log.debug("Located '{}' with primary strategy: {}", elementName, strategy);
                }

                return element;

            } catch (Exception e) {
                log.debug("Strategy {} failed for '{}': {} – trying next...",
                        i + 1, elementName, strategy);
            }
        }

        // All strategies exhausted
        String report = buildFailureReport(elementName, attempted);
        log.error(report);
        throw new NoSuchElementException(report);
    }

    /**
     * Returns the strategy string used in the last successful find().
     */
    public String getLastUsedStrategy() {
        return lastUsedStrategy;
    }

    /**
     * Returns true if the last find() used a fallback (healing was required).
     */
    public boolean wasHealingRequired() {
        return lastHealingRequired;
    }

    // ── Convenience Factory Methods ───────────────────────────────────────────

    /**
     * Builds a standard 4-strategy chain for a text input with id, name, css, and xpath.
     */
    public static By[] inputStrategies(String id, String name, String cssClass) {
        return new By[]{
            By.id(id),
            By.name(name),
            By.cssSelector("." + cssClass),
            By.cssSelector("[name='" + name + "']"),
            By.xpath("//input[@id='" + id + "']")
        };
    }

    /**
     * Builds a standard strategy chain for a submit button.
     */
    public static By[] buttonStrategies(String id, String text) {
        return new By[]{
            By.id(id),
            By.cssSelector("button[type='submit']"),
            By.xpath("//button[normalize-space(text())='" + text + "']"),
            By.xpath("//input[@type='submit']"),
            By.xpath("//*[@id='" + id + "']")
        };
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void emitHealingWarning(
            String elementName,
            By primary,
            By used,
            List<String> attempted) {

        String separator = "═".repeat(72);
        String warning = String.format("""
                %n%s
                ⚠  SELF-HEALING TRIGGERED  –  %s
                %s
                  Primary  (FAILED): %s
                  Fallback (USED)  : %s
                  All attempts     :
                %s
                  ACTION: Update the primary locator in the Page Object, or
                  trigger the AI healing agent webhook to auto-open a fix PR.
                %s%n""",
                separator,
                elementName,
                separator,
                primary,
                used,
                formatAttempts(attempted),
                separator
        );

        log.warn(warning);

        /*
         *AI Healing Webhook Hook (extend for production):
         *
         * String webhookUrl = System.getenv("AI_HEALING_WEBHOOK_URL");
         * if (webhookUrl != null && !webhookUrl.isBlank()) {
         *     HealingWebhook.fire(webhookUrl, elementName, primary.toString(),
         *                         used.toString(), driver.getCurrentUrl());
         * }
         */
    }

    private String formatAttempts(List<String> attempted) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attempted.size(); i++) {
            sb.append(String.format("    %d. %s%n", i + 1, attempted.get(i)));
        }
        return sb.toString();
    }

    private String buildFailureReport(String elementName, List<String> attempted) {
        return String.format(
            "[SelfHealingLocator] Could not locate '%s' with any of the %d strategies:%n%s",
            elementName, attempted.size(), formatAttempts(attempted));
    }
}
