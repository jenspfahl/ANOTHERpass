package de.jepfa.yapm.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import de.jepfa.yapm.R

// Inspired by - https://stackoverflow.com/a/79286757
// Posted by Shouheng Wang, modified by community. See post 'Timeline' for change history
// Retrieved 2026-02-13, License - CC BY-SA 4.0

object StatusAndNavigationBarUtils {
    private const val TAG_STATUS_BAR = "TAG_STATUS_BAR"

    fun correctInsetsAndStatusBar(activity: Activity, rootView: View,
                                  correctTop: Boolean = true, correctBottom: Boolean = true) {
        ViewGroupCompat.installCompatInsetsDispatch(rootView)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            if (v.layoutParams is ViewGroup.MarginLayoutParams) {
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    leftMargin = insets.left
                    topMargin = if (correctTop) insets.top else topMargin
                    bottomMargin = if (correctBottom) insets.bottom else bottomMargin
                    rightMargin = insets.right
                }
            }
            else {
                v.setPaddingRelative(
                    insets.left,
                    if (correctTop) insets.top else 0,
                    insets.right,
                    if (correctBottom) insets.bottom else 0
                )
            }

            WindowInsetsCompat.CONSUMED
        }

        val statusBarColor = activity.getColor(R.color.black)

        setStatusBarColor(activity, statusBarColor)
        activity.window.statusBarColor = statusBarColor
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightNavigationBars = false
    }


    fun setStatusBarColor(activity: Activity, @ColorInt color: Int): View? {
        transparentStatusBar(activity.window)
        return applyStatusBarColor(activity.window, color)
    }

    fun transparentStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        val vis = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = option or vis
        window.statusBarColor = Color.TRANSPARENT
    }

    private fun applyStatusBarColor(window: Window, color: Int): View? {
        val parent = window.decorView
        if (parent !is ViewGroup) return null
        var fakeStatusBarView = parent.findViewWithTag<View?>(TAG_STATUS_BAR)
        if (fakeStatusBarView != null) {
            if (fakeStatusBarView.isGone) {
                fakeStatusBarView.visibility = View.VISIBLE
            }
            fakeStatusBarView.setBackgroundColor(color)
        } else {
            fakeStatusBarView = createStatusBarView(window.context, color)
            parent.addView(fakeStatusBarView)
        }
        return fakeStatusBarView
    }

    private fun createStatusBarView(context: Context, color: Int): View {
        val statusBarView = View(context)
        statusBarView.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(context)
            )
        )
        statusBarView.setBackgroundColor(color)
        statusBarView.tag = TAG_STATUS_BAR
        return statusBarView
    }

    fun getStatusBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }
}


