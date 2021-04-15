package de.jepfa.yapm.ui.label

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import com.pchmn.materialchips.model.Chip
import de.jepfa.yapm.R

class LabelChip(val color: Int?, name: String, description: String): Chip(name.toUpperCase(), description) {

    constructor(name: String, description: String) : this(null, name.toUpperCase(), description)

    fun getColor(context: Context): Int {
        val colorCode = color ?: R.color.colorPrimaryDark
        return context.getColor(colorCode)
    }
}