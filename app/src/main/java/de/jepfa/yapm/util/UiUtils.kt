package de.jepfa.yapm.util

import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.chip.Chip
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.label.Label
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
    return if (showIds) "$name [:${id?:"?"}]" else name
}

fun shortenBase64String(base64String: String, length: Int = 8): String {
    return base64String
        .toLowerCase(Locale.ROOT)
        .replace(Regex("[^0-9a-z]"),"")
        .take(length)
}

fun createAndAddLabelChip(
    label: Label,
    container: ViewGroup,
    thinner: Boolean,
    context: Context?,
    outlined: Boolean = false,
    placedOnAppBar: Boolean = false,
    ): Chip {
    val chip = createLabelChip(label, thinner, context, outlined, placedOnAppBar)
    container.addView(chip)
    return chip
}

fun createLabelChip(
    label: Label,
    thinner: Boolean,
    context: Context?,
    outlined: Boolean = false,
    placedOnAppBar: Boolean = false,
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
        if (outlined) {
            chip.chipStrokeWidth = 1.0F
            chip.chipStrokeColor = ColorStateList.valueOf(label.getColor(it))
            chip.setTextColor(label.getColor(it))
            label.iconResId?.let { iconResId ->
                chip.chipIcon =
                    AppCompatResources.getDrawable(context, iconResId)
                chip.chipIconTint = chip.chipStrokeColor
                chip.chipEndPadding = 0.0F
                if (thinner) {
                    chip.textStartPadding = 0.0F
                    chip.chipIconSize = 20.0F
                }
                else {
                    chip.textStartPadding = 4.0F
                    chip.iconStartPadding = 6.0F
                    chip.chipEndPadding = 4.0F
                }
                chip.isChipIconVisible = true
            }

            if (placedOnAppBar) {
                chip.chipBackgroundColor = ColorStateList.valueOf(context.getColor(R.color.BlackGray))

            }
            else {
                val isDarkMode = context.resources.getBoolean(R.bool.dark_mode)
                if (!isDarkMode) {
                    chip.chipBackgroundColor =
                        ColorStateList.valueOf(context.getColor(android.R.color.background_light))
                }

            }
        }
        else {
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

fun dateToNiceString(date: Date?, context: Context, withPreposition: Boolean = true): String {
    if (date != null) {
        val date = date.removeTime()
        val today = Date().removeTime()
        val yesterday = Date().yesterday().removeTime()
        val tomorrow = Date().tomorrow().removeTime()
        if (date == today) {
            return context.getString(R.string.date_today)
        }
        if (date == yesterday) {
            return context.getString(R.string.date_yesterday)
        }
        if (date == tomorrow) {
            return context.getString(R.string.date_tomorrow)
        }
        if (withPreposition) {
            return context.getString(
                R.string.date_on_date,
                date.toSimpleDateFormat()
            )
        }
        else {
            return date.toSimpleDateFormat()
        }
    }
    else {
        return "??"
    }
}

fun dateTimeToNiceString(dateTime: Date?, context: Context): String {
    if (dateTime != null) {
        val date = dateTime.removeTime()
        val today = Date().removeTime()
        val yesterday = Date().yesterday().removeTime()
        if (date == today) {
            return context.getString(R.string.date_today_at, dateTime.toSimpleTimeFormat())
        }
        if (date == yesterday) {
            return context.getString(R.string.date_yesterday_at, dateTime.toSimpleTimeFormat())
        }
        return context.getString(R.string.date_on_date_at,
            dateTime.toSimpleDateFormat(), dateTime.toSimpleTimeFormat())
    }
    else {
        return "??"
    }
}


fun formatAsDateTime(s: String?, context: Context): String {
    if (s != null) {
        val timestamp = s.toLongOrNull()
        if (timestamp != null) {
            return dateTimeToNiceString(Date(timestamp), context)
        }
        else {
            return s
        }
    }
    return "??"
}

fun emoji(unicode: Int): String {
    return String(Character.toChars(unicode))
}

fun ensureHttp(s: String): String {
    if (s.isBlank()) {
        return s
    }
    return if (s.startsWith(prefix = "http", ignoreCase = true)) {
        s
    }
    else {
        "https://" + s
    }
}

fun getAppNameFromPackage(packageName: String, context: Context): String? {
    val pm: PackageManager = context.packageManager
    val ai: ApplicationInfo? = try {
        pm.getApplicationInfo(packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    return ai?.loadLabel(pm)?.toString()
}

