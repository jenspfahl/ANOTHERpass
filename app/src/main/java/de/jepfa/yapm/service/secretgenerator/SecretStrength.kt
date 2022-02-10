package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.R

enum class SecretStrength(val pseudoPhraseLength: Int, val ordinaryPasswordLength: Int, val nameId: Int) {
    ONE_WORD(4, 4, -1),
    TWO_WORDS(8, 8, -1),
    EASY(12, 10, R.string.EASY),
    NORMAL(16, 12, R.string.NORMAL),
    STRONG(20, 16, R.string.STRONG),
    ULTRA(24, 20, R.string.ULTRA),
    EXTREME(28, 24, R.string.EXTREME),
    HYPER(32, 28, R.string.HYPER),
}