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
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
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

    val webExtensionViewModel: WebExtensionViewModel by viewModels {
        WebExtensionViewModelFactory(getApp())
    }

    fun getProgressBar(): ProgressBar? {
        if (viewProgressBar == null) {
            viewProgressBar = findViewById(R.id.progressBar)
        }
        return viewProgressBar
    }

    fun showProgressBar(progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }


    fun hideProgressBar(progressBar: ProgressBar) {
        progressBar.visibility = View.INVISIBLE
        window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun getApp(): YapmApp {
        return application as YapmApp
    }

    fun hideKeyboard(view: View) {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this))
        supportActionBar?.setDisplayHomeAsUpEnabled(enableBack)

    }

    fun correctInsets(rootView: View) {
        ViewGroupCompat.installCompatInsetsDispatch(rootView)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                view.setBackgroundColor(getColor(R.color.black))

                // Adjust padding to avoid overlap
                view.setPadding(0, statusBarInsets.top, 0, 0)
                insets
            }
        } else {
            // For Android 14 and below
            window.statusBarColor = getColor(R.color.black)
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = false
        }
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
    fun inflateActionsMenu(menu: Menu, id: Int, showGroupDivider: Boolean = false) {
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
            menu.isGroupDividerEnabled = showGroupDivider
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

    fun isActivityInForeground() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

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