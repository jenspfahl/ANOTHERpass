package de.jepfa.yapm.util

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.provider.PasteContentProvider
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.DropVaultUseCase
import de.jepfa.yapm.util.PreferenceUtil.PREF_WARN_BEFORE_COPY_TO_CB

object ClipboardUtil {

    fun copyEncPasswordWithCheck(encPassword: Encrypted, activity: SecureActivity) {
        val warn = PreferenceUtil.getAsBool(PREF_WARN_BEFORE_COPY_TO_CB, true, activity)
        if (warn) {
            AlertDialog.Builder(activity)
                .setTitle("Copy password")
                .setMessage("Copying passwords to clipboard may leak your password since any other app can listen to clipboard changes and read from it. Use 'Privacy/Test C&P password' to figure out if someone reads the clipboard beside you. You can disable this warning in 'Settings/Security'.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.copy) { dialog, whichButton ->
                    copyEncPassword(encPassword, activity)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        else {
            copyEncPassword(encPassword, activity)
        }
    }

    private fun copyEncPassword(encPassword: Encrypted, activity: SecureActivity) {
        val key = activity.masterSecretKey
        if (key != null) {
            val passwd = SecretService.decryptPassword(key, encPassword)
            copyPassword(passwd, activity)
            passwd.clear()
        }
    }

    private fun copyPassword(password: Password, context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Password", password.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.toast_copied_to_clipboard, Toast.LENGTH_LONG).show()
    }

    fun copyTestPasteConsumer(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(
            context.contentResolver,
            "Test paste consumer",
            PasteContentProvider.contentUri
        )
        PasteContentProvider.enablePushNotification = true
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.toast_test_copypaste_password, Toast.LENGTH_LONG).show()

    }

    fun clearClips(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", "")
        clipboard.setPrimaryClip(clip)
    }
}