package de.jepfa.yapm.ui.errorhandling

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

/*
 Taken from https://github.com/hardik-trivedi/ForceClose
 */
class ExceptionHandler(private val myContext: Activity) : Thread.UncaughtExceptionHandler {

    private val LINE_SEPARATOR = System.lineSeparator()

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        val errorReport = StringBuilder()
        errorReport.append("************ CAUSE OF ERROR ************\n\n")
        errorReport.append(stackTrace.toString())
        errorReport.append("\n************ DEVICE INFORMATION ***********\n")
        errorReport.append("Brand: ")
        errorReport.append(Build.BRAND)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Device: ")
        errorReport.append(Build.DEVICE)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Model: ")
        errorReport.append(Build.MODEL)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Id: ")
        errorReport.append(Build.ID)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Product: ")
        errorReport.append(Build.PRODUCT)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("\n************ FIRMWARE ************\n")
        errorReport.append("SDK: ")
        errorReport.append(Build.VERSION.SDK)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Release: ")
        errorReport.append(Build.VERSION.RELEASE)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Incremental: ")
        errorReport.append(Build.VERSION.INCREMENTAL)
        errorReport.append(LINE_SEPARATOR)
        val intent = Intent(myContext, ErrorActivity::class.java)
        intent.putExtra("error", errorReport.toString())
        myContext.startActivity(intent)
        Process.killProcess(Process.myPid())

        System.exit(10)
    }
}