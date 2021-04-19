package de.jepfa.yapm.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.integration.android.IntentIntegrator
import de.jepfa.yapm.ui.qrcode.CaptureActivity


object QRCodeUtil {

    fun generateQRCode(header: String?, data: String, color: Int = Color.BLACK): Bitmap {
        val width = 500
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val codeWriter = MultiFormatWriter()
        try {
            val bitMatrix = codeWriter.encode(data, BarcodeFormat.QR_CODE, width, height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) color else Color.WHITE)
                }
            }
        } catch (e: WriterException) {
            Log.d("QrCodeActivity", "generateQRCode: ${e.message}")
        }

        header?.let {
            val textPaint = TextPaint()
            textPaint.isAntiAlias = true
            textPaint.textSize = 32f
            textPaint.color = color

            val textWidth = textPaint.measureText(header).toInt()
            val canvas = Canvas(bitmap)
            canvas.drawText(header, (width - textWidth) / 2f, 25f, textPaint)
        }
        return bitmap
    }

    private fun printHeader(it: String) {

    }

    fun scanQRCode(fragment: Fragment, prompt: String) {
        val integrator = IntentIntegrator.forSupportFragment(fragment).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(false)
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt(prompt)
            setBarcodeImageEnabled(false)
            setBeepEnabled(false)
        }
        integrator.initiateScan()
    }
}