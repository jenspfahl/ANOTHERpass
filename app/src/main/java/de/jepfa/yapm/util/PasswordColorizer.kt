package de.jepfa.yapm.util

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_COLORED_PASSWORD
import java.lang.Integer.min
import de.jepfa.yapm.model.secret.Password.FormattingStyle as PresentationMode

object PasswordColorizer {

    private const val WORD_WIDTH = 4

    fun spannableString(password: Password, context: Context?): CharSequence {
        val showFormatted = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_SHOW_FORMATTED, context)
        val multiLine = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_WORDS_ON_NL, context)
        val presentationMode = Password.FormattingStyle.createFromFlags(multiLine, showFormatted)

        return spannableObfusableString(password, presentationMode, obfuscated = false, context)
    }

    fun spannableString(password: Password, presentationMode: PresentationMode, context: Context?): CharSequence {
        return spannableObfusableAndMaskableString(password, presentationMode, maskPassword = false, obfuscated = false, context)
    }

    fun spannableObfusableString(password: Password, presentationMode: PresentationMode, obfuscated: Boolean, context: Context?): CharSequence {
        return spannableObfusableAndMaskableString(password, presentationMode, maskPassword = false, obfuscated = obfuscated, context)
    }

    fun spannableObfusableAndMaskableString(
        password: Password,
        presentationMode: PresentationMode,
        maskPassword: Boolean,
        obfuscated: Boolean,
        context: Context?
    ): SpannableString {
        if (context == null) return SpannableString("")

        val multiLine = presentationMode.isMultiLine()
        val raw = presentationMode == PresentationMode.RAW
        var spannedString = SpannableString(password.toFormattedPassword(presentationMode, maskPassword))

        val colorizePasswd = PreferenceService.getAsBool(PREF_COLORED_PASSWORD, context)

        val length = spannedString.length
        val stepWidth = getStepWidth(multiLine)
        if (colorizePasswd) {
            if (password.isNumeric()) {
                spannedString.setSpan(
                    ForegroundColorSpan(context.getColor(R.color.colorAltAccent)),
                    0,
                    spannedString.length,
                    Spanned.SPAN_MARK_MARK
                )
            }
            else if (raw) {
                spannedString.setSpan(
                    ForegroundColorSpan(context.getColor(R.color.colorPrimaryDark)),
                    0,
                    spannedString.length,
                    Spanned.SPAN_MARK_MARK
                )
            }
            else {
                for (i in 0 until length step stepWidth) {
                    val start1 = i
                    val start2 = ensureLength(start1 + WORD_WIDTH, length)

                    spannedString.setSpan(
                        ForegroundColorSpan(context.getColor(R.color.colorAltAccent)),
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
            }
        }
        if (obfuscated) {
            spannedString.setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
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