package de.jepfa.yapm.util

import android.app.Activity
import android.content.Context
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
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.qrcode.CaptureActivity
import de.jepfa.yapm.service.PreferenceService.PREF_COLORIZE_MP_QRCODES


object QRCodeUtil {

    private val MAX_HEADER_LENGTH = 20

    fun generateQRCode(header: String?, data: String, color: Int = Color.BLACK, context: Context): Bitmap {
        var cutHeader = header
        if (header != null && header.length > MAX_HEADER_LENGTH) {
            cutHeader = header.substring(0, MAX_HEADER_LENGTH) + "..."
        }
        val colorize = PreferenceService.getAsBool(PREF_COLORIZE_MP_QRCODES, context)
        val printColor = if (colorize) color else Color.BLACK

        var size =  if (data.length > 256) 550 else 500
        val width = size
        val height = size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val codeWriter = MultiFormatWriter()
        try {
            val bitMatrix = codeWriter.encode(data, BarcodeFormat.QR_CODE, width, height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) printColor else Color.WHITE)
                }
            }
        } catch (e: WriterException) {
            Log.d("QrCodeActivity", "generateQRCode: ${e.message}")
        }

        cutHeader?.let {
            val textPaint = TextPaint()
            textPaint.isAntiAlias = true
            textPaint.textSize = 32f
            textPaint.color = printColor

            val textWidth = textPaint.measureText(cutHeader).toInt()
            val canvas = Canvas(bitmap)
            canvas.drawText(cutHeader, (width - textWidth) / 2f, 25f, textPaint)
        }
        return bitmap
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

    fun scanQRCode(activity: Activity, prompt: String) {
        val integrator = IntentIntegrator(activity).apply {
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