package de.jepfa.yapm.ui.qrcode

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ShowCredentialActivity

class QrCodeActivity : SecureActivity() {

    private var idExtra: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        val headTextView: TextView = findViewById(R.id.textview_head)
        val subTextView: TextView = findViewById(R.id.textview_subtext)
        val qrCodeImageView: ImageView = findViewById(R.id.imageview_qrcode)


        idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)

        val key = masterSecretKey
        if (key != null) {
            val nameBase64 = intent.getStringExtra(EncCredential.EXTRA_CREDENTIAL_NAME)
            val additionalInfoBase64 = intent.getStringExtra(EncCredential.EXTRA_CREDENTIAL_ADDITIONAL_INFO)
            val passwordBase64 = intent.getStringExtra(EncCredential.EXTRA_CREDENTIAL_PASSWORD)

            val encName = Encrypted.fromBase64String(nameBase64)
            val encAdditionalInfo = Encrypted.fromBase64String(additionalInfoBase64)
            val encPassword = Encrypted.fromBase64String(passwordBase64)

            val name = SecretService.decryptCommonString(key, encName)
            val additionalInfo = SecretService.decryptCommonString(key, encAdditionalInfo)
            val password = SecretService.decryptPassword(key, encPassword)

            headTextView.text = name
            subTextView.text = additionalInfo

            if (!password.isEmpty()) {
                val bitmap = generateQRCode(password.toString())
                qrCodeImageView.setImageBitmap(bitmap)
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(this, ShowCredentialActivity::class.java) //TODO be moer generic
            upIntent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, idExtra)
            navigateUpTo(upIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun lock() {
        recreate()
    }

    private fun generateQRCode(text: String): Bitmap {
        val width = 500
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val codeWriter = MultiFormatWriter()
        try {
            val bitMatrix = codeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        } catch (e: WriterException) { Log.d("QrCodeActivity", "generateQRCode: ${e.message}") }
        return bitmap
    }
}