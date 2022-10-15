package de.jepfa.yapm.ui.qrcode

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.*
import com.journeyapps.barcodescanner.CaptureActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.toastText


class CaptureActivity: CaptureActivity() {

    companion object {
        val RESULT_FROM_IMAGE_FILE = 9823652
        val DATA_FROM_IMAGE_FILE = "qrDataFromFile"
    }

    private val scanFile = 1

    private var torchOn = false
    private lateinit var scannerView: DecoratedBarcodeView

    override fun initializeContent(): DecoratedBarcodeView? {
        setContentView(R.layout.qrcode_capture)

        val back: ImageView = findViewById(R.id.back)
        val toggleFlash: ImageView = findViewById(R.id.flash_light)
        val scanImageView: ImageView = findViewById(R.id.scan_image)
        scannerView = findViewById<View>(R.id.zxing_barcode_scanner) as DecoratedBarcodeView

        back.setOnClickListener {
            finish()
        }

        toggleFlash.setOnClickListener {
            torchOn = !torchOn
            if (torchOn) {
                scannerView.setTorchOn()
                toggleFlash.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_baseline_flashlight_off_24))
            } else {
                scannerView.setTorchOff()
                toggleFlash.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_baseline_flashlight_on_24))
            }
        }

        scanImageView.setOnClickListener {
            PermissionChecker.verifyReadStoragePermissions(this)
            if (PermissionChecker.hasReadStoragePermissions(this)) {
                val intent = Intent()
                    .setType("image/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_select_image_to_scan))
                startActivityForResult(chooserIntent, scanFile)
            }
        }

        return scannerView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == scanFile) {
            data?.let {
                val selectedFile = data.data

                if (selectedFile != null && FileUtil.isExternalStorageReadable()) {
                    try {
                        val content = FileUtil.readBinaryFile(this, selectedFile)
                        if (content != null) {
                            val bitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
                            val result = scanQRImage(bitmap)
                            if (result != null) {
                                val resultIntent = Intent()
                                resultIntent.putExtra(DATA_FROM_IMAGE_FILE, result)
                                setResult(RESULT_FROM_IMAGE_FILE, resultIntent)
                                finish()
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SCAN_QR", "cannot import file $selectedFile", e)
                    }
                }
            }
            toastText(this, R.string.image_to_scan_failed)
        }
    }

    private fun scanQRImage(bMap: Bitmap): String? {
        val intArray = IntArray(bMap.width * bMap.height)
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.width, 0, 0, bMap.width, bMap.height)
        val source = RGBLuminanceSource(bMap.width, bMap.height, intArray)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        try {
            return reader.decode(bitmap)?.text
        } catch (e: Exception) {
            Log.e("SCAN_QR", "Error scanning file", e)
        }
        return null
    }
}