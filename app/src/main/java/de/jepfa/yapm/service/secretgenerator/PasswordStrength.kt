package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.R

enum class PasswordStrength(val passwordLength: Int, val nameId: Int) {
    EASY(10, R.string.EASY),
    NORMAL(12, R.string.NORMAL),
    STRONG(16, R.string.STRONG),
    ULTRA(20, R.string.ULTRA),
    EXTREME(24, R.string.EXTREME),
    HYPER(28, R.string.HYPER),
}