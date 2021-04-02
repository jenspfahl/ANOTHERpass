package de.jepfa.yapm.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import de.jepfa.yapm.ui.createvault.CreateVaultActivity

open class BaseActivity : AppCompatActivity() {

    private var viewProgressBar: ProgressBar? = null


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

}