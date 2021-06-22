package de.jepfa.yapm.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.errorhandling.ExceptionHandler
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory
import de.jepfa.yapm.viewmodel.LabelViewModel
import de.jepfa.yapm.viewmodel.LabelViewModelFactory

open class BaseActivity : AppCompatActivity() {

    protected var enableBack = false

    private var viewProgressBar: ProgressBar? = null

    val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory(getApp())
    }

    val labelViewModel: LabelViewModel by viewModels {
        LabelViewModelFactory(getApp())
    }

    fun getProgressBar(): ProgressBar? {
        if (viewProgressBar == null) {
            viewProgressBar = findViewById(R.id.progressBar)
        }
        return viewProgressBar
    }

    fun getApp(): YapmApp {
        return application as YapmApp
    }

    fun hideKeyboard(view: View) {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this))
        supportActionBar?.setDisplayHomeAsUpEnabled(enableBack)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (enableBack && id == android.R.id.home) {
            val upIntent = Intent(this.intent)
            navigateUpTo(upIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}