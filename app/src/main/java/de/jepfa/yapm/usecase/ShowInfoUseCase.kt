package de.jepfa.yapm.usecase

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.util.DebugInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ShowInfoUseCase {

    fun execute(activity: BaseActivity): Boolean {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.applicationInfo.loadIcon(activity.packageManager)
        val message = activity.getString(R.string.app_name) + ", Version " + DebugInfo.getVersionName(
            activity
        ) +
                System.lineSeparator() + " \u00A9 Jens Pfahl 2021"
        builder.setTitle(R.string.title_about_the_app)
            .setMessage(message)
            .setIcon(icon)
            .show()
        return true
    }

}