package de.jepfa.yapm.util

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.label.Label

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
    val chip = Chip(context)
    chip.text = label.name
    chip.textAlignment = View.TEXT_ALIGNMENT_CENTER
    if (thinner) {
        chip.textSize = 12.0f
        chip.chipCornerRadius = 32.0f
        chip.chipMinHeight = 24.0f
        chip.setEnsureMinTouchTargetSize(true)
        chip.ensureAccessibleTouchTarget(32)
    }
    context?.let {
        chip.chipBackgroundColor = ColorStateList.valueOf(label.getColor(it))
        chip.setTextColor(it.getColor(android.R.color.white))
    }
    container.addView(chip)
    return chip
}
