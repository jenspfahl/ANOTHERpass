package de.jepfa.yapm.ui

import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AsyncWithProgressBar(
    val activity: BaseActivity?,
    val backgroundHandler: suspend () -> Boolean,
    private val postHandler: (backgroundResult: Boolean) -> Unit
)
{
    constructor(activity: BaseActivity?,
                backgroundHandler: () -> Boolean): this(activity, backgroundHandler, {})

    private lateinit var progressBar: ProgressBar
    init {
        val activityProgressBar = activity?.getProgressBar()
        if (activityProgressBar != null) {
            progressBar = activityProgressBar
            showProgressBar()
            CoroutineScope(Dispatchers.IO).launch {
                val result = backgroundHandler()
                CoroutineScope(Dispatchers.Main).launch {
                    postHandler(result)
                    hideProgressBar()
                }
            }
        }
        else {
            Log.w("ASYNC", "no progressbar, invoke in UI thread")
            val result = runBlocking {
                return@runBlocking backgroundHandler()
            }
            postHandler.invoke(result)
        }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }


    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
        activity?.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}