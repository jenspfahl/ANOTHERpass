package de.jepfa.yapm.service.secretgenerator

data class PasswordGeneratorSpec(val strength: PasswordStrength = PasswordStrength.NORMAL,
                                 val onlyLowerCase: Boolean = false,
                                 val excludeSpecialChars: Boolean = false,
                                 val onlyCommonSpecialChars: Boolean = false,
                                 val noDigits: Boolean = false) : GeneratorSpec