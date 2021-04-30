package de.jepfa.yapm.ui.label

import android.content.Context
import com.pchmn.materialchips.model.Chip
import de.jepfa.yapm.R

class LabelChip(val rgbColor: Int?, name: String, val description: String): Chip(name.toUpperCase().trim(), description) {

    constructor(name: String, description: String) : this(null, name.toUpperCase().trim(), description)

    fun getColor(context: Context): Int {
        return rgbColor ?: context.getColor(DEFAULT_CHIP_COLOR_ID)
    }

    companion object {
        const val DEFAULT_CHIP_COLOR_ID = R.color.colorPrimaryDark
    }
}