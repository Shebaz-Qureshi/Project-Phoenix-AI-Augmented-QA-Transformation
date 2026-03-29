package com.phoenix.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * ============================================================================
 * DriverManager  –  Thread-Safe WebDriver Factory
 * ============================================================================
 * Uses ThreadLocal<WebDriver> so parallel TestNG tests each get their own
 * isolated browser instance — no shared state, no race conditions.
 *
 * WebDriverManager (bonigarcia) automatically downloads the correct browser
 * driver binary. No manual chromedriver download or PATH configuration needed.
 *
 * Supported browsers: chrome | firefox | edge
 * Headless mode: set headless=true in config.properties or -Dheadless=true
 * ============================================================================
 */
public class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    // One WebDriver per thread – safe for parallel execution
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    private DriverManager() { /* utility class */ }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises and stores a WebDriver for the current thread.
     * Call this in @BeforeMethod or @BeforeClass.
     */
    public static void initDriver() {
        String browser  = ConfigReader.getOrDefault("browser",  "chrome").toLowerCase();
        boolean headless = ConfigReader.getBool("headless");

        log.info("Initialising {} driver (headless={})", browser, headless);

        WebDriver driver = switch (browser) {
            case "firefox" -> createFirefox(headless);
            case "edge"    -> createEdge(headless);
            default        -> createChrome(headless);
        };

        applyTimeouts(driver);
        driver.manage().window().maximize();
        driverHolder.set(driver);
        log.info("Driver ready: {}", driver.getClass().getSimpleName());
    }

    /**
     * Returns the WebDriver for the current thread.
     * Throws if initDriver() was not called first.
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverHolder.get();
        if (driver == null) {
            throw new IllegalStateException(
                "WebDriver not initialised for this thread. " +
                "Call DriverManager.initDriver() in @BeforeMethod.");
        }
        return driver;
    }

    /**
     * Quits and removes the WebDriver for the current thread.
     * Call this in @AfterMethod or @AfterClass.
     */
    public static void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("Driver quit successfully");
            } catch (Exception e) {
                log.warn("Exception while quitting driver: {}", e.getMessage());
            } finally {
                driverHolder.remove();
            }
        }
    }

    // ── Private factory methods ───────────────────────────────────────────────

    private static WebDriver createChrome(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
            "--disable-extensions",
            "--disable-popup-blocking",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1280,720"
        );
        if (headless) {
            opts.addArguments("--headless=new");
        }
        return new ChromeDriver(opts);
    }

    private static WebDriver createFirefox(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions opts = new FirefoxOptions();
        opts.addArguments("--width=1280", "--height=720");
        if (headless) {
            opts.addArguments("--headless");
        }
        return new FirefoxDriver(opts);
    }

    private static WebDriver createEdge(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions opts = new EdgeOptions();
        opts.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--window-size=1280,720"
        );
        if (headless) {
            opts.addArguments("--headless=new");
        }
        return new EdgeDriver(opts);
    }

    private static void applyTimeouts(WebDriver driver) {
        int implicit   = ConfigReader.getInt("implicitWait");
        int pageLoad   = ConfigReader.getInt("pageLoadTimeout");
        int script     = ConfigReader.getInt("scriptTimeout");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicit));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoad));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(script));
    }
}
