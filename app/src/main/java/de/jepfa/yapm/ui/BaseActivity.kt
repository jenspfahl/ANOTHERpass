package de.jepfa.yapm.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.forEach
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.errorhandling.ExceptionHandler
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.toastText
import de.jepfa.yapm.viewmodel.*

open class BaseActivity : AppCompatActivity() {

    protected var enableBack = false

    private var viewProgressBar: ProgressBar? = null

    val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory(getApp())
    }

    val labelViewModel: LabelViewModel by viewModels {
        LabelViewModelFactory(getApp())
    }

    val usernameTemplateViewModel: UsernameTemplateViewModel by viewModels {
        UsernameTemplateViewModelFactory(getApp())
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionChecker.PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toastText(applicationContext, R.string.permission_granted_please_repeat)
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                toastText(applicationContext, R.string.permission_denied)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun inflateActionsMenu(menu: Menu, id: Int) {
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        menuInflater.inflate(id, menu)
        if (menu is MenuBuilder) {
            menu.forEach { item ->
                if (isActionItemInOverflowMenu(item)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        item.iconTintList = ColorStateList.valueOf(resources.getColor(R.color.Gray))
                    }
                    else {
                        item.icon = null
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun isActionItemInOverflowMenu(item: MenuItem): Boolean {
        return if (item is MenuItemImpl) {
            return if (item.requiresActionButton()) false
            else if (item.requestsActionButton()) true
            else !item.showsTextAsAction()
        }
        else {
            return false
        }
    }

}