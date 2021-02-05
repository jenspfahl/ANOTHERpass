package de.jepfa.yapm.service.secretgenerator

data class PassphraseGeneratorSpec(val strength: PasswordStrength = PasswordStrength.NORMAL,
                                   val wordBeginningUpperCase: Boolean = false,
                                   val addDigit: Boolean = false,
                                   val addSpecialChar: Boolean = false) {
}