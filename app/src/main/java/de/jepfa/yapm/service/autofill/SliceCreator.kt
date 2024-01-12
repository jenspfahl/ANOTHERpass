package de.jepfa.yapm.service.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.text.TextUtils
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi

object SliceCreator {

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.R)
    fun createSlice(
        imeSpec: InlinePresentationSpec,
        title: CharSequence?,
        subtitle: CharSequence?,
        startIcon: Icon?,
        endIcon: Icon?,
        contentDescription: CharSequence,
        attribution: PendingIntent
    ): Slice? {

        val imeStyle = imeSpec.style
        if (!UiVersions.getVersions(imeStyle).contains(UiVersions.INLINE_UI_VERSION_1)) {
            return null
        }

        val builder = InlineSuggestionUi.newContentBuilder(attribution).setContentDescription(contentDescription)
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title!!)
        }
        if (!TextUtils.isEmpty(subtitle)) {
            builder.setSubtitle(subtitle!!)
        }
        if (startIcon != null) {
            startIcon.setTintBlendMode(BlendMode.DST)
            builder.setStartIcon(startIcon)
        }
        if (endIcon != null) {
            endIcon.setTintBlendMode(BlendMode.DST)
            builder.setEndIcon(endIcon)
        }
        return builder.build().slice
    }
}