package com.phoenix.listeners;

import com.phoenix.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * ============================================================================
 * RetryAnalyzer  –  Automatic Retry on Failure
 * ============================================================================
 * Retries failing tests up to the configured retryCount.
 * Attach via @Test(retryAnalyzer = RetryAnalyzer.class) or
 * through the RetryListener (applies globally without annotation).
 * ============================================================================
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    private int retryCount = 0;
    private final int maxRetry = ConfigReader.getInt("retryCount");

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetry) {
            retryCount++;
            log.warn(" Retrying test '{}' ({}/{})",
                    result.getName(), retryCount, maxRetry);
            return true;
        }
        log.error("Test '{}' exhausted all {} retries", result.getName(), maxRetry);
        return false;
    }
}
