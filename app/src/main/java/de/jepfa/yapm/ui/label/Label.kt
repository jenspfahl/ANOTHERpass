package de.jepfa.yapm.ui.label

import android.content.Context
import de.jepfa.yapm.R
import java.util.*

data class Label(
    val labelId: Int?,
    var name: String,
    var description: String,
    var colorRGB: Int?,
    var iconResId: Int? = null
) {

    init {
        name = name.uppercase(Locale.ROOT).trim()
    }

    constructor(name: String, colorRGB: Int?, iconResId: Int? = null) : this(null, name, "", colorRGB, iconResId)

    constructor(name: String, description: String) : this(
        null,
        name,
        description,
        null)

    fun getColor(context: Context): Int {
        return colorRGB ?: context.getColor(DEFAULT_CHIP_COLOR_ID)
    }

    companion object {
        const val DEFAULT_CHIP_COLOR_ID = R.color.colorPrimaryDark
    }
}