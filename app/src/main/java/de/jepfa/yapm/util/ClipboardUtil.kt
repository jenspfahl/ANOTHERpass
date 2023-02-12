package de.jepfa.yapm.util

import androidx.appcompat.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.provider.PasteContentProvider
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_WARN_BEFORE_COPY_TO_CB
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity

object ClipboardUtil {

    fun copyEncPasswordWithCheck(encPassword: Encrypted, obfuscationKey : Key?, activity: SecureActivity) {
        val warn = PreferenceService.getAsBool(PREF_WARN_BEFORE_COPY_TO_CB, activity)
        if (warn) {
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.title_copy_password))
                .setMessage(activity.getString(R.string.message_copy_password))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.copy) { dialog, whichButton ->
                    copyEncPassword(encPassword, obfuscationKey, activity)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        else {
            copyEncPassword(encPassword, obfuscationKey, activity)
        }
    }

    private fun copyEncPassword(encPassword: Encrypted, obfuscationKey : Key?, activity: SecureActivity) {
        activity.masterSecretKey?.let{ key ->
            val passwd = SecretService.decryptPassword(key, encPassword)
            obfuscationKey?.let {
                passwd.deobfuscate(it)
            }
            copyPassword(passwd, activity)
            passwd.clear()
        }
    }

    fun copy(label: String, text: CharSequence, context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        
        clipboard.setPrimaryClip(clip)
    }

    private fun copyPassword(password: Password, context: Context) {
        copy("Password", password.toRawFormattedPassword(), context)
        toastText(context, R.string.toast_copied_to_clipboard)
    }

    fun copyTestPasteConsumer(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(
            context.contentResolver,
            "Test copy password",
            PasteContentProvider.contentUri
        )
        PasteContentProvider.enablePushNotification = true
        clipboard.setPrimaryClip(clip)
        toastText(context, R.string.toast_test_copypaste_password)

    }

    fun clearClips(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        }
        else {
            val clip = ClipData.newPlainText("", "")
            clipboard.setPrimaryClip(clip)
        }
    }
}