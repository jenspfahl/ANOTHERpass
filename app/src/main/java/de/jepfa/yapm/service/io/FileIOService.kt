package de.jepfa.yapm.service.io

import android.R.attr.data
import android.app.IntentService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.QRCodeUtil
import java.lang.reflect.Type
import java.text.SimpleDateFormat
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
        const val PARAM_INCLUDE_MK = "param_include_mk"

        const val JSON_APP_VERSION_CODE = "appVersionCode"
        const val JSON_APP_VERSION_NAME = "appVersionName"
        const val JSON_CREATION_DATE = "creationDate"
        const val JSON_VAULT_ID = "vaultId"
        const val JSON_ENC_MK = "encMk"
        const val JSON_CREDENTIALS = "credentials"
        const val JSON_CREDENTIALS_COUNT = "credentialsCount"

        val CREDENTIALS_TYPE = object : TypeToken<List<EncCredential>>() {}.type
        val SDF_DT_MEDIUM =
                SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM)
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
        var message = ""
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
            val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            root.addProperty(JSON_APP_VERSION_CODE, pInfo.versionCode)
            root.addProperty(JSON_APP_VERSION_NAME, pInfo.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("BACKUPALL", "cannot get version code", e)
        }

        root.addProperty(JSON_CREATION_DATE, SDF_DT_MEDIUM.format(Date()))
        val salt = PreferenceUtil.get(PreferenceUtil.PREF_SALT, this)
        salt?.let {
            root.addProperty(JSON_VAULT_ID, it)
        }

        if (includeMasterkey) {
            val encStoredMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, this)
            if (encStoredMasterKey != null) {

                val mkKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
                val encMasterKeyBase64 = SecretService.decryptEncrypted(mkKey, encStoredMasterKey).toBase64String()
                root.addProperty(JSON_ENC_MK, encMasterKeyBase64)
            }

        }


        val credentials = getApp().repository.getAllSync()

        root.add(JSON_CREDENTIALS, GSON.toJsonTree(credentials, CREDENTIALS_TYPE))
        root.addProperty(JSON_CREDENTIALS_COUNT, credentials.size)

        return root
    }

    private fun saveQrCodeAsImage(intent: Intent) {
        var message = ""
        if (FileUtil.isExternalStorageWritable()) {
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)

            val uri = intent.getParcelableExtra<Uri>(PARAM_FILE_URI)
            val encQrcBase64 = intent.getStringExtra(PARAM_QRC)
            val encQrc = Encrypted.fromBase64String(encQrcBase64)
            val qrc = SecretService.decryptPassword(tempKey, encQrc)

            val fileOutStream = contentResolver.openOutputStream(uri)
            val bitmap = QRCodeUtil.generateQRCode(qrc.toString())
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