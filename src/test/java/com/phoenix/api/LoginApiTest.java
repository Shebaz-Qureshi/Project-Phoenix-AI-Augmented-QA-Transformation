package com.phoenix.api;

import com.phoenix.utils.ConfigReader;
import com.phoenix.utils.ExtentReportManager;
import com.aventstack.extentreports.Status;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/**
 * ============================================================================
 * LoginApiTest  –  REST Assured API Tests
 * ============================================================================
 * Validates the backend contract BEFORE any UI tests run (CI gate).
 * Uses its own @Before/@AfterMethod (no WebDriver needed for API tests).
 *
 * Covers:
 *   - Health check (GET /)
 *   - Valid login (POST /login – form-encoded)
 *   - Invalid credentials responses
 *   - Empty field validation
 *   - Response time SLA
 *   - Content-Type header assertion
 * ============================================================================
 */
public class LoginApiTest {

    private static final Logger log = LogManager.getLogger(LoginApiTest.class);
    private static final String VALID_USER = ConfigReader.get("validUsername");
    private static final String VALID_PASS = ConfigReader.get("validPassword");
    private ApiClient apiClient;

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        log.info("════ API @BeforeMethod: {} ════", method.getName());
        apiClient = new ApiClient();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            default                  -> "SKIPPED";
        };
        log.info("════ API @AfterMethod: {} — {} ════", result.getName(), status);
        ExtentReportManager.removeTest();
    }

    // ── Health Check ──────────────────────────────────────────────────────────

    @Test(
        groups      = {"api", "smoke"},
        description = "GET / – verify the site is reachable and returns HTTP 200",
        priority    = 1
    )
    public void testSiteHealthCheck() {
        log("GET / – health check");
        Response response = apiClient.getHealth();

        log("Status: " + response.getStatusCode() + " | Time: " + response.getTime() + "ms");
        apiClient.assertStatus(response, 200);
        apiClient.assertBodyContains(response, "The Internet");
        apiClient.assertHeaderContains(response, "Content-Type", "text/html");

        logExtent("Health check passed – HTTP 200, body contains 'The Internet'");
    }

    // ── POST /login – Valid Credentials ──────────────────────────────────────

    @Test(
        groups      = {"api", "smoke"},
        description = "POST /login with valid credentials must produce a redirect (302) or 200",
        priority    = 2
    )
    public void testValidLoginReturnsRedirectOrOk() {
        log("POST /login – valid credentials");
        Response response = apiClient.postLoginForm(VALID_USER, VALID_PASS);

        log("Status: " + response.getStatusCode() + " | Time: " + response.getTime() + "ms");

        // This sandbox returns 302 → /secure on success
        // A production KYC API would return 200 + JWT body
        apiClient.assertStatusIn(response, 200, 302);

        if (response.getStatusCode() == 302) {
            String location = response.getHeader("Location");
            Assert.assertNotNull(location, "Redirect Location header must be present");
            Assert.assertTrue(location.contains("secure"),
                "Redirect should point to /secure, got: " + location);
            logExtent("Valid login: HTTP 302 → Location: " + location);
        } else {
            logExtent("Valid login: HTTP 200 (JSON endpoint)");
        }
    }

    // ── POST /login – Invalid Credentials ─────────────────────────────────────

    @Test(
        groups      = {"api", "regression"},
        description = "POST /login with invalid username must return an error body",
        priority    = 3
    )
    public void testInvalidUsernameReturnsError() {
        log("POST /login – invalid username");
        Response response = apiClient.postLoginForm("invalid_user_xyz", VALID_PASS);

        log("Status: " + response.getStatusCode());
        // The sandbox re-renders the form (HTTP 200) with an error flash
        apiClient.assertStatus(response, 200);
        apiClient.assertBodyContains(response, "Your username is invalid");
        logExtent("Invalid username correctly rejected – error body confirmed");
    }

    @Test(
        groups      = {"api", "regression"},
        description = "POST /login with invalid password must return an error body",
        priority    = 4
    )
    public void testInvalidPasswordReturnsError() {
        log("POST /login – invalid password");
        Response response = apiClient.postLoginForm(VALID_USER, "wrong_password_!@#");

        log("Status: " + response.getStatusCode());
        apiClient.assertStatus(response, 200);
        apiClient.assertBodyContains(response, "Your password is invalid");
        logExtent("Invalid password correctly rejected – error body confirmed");
    }

    @Test(
        groups      = {"api", "regression"},
        description = "POST /login with empty credentials must be handled gracefully",
        priority    = 5
    )
    public void testEmptyCredentialsHandledGracefully() {
        log("POST /login – empty credentials");
        Response response = apiClient.postLoginForm("", "");

        log("Status: " + response.getStatusCode());
        // Should NOT return 500 – any 2xx or 4xx is acceptable
        int status = response.getStatusCode();
        Assert.assertTrue(
            status < 500,
            "Empty credentials should not cause a server error (5xx). Got: " + status
        );
        logExtent("Empty credentials handled gracefully – status: " + status);
    }

    // ── Response Time SLA ─────────────────────────────────────────────────────

    @Test(
        groups      = {"api", "performance"},
        description = "POST /login must respond within the 5000ms SLA",
        priority    = 6
    )
    public void testLoginResponseTimeSLA() {
        log("POST /login – SLA check (max 5000ms)");
        Response response = apiClient.postLoginForm(VALID_USER, VALID_PASS);

        long elapsed = response.getTime();
        log("Response time: " + elapsed + "ms");

        apiClient.assertResponseTimeBelow(response, 5_000);
        logExtent("SLA passed – response time: " + elapsed + "ms (limit: 5000ms)");
    }

    // ── Headers ───────────────────────────────────────────────────────────────

    @Test(
        groups      = {"api", "regression"},
        description = "GET / must include a valid Content-Type header",
        priority    = 7
    )
    public void testResponseHeadersPresent() {
        log("GET / – checking Content-Type header");
        Response response = apiClient.getHealth();

        apiClient.assertHeaderContains(response, "Content-Type", "text/html");
        String contentType = response.getHeader("Content-Type");
        log("Content-Type: " + contentType);
        logExtent("Content-Type header verified: " + contentType);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void log(String message) {
        log.info("[API] {}", message);
    }

    private void logExtent(String message) {
        try {
            ExtentReportManager.getTest().log(Status.INFO, "[API] " + message);
        } catch (Exception ignored) {}
    }
}
