package de.jepfa.yapm.service.overlay

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.putEncryptedExtra

object DetachHelper {
    val EXTRA_PASSWD = "password"
    val EXTRA_PRESENTATION_MODE = "presentationMode"

    fun detachPassword(activity: SecureActivity, encPassword: Encrypted, obfuscationKey : Key?, formattingStyle: Password.FormattingStyle?) =
            if (!PermissionChecker.hasOverlayPermission(activity)) {

                AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.title_missing_overlay_permission))
                        .setMessage(activity.getString(R.string.message_missing_overlay_permission))
                        .setPositiveButton(activity.getString(R.string.open_overlay_permission)
                        ) { _, _ ->
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                            intent.data =
                                Uri.parse("package:" + activity.applicationInfo.packageName)
                            activity.startActivity(intent)
                        }
                    .setNegativeButton(R.string.close,
                                { dialogInterface, _ -> dialogInterface.cancel() })
                        .show()
                false
            } else {

                activity.masterSecretKey?.let{ key ->
                    val password = SecretService.decryptPassword(key, encPassword)
                    obfuscationKey?.let {
                        password.deobfuscate(it)
                    }
                    val transSK = SecretService.getAndroidSecretKey(ALIAS_KEY_TRANSPORT)
                    val encPassword = SecretService.encryptPassword(transSK, password)

                    val intent = Intent(activity, OverlayShowingService::class.java)
                    intent.putEncryptedExtra(EXTRA_PASSWD, encPassword)
                    intent.putExtra(EXTRA_PRESENTATION_MODE, formattingStyle?.ordinal)
                    activity.startService(intent)

                    activity.moveTaskToBack(true)
                    password.clear()
                }
                true
            }
}