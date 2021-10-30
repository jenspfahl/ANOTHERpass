package de.jepfa.yapm.usecase.app

import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.SpannableString
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.linkify
import de.jepfa.yapm.util.linkifyDialogMessage

object ShowInfoUseCase: BasicUseCase<BaseActivity>() {

    override fun execute(activity: BaseActivity): Boolean {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.applicationInfo.loadIcon(activity.packageManager)

        val message = activity.getString(R.string.app_name) + ", Version " + DebugInfo.getVersionName(
            activity
        ) +
                System.lineSeparator() + " \u00A9 Jens Pfahl 2021" +
                System.lineSeparator() + System.lineSeparator() +
                activity.getString(R.string.this_app_is_foss) +
                System.lineSeparator() + activity.getString(
            R.string.visit_foss_site,
            Constants.FOSS_SITE
        )

        val spanMessage = SpannableString(message)
        linkify(spanMessage)

        val dialog = builder.setTitle(R.string.title_about_the_app)
            .setMessage(spanMessage)
            .setIcon(icon)
            .setNegativeButton(R.string.close) { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton(R.string.licences) { dialog, _ ->
                dialog.dismiss()
                OssLicensesMenuActivity.setActivityTitle(activity.getString(R.string.licences_title))
                activity.startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
            }
            .create()

        dialog.show()
        linkifyDialogMessage(dialog)

        return true
    }

}