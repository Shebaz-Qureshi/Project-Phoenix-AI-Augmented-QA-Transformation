package com.phoenix.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * ExtentReportManager  –  Thread-Safe Extent Reports Factory
 * ============================================================================
 * Produces a single ExtentReports instance (singleton) and stores per-test
 * ExtentTest instances in a ThreadLocal so parallel tests write independently.
 *
 * Report output: extent-reports/PhoenixReport_<timestamp>.html
 *
 * Usage in TestListener:
 *   ExtentReportManager.createTest("My Test Name");
 *   ExtentReportManager.getTest().pass("Step passed");
 *   ExtentReportManager.getTest().fail("Something went wrong");
 * ============================================================================
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testHolder = new ThreadLocal<>();

    private ExtentReportManager() { /* utility class */ }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Creates and configures the ExtentReports instance.
     * Must be called ONCE in @BeforeSuite.
     */
    public static synchronized void initReports() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        String reportPath   = ConfigReader.getOrDefault("reportPath", "extent-reports/");
        String reportName   = ConfigReader.getOrDefault("reportName", "Phoenix QA Report");
        String reportTitle  = ConfigReader.getOrDefault("reportTitle", "Test Execution Report");
        String reportFile   = reportPath + "PhoenixReport_" + timestamp + ".html";

        // Spark reporter – modern interactive HTML with dark/light theme support
        ExtentSparkReporter spark = new ExtentSparkReporter(reportFile);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle(reportTitle);
        spark.config().setReportName(reportName);
        spark.config().setEncoding("UTF-8");
        spark.config().setTimelineEnabled(true);
        spark.config().setCss(customCss());

        extent = new ExtentReports();
        extent.attachReporter(spark);

        // System metadata shown in the report dashboard
        extent.setSystemInfo("Framework",    "Selenium 4 + TestNG + Extent Reports 5");
        extent.setSystemInfo("Browser",      ConfigReader.getOrDefault("browser", "chrome"));
        extent.setSystemInfo("Headless",     ConfigReader.getOrDefault("headless", "false"));
        extent.setSystemInfo("Base URL",     ConfigReader.getOrDefault("baseUrl", "N/A"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("OS",           System.getProperty("os.name"));
        extent.setSystemInfo("Author",       "Shebaz Qureshi");
        extent.setSystemInfo("Project",      "Project Phoenix – KYC Portal QA");

        log.info("Extent report initialised at: {}", reportFile);
    }

    /**
     * Writes all test data to disk.
     * Must be called ONCE in @AfterSuite.
     */
    public static synchronized void flushReports() {
        if (extent != null) {
            extent.flush();
            log.info("Extent report flushed successfully");
        }
    }

    // ── Per-test API ──────────────────────────────────────────────────────────

    /** Creates a test node in the report and stores it for the current thread. */
    public static ExtentTest createTest(String testName) {
        ExtentTest test = extent.createTest(testName);
        testHolder.set(test);
        return test;
    }

    /** Creates a test node with a description. */
    public static ExtentTest createTest(String testName, String description) {
        ExtentTest test = extent.createTest(testName, description);
        testHolder.set(test);
        return test;
    }

    /** Returns the ExtentTest for the current thread. */
    public static ExtentTest getTest() {
        ExtentTest test = testHolder.get();
        if (test == null) {
            throw new IllegalStateException(
                "ExtentTest not initialised for this thread. " +
                "Call ExtentReportManager.createTest() first.");
        }
        return test;
    }

    /** Removes the ExtentTest reference for the current thread. */
    public static void removeTest() {
        testHolder.remove();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String customCss() {
        return
            ".brand-logo { display: none; } " +
            ".report-name { font-size: 22px; font-weight: 700; color: #1A73E8; } " +
            ".test-name { font-weight: 600; } " +
            ".badge-primary { background-color: #1A73E8; } ";
    }
}
