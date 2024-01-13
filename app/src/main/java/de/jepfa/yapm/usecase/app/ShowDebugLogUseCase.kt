package de.jepfa.yapm.usecase.app

import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.SpannableString
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.intro.LicencesActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.util.*
import java.util.*

object ShowDebugLogUseCase: BasicUseCase<BaseActivity>() {

    override fun execute(activity: BaseActivity): Boolean {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.resources.getDrawable(R.mipmap.ic_logo)
        val logs = DebugInfo.getDebugLog(activity)
        val message = "Log length: ${logs.length} chars\nCapture time: ${Date()}\n\n$logs"
        builder.setTitle(R.string.debug)
            .setMessage(message)
            .setIcon(icon)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.copy) { dialog, _ ->
                ClipboardUtil.copy("logcat logs", logs, activity)
                toastText(activity, activity.getString(R.string.toast_bug_report_copied))
            }
            .setNeutralButton("Clear logs") { dialog, _ ->
                DebugInfo.clearLogs()
                dialog.dismiss()
            }
            .show()



        return true
    }

}