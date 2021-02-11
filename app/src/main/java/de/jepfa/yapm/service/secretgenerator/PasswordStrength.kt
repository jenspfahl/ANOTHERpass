package de.jepfa.yapm.service.secretgenerator

enum class PasswordStrength(val passwordLength: Int) {
    EASY(10),
    NORMAL(12),
    STRONG(16),
    SUPER_STRONG(20),
    EXTREME(24),
}