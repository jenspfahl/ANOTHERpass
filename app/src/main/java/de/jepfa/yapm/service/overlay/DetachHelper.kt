package de.jepfa.yapm.service.overlay

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.putEncryptedExtra

object DetachHelper {
    val EXTRA_PASSWD = "password"
    val EXTRA_USER = "user"
    val EXTRA_PRESENTATION_MODE = "presentationMode"

    fun detachPassword(
        activity: SecureActivity,
        encUser: Encrypted,
        encPassword: Encrypted,
        obfuscationKey : Key?,
        formattingStyle: Password.FormattingStyle?) =
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
                    val user = SecretService.decryptCommonString(key, encUser)
                    val password = SecretService.decryptPassword(key, encPassword)
                    obfuscationKey?.let {
                        password.deobfuscate(it)
                    }
                    val transSK = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, activity)
                    val encPassword = SecretService.encryptPassword(transSK, password)

                    val intent = Intent(activity, OverlayShowingService::class.java)
                    intent.putEncryptedExtra(EXTRA_PASSWD, encPassword)

                    if (user.isNotBlank()) {
                        val encUser = SecretService.encryptCommonString(transSK, user)
                        intent.putEncryptedExtra(EXTRA_USER, encUser)
                    }

                    intent.putExtra(EXTRA_PRESENTATION_MODE, formattingStyle?.ordinal)
                    activity.startService(intent)

                    activity.moveTaskToBack(true)
                    password.clear()
                }
                true
            }
}