package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.R

enum class PassphraseStrength(val passwordLength: Int, val nameId: Int) {
    EASY(12, R.string.EASY),
    NORMAL(16, R.string.NORMAL),
    STRONG(20, R.string.STRONG),
    ULTRA(24, R.string.ULTRA),
    EXTREME(28, R.string.EXTREME),
    HYPER(32, R.string.HYPER),
}