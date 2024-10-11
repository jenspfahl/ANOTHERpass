package de.jepfa.yapm.usecase.app

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.util.*
import java.util.*

object ShowDebugLogUseCase: BasicUseCase<BaseActivity>() {

    override fun execute(activity: BaseActivity): Boolean {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.resources.getDrawable(R.drawable.ic_baseline_memory_24)
        val logs = DebugInfo.getDebugLog(activity)
        val message = "Log length: ${logs.length} chars\nCapture time: ${Date()}\n\n$logs"

        val container = ScrollView(builder.context)
        val view = HorizontalScrollView(builder.context)
        container.addView(view)
        view.setPadding(32)
        val textView = TextView(builder.context)
        textView.typeface = Typeface.MONOSPACE
        textView.setHorizontallyScrolling(true)
        textView.text = message
        view.addView(textView)

        builder.setTitle(R.string.debug)
            .setView(container)
            .setIcon(icon)
            .setPositiveButton(R.string.close) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.copy) { dialog, _ ->
                ClipboardUtil.copy("logcat logs", logs, activity)
                toastText(activity, "Copied to clipboard")
            }
            .setNeutralButton("Clear logs") { dialog, _ ->
                DebugInfo.clearLogs()
                dialog.dismiss()
            }
            .show()



        return true
    }

}