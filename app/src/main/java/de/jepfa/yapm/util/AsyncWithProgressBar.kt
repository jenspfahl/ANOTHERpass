package de.jepfa.yapm.util

import android.os.AsyncTask
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import de.jepfa.yapm.ui.BaseActivity

class AsyncWithProgressBar(val activity: BaseActivity, val progressBar: ProgressBar, val handler: () -> Boolean) : AsyncTask<Void, Void, Boolean>() {
    init {
        execute()
    }

    override fun onPreExecute() {
        super.onPreExecute()
        progressBar.setVisibility(View.VISIBLE)
        activity.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        return handler()
    }

    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        progressBar.setVisibility(View.INVISIBLE)
        activity.getWindow().clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}