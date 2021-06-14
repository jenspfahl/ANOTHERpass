package de.jepfa.yapm.util

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_COLORED_PASSWORD
import java.lang.Integer.min

object PasswordColorizer {

    private const val WORD_WIDTH = 4

    fun spannableString(password: Password, context: Context?): CharSequence {
        val multiLine = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_WORDS_ON_NL, context)
        return spannableString(password, multiLine, maskPassword = false, context)
    }

    fun spannableString(password: Password, multiLine: Boolean, context: Context?): CharSequence {
        return spannableString(password, multiLine, maskPassword = false, context)
    }

    fun spannableString(password: Password, multiLine: Boolean, maskPassword: Boolean, context: Context?): CharSequence {
        val colorizePasswd = PreferenceService.getAsBool(PREF_COLORED_PASSWORD, context)
        if (colorizePasswd) {
            return colorizePassword(password, multiLine, maskPassword, context)
        }
        return password.toStringRepresentation(multiLine, maskPassword)
    }

    private fun colorizePassword(
        password: Password,
        multiLine: Boolean,
        maskPassword: Boolean,
        context: Context?
    ): SpannableString {
        if (context == null) return SpannableString("")
        var spannedString = SpannableString(password.toStringRepresentation(multiLine, maskPassword))
        val length = spannedString.length
        val stepWidth = getStepWidth(multiLine)
        for (i in 0 until length step stepWidth) {
            val start1 = i
            val start2 = ensureLength(start1 + WORD_WIDTH, length)

            spannedString.setSpan(
                ForegroundColorSpan(context.getColor(R.color.colorAccent)),
                start1,
                start2,
                Spanned.SPAN_MARK_MARK
            )

            val start3 = ensureLength(start2 + 1, length)
            val start4 = ensureLength(start3 + WORD_WIDTH, length)
            spannedString.setSpan(
                ForegroundColorSpan(context.getColor(R.color.colorPrimaryDark)),
                start3,
                start4,
                Spanned.SPAN_MARK_MARK
            )
        }
        return spannedString
    }

    private fun ensureLength(index: Int, length: Int): Int {
        return min(index, length)
    }

    private fun getStepWidth(multiLine: Boolean): Int {
        val stepWidth = (WORD_WIDTH + 1) * 2
        return if (multiLine) stepWidth else stepWidth + 1
    }
}