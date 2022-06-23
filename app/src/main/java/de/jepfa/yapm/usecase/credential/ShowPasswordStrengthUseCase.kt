package de.jepfa.yapm.usecase.credential

import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import de.jepfa.yapm.service.secretgenerator.password.PasswordGenerator
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.emoji
import de.jepfa.yapm.util.secondsToYear
import de.jepfa.yapm.util.toExponentFormat
import de.jepfa.yapm.util.toReadableFormat

object ShowPasswordStrengthUseCase: InputUseCase<ShowPasswordStrengthUseCase.Input, SecureActivity>() {

    private val passwordGenerator = PasswordGenerator(context = null)

    data class Input(val password: Password, val titleId: Int)

    override fun doExecute(input: Input, activity: SecureActivity): Boolean {
        val combinations = guessPasswordCombinations(input.password)
        showPasswordStrength(combinations, input.titleId, activity)

        return true
    }

    fun showPasswordStrength(combinations: Double, titleId: Int, activity: SecureActivity) {
        val entropy = passwordGenerator.calcEntropy(combinations)
        val bruteForceWithPentium = passwordGenerator.calcBruteForceWaitingSeconds(
            combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_PENTIUM
        )
        val bruteForceWithSupercomp = passwordGenerator.calcBruteForceWaitingSeconds(
            combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_SUPERCOMP
        )

        var strengthLevel = emoji(0x1f625)
        if (entropy >= 128) strengthLevel = emoji(0x1f606)
        else if (entropy >= 60) strengthLevel = emoji(0x1f642)
        else if (entropy >= 36) strengthLevel = emoji(0x1f610)
        else if (entropy >= 28) strengthLevel = emoji(0x1f641)
        AlertDialog.Builder(activity)
            .setTitle(titleId)
            .setIcon(R.drawable.ic_baseline_fitness_center_24)
            .setMessage(
                activity.getString(R.string.combinations) + ": " +
                        System.lineSeparator() +
                        combinations.toReadableFormat() +
                        System.lineSeparator() +
                        "(${combinations.toExponentFormat()})" +
                        System.lineSeparator() +
                        System.lineSeparator() +
                        activity.getString(R.string.entropy) + ": " +
                        System.lineSeparator() +
                        entropy.toInt() + " " + strengthLevel +
                        System.lineSeparator() +
                        System.lineSeparator() +
                        activity.getString(R.string.bruteforce_years_pc) + ": " +
                        System.lineSeparator() +
                        bruteForceWithPentium.secondsToYear().toReadableFormat() +
                        System.lineSeparator() +
                        "(${bruteForceWithPentium.secondsToYear().toExponentFormat()})" +
                        System.lineSeparator() +
                        System.lineSeparator() +
                        activity.getString(R.string.bruteforce_year_supercomp) + ": " +
                        System.lineSeparator() +
                        bruteForceWithSupercomp.secondsToYear().toReadableFormat() +
                        System.lineSeparator() +
                        "(${bruteForceWithSupercomp.secondsToYear().toExponentFormat()})"
            )
            .show()
    }


    fun guessPasswordCombinations(password: Password): Double {
        // rudimentary combination calculation by assuming a-Z, A-Z, 0-9 and some typical special chars
        val containsLowerCase =
            containsChars(password, PasswordGenerator.DEFAULT_ALPHA_CHARS_LOWER_CASE)
        val containsUpperCase =
            containsChars(password, PasswordGenerator.DEFAULT_ALPHA_CHARS_UPPER_CASE)
        val containsDigits = containsChars(password, PasswordGenerator.DEFAULT_DIGITS)
        val containsSpecialChars = containsChars(password, PasswordGenerator.DEFAULT_SPECIAL_CHARS)
        return Math.pow(
            (containsLowerCase + containsUpperCase + containsDigits + containsSpecialChars).toDouble(),
            password.length.toDouble()
        )
    }

    private fun containsChars(password: Password, chars: String) =
        if (password.contains(Regex("[${Regex.escape(chars)}]"))) chars.length
        else 0

}