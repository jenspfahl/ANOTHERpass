package de.jepfa.yapm.usecase.app

import android.annotation.SuppressLint
import android.graphics.Typeface
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


object ShowServerLogUseCase: BasicUseCase<BaseActivity>() {

    override fun execute(activity: BaseActivity): Boolean {
        val builder = AlertDialog.Builder(activity)
        val icon = activity.resources.getDrawable(R.drawable.outline_list_24, null)
        val logs = DebugInfo.getServerLog(activity)

        val container = ScrollView(builder.context)
        val view = HorizontalScrollView(builder.context)
        container.addView(view)
        view.setPadding(32)
        val textView = TextView(builder.context)
        textView.typeface = Typeface.MONOSPACE
        textView.setHorizontallyScrolling(true)
        textView.text = logs
        view.addView(textView)

        builder.setTitle("Server logs")
            .setView(container)
            .setIcon(icon)
            .setPositiveButton(R.string.close) { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.copy) { dialog, _ ->
                ClipboardUtil.copy("server logs", logs, activity)
                toastText(activity, "Copied to clipboard")
            }
            .show()



        return true
    }

}