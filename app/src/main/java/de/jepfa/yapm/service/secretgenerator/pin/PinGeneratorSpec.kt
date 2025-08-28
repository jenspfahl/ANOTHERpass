package de.jepfa.yapm.service.secretgenerator.pin

import de.jepfa.yapm.service.secretgenerator.GeneratorSpec
import de.jepfa.yapm.service.secretgenerator.SecretStrength

data class PinGeneratorSpec(
    val length: Int = 4,
) : GeneratorSpec