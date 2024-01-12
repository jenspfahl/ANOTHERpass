package de.jepfa.yapm.ui.errorhandling

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toastText
import java.net.URLEncoder

/*
  Inspired from https://github.com/hardik-trivedi/ForceClose
*/
class ErrorActivity : AppCompatActivity() {


    private var fromErrorCatcher: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this))

        setContentView(R.layout.activity_error)
        var errorHeaderView = findViewById<TextView>(R.id.error_header)
        var errorTraceView = findViewById<TextView>(R.id.bug_report)
        var userDescription = findViewById<EditText>(R.id.error_user_description)
        fromErrorCatcher = intent.getBooleanExtra(EXTRA_FROM_ERROR_CATCHER, false)
        var errorText = intent.getCharSequenceExtra(EXTRA_EXCEPTION)?.toString() ?: ""

        val details = DebugInfo.getDebugInfo(this)
        errorText += details

        errorTraceView.text = errorText

        if (!fromErrorCatcher) {
            errorHeaderView.visibility = ViewGroup.GONE
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.report_a_bug)
        }

        val buttonReportBug = findViewById<Button>(R.id.button_report_bug)
        buttonReportBug.setOnClickListener {

            if (userDescription.text.isNotBlank()) {
                errorText = "${userDescription.text}\n\n$errorText"
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.button_report_bug)
                .setMessage(R.string.report_a_bug_desc)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { dialog, whichButton ->
                    ClipboardUtil.copy("bug report", errorText, this)
                    val errorTextUrlSafe = URLEncoder.encode(errorText.toString(), "UTF-8")
                    val anonymizedVaultId = SaltService.getAnonymizedVaultId(this)
                    val bugReportUrl = Constants.BUG_REPORT_SITE.format("Error report from user [#$anonymizedVaultId]", errorTextUrlSafe)
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bugReportUrl))
                    startActivity(browserIntent)

                    toastText(this, getString(R.string.toast_bug_report_copied))
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (!fromErrorCatcher && id == android.R.id.home) {
            val upIntent = Intent(this.intent)
            navigateUpTo(upIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_EXCEPTION = "de.jepfa.yapm.EXTRA_EXCEPTION"
        const val EXTRA_FROM_ERROR_CATCHER = "de.jepfa.yapm.EXTRA_FROM_ERROR_CATCHER"
    }
}