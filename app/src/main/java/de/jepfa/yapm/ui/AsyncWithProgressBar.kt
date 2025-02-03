package de.jepfa.yapm.ui

import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import de.jepfa.yapm.util.Constants.LOG_PREFIX
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
            activity?.showProgressBar(progressBar)
            CoroutineScope(Dispatchers.IO).launch {
                val result = backgroundHandler()
                CoroutineScope(Dispatchers.Main).launch {
                    postHandler(result)
                    activity?.hideProgressBar(progressBar)
                }
            }
        }
        else {
            Log.w(LOG_PREFIX + "ASYNC", "no progressbar, invoke in UI thread")
            val result = runBlocking {
                return@runBlocking backgroundHandler()
            }
            postHandler.invoke(result)
        }
    }


}