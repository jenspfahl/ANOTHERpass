package de.jepfa.yapm.usecase

import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.util.Linkify
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import android.text.method.LinkMovementMethod

import android.widget.TextView
import de.jepfa.yapm.util.*


object ShowInfoUseCase {

    fun execute(activity: BaseActivity): Boolean {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.applicationInfo.loadIcon(activity.packageManager)

        val message = activity.getString(R.string.app_name) + ", Version " + DebugInfo.getVersionName(activity) +
                System.lineSeparator() + " \u00A9 Jens Pfahl 2021" +
                System.lineSeparator() + System.lineSeparator() +
                activity.getString(R.string.this_app_is_foss) +
                System.lineSeparator() + activity.getString(R.string.visit_foss_site, Constants.FOSS_SITE)

        val spanMessage = SpannableString(message)
        linkify(spanMessage)

        val dialog = builder.setTitle(R.string.title_about_the_app)
            .setMessage(spanMessage)
            .setIcon(icon)
            .create()

        dialog.show()
        linkifyDialogMessage(dialog)

        return true
    }

}