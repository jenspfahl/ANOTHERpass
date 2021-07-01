package de.jepfa.yapm.service.secretgenerator.password

import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import java.util.*

class PasswordGenerator : GeneratorBase<PasswordGeneratorSpec>() {

    val ALPHA_CHARS_LOWER_CASE = "abcdefghijklmnopqrstuvwxyz"
    val ALPHA_CHARS_UPPER_CASE = ALPHA_CHARS_LOWER_CASE.toUpperCase(Locale.ROOT)
    val DIGITS = "0123456789"
    val SPECIAL_CHARS = "!?-,.:/$%&@#_;"

    override fun generate(spec: PasswordGeneratorSpec): Password {
        while(true){
            val buffer = generatePassword(spec)
            if (matchSpec(spec, buffer)) {
                return Password(buffer)
            }
        }
    }

    override fun calcCombinationCount(spec: PasswordGeneratorSpec): Double {
        val material = extractMaterial(spec)
        //TODO contains also all #matchSpec() exclusions which shouldn't
        return Math.pow(material.length.toDouble(), spec.strength.ordinaryPasswordLength.toDouble())
    }

    private fun matchSpec(spec: PasswordGeneratorSpec, buffer: CharArray): Boolean {
        if (!spec.onlyLowerCase && !containsChar(buffer, ALPHA_CHARS_UPPER_CASE)) {
            return false
        }
        if (!spec.noDigits && !containsChar(buffer, DIGITS)) {
            return false
        }
        if (!spec.excludeSpecialChars && !containsChar(buffer, SPECIAL_CHARS)) {
            return false
        }

        return true
    }

    private fun containsChar(buffer: CharArray, material: String): Boolean {
        return buffer.filter { c -> material.contains(c) }.isNotEmpty()
    }

    private fun generatePassword(spec: PasswordGeneratorSpec): CharArray {
        val buffer = CharArray(spec.strength.ordinaryPasswordLength)
        val material = extractMaterial(spec)

        for (i in 0 until buffer.size) {
            buffer[i] = random(material)
        }
        return buffer
    }

    private fun extractMaterial(spec: PasswordGeneratorSpec): String {
        var material = ALPHA_CHARS_LOWER_CASE
        if (!spec.noDigits) {
            material += DIGITS
        }
        if (!spec.excludeSpecialChars) {
            material += SPECIAL_CHARS
        }
        if (!spec.onlyLowerCase) {
            material += ALPHA_CHARS_UPPER_CASE
        }
        return material
    }

    private fun random(material: String): Char {
        val index = random.nextInt(material.length)

        return material[index]
    }

}