package de.jepfa.yapm.ui.errorhandling

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.toastText
import java.net.URLEncoder

/*
  Inspired from https://github.com/hardik-trivedi/ForceClose
*/
class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this))

        setContentView(R.layout.activity_error)
        var error = findViewById<TextView>(R.id.bug_report)
        val errorText = intent.getStringExtra("error")
        error.text = errorText

        val buttonReportBug = findViewById<Button>(R.id.button_report_bug)
        buttonReportBug.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.button_report_bug)
                .setMessage("The bug will be sent to a bug report tracker website (Github). Wou need a Github account for doing this. Additionally the bug is copied to the clipboard. Thanks for helping.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { dialog, whichButton ->
                    ClipboardUtil.copy("bug report", errorText, this)
                    val errorTextUrlSafe = URLEncoder.encode(errorText, "UTF-8")
                    val bugReportUrl = Constants.BUG_REPORT_SITE.format("Something went wrong :-(", errorTextUrlSafe)
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bugReportUrl))
                    startActivity(browserIntent)

                    toastText(this, R.string.toast_bug_report_copied)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()


        }

        val buttonRestart = findViewById<Button>(R.id.button_restart)
        buttonRestart.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}