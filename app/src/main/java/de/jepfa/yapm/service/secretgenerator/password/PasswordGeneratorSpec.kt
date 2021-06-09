package de.jepfa.yapm.service.secretgenerator.password

import de.jepfa.yapm.service.secretgenerator.GeneratorSpec
import de.jepfa.yapm.service.secretgenerator.SecretStrength

data class PasswordGeneratorSpec(val strength: SecretStrength = SecretStrength.NORMAL,
                                 val onlyLowerCase: Boolean = false,
                                 val excludeSpecialChars: Boolean = false,
                                 val noDigits: Boolean = false) : GeneratorSpec