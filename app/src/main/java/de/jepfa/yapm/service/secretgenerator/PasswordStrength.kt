package de.jepfa.yapm.service.secretgenerator

enum class PasswordStrength(val passwordLength: Int) {
    NORMAL(12), STRONG(16), SUPER_STRONG(20)
}