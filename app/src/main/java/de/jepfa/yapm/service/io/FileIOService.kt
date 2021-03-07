package de.jepfa.yapm.service.io

import android.app.IntentService
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.QRCodeUtil
import java.io.FileOutputStream

class FileIOService: IntentService("FileIOService") {

    companion object {
        val ACTION_SAVE_QRC = "action_saveQrc"
        val PARAM_FILE_URI = "param_fileUrl"
        val PARAM_QRC = "param_qrc"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        when (intent.action) {
            ACTION_SAVE_QRC -> saveQrCodeAsImage(intent)
        }
    }

    private fun saveQrCodeAsImage(intent: Intent) {
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
            Toast.makeText(applicationContext, "QR code as image saved", Toast.LENGTH_LONG).show() // TODO doesnt work

        }
        else {
            Toast.makeText(applicationContext, "Permission to write to external storage is missing", Toast.LENGTH_LONG).show() //TODO
        }
    }
}