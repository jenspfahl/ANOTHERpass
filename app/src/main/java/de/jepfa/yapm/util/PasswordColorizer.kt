package de.jepfa.yapm.util

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.util.PreferenceUtil.PREF_COLORED_PASSWORD
import java.lang.Integer.min

object PasswordColorizer {

    private const val STEP_WIDTH = 11
    private const val WORD_WIDTH = 4

    fun spannableString(password: Password, context: Context): CharSequence {
        val colorizePasswd = PreferenceUtil.getBool(PREF_COLORED_PASSWORD, true, context)
        if (colorizePasswd) {
            return colorizePassword(password, context)
        }
        return password.debugToString()
    }

    private fun colorizePassword(
        password: Password,
        context: Context
    ): SpannableString {
        var spannedString = SpannableString(password.debugToString())
        val length = spannedString.length
        for (i in 0 until length step STEP_WIDTH) {
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
}