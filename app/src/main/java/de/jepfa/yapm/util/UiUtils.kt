package de.jepfa.yapm.util

import android.content.Context
import android.widget.Toast
import de.jepfa.yapm.service.PreferenceService


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
    val showIds = PreferenceService.getAsBool(PreferenceService.PREF_SHOW_IDS, context)
    return if (showIds) "$name [:${id?:"new"}]" else name
}
