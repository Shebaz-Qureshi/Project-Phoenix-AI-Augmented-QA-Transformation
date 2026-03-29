package com.phoenix.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * ============================================================================
 * ScreenshotUtils  –  Capture & Embed Screenshots in Extent Reports
 * ============================================================================
 */
public class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final String SCREENSHOT_DIR = "extent-reports/screenshots/";

    private ScreenshotUtils() { /* utility class */ }

    /**
     * Captures a screenshot and returns it as a Base64 string for inline
     * embedding into the Extent Report (no external file needed).
     */
    public static String captureBase64(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.error("Failed to capture Base64 screenshot: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Captures a screenshot and saves it to disk.
     * Returns the file path for use in reports.
     */
    public static String captureToFile(WebDriver driver, String testName) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String safeName = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filePath = SCREENSHOT_DIR + safeName + "_" + timestamp + ".png";

        try {
            File src  = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File(filePath);
            FileUtils.copyFile(src, dest);
            log.info("Screenshot saved: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to save screenshot for '{}': {}", testName, e.getMessage());
            return "";
        }
    }

    /**
     * Embeds a Base64 screenshot directly into the Extent Report.
     * Preferred over file paths for portability.
     */
    public static void attachToReport(WebDriver driver, String label) {
        if (driver == null) return;
        try {
            String base64 = captureBase64(driver);
            if (!base64.isEmpty()) {
                ExtentReportManager.getTest()
                        .addScreenCaptureFromBase64String(base64, label);
                log.debug("Screenshot attached to report: {}", label);
            }
        } catch (Exception e) {
            log.warn("Could not attach screenshot '{}': {}", label, e.getMessage());
        }
    }
}
