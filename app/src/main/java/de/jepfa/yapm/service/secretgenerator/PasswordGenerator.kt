package de.jepfa.yapm.service.secretgenerator

import de.jepfa.yapm.model.Password
import java.security.SecureRandom

class PasswordGenerator : GeneratorBase<PasswordGeneratorSpec>() {

    val ALPHA_CHARS_LOWER_CASE = "abcdefghijklmnopqrstuvwxyz"
    val ALPHA_CHARS_UPPER_CASE = ALPHA_CHARS_LOWER_CASE.toUpperCase()
    val DIGITS = "0123456789"
    val SPECIAL_CHARS_1 = "!?-_,.;:/"
    val SPECIAL_CHARS_2 = "$%&()[]{}"

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

        return Math.pow(material.length.toDouble(), spec.strength.passwordLength.toDouble())
    }

    private fun matchSpec(spec: PasswordGeneratorSpec, buffer: CharArray): Boolean {
        if (spec.onlyLowerCase && containsChar(buffer, ALPHA_CHARS_UPPER_CASE)) {
            return false
        }
        if (spec.noDigits && containsChar(buffer, DIGITS)) {
            return false
        }
        if (spec.excludeSpecialChars && containsChar(buffer, SPECIAL_CHARS_1 + SPECIAL_CHARS_2)) {
            return false
        }
        if (spec.onlyCommonSpecialChars && containsChar(buffer, SPECIAL_CHARS_2)) {
            return false
        }

        return true
    }

    private fun containsChar(buffer: CharArray, material: String): Boolean {
        return buffer.filter { c -> material.contains(c) }.isNotEmpty()
    }

    private fun generatePassword(spec: PasswordGeneratorSpec): CharArray {
        val buffer = CharArray(spec.strength.passwordLength)
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
            material += SPECIAL_CHARS_1
            if (!spec.onlyCommonSpecialChars) {
                material += SPECIAL_CHARS_2
            }
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