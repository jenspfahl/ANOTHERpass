package de.jepfa.yapm.util

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.chip.Chip
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.label.Label
import java.text.ParsePosition
import java.util.*

fun toastText(context: Context?, text: String) {
    if (context != null) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}

fun toastText(context: Context?, id: Int) {
    if (context != null) {
        Toast.makeText(context, id, Toast.LENGTH_LONG).show()
    }
}

fun enrichId(context: Context, name: String, id: Int?): String {
    val showIds = PreferenceService.getAsBool(PreferenceService.PREF_SHOW_CREDENTIAL_IDS, context)
    return if (showIds) "$name [:${id?:"new"}]" else name
}

fun createAndAddLabelChip(
    label: Label,
    container: ViewGroup,
    thinner: Boolean,
    context: Context?
): Chip {
    val chip = createLabelChip(label, thinner, context)
    container.addView(chip)
    return chip
}

fun createLabelChip(
    label: Label,
    thinner: Boolean,
    context: Context?
): Chip {
    val chip = Chip(context)
    chip.text = label.name
    chip.tag = label.labelId
    chip.textAlignment = View.TEXT_ALIGNMENT_CENTER
    if (thinner) {
        chip.textSize = 12.0f
        chip.chipCornerRadius = 32.0f
        chip.chipMinHeight = 24.0f
        chip.setEnsureMinTouchTargetSize(true)
        chip.ensureAccessibleTouchTarget(32)
    }
    context?.let {
        if (label.labelId != -1) {
            chip.chipBackgroundColor = ColorStateList.valueOf(label.getColor(it))
            chip.setTextColor(it.getColor(android.R.color.white))
        }
    }
    return chip
}

fun linkify(s: SpannableString) {
    Linkify.addLinks(s, Linkify.WEB_URLS)
}

fun linkify(textView: TextView) {
    textView.text = ensureHttp(textView.text.toString())
    Linkify.addLinks(textView, Linkify.WEB_URLS)
    textView.movementMethod = LinkMovementMethod.getInstance()
}

fun linkifyDialogMessage(dialog: Dialog) {
    val msgTextView = dialog.findViewById<TextView>(android.R.id.message)
    msgTextView?.let {
        it.movementMethod = LinkMovementMethod.getInstance()
    }
}

fun dateToString(date: Date?): String {
    if (date != null) {
        return Constants.SDF_DT_MEDIUM.format(date)
    }
    else {
        return "??"
    }
}


fun formatAsDate(s: String?): String {
    if (s != null) {
        val timestamp = s.toLongOrNull()
        if (timestamp != null) {
            return dateToString(Date(timestamp))
        }
        else {
            return s
        }
    }
    return "??"
}

private fun ensureHttp(s: String): String {
    if (s.startsWith(prefix = "http", ignoreCase = true)) {
        return s
    }
    else {
        return "http://" + s
    }
}

