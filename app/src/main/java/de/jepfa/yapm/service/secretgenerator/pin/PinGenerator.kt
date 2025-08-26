package de.jepfa.yapm.service.secretgenerator.pin

import android.content.Context
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import java.security.SecureRandom
import kotlin.math.pow

class PinGenerator(
    val digits: String = DEFAULT_DIGITS,
    context: Context?,
    secureRandom: SecureRandom? = null,
) : GeneratorBase<PinGeneratorSpec>(context, secureRandom) {


    override fun generate(spec: PinGeneratorSpec): Password {
        val buffer = CharArray(spec.length)

        for (i in buffer.indices) {
            buffer[i] = random(digits)
        }
        return Password(buffer)
    }

    override fun calcCombinationCount(spec: PinGeneratorSpec): Double {
        return digits.length.toDouble().pow(spec.length.toDouble())
    }


}