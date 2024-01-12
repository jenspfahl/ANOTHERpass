package de.jepfa.yapm.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.integration.android.IntentIntegrator
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_COLORIZE_MP_QRCODES
import de.jepfa.yapm.service.PreferenceService.PREF_QRCODES_WITH_HEADER
import de.jepfa.yapm.ui.qrcode.CaptureActivity
import de.jepfa.yapm.ui.qrcode.CaptureActivity.Companion.DATA_FROM_IMAGE_FILE
import de.jepfa.yapm.ui.qrcode.CaptureActivity.Companion.RESULT_FROM_IMAGE_FILE
import de.jepfa.yapm.util.Constants.LOG_PREFIX


object QRCodeUtil {

    private val MAX_HEADER_LENGTH = 20

    fun generateQRCode(header: String?, data: CharSequence, color: Int = Color.BLACK, context: Context): Bitmap {
        var cutHeader = header
        if (header != null && header.length > MAX_HEADER_LENGTH) {
            cutHeader = header.substring(0, MAX_HEADER_LENGTH) + "..."
        }
        val colorize = PreferenceService.getAsBool(PREF_COLORIZE_MP_QRCODES, context)
        val showHeader = PreferenceService.getAsBool(PREF_QRCODES_WITH_HEADER, context)
        val printColor = if (colorize) color else Color.BLACK

        val isBig = data.length >= 120
        val size =  if (isBig) 550 else 500
        val width = size
        val height = size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val codeWriter = MultiFormatWriter()
        try {
            val bitMatrix = codeWriter.encode(
                data.toString(),
                BarcodeFormat.QR_CODE,
                width,
                height,
                mapOf(Pair(EncodeHintType.MARGIN, 3)))
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) printColor else Color.WHITE)
                }
            }
        } catch (e: WriterException) {
            Log.d(LOG_PREFIX + "QrCodeActivity", "generateQRCode: ${e.message}")
        }

        if (showHeader) {
            cutHeader?.let {
                val textPaint = TextPaint()
                textPaint.isAntiAlias = true
                textPaint.textSize = if (isBig) 28f else 32f
                textPaint.color = printColor

                val textWidth = textPaint.measureText(cutHeader).toInt()
                val canvas = Canvas(bitmap)
                canvas.drawText(
                    cutHeader,
                    (width - textWidth) / 2f,
                    if (isBig) 25f else 35f,
                    textPaint
                )
            }
        }
        return bitmap
    }

    fun scanQRCode(fragment: Fragment, prompt: String) {
        val timeout = PreferenceService.getAsInt(PreferenceService.PREF_LOGOUT_TIMEOUT, fragment.requireContext()) * 60000
        val integrator = IntentIntegrator.forSupportFragment(fragment).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(false)
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt(prompt)
            setBarcodeImageEnabled(false)
            setTimeout(timeout.toLong())
            setBeepEnabled(false)
        }
        integrator.initiateScan()
    }

    fun scanQRCode(activity: Activity, prompt: String) {
        val timeout = PreferenceService.getAsInt(PreferenceService.PREF_LOGOUT_TIMEOUT, activity) * 60000
        val integrator = IntentIntegrator(activity).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(false)
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt(prompt)
            setBarcodeImageEnabled(false)
            setTimeout(timeout.toLong())
            setBeepEnabled(false)
        }
        integrator.initiateScan()
    }

    fun extractContentFromIntent(requestCode: Int, resultCode: Int, data: Intent?): String? {

        val result = if (resultCode == RESULT_FROM_IMAGE_FILE && data != null) {
            data.getStringExtra(DATA_FROM_IMAGE_FILE)
        }
        else {
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.contents
        }
        if (result != null) {
            val withoutTrailingZeroChars = result.substringBefore(0.toChar())
            return withoutTrailingZeroChars
        }
        return null
    }
}