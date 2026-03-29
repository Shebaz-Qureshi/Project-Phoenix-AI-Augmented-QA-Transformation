# Project Phoenix 

Developed by Shebaz Zahid Qureshi
  
Stack: **Selenium 4 · Maven · TestNG · Extent Reports 5 · REST Assured · Eclipse**

Additionally AI Self Healing
---

## Architecture Overview

```
project-phoenix-selenium/
│
├── src/main/java/com/phoenix/
│   ├── pages/
│   │   ├── BasePage.java              ← Shared waits, JS helpers, navigation
│   │   ├── LoginPage.java             ← POM: /login with self-healing locators
│   │   └── UploadPage.java            ← POM: /upload + dynamic content
│   ├── api/
│   │   └── ApiClient.java             ← REST Assured wrapper (POST /login, GET /)
│   ├── helpers/
│   │   └── SelfHealingLocator.java    ← Core self-healing engine
│   ├── listeners/
│   │   ├── TestListener.java          ← TestNG → Extent Reports bridge
│   │   ├── RetryAnalyzer.java         ← Auto-retry on failure
│   │   └── RetryListener.java         ← Global retry injection (no annotation needed)
│   └── utils/
│       ├── ConfigReader.java          ← config.properties + system property override
│       ├── DriverManager.java         ← Thread-safe WebDriver factory (WebDriverManager)
│       ├── ExtentReportManager.java   ← Thread-safe Extent Reports factory
│       └── ScreenshotUtils.java       ← Base64 + file screenshot capture
│
├── src/test/java/com/phoenix/
│   ├── ui/
│   │   ├── BaseTest.java              ← @BeforeMethod/@AfterMethod for all UI tests
│   │   ├── LoginTest.java             ← 10 UI tests: happy path, errors, self-healing
│   │   └── UploadTest.java            ← 5 UI tests: KYC upload + dynamic content
│   └── api/
│       └── LoginApiTest.java          ← 7 API tests: health, auth, SLA, headers
│
├── src/main/resources/
│   ├── config.properties              ← All configurable values
│   └── log4j2.xml                     ← Logging configuration
│
├── src/test/resources/testdata/
│   └── kyc-sample.txt                 ← Test fixture for upload tests
│
├── testng.xml                          ← Full suite (API → Login → Upload)
├── testng-api.xml                      ← API tests only
├── testng-ui.xml                       ← UI tests only
├── pom.xml                             ← Maven dependencies + Surefire config
├── Jenkinsfile                         ← Jenkins Declarative Pipeline
└── .github/workflows/ci.yml           ← GitHub Actions pipeline
```

---

## Prerequisites

| Tool | Version | Verify |
|------|---------|--------|
| Java JDK | 11+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Eclipse IDE | 2023-03+ | Install from [eclipse.org](https://www.eclipse.org/downloads/) |
| Chrome | Any current | `google-chrome --version` |
| Git | 2.30+ | `git --version` |

> **No manual ChromeDriver download needed.** WebDriverManager auto-downloads the matching driver binary at runtime.

---

## Eclipse Setup (Step by Step)

### 1. Install Eclipse Plugins
Open Eclipse → **Help → Eclipse Marketplace** and install:
- **TestNG for Eclipse** (by Beust) — enables running TestNG suites directly
- **Maven Integration for Eclipse (m2e)** — usually pre-installed

### 2. Import the Project
```
File → Import → Maven → Existing Maven Projects
→ Root Directory: [path to project-phoenix-selenium]
→ Select pom.xml → Finish
```
Eclipse will download all dependencies automatically (may take 2–3 minutes on first import).

### 3. Verify Project Build Path
Right-click project → **Build Path → Configure Build Path**  
Confirm `src/main/java`, `src/test/java`, `src/main/resources`, `src/test/resources` are all listed as source folders.

---

## Quick Start — Run Tests in Eclipse

### Option A: Run via TestNG XML (Recommended)
1. Right-click `testng.xml` in the Project Explorer
2. Select **Run As → TestNG Suite**
3. The full suite runs: API tests first, then UI tests

### Option B: Run a single test class
1. Right-click `LoginTest.java`
2. Select **Run As → TestNG Test**

### Option C: Run via Maven (command line or Eclipse Maven Run)
```bash
# Full suite
mvn test

# API tests only
mvn test -P api

# UI tests only
mvn test -P ui

# Chrome headless (CI mode)
mvn test -Dheadless=true

# Firefox
mvn test -Dbrowser=firefox

# Specific test class
mvn test -Dtest=LoginTest

# Specific test method
mvn test -Dtest=LoginTest#testSuccessfulLogin
```

---

## Configuration

All settings live in `src/main/resources/config.properties`.  
Any value can be overridden at runtime with a `-D` Maven system property.

| Property | Default | Override Example |
|----------|---------|-----------------|
| `browser` | `chrome` | `-Dbrowser=firefox` |
| `headless` | `false` | `-Dheadless=true` |
| `baseUrl` | `https://the-internet.herokuapp.com` | `-Dbaseurl=https://staging.yourdomain.com` |
| `validUsername` | `tomsmith` | `-DvalidUsername=admin` |
| `validPassword` | `SuperSecretPassword!` | `-DvalidPassword=secret` |
| `explicitWait` | `15` | `-DexplicitWait=30` |
| `retryCount` | `1` | `-DretryCount=2` |

---

## Viewing the Extent Report

After any test run the report is generated at:
```
extent-reports/PhoenixReport_<timestamp>.html
```

**To open in Eclipse:**  
Right-click the HTML file → **Open With → Web Browser**

**To open in your system browser:**
```bash
# macOS
open extent-reports/PhoenixReport_*.html

# Linux
xdg-open extent-reports/PhoenixReport_*.html

# Windows
start extent-reports/PhoenixReport_*.html
```

The report includes:
- Pass/Fail/Skip dashboard with charts
- Per-test step logs
- Failure screenshots (embedded Base64 — no external files needed)
- System info (browser, OS, Java version, base URL)
- Timeline view

---

## Self-Healing Locators

`SelfHealingLocator.java` accepts a **priority chain** of `By` locators. When the primary one fails, it tries the next and logs a structured warning:

```
═══════════════════════════════════════════════════════════════════
  SELF-HEALING TRIGGERED  –  Username field
═══════════════════════════════════════════════════════════════════
  Primary  (FAILED): By.id: username
  Fallback (USED)  : By.cssSelector: [name='username']
  ACTION: Update the primary locator in the Page Object.
═══════════════════════════════════════════════════════════════════
```

The self-healing **demo test** (`testSelfHealingLocatorFallback` in `LoginTest`) removes the `#username` id from the DOM at runtime and proves the healer completes the login using `[name='username']` instead.

---

## CI/CD

### GitHub Actions
Push to `main` or `develop` and the pipeline runs automatically:
1. **API Tests** → must pass before UI runs
2. **UI Tests** → Chrome headless on Ubuntu
3. **Extent Report** → uploaded as a downloadable artifact

Add these repository secrets: `BASE_URL`, `VALID_USERNAME`, `VALID_PASSWORD`

### Jenkins
Use the included `Jenkinsfile`. Configure:
- Tool: `Maven-3.9` and `JDK-11` in Jenkins Global Tool Configuration
- Credentials: `PHOENIX_VALID_USERNAME`, `PHOENIX_VALID_PASSWORD`
- Plugin: HTML Publisher (for Extent Report tab in Jenkins)

---

## Design Decisions

| Choice | Reason |
|--------|--------|
| **Selenium 4** over Selenium 3 | Native relative locators, CDP protocol access, improved `Actions` API |
| **WebDriverManager** | Eliminates manual chromedriver management — zero setup friction |
| **ThreadLocal `<WebDriver>`** | Safe parallel test execution without shared state |
| **TestNG over JUnit** | Native parallel execution, data providers, `dependsOnGroups`, XML suite config |
| **Extent Reports 5** | Richer HTML output than Surefire/Maven reports; Base64 screenshot embedding |
| **REST Assured** | Industry-standard Java API testing DSL; matches the assessment requirement |
| **Strategy chain pattern** | Transparent and auditable healing vs. magic ML-based approaches |

---

*Project Phoenix | "Architected for the future by Shebaz Zahid Qureshi, not just the current sprint."*
