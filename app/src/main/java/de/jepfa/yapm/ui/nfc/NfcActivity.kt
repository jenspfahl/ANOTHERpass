package de.jepfa.yapm.ui.nfc

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.nfc.NdefTag
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.util.getEncryptedExtra


/**
 * Inspired by https://proandroiddev.com/working-with-nfc-tags-on-android-c1e5af47a3db
 */
class NfcActivity : NfcBaseActivity() {

    private var encData: Encrypted? = null
    private var mode: String? = null
    private lateinit var nfcStatusTextView: TextView

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        checkSession = !intent.getBooleanExtra(EXTRA_NO_SESSION_CHECK, false)

        super.onCreate(savedInstanceState)

        if (checkSession && Session.isDenied()) {
            return
        }

        setContentView(R.layout.activity_nfc)

        nfcStatusTextView = findViewById(R.id.read_nfc_status)
        val nfcExplanationTextView: TextView = findViewById(R.id.nfc_explanation)
        val nfcImageView: ImageView = findViewById(R.id.imageview_nfc_icon)
        val nfcWriteTagButton: Button = findViewById(R.id.button_write_nfc_tag)

        nfcImageView.setOnClickListener {
            ndefTag?.let {tag ->
                val text =
                    "UUD=${tag.tagId} \n" +
                            "size=${tag.getSize()} \n" +
                            "maxSize=${tag.getMaxSize()} \n" +
                            "freeSize=${tag.getFreeSize()} \n" +
                            "data: ${tag.data}"

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.nfc_tag_content))
                    .setMessage(text)
                    .show()
            }
            true
        }

        mode = intent.getStringExtra(EXTRA_MODE)
        val withAppRecord = intent.getBooleanExtra(EXTRA_WITH_APP_RECORD, false)
        if (mode == EXTRA_MODE_RO) {
            nfcExplanationTextView.text = getString(R.string.nfc_explanation_readonly)
            nfcWriteTagButton.visibility = View.GONE
            title = getString(R.string.title_read_nfc_tag)
        }
        else {
            encData = intent.getEncryptedExtra(EXTRA_DATA)
            nfcExplanationTextView.text = getString(R.string.nfc_explanation_readwrite)
            title = getString(R.string.title_write_nfc_tag)
            nfcWriteTagButton.setOnClickListener {
                if (ndefTag == null) {
                    Toast.makeText(this, R.string.nfc_tapping_needed, Toast.LENGTH_LONG).show()
                }

                ndefTag?.let { t ->
                    val size = t.getSize()
                    if (size != null && size > 0) {
                        AlertDialog.Builder(this)
                            .setTitle(R.string.title_write_nfc_tag)
                            .setMessage(R.string.message_write_nfc_tag)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                                writeTag(t, withAppRecord)
                                true
                            }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                    }
                    else {
                        writeTag(t, withAppRecord)
                    }
                }
            }
        }

        if (nfcAdapter == null) {
            Toast.makeText(this, getString(R.string.nfc_not_supported), Toast.LENGTH_LONG).show()
        }
        else if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, getString(R.string.nfc_not_enabled), Toast.LENGTH_LONG).show()
        }
    }

    override fun lock() {
        recreate()
    }

    override fun handleTag() {
        val tData = ndefTag?.data
        if (mode == EXTRA_MODE_RO && tData != null) {
            nfcStatusTextView.text = getString(R.string.nfc_tag_detected)
            intent.putExtra(EXTRA_SCANNED_NDC_TAG_DATA, tData)
            setResult(ACTION_READ_NFC_TAG, intent)
            finish()
        }
        else {
            updateView()
        }
    }

    private fun updateView() {
        ndefTag?.let {
            val size = it.getSize()
            if (size == null) {
                nfcStatusTextView.text = getString(R.string.nfc_tag_detected)
            }
            else if (size > 0) {
                nfcStatusTextView.text = getString(R.string.nfc_tag_not_empty_detected)
            }
            else {
                nfcStatusTextView.text = getString(R.string.nfc_tag_empty_detected)
            }
        }
    }

    private fun writeTag(t: NdefTag, withAppRecord: Boolean) {
        encData?.let { eD ->
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val data = SecretService.decryptPassword(tempKey, eD)

            try {
                val message = NfcService.createNdefMessage(this, data.toByteArray(), withAppRecord)
                val maxSize = t.getMaxSize()
                if (maxSize != null && maxSize < message.byteArrayLength) {
                    Toast.makeText(this, R.string.nfc_not_enough_space, Toast.LENGTH_LONG).show()
                    return
                }
                t.writeData(message)
                Toast.makeText(this, R.string.nfc_successfully_written, Toast.LENGTH_LONG).show()
                nfcStatusTextView.text = getString(R.string.nfc_tap_again)
            } catch (e: Exception) {
                Log.e("NFC", "Cannot write tag", e)
                Toast.makeText(this, R.string.nfc_cannot_write, Toast.LENGTH_LONG).show()
            } finally {
                data.clear()
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_MODE_RO = "readonly"
        const val EXTRA_MODE_RW = "readwrite"
        const val EXTRA_WITH_APP_RECORD= "withAppRecord"
        const val EXTRA_DATA= "data"
        const val EXTRA_NO_SESSION_CHECK = "noSessionCheck"
        const val EXTRA_SCANNED_NDC_TAG_DATA = "scannedData"

        const val ACTION_READ_NFC_TAG = 1
    }

}