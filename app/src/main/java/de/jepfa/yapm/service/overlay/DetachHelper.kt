package de.jepfa.yapm.service.overlay

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity

object DetachHelper {
    val EXTRA_PASSWD = "password"

    fun detachPassword(activity: SecureActivity, credential: EncCredential) =
            if (!Settings.canDrawOverlays(activity)) {

                AlertDialog.Builder(activity)
                        .setTitle("Missing permission")
                        .setMessage("App cannot draw over other apps. Enable permission and try again.")
                        .setPositiveButton("Open permission",
                                DialogInterface.OnClickListener { _, i ->
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                                    intent.data = Uri.parse("package:" + activity.applicationInfo.packageName)
                                    activity.startActivity(intent)
                                })
                        .setNegativeButton("Close",
                                { dialogInterface, i -> dialogInterface.cancel() })
                        .show()
                false
            } else {

                val key = activity.masterSecretKey
                if (key != null) {
                    val password = SecretService.decryptPassword(key, credential.password)

                    val intent = Intent(activity, OverlayShowingService::class.java)
                    intent.putExtra(EXTRA_PASSWD, password.data)
                    activity.startService(intent)

                    activity.moveTaskToBack(true)
                    password.clear()
                }
                true
            }
}