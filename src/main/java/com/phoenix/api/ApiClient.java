package com.phoenix.api;

import com.phoenix.utils.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.restassured.RestAssured.given;

/**
 * ============================================================================
 * ApiClient  –  REST Assured HTTP Client
 * ============================================================================
 * Wraps all REST Assured interactions behind a clean API so tests never
 * make raw given().when().then() calls inline.
 *
 * Covers:
 *   - POST /login  (form-encoded + JSON variants)
 *   - GET /        (health check)
 *   - Response time SLA assertion
 *
 * In the real KYC Portal, add endpoints for:
 *   - POST /api/kyc/documents  (upload)
 *   - GET  /api/kyc/status/{merchantId}
 *   - POST /api/auth/token     (OAuth2)
 * ============================================================================
 */
public class ApiClient {

    private static final Logger log = LogManager.getLogger(ApiClient.class);
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = ConfigReader.get("apiBaseUrl");
        RestAssured.baseURI = baseUrl;
        // Log all requests and responses in DEBUG level (suppress in CI via log4j config)
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // Request Specification

    private RequestSpecification baseSpec() {
        return given()
                .header("Accept",     "text/html,application/json")
                .header("User-Agent", "ProjectPhoenix-QA/1.0")
                .redirects().follow(false);   // Capture redirects, not final destination
    }

    // Endpoints

    /**
     * POST /login with form-encoded body (application/x-www-form-urlencoded).
     * This matches the target site's actual contract.
     */
    public Response postLoginForm(String username, String password) {
        log.info("POST /login [form] username={}", username);
        return baseSpec()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post("/login")
                .then()
                .extract().response();
    }

    /**
     * POST /login with JSON body – demonstrates content-type negotiation.
     */
    public Response postLoginJson(String username, String password) {
        log.info("POST /login [json] username={}", username);
        return baseSpec()
                .contentType("application/json")
                .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                .when()
                .post("/login")
                .then()
                .extract().response();
    }

    /**
     * GET / – lightweight health check that the server is reachable.
     */
    public Response getHealth() {
        log.info("GET / (health check)");
        return given()
                .header("Accept", "text/html")
                .when()
                .get("/")
                .then()
                .extract().response();
    }

    // ── Assertion Helpers ─────────────────────────────────────────────────────

    public void assertStatus(Response response, int expectedStatus) {
        int actual = response.getStatusCode();
        if (actual != expectedStatus) {
            throw new AssertionError(
                "Expected HTTP " + expectedStatus + ", got " + actual +
                "\nBody: " + response.getBody().asString().substring(0, Math.min(300, response.getBody().asString().length()))
            );
        }
        log.info("Status assertion passed: {}", actual);
    }

    public void assertStatusIn(Response response, int... expectedStatuses) {
        int actual = response.getStatusCode();
        for (int expected : expectedStatuses) {
            if (actual == expected) {
                log.info("Status assertion passed: {}", actual);
                return;
            }
        }
        throw new AssertionError("HTTP " + actual + " not in expected set");
    }

    public void assertBodyContains(Response response, String text) {
        String body = response.getBody().asString();
        if (!body.contains(text)) {
            throw new AssertionError(
                "Expected body to contain '" + text + "'\n" +
                "Actual body snippet: " + body.substring(0, Math.min(500, body.length()))
            );
        }
        log.info("Body assertion passed – found: '{}'", text);
    }

    public void assertHeaderContains(Response response, String header, String value) {
        String actual = response.getHeader(header);
        if (actual == null || !actual.contains(value)) {
            throw new AssertionError(
                "Header '" + header + "' expected to contain '" + value +
                "', got: " + actual
            );
        }
        log.info("Header assertion passed: {} contains '{}'", header, value);
    }

    public void assertResponseTimeBelow(Response response, long maxMillis) {
        long actual = response.getTime();
        if (actual > maxMillis) {
            throw new AssertionError(
                "Response time " + actual + "ms exceeded SLA of " + maxMillis + "ms"
            );
        }
        log.info("Response time SLA passed: {}ms < {}ms", actual, maxMillis);
    }
}
