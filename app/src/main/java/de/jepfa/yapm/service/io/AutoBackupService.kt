package de.jepfa.yapm.service.io

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_AUTO_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_MASTER_KEY_IN_AUTO_BACKUP_FILE
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_SETTINGS_IN_AUTO_BACKUP_FILE
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.toastText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AutoBackupService {

    private val backupFileCreationMutex = Mutex()

    fun registerAutoBackupUri(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver

        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)

        PreferenceService.putString(PreferenceService.DATA_VAULT_AUTO_EXPORT_URI, uri.toString(), context)
    }

    fun isAutoBackupConfigured(context: Context) = getAutoBackupFile(context) != null

    fun getAutoBackupFile(context: Context): DocumentFile? {
        val destUriAsString = PreferenceService.getAsString(PreferenceService.DATA_VAULT_AUTO_EXPORT_URI, context) ?: return null
        val destUri = Uri.parse(destUriAsString)

        return DocumentFile.fromSingleUri(context, destUri)
    }

    fun autoExportVault(context: Context) {
        if (isAutoBackupConfigured(context)) {
            autoExportVault(context) {}
        }
    }

    fun autoExportVault(context: Context, postHandler: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            backupFileCreationMutex.withLock {
                Log.d("IOM", "work")
                val success = autoExportVaultSync(context)
                Log.d("IOM", "done")

                CoroutineScope(Dispatchers.Main).launch {
                    if (success)
                        toastText(context, R.string.auto_export_done)
                    else
                        toastText(context, R.string.auto_export_failed)

                    postHandler(success)
                }
            }

        }
    }

    private fun autoExportVaultSync(context: Context): Boolean {

        val backupFile = getAutoBackupFile(context)

        if (backupFile!= null && backupFile.canWrite()) {
            val includeMasterKeyDefault = PreferenceService.getAsBool(
                PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE, true, context
            )
            val includeMasterKey = PreferenceService.getAsBool(
                PREF_INCLUDE_MASTER_KEY_IN_AUTO_BACKUP_FILE, includeMasterKeyDefault, context
            )

            val includePrefsDefault =
                PreferenceService.getAsBool(PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE, true, context)
            val includePrefs = PreferenceService.getAsBool(
                PREF_INCLUDE_SETTINGS_IN_AUTO_BACKUP_FILE, includePrefsDefault, context
            )

            val success = VaultExportService.createVaultFile(context, context.applicationContext as YapmApp, includeMasterKey, includePrefs, backupFile.uri)
            if (success) {
                PreferenceService.putCurrentDate(DATA_VAULT_AUTO_EXPORTED_AT, context)
                return true
            }
            else {
                Log.w("IO", "cannot create auto-backup file")
                return false
            }

        }
        else {
            Log.w("IO", "cannot auto-backup")
            return false
        }

    }
}