package de.jepfa.yapm.ui.errorhandling

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.Constants

/*
Taken from https://github.com/hardik-trivedi/ForceClose
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
                .setMessage("The bug will be copied to the clipboard and the bug report tracker website will be open. Please paste the report there and send it. Thanks.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { dialog, whichButton ->
                    ClipboardUtil.copy("bug report", errorText, this)
                    val browserIntent = Intent(Intent.ACTION_VIEW, Constants.BUG_REPORT_SITE)
                    startActivity(browserIntent)

                    Toast.makeText(this, R.string.toast_bug_report_copied, Toast.LENGTH_LONG).show()
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