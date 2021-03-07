package de.jepfa.yapm.ui.qrcode

import android.app.AlertDialog
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
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity

class QrCodeActivity : SecureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Secret.isDenied()) {
            return
        }

        setContentView(R.layout.activity_qr_code)

        val headTextView: TextView = findViewById(R.id.textview_head)
        val subTextView: TextView = findViewById(R.id.textview_subtext)
        val qrCodeImageView: ImageView = findViewById(R.id.imageview_qrcode)

        val encHead = Encrypted.fromBase64String(intent.getStringExtra(EXTRA_HEADLINE))
        val encSub = Encrypted.fromBase64String(intent.getStringExtra(EXTRA_SUBTEXT))
        val encQRC = Encrypted.fromBase64String(intent.getStringExtra(EXTRA_QRCODE))

        val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
        val head = SecretService.decryptCommonString(tempKey, encHead)
        val sub = SecretService.decryptCommonString(tempKey, encSub)
        val qrc = SecretService.decryptPassword(tempKey, encQRC)

        headTextView.text = head
        subTextView.text = sub

        if (!qrc.isEmpty()) {
            val bitmap = generateQRCode(qrc.toString())
            qrCodeImageView.setImageBitmap(bitmap)
            qrCodeImageView.setOnLongClickListener {
                AlertDialog.Builder(this)
                        .setTitle(head)
                        .setMessage(qrc.toString())
                        .show()
                true
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {

            navigateUpTo(intent)
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

    companion object {
        const val EXTRA_HEADLINE = "head"
        const val EXTRA_SUBTEXT = "sub"
        const val EXTRA_QRCODE = "qrc"
    }
}