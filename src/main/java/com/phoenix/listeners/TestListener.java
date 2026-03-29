package com.phoenix.listeners;

import com.phoenix.utils.ConfigReader;
import com.phoenix.utils.DriverManager;
import com.phoenix.utils.ExtentReportManager;
import com.phoenix.utils.ScreenshotUtils;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * ============================================================================
 * TestListener  –  TestNG Listener → Extent Reports Bridge
 * ============================================================================
 * Wires TestNG lifecycle events into Extent Reports automatically.
 * Declared in testng.xml — no annotation needed in test classes.
 *
 * Lifecycle hooks:
 *   @BeforeSuite   → initReports()
 *   @AfterSuite    → flushReports()
 *   @BeforeTest    → createTest()
 *   @AfterTest     → log pass / fail / skip + screenshot on failure
 * ============================================================================
 */
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    // ── Suite ─────────────────────────────────────────────────────────────────

    @Override
    public void onStart(ITestContext context) {
        log.info("═══ Suite starting: {} ═══", context.getName());
        ExtentReportManager.initReports();
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("═══ Suite finished: {} ═══", context.getName());
        ExtentReportManager.flushReports();
    }

    // ── Individual Tests ──────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        String testName = getFullTestName(result);
        String desc     = result.getMethod().getDescription();

        log.info("──── Test STARTED: {} ────", testName);
        ExtentReportManager.createTest(testName, desc);

        // Add category tags from TestNG groups
        String[] groups = result.getMethod().getGroups();
        for (String group : groups) {
            ExtentReportManager.getTest().assignCategory(group);
        }
        ExtentReportManager.getTest().assignAuthor("Shebaz Qureshi");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = getFullTestName(result);
        long durationMs = result.getEndMillis() - result.getStartMillis();

        log.info("✅ Test PASSED: {} ({}ms)", testName, durationMs);
        ExtentReportManager.getTest()
                .log(Status.PASS,
                     MarkupHelper.createLabel("PASS  –  " + durationMs + "ms", ExtentColor.GREEN));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = getFullTestName(result);
        Throwable cause = result.getThrowable();

        log.error("❌ Test FAILED: {}", testName);
        log.error("Failure cause: {}", cause != null ? cause.getMessage() : "Unknown");

        ExtentReportManager.getTest()
                .log(Status.FAIL,
                     MarkupHelper.createLabel("FAIL", ExtentColor.RED));

        if (cause != null) {
            ExtentReportManager.getTest().fail(cause);
        }

        // Screenshot on failure
        if (ConfigReader.getBool("screenshotOnFailure")) {
            captureFailureScreenshot(testName);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getFullTestName(result);
        Throwable cause = result.getThrowable();

        log.warn("⏭ Test SKIPPED: {}", testName);
        ExtentReportManager.getTest()
                .log(Status.SKIP,
                     MarkupHelper.createLabel("SKIPPED", ExtentColor.YELLOW));

        if (cause != null) {
            ExtentReportManager.getTest().skip(cause.getMessage());
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        log.warn("Test failed within success percentage: {}", getFullTestName(result));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String getFullTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName()
               + " :: "
               + result.getName();
    }

    private void captureFailureScreenshot(String testName) {
        try {
            WebDriver driver = DriverManager.getDriver();
            ScreenshotUtils.attachToReport(driver, "Failure Screenshot – " + testName);
        } catch (IllegalStateException e) {
            // Driver may not be initialised for API tests – this is expected
            log.debug("No driver available for screenshot (API test): {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Could not capture failure screenshot: {}", e.getMessage());
        }
    }
}
