package de.jepfa.yapm.ui.errorhandling

import android.app.Activity
import android.content.Intent
import android.os.Process
import android.util.Log
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.io.PrintWriter
import java.io.StringWriter

/*
 Inspired from https://github.com/hardik-trivedi/ForceClose
 */
class ExceptionHandler(private val context: Activity) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        Log.d(LOG_PREFIX + "EXH", "caught exception", exception)
        val errorReport = StringBuilder()
        errorReport.append("************ CAUSE OF ERROR ************\n\n")
        errorReport.append(stackTrace.toString())

        val intent = Intent(context, ErrorActivity::class.java)
        intent.putExtra(ErrorActivity.EXTRA_EXCEPTION, errorReport.toString())
        intent.putExtra(ErrorActivity.EXTRA_FROM_ERROR_CATCHER, true)
        context.startActivity(intent)

        Process.killProcess(Process.myPid())
        System.exit(10)
    }

}

