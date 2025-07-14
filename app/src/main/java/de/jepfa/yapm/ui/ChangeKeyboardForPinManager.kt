package de.jepfa.yapm.ui

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.util.toastText

/**
 * List of editText is all PIN views, at least one. First view is which hold the icon
 */
open class ChangeKeyboardForPinManager(
    private val context: Context,
    private val editTextViews: List<EditText>,
    )
{

    private var showNumberPad = false

    fun create(changeImeiButton: ImageView) {
        val hideNumberPad = PreferenceService.getAsBool(PreferenceService.PREF_HIDE_NUMBER_PAD_FOR_PIN, context)
        if (hideNumberPad) {
            changeImeiButton.visibility = View.GONE
        }
        else {
            changeImeiButton.setOnClickListener {
                showNumberPad = !showNumberPad
                if (showNumberPad) {
                    toastText(context, context.getString(R.string.show_number_keyboard_for_login))
                }
                else {
                    toastText(context, context.getString(R.string.show_common_keyboard_for_login))
                }
                updateShowNumberPad(changeImeiButton, showImei = true, context)
                PreferenceService.putBoolean(
                    PreferenceService.PREF_SHOW_NUMBER_PAD_FOR_PIN,
                    showNumberPad,
                    context
                )
            }

            showNumberPad = PreferenceService.getAsBool(PreferenceService.PREF_SHOW_NUMBER_PAD_FOR_PIN, context)
            updateShowNumberPad(changeImeiButton, showImei = false, context)
        }
    }


    private fun updateShowNumberPad(
        changeImeiButton: ImageView,
        showImei: Boolean,
        context: Context
    ) {

        if (showNumberPad) {
            editTextViews.forEach { editTextView ->
                editTextView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                editTextView.typeface = Typeface.DEFAULT
            }
            changeImeiButton.setImageDrawable(context.getDrawable(R.drawable.baseline_abc_24))
        } else {
            editTextViews.forEach { editTextView ->
                editTextView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editTextView.typeface = Typeface.DEFAULT

            }
            changeImeiButton.setImageDrawable(context.getDrawable(R.drawable.baseline_123_24))
        }

       if (showImei) {
            val firstView = editTextViews.first()
            firstView.requestFocus()
           val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
           imm.showSoftInput(firstView, 0)
        }
    }

}