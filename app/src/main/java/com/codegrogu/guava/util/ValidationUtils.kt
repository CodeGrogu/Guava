package com.codegrogu.guava.util

object ValidationUtils {
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_REGEX.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        // At least 8 characters, 1 digit, 1 uppercase, 1 lowercase
        return password.length >= 8 && 
               password.any { it.isDigit() } && 
               password.any { it.isUpperCase() } &&
               password.any { it.isLowerCase() }
    }

    fun isValidName(name: String): Boolean {
        return name.trim().split(" ").size >= 2
    }
}
