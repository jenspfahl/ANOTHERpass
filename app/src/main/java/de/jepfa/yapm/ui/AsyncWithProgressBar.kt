package de.jepfa.yapm.ui

import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import de.jepfa.yapm.usecase.UseCaseOutput

class AsyncWithProgressBar(
    val activity: BaseActivity?,
    val backgroundHandler: () -> Boolean,
    private val postHandler: (backgroundResult: Boolean) -> Unit
) : AsyncTask<Void, Void, Boolean>()
{
    constructor(activity: BaseActivity?,
                backgroundHandler: () -> Boolean): this(activity, backgroundHandler, {})

    private lateinit var progressBar: ProgressBar
    init {
        val activityProgressBar = activity?.getProgressBar()
        if (activityProgressBar != null) {
            progressBar = activityProgressBar
            execute()
        }
        else {
            Log.w("ASYNC", "no progressbar, invoke in UI thread")
            val result = backgroundHandler.invoke()
            postHandler.invoke(result)
        }
    }

    override fun onPreExecute() {
        super.onPreExecute()
        progressBar.visibility = View.VISIBLE
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        return backgroundHandler()
    }

    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        progressBar.visibility = View.INVISIBLE
        activity?.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        postHandler.invoke(result)
    }
}