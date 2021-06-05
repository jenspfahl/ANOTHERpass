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
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.Constants.SDF_DT_MEDIUM
import java.lang.reflect.Type
import java.util.*


class FileIOService: IntentService("FileIOService") {

    class EncryptedSerializer : JsonSerializer<Encrypted> {
        override fun serialize(src: Encrypted?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            src?.let {
                return JsonPrimitive(it.toBase64String())
            }

            return JsonPrimitive("")
        }
    }

    private val handler = Handler()

    companion object {
        const val ACTION_SAVE_QRC = "action_saveQrc"
        const val ACTION_EXPORT_VAULT = "action_exportVault"

        const val PARAM_FILE_URI = "param_fileUrl"
        const val PARAM_QRC = "param_qrc"
        const val PARAM_QRC_HEADER = "param_qrc_header"
        const val PARAM_QRC_COLOR = "param_qrc_color"
        const val PARAM_INCLUDE_MK = "param_include_mk"

        const val JSON_APP_VERSION_CODE = "appVersionCode"
        const val JSON_APP_VERSION_NAME = "appVersionName"
        const val JSON_CREATION_DATE = "creationDate"
        const val JSON_VAULT_ID = "vaultId"
        const val JSON_ENC_MK = "encMk"
        const val JSON_CREDENTIALS = "credentials"
        const val JSON_CREDENTIALS_COUNT = "credentialsCount"
        const val JSON_LABELS = "labels"
        const val JSON_LABELS_COUNT = "labelsCount"

        val CREDENTIALS_TYPE = object : TypeToken<List<EncCredential>>() {}.type
        val LABELS_TYPE = object : TypeToken<List<EncLabel>>() {}.type

        val GSON = GsonBuilder()
                .registerTypeAdapter(Encrypted::class.java, EncryptedSerializer())
                .create()
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
                message = "Backup file saved"
            }
            else {
                message = "Error while creating the backup file"
            }
        }
        else {
            message = "Permission to write to external storage is missing"
        }
        if (message.isNotBlank()) {
            handler.post {
                Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
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
        salt?.let {
            root.addProperty(JSON_VAULT_ID, it)
        }

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
            message = "QR code as image saved"

        }
        else {
            message = "Permission to write to external storage is missing"
        }
        handler.post {
            Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun getApp(): YapmApp {
        return application as YapmApp
    }
}