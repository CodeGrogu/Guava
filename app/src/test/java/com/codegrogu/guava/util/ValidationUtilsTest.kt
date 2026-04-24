package com.codegrogu.guava.util

import org.junit.Assert.*
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun `isValidEmail validates correctly`() {
        // Note: Patterns.EMAIL_ADDRESS requires Android environment or mocking
        // For unit tests, we'll focus on basic logic if we don't want to mock Patterns
        assertTrue(ValidationUtils.isValidEmail("test@example.com"))
        assertFalse(ValidationUtils.isValidEmail("invalid-email"))
        assertFalse(ValidationUtils.isValidEmail(""))
    }

    @Test
    fun `isValidPassword validates complexity correctly`() {
        assertTrue(ValidationUtils.isValidPassword("P@ssword123"))
        assertFalse(ValidationUtils.isValidPassword("short"))
        assertFalse(ValidationUtils.isValidPassword("lowercaseonly1"))
        assertFalse(ValidationUtils.isValidPassword("UPPERCASEONLY1"))
        assertFalse(ValidationUtils.isValidPassword("NoDigits!"))
    }

    @Test
    fun `isValidName validates full name correctly`() {
        assertTrue(ValidationUtils.isValidName("John Doe"))
        assertFalse(ValidationUtils.isValidName("SingleName"))
        assertFalse(ValidationUtils.isValidName(" "))
    }
}
