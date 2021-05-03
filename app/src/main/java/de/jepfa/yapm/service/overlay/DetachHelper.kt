package de.jepfa.yapm.service.overlay

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.putEncryptedExtra

object DetachHelper {
    val EXTRA_PASSWD = "password"
    val EXTRA_MULTILINE = "multiline"

    fun detachPassword(activity: SecureActivity, encPassword: Encrypted, multiLine: Boolean?) =
            if (!Settings.canDrawOverlays(activity)) {

                AlertDialog.Builder(activity)
                        .setTitle("Missing permission")
                        .setMessage("App cannot draw over other apps. Enable permission and try again.")
                        .setPositiveButton("Open permission"
                        ) { _, _ ->
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                            intent.data =
                                Uri.parse("package:" + activity.applicationInfo.packageName)
                            activity.startActivity(intent)
                        }
                    .setNegativeButton("Close",
                                { dialogInterface, _ -> dialogInterface.cancel() })
                        .show()
                false
            } else {

                val key = activity.masterSecretKey
                if (key != null) {
                    val password = SecretService.decryptPassword(key, encPassword)
                    val transSK = SecretService.getAndroidSecretKey(ALIAS_KEY_TRANSPORT)
                    val encPassword = SecretService.encryptPassword(transSK, password)

                    val intent = Intent(activity, OverlayShowingService::class.java)
                    intent.putEncryptedExtra(EXTRA_PASSWD, encPassword)
                    intent.putExtra(EXTRA_MULTILINE, multiLine)
                    activity.startService(intent)

                    activity.moveTaskToBack(true)
                    password.clear()
                }
                true
            }
}