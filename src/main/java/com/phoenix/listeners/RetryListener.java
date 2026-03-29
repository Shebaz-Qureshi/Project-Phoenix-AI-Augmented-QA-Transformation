package com.phoenix.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * ============================================================================
 * RetryListener  –  Global Retry Injection
 * ============================================================================
 * Applies RetryAnalyzer to ALL test methods without requiring the
 * @Test(retryAnalyzer = ...) annotation on each test.
 * Declared in testng.xml under <listeners>.
 * ============================================================================
 */
public class RetryListener implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        // Inject RetryAnalyzer into every @Test method globally
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
