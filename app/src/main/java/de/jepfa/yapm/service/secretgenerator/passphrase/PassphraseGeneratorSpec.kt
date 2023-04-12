package de.jepfa.yapm.service.secretgenerator.passphrase

import de.jepfa.yapm.service.secretgenerator.GeneratorSpec
import de.jepfa.yapm.service.secretgenerator.SecretStrength

data class PassphraseGeneratorSpec(
    val strength: SecretStrength = SecretStrength.NORMAL,
    val wordBeginningUpperCase: Boolean = false,
    val addDigit: Boolean = false,
    val addSpecialChar: Boolean = false,
    val useExtendedSpecialChars: Boolean = false,
): GeneratorSpec