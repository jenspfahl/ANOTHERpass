package de.jepfa.yapm.service.secretgenerator

data class PasswordGeneratorSpec(val strength: PasswordStrength = PasswordStrength.NORMAL,
                                 val onlyUpperCase: Boolean = false,
                                 val excludeSpecialChars: Boolean = false,
                                 val onlyCommonSpecialChars: Boolean = false) {
}