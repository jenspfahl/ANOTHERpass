package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.R

enum class PassphraseStrength(val passwordLength: Int, val nameId: Int) {
    EASY(12, R.string.EASY),
    NORMAL(16, R.string.NORMAL),
    STRONG(20, R.string.STRONG),
    SUPER_STRONG(24, R.string.SUPER_STRONG),
    EXTREME(28, R.string.EXTREME),
}