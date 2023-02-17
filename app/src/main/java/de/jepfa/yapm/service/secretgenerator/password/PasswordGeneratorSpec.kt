package de.jepfa.yapm.service.secretgenerator.password

import de.jepfa.yapm.service.secretgenerator.GeneratorSpec
import de.jepfa.yapm.service.secretgenerator.SecretStrength

data class PasswordGeneratorSpec(
    val strength: SecretStrength = SecretStrength.NORMAL,
    val noUpperCase: Boolean = false,
    val noSpecialChars: Boolean = false,
    val noDigits: Boolean = false,
    val useExtendedSpecialChars: Boolean = false,
) : GeneratorSpec