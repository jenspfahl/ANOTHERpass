package de.jepfa.yapm.service.secretgenerator.passphrase

import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import java.lang.Math.ceil
import java.lang.Math.pow

val DEFAULT_VOCALS = "aeiouy"
val DEFAULT_CONSONANTS = "bcdfghjklmnpqrstvwxz"
val DEFAULT_DIGITS = "1234567890"
val DEFAULT_SPECIAL_CHARS = "!?-,.:/$%&@#"

class PassphraseGenerator(
    val vocals: String = DEFAULT_VOCALS,
    val consonants: String = DEFAULT_CONSONANTS,
    val digits: String = DEFAULT_DIGITS,
    val specialChars: String = DEFAULT_SPECIAL_CHARS
): GeneratorBase<PassphraseGeneratorSpec>() {


    override fun generate(spec: PassphraseGeneratorSpec): Password {
        var buffer = Password(CharArray(0))

        for (i in 0 until strengthToWordCount(spec.strength)) {
            val word = generateWord()
            buffer.add(word)
        }

        if (spec.wordBeginningUpperCase) {
            buffer.replace(0, buffer.get(0).toUpperCase())
        }

        if (spec.addDigit) {
            buffer.add(random(digits))
        }

        if (spec.addSpecialChar) {
            buffer.add(random(specialChars))
        }

        return buffer
    }

    private fun generateWord(): Password {
        val word = generateTuple()
        val recentEndsWithVocal = isVocal(word.last())
        val next = generateTuple(recentEndsWithVocal)

        word.add(next)

        return word
    }

    private fun generateTuple(recentEndsWithVocal: Boolean = false): Password {
        val buffer = CharArray(2)
        buffer[0] = random(alphabet())
        val char = buffer[0]
        var material = when(isVocal(char)) {
            true ->  alphabet()
            else -> vocals
        }
        if (recentEndsWithVocal && isConsonant(char)) {
            material += char
        }

        buffer[1] = random(material)
        return Password(buffer)
    }

    override fun calcCombinationCount(spec: PassphraseGeneratorSpec): Double {
        val vocalLength = vocals.length.toDouble()
        val consonantLength = consonants.length.toDouble()
        val alphabetLength = alphabet().length.toDouble()

        val vocalChance = vocalLength / alphabetLength
        val consonantChance = consonantLength / alphabetLength

        val tupleCombinations = alphabetLength * ((vocalChance * alphabetLength) + (consonantChance * vocalLength)) // this is the material = when(isVocal) expression
        val tupleCombinationsWithVocalAtLast = vocalLength * alphabetLength // these tuples allow subsequent tuples with duplicate consonants

        val wordCombinations = (tupleCombinations * tupleCombinations) + (tupleCombinationsWithVocalAtLast * consonantLength) // add tuples with duplicate consonants for each consonant
        var totalCombinations = ceil(pow(wordCombinations, strengthToWordCount(spec.strength).toDouble()))

        if (spec.addDigit) {
            totalCombinations *= digits.length
        }
        if (spec.addSpecialChar) {
            totalCombinations *= specialChars.length
        }
        return totalCombinations
    }

    private fun random(material: String): Char {
        val index = random.nextInt(material.length)

        return material[index]
    }

    private fun isVocal(char: Char): Boolean {
        return vocals.contains(char, true)
    }

    private fun isConsonant(char: Char): Boolean {
            return consonants.contains(char, true)
    }

    private fun strengthToWordCount(strength: SecretStrength): Int {
        return strength.pseudoPhraseLength / 4
    }

    private fun alphabet() = vocals + consonants

}