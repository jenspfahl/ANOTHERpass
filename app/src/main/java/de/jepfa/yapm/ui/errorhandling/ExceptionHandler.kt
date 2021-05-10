package de.jepfa.yapm.ui.errorhandling

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Process
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.DebugInfo.getDebugInfo
import de.jepfa.yapm.util.addLabelLine
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Character.LINE_SEPARATOR

/*
 Inspired from https://github.com/hardik-trivedi/ForceClose
 */
class ExceptionHandler(private val context: Activity) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        val errorReport = StringBuilder()
        errorReport.append("************ CAUSE OF ERROR ************\n\n")
        errorReport.append(stackTrace.toString())

        errorReport.append(getDebugInfo(context))

        val intent = Intent(context, ErrorActivity::class.java)
        intent.putExtra("error", errorReport.toString())
        context.startActivity(intent)
        Process.killProcess(Process.myPid())

        System.exit(10)
    }

}

