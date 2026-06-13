package com.taskplanner.android.core.util

object PasswordValidator {
    private const val SPECIAL_CHARS = "!@#\$%^&*()_+-=[]{}|;:,.<>?"

    data class Result(val isValid: Boolean, val message: String)

    fun isValidPassword(password: String): Result {
        if (password.length < 8) {
            return Result(false, "Пароль должен содержать минимум 8 символов")
        }
        if (!password.any { it.isDigit() }) {
            return Result(false, "Добавьте хотя бы одну цифру (0-9)")
        }
        if (!password.any { it.isLowerCase() }) {
            return Result(false, "Добавьте хотя бы одну строчную букву (a-z)")
        }
        if (!password.any { it.isUpperCase() }) {
            return Result(false, "Добавьте хотя бы одну заглавную букву (A-Z)")
        }
        if (!password.any { it in SPECIAL_CHARS }) {
            return Result(false, "Добавьте хотя бы один спецсимвол (!@#\$%^&* и т.д.)")
        }
        if (password.contains(' ')) {
            return Result(false, "Пароль не должен содержать пробелы")
        }
        return Result(true, "Пароль надёжный")
    }

    fun passwordsMatch(a: String, b: String): Boolean = a.isNotEmpty() && b.isNotEmpty() && a == b

    fun hasMinLength(p: String): Boolean = p.length >= 8
    fun hasDigit(p: String): Boolean = p.any { it.isDigit() }
    fun hasLowercase(p: String): Boolean = p.any { it.isLowerCase() }
    fun hasUppercase(p: String): Boolean = p.any { it.isUpperCase() }
    fun hasSpecial(p: String): Boolean = p.any { it in SPECIAL_CHARS }
    fun hasNoSpaces(p: String): Boolean = p.isNotEmpty() && !p.contains(' ')
}
