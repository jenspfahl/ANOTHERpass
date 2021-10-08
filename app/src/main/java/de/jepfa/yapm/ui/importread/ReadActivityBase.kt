package de.jepfa.yapm.ui.importread

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.export.ExportContainer
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.JsonService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.MasterPasswordService.generateEncMasterPasswdSK
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.nfc.NfcBaseActivity
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.QRCodeUtil


abstract class ReadActivityBase : NfcBaseActivity() {

    lateinit var scannedTextView : TextView

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

        scannedTextView  = findViewById(R.id.text_scan_data)

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
            Toast.makeText(this, getString(R.string.nfc_tag_detected), Toast.LENGTH_LONG).show()
            ndefTag?.let {
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
            return data?.getStringExtra(NfcActivity.EXTRA_SCANNED_NDC_TAG_DATA)
        }
        else {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null && result.contents != null) {
                return result.contents
            }
        }
        return null
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
                return name
            }
        }
        return null
    }
}