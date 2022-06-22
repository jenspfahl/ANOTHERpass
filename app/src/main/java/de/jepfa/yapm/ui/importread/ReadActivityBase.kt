package de.jepfa.yapm.ui.importread

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.nfc.NfcBaseActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.QRCodeUtil
import de.jepfa.yapm.util.enrichId
import de.jepfa.yapm.util.toastText


abstract class ReadActivityBase : NfcBaseActivity() {

    protected var isFromQRScan = false

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }

        setContentView(getLayoutId())

        val scanQrCodeImageView = findViewById<ImageView>(R.id.imageview_scan_qrcode)
        scanQrCodeImageView.setOnClickListener {
            QRCodeUtil.scanQRCode(this, getString(R.string.scanning_qrcode))
        }

        val scanNfcImageView: ImageView = findViewById(R.id.imageview_scan_nfc)
        if (!NfcService.isNfcAvailable(this)) {
            scanNfcImageView.visibility = View.GONE
        }
        scanNfcImageView.setOnClickListener {
            NfcService.scanNfcTag(this)
        }

    }

    override fun handleTag() {
        Log.i("VERIFY", "tag detected " + ndefTag?.tagId)
        if (ndefTag != null) {
            toastText(this, R.string.nfc_tag_detected)
            ndefTag?.let {
                isFromQRScan = false
                handleScannedData(it.data ?: "")
            }
        }
    }

    override fun lock() {
        recreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val scanned = getScannedFromIntent(requestCode, resultCode, data)
        if (scanned != null) {
            handleScannedData(scanned)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getScannedFromIntent(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode == NfcActivity.ACTION_READ_NFC_TAG) {
            isFromQRScan = false
            return data?.getStringExtra(NfcActivity.EXTRA_SCANNED_NDC_TAG_DATA)
        }
        else {
            isFromQRScan = true
            return QRCodeUtil.extractContentFromIntent(requestCode, resultCode, data)
        }
    }

    abstract fun getLayoutId(): Int

    abstract fun handleScannedData(scanned: String)


    fun getNameFromECR(ecr: EncExportableCredential): String? {
        val encName = ecr.n
        val encPasswd = ecr.p
        masterSecretKey?.let{key ->
            val name = SecretService.decryptCommonString(key, encName)
            val passwd = SecretService.decryptPassword(key, encPasswd)
            val isKnown = passwd.isValid()
            passwd.clear()
            if (isKnown) {
                val enrichedName = enrichId(this, name, ecr.i)
                return enrichedName
            }
        }
        return null
    }
}