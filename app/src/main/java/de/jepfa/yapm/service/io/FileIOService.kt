package de.jepfa.yapm.service.io

import android.app.IntentService
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.util.Log
import de.jepfa.yapm.BuildConfig.APPLICATION_ID
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService.createVaultFile
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.QRCodeUtil
import de.jepfa.yapm.util.getEncryptedExtra
import de.jepfa.yapm.util.toastText
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException


class FileIOService: IntentService("FileIOService") {

    private val handler = Handler()

    companion object {

        const val ACTION_SAVE_QRC = "action_saveQrc"
        const val ACTION_EXPORT_VAULT = "action_exportVault"
        const val ACTION_EXPORT_PLAIN_CREDENTIALS = "action_exportPlainCredentials"
        const val ACTION_EXPORT_AS_KDBX = "action_exportAsKDBX"

        const val PARAM_FILE_URI = "param_file_url"
        const val PARAM_KEEPASS_PASSWORD = "param_keepass_password"
        const val PARAM_QRC = "param_qrc"
        const val PARAM_QRC_HEADER = "param_qrc_header"
        const val PARAM_QRC_COLOR = "param_qrc_color"
        const val PARAM_INCLUDE_MK = "param_include_mk"
        const val PARAM_INCLUDE_PREFS = "param_include_prefs"

        internal fun bitmapToJpegFile(contentResolver: ContentResolver, bitmap: Bitmap, destUri: Uri): Boolean {
            return try {
                val fileOutStream = contentResolver.openOutputStream(destUri) ?: return false
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutStream)
            } catch (e: IOException) {
                DebugInfo.logException("TS", "cannot create file", e)
                false
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI)
        if (!isUriValid(uri)) {
            DebugInfo.logException("IO", "invalid export URI: $uri")
            return
        }

        when (intent.action) {
            ACTION_SAVE_QRC -> saveQrCodeAsImage(intent)
            ACTION_EXPORT_VAULT -> exportVault(intent)
            ACTION_EXPORT_PLAIN_CREDENTIALS -> exportPlainCredentials(intent)
            ACTION_EXPORT_AS_KDBX -> exportAsKdbx(intent)
        }
    }

    private fun isUriValid(uri: Uri?): Boolean {
        val appPath = getApp().applicationContext.dataDir.path
        val uriPath = uri?.path?:""
        return (!uriPath.startsWith(appPath))
    }

    private fun exportVault(intent: Intent) {
        val message: String
        if (FileUtil.isExternalStorageWritable()) {
            val includeMasterKey = intent.getBooleanExtra(PARAM_INCLUDE_MK, false)
            val includePreferences = intent.getBooleanExtra(PARAM_INCLUDE_PREFS, false)
            val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI)
            val success =
                if (uri != null) createVaultFile(applicationContext, getApp(), includeMasterKey, includePreferences, uri)
                else false

            if (success) {
                message = getString(R.string.backup_file_saved)
                PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_EXPORTED_AT, applicationContext)
                if (includeMasterKey) {
                    PreferenceService.putCurrentDate(PreferenceService.DATA_MK_EXPORTED_AT, applicationContext)
                }
            }
            else {
                message = getString(R.string.backup_failed)
            }
        }
        else {
            message = getString(R.string.backup_permission_missing)
        }
        if (message.isNotBlank()) {
            handler.post {
                toastText(baseContext, message)
            }
        }

    }

    private fun exportPlainCredentials(intent: Intent) {
        var message: String
        if (FileUtil.isExternalStorageWritable()) {
            val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI) ?: return

            val csvData = CsvService.createCsvExportContent(
                getApp().credentialRepository.getAllSync(), Session.getMasterKeySK()
            )

            var success = false
            if (csvData != null) {
                success = CsvService.writeCsvExportFile(this, uri, csvData)
            }

            message = if (success) {
                getString(R.string.csv_file_saved)
            } else {
                getString(R.string.something_went_wrong)
            }
        }
        else {
            message = getString(R.string.backup_permission_missing)
        }
        if (message.isNotBlank()) {
            handler.post {
                toastText(baseContext, message)
            }
        }

    }

    private fun exportAsKdbx(intent: Intent) {
        var message: String
        if (FileUtil.isExternalStorageWritable()) {
            val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI) ?: return

            val key = Session.getMasterKeySK() ?: return

            val encKeepassPassword = intent.getEncryptedExtra(PARAM_KEEPASS_PASSWORD) ?: return
            val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, this)
            val keepassPassword = SecretService.decryptPassword(tempKey, encKeepassPassword)


            val byteStream = ByteArrayOutputStream()
            var success = KdbxService.createKdbxExportContent(
                keepassPassword,
                getApp().credentialRepository.getAllSync(),
                key,
                byteStream,
                this
            )
            keepassPassword.clear()

            if (success) {
                success = FileUtil.writeFile(this, uri, byteStream)
            }

            message = if (success) {
                getString(R.string.kdbx_file_saved)
            } else {
                getString(R.string.something_went_wrong)
            }
        }
        else {
            message = getString(R.string.backup_permission_missing)
        }
        if (message.isNotBlank()) {
            handler.post {
                toastText(baseContext, message)
            }
        }

    }

    private fun saveQrCodeAsImage(intent: Intent) {
        var message: String
        if (FileUtil.isExternalStorageWritable()) {
            val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, applicationContext)

            val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI)
            val encQrc = intent.getEncryptedExtra(PARAM_QRC, Encrypted.empty())
            val encHeader = intent.getEncryptedExtra(PARAM_QRC_HEADER, Encrypted.empty())
            val qrcColor = intent.getIntExtra(PARAM_QRC_COLOR, Color.BLACK)
            val qrc = SecretService.decryptPassword(tempKey, encQrc)
            val header = SecretService.decryptCommonString(tempKey, encHeader)

            val bitmap = QRCodeUtil.generateQRCode(header, qrc.toRawFormattedPassword(), qrcColor, this)
            val success =
                if (uri != null) bitmapToJpegFile(contentResolver, bitmap, uri)
                else false
            if (success) {
                message = getString(R.string.qr_code_saved)
            }
            else {
                message = getString(R.string.qr_code_save_failed)
            }

        }
        else {
            message = getString(R.string.qr_code_permission_missing)
        }
        handler.post {
            toastText(baseContext, message)
        }
    }

    private fun getApp(): YapmApp {
        return application as YapmApp
    }
}