package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.R

enum class PasswordStrength(val passwordLength: Int, val nameId: Int) {
    EASY(10, R.string.EASY),
    NORMAL(12, R.string.NORMAL),
    STRONG(16, R.string.STRONG),
    SUPER_STRONG(20, R.string.SUPER_STRONG),
    EXTREME(24, R.string.EXTREME),
}