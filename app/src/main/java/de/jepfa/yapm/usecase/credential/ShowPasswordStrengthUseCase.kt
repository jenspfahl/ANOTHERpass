package de.jepfa.yapm.usecase.credential

import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secretgenerator.GeneratorBase
import de.jepfa.yapm.service.secretgenerator.password.PasswordGenerator
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.emoji
import de.jepfa.yapm.util.secondsToYear
import de.jepfa.yapm.util.toExponentFormat
import de.jepfa.yapm.util.toReadableFormat

object ShowPasswordStrengthUseCase: InputUseCase<ShowPasswordStrengthUseCase.Input, BaseActivity>() {

    private val passwordGenerator = PasswordGenerator(context = null)

    data class Input(val password: Password, val titleId: Int)

    override suspend fun doExecute(input: Input, activity: BaseActivity): Boolean {
        val combinations = guessPasswordCombinations(input.password)
        showPasswordStrength(combinations, input.titleId, activity)

        return true
    }

    fun showPasswordStrength(combinations: Double, titleId: Int, activity: BaseActivity) {
        val entropy = passwordGenerator.calcEntropy(combinations)
        val bruteForceWithPentium = passwordGenerator.calcBruteForceWaitingSeconds(
            combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_PENTIUM
        )
        val bruteForceWithSupercomp = passwordGenerator.calcBruteForceWaitingSeconds(
            combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_SUPERCOMP
        )

        var strengthLevel =
        if (entropy >= 128) emoji(0x1f606)              // 😆 too strong
            else if (entropy >= 80) emoji(0x1f603)      // 😃 strong
            else if (entropy >= 65) emoji(0x1f642)      // 🙂 ok
            else if (entropy >= 50) emoji(0x1f610)      // 😐 weak
            else if (entropy >= 28) emoji(0x1f641)      // 🙁 poor
            else emoji(0x1f625)                         // 😥 very poor

        val markerIconCombinations = "%iconCombination%"
        val markerIconEntropy = "%iconEntropy%"
        val markerIconPentium = "%iconPentium%"
        val markerIconSuperComp = "%iconSuperComp%"

        val intent = ""
        val sb = StringBuilder(
            markerIconCombinations + " " + activity.getString(R.string.combinations) + ": " +
                System.lineSeparator() +
                System.lineSeparator() +
                intent + combinations.toReadableFormat() +
                System.lineSeparator()
        )

        if (combinations >= 1_000_000) {
            sb.append(
                intent + "(${combinations.toExponentFormat()})" +
                        System.lineSeparator()
            )
        }

        sb.append(
            System.lineSeparator() +
                    markerIconEntropy + " " + activity.getString(R.string.entropy) + ": " +
                    System.lineSeparator() +
                    System.lineSeparator() +
                    intent + entropy.toInt() + " " + strengthLevel +
                    System.lineSeparator() +
                    System.lineSeparator() +
                    markerIconPentium + " " +
                    activity.getString(
                        R.string.bruteforce_years_pc,
                        GeneratorBase.BRUTEFORCE_ATTEMPTS_PENTIUM.toReadableFormat()
                    ) + ": " +
                    System.lineSeparator() +
                    System.lineSeparator() +
                    intent + bruteForceWithPentium.secondsToYear().toReadableFormat() +
                    " ${activity.getString(R.string.years)}" +
                    System.lineSeparator()
        )

        if (bruteForceWithPentium.secondsToYear() >= 1_000_000) {
            sb.append(
                intent + "(${bruteForceWithPentium.secondsToYear().toExponentFormat()})" +
                        System.lineSeparator()
            )
        }

        sb.append(
                    System.lineSeparator() +
                    markerIconSuperComp + " " +
                    activity.getString(
                        R.string.bruteforce_year_supercomp,
                        GeneratorBase.BRUTEFORCE_ATTEMPTS_SUPERCOMP.toReadableFormat()
                    ) + ": " +
                    System.lineSeparator() +
                    System.lineSeparator() +
                            intent + bruteForceWithSupercomp.secondsToYear().toReadableFormat() +
                    " ${activity.getString(R.string.years)}" +
                    System.lineSeparator()
        )

        if (bruteForceWithSupercomp.secondsToYear() >= 100_000_000) {
            sb.append(
                intent + "(${bruteForceWithSupercomp.secondsToYear().toExponentFormat()})"
            )
        }

        val text = sb.toString()
        val span = SpannableString(text)

        replaceIconMarker(activity, text, markerIconCombinations, R.drawable.ic_baseline_casino_20, span)
        replaceIconMarker(activity, text, markerIconEntropy, R.drawable.ic_baseline_fitness_center_20, span)
        replaceIconMarker(activity, text, markerIconPentium, R.drawable.ic_baseline_computer_20, span)
        replaceIconMarker(activity, text, markerIconSuperComp, R.drawable.ic_baseline_dns_20, span)

        AlertDialog.Builder(activity)
            .setTitle(titleId)
            .setIcon(R.drawable.ic_baseline_fitness_center_24)
            .setMessage(span)
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun replaceIconMarker(
        activity: BaseActivity,
        text: String,
        marker: String,
        iconId: Int,
        span: SpannableString
    ) {
        activity.getDrawable(iconId)?.let { drawable ->
            val startIndex = text.indexOf(marker)
            if (startIndex != -1) {
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                span.setSpan(
                    ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM),
                    startIndex,
                    startIndex + marker.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
        }
    }


    fun guessPasswordCombinations(password: Password): Double {
        // rudimentary combination calculation by assuming a-Z, A-Z, 0-9 and some typical special chars
        val containsLowerCase =
            containsChars(password, GeneratorBase.DEFAULT_ALPHA_CHARS_LOWER_CASE)
        val containsUpperCase =
            containsChars(password, GeneratorBase.DEFAULT_ALPHA_CHARS_UPPER_CASE)
        val containsDigits = containsChars(password, GeneratorBase.DEFAULT_DIGITS)
        val containsSpecialChars = containsChars(password, GeneratorBase.DEFAULT_SPECIAL_CHARS)
        val containsExtendedSpecialChars = containsChars(password, GeneratorBase.EXTENDED_SPECIAL_CHARS)
        return Math.pow(
            (containsLowerCase + containsUpperCase + containsDigits + containsSpecialChars + containsExtendedSpecialChars).toDouble(),
            password.length.toDouble()
        )
    }

    private fun containsChars(password: Password, chars: String) =
        if (password.contains(Regex("[${Regex.escape(chars)}]"))) chars.length
        else 0

}