package com.phoenix.ui;

import com.phoenix.utils.ConfigReader;
import com.phoenix.utils.DriverManager;
import com.phoenix.utils.ExtentReportManager;
import com.phoenix.utils.ScreenshotUtils;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * ============================================================================
 * BaseTest  –  Parent Class for All UI TestNG Test Classes
 * ============================================================================
 * Handles:
 *   @BeforeMethod  → Initialise WebDriver + log test start to Extent
 *   @AfterMethod   → Capture screenshot if failed + quit WebDriver
 *
 * All UI test classes extend this. API test classes use a separate
 * ApiBaseTest that skips WebDriver entirely.
 * ============================================================================
 */
public abstract class BaseTest {

    protected final Logger log = LogManager.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        log.info("════ @BeforeMethod: {} ════", method.getName());
        DriverManager.initDriver();

        // Navigate to base URL to warm up the session
        String baseUrl = ConfigReader.get("baseUrl");
        DriverManager.getDriver().get(baseUrl);

        // Log the browser info to the Extent report step
        try {
            ExtentReportManager.getTest()
                    .log(Status.INFO, "Browser: "
                            + ConfigReader.getOrDefault("browser", "chrome")
                            + " | URL: " + baseUrl);
        } catch (IllegalStateException ignored) {
            // TestListener creates the ExtentTest; if not ready yet, skip silently
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        log.info("════ @AfterMethod: {} — status={} ════",
                result.getName(), statusLabel(result.getStatus()));

        // Screenshot already captured in TestListener on failure;
        // capture a final-state screenshot for passed tests too if desired
        if (result.getStatus() == ITestResult.SUCCESS) {
            try {
                ScreenshotUtils.attachToReport(
                        DriverManager.getDriver(), "Final State – " + result.getName());
            } catch (Exception ignored) {}
        }

        DriverManager.quitDriver();
    }

    // ── Utility methods available to all test classes ─────────────────────────

    /** Logs an info step to the Extent Report and to Log4j. */
    protected void logStep(String message) {
        log.info("[STEP] {}", message);
        try {
            ExtentReportManager.getTest().log(Status.INFO, message);
        } catch (Exception ignored) {}
    }

    /** Logs a pass step to the Extent Report. */
    protected void logPass(String message) {
        log.info("[PASS] {}", message);
        try {
            ExtentReportManager.getTest().log(Status.PASS, message);
        } catch (Exception ignored) {}
    }

    /** Logs a warning to the Extent Report. */
    protected void logWarning(String message) {
        log.warn("[WARN] {}", message);
        try {
            ExtentReportManager.getTest().log(Status.WARNING, message);
        } catch (Exception ignored) {}
    }

    private String statusLabel(int status) {
        return switch (status) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            case ITestResult.SKIP    -> "SKIPPED";
            default                  -> "UNKNOWN";
        };
    }
}
