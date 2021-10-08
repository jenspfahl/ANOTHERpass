package de.jepfa.yapm.service.io

import android.app.IntentService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.JsonService.CREDENTIALS_TYPE
import de.jepfa.yapm.service.io.JsonService.GSON
import de.jepfa.yapm.service.io.JsonService.JSON_APP_VERSION_CODE
import de.jepfa.yapm.service.io.JsonService.JSON_APP_VERSION_NAME
import de.jepfa.yapm.service.io.JsonService.JSON_CIPHER_ALGORITHM
import de.jepfa.yapm.service.io.JsonService.JSON_CREATION_DATE
import de.jepfa.yapm.service.io.JsonService.JSON_CREDENTIALS
import de.jepfa.yapm.service.io.JsonService.JSON_CREDENTIALS_COUNT
import de.jepfa.yapm.service.io.JsonService.JSON_ENC_MK
import de.jepfa.yapm.service.io.JsonService.JSON_LABELS
import de.jepfa.yapm.service.io.JsonService.JSON_LABELS_COUNT
import de.jepfa.yapm.service.io.JsonService.JSON_VAULT_ID
import de.jepfa.yapm.service.io.JsonService.JSON_VAULT_VERSION
import de.jepfa.yapm.service.io.JsonService.LABELS_TYPE
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.Constants.SDF_DT_MEDIUM
import de.jepfa.yapm.util.Constants.UNKNOWN_VAULT_VERSION
import java.util.*


class FileIOService: IntentService("FileIOService") {

    private val handler = Handler()

    companion object {
        const val ACTION_SAVE_QRC = "action_saveQrc"
        const val ACTION_EXPORT_VAULT = "action_exportVault"

        const val PARAM_FILE_URI = "param_fileUrl"
        const val PARAM_QRC = "param_qrc"
        const val PARAM_QRC_HEADER = "param_qrc_header"
        const val PARAM_QRC_COLOR = "param_qrc_color"
        const val PARAM_INCLUDE_MK = "param_include_mk"

    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        when (intent.action) {
            ACTION_SAVE_QRC -> saveQrCodeAsImage(intent)
            ACTION_EXPORT_VAULT -> exportVault(intent)
        }
    }

    private fun exportVault(intent: Intent) {
        var message: String
        if (FileUtil.isExternalStorageWritable()) {
            val jsonData = exportToJson(intent.getBooleanExtra(PARAM_INCLUDE_MK, false))
            val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI)
            val success = writeExportFile(uri, jsonData)

            if (success) {
                message = getString(R.string.backup_file_saved)
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

    private fun writeExportFile(uri: Uri, jsonData: JsonObject): Boolean {
        var success = false
        try {
            success = FileUtil.writeFile(this, uri, jsonData.toString())
            val content: String? = FileUtil.readFile(this, uri)
            if (TextUtils.isEmpty(content)) {
                Log.e("BACKUP", "Empty file created: $uri")
                success = false
            }
        } catch (e: Exception) {
            Log.e("BACKUP", "Cannot write file $uri", e)
        }
        return success
    }

    private fun exportToJson(includeMasterkey: Boolean): JsonObject {
        val root = JsonObject()
        try {
            root.addProperty(JSON_APP_VERSION_CODE, DebugInfo.getVersionCode(applicationContext))
            root.addProperty(JSON_APP_VERSION_NAME, DebugInfo.getVersionName(applicationContext))
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("BACKUPALL", "cannot get version code", e)
        }

        root.addProperty(JSON_CREATION_DATE, SDF_DT_MEDIUM.format(Date()))
        val salt = SaltService.getSaltAsBase64String(this)
        salt.let {
            root.addProperty(JSON_VAULT_ID, it)
        }

        val vaultVersion = PreferenceService.getAsString(PreferenceService.DATA_VAULT_VERSION, this) ?: UNKNOWN_VAULT_VERSION.toString()
        root.addProperty(JSON_VAULT_VERSION, vaultVersion)

        val cipherAlgorithm = SecretService.getCipherAlgorithm(this)
        root.addProperty(JSON_CIPHER_ALGORITHM, cipherAlgorithm.name)

        if (includeMasterkey) {
            val encStoredMasterKey = PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, this)
            if (encStoredMasterKey != null) {

                val mkKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
                val encMasterKeyBase64 = SecretService.decryptEncrypted(mkKey, encStoredMasterKey).toBase64String()
                root.addProperty(JSON_ENC_MK, encMasterKeyBase64)
            }

        }

        val credentials = getApp().credentialRepository.getAllSync()

        root.add(JSON_CREDENTIALS, GSON.toJsonTree(credentials, CREDENTIALS_TYPE))
        root.addProperty(JSON_CREDENTIALS_COUNT, credentials.size)


        val labels = getApp().labelRepository.getAllSync()

        root.add(JSON_LABELS, GSON.toJsonTree(labels, LABELS_TYPE))
        root.addProperty(JSON_LABELS_COUNT, labels.size)

        return root
    }

    private fun saveQrCodeAsImage(intent: Intent) {
        var message: String
        if (FileUtil.isExternalStorageWritable()) {
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)

            val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI)
            val encQrc = intent.getEncryptedExtra(PARAM_QRC, Encrypted.empty())
            val encHeader = intent.getEncryptedExtra(PARAM_QRC_HEADER, Encrypted.empty())
            val qrcColor = intent.getIntExtra(PARAM_QRC_COLOR, Color.BLACK)
            val qrc = SecretService.decryptPassword(tempKey, encQrc)
            val header = SecretService.decryptCommonString(tempKey, encHeader)

            val fileOutStream = contentResolver.openOutputStream(uri)
            val bitmap = QRCodeUtil.generateQRCode(header, qrc.toString(), qrcColor, this)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutStream)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutStream)
            message = getString(R.string.qr_code_saved)

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