package de.jepfa.yapm.ui.nfc

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_MASTER_PASSWORD_TOKEN_NFC_TAG_ID
import de.jepfa.yapm.service.nfc.NdefTag
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.util.getEncryptedExtra
import de.jepfa.yapm.util.toastText


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
        val nfcProtectCopyingSwitch: SwitchCompat = findViewById(R.id.switch_protect_mpt_against_copying)
        val nfcWriteProtectSwitch: SwitchCompat = findViewById(R.id.switch_make_nfc_tag_write_protected)

        val protectCopyingMpt = intent.getBooleanExtra(EXTRA_PROTECT_COPYING_MPT, false)
        if (protectCopyingMpt) {
            nfcProtectCopyingSwitch.visibility = View.VISIBLE
            nfcProtectCopyingSwitch.isChecked = PreferenceService.isPresent(DATA_MASTER_PASSWORD_TOKEN_NFC_TAG_ID,this)
        }

        val nfcWriteTagButton: Button = findViewById(R.id.button_write_nfc_tag)

        nfcImageView.setOnLongClickListener {
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
            nfcWriteProtectSwitch.visibility = View.GONE
            title = getString(R.string.title_read_nfc_tag)
        }
        else {
            encData = intent.getEncryptedExtra(EXTRA_DATA)
            nfcExplanationTextView.text = getString(R.string.nfc_explanation_readwrite)
            title = getString(R.string.title_write_nfc_tag)
            nfcWriteTagButton.setOnClickListener {
                if (ndefTag == null) {
                    toastText(this, R.string.nfc_tapping_needed)
                    return@setOnClickListener
                }
                else if (ndefTag?.isWriteProtected() == true) {
                    toastText(this, R.string.cannot_write_protected_nfc_tag)
                    return@setOnClickListener
                }

                ndefTag?.let { t ->
                    val size = t.getSize()
                    if (size != null && size > 0) {
                        AlertDialog.Builder(this)
                            .setTitle(R.string.title_write_nfc_tag)
                            .setMessage(R.string.message_write_nfc_tag)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                                writeTag(t, withAppRecord, nfcWriteProtectSwitch.isChecked, nfcProtectCopyingSwitch.isChecked)
                            }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                    }
                    else {
                        writeTag(t, withAppRecord, nfcWriteProtectSwitch.isChecked, nfcProtectCopyingSwitch.isChecked)
                    }
                }
            }
        }

        if (nfcAdapter == null) {
            toastText(this, R.string.nfc_not_supported)
        }
        else if (nfcAdapter?.isEnabled == false) {
            toastText(this, R.string.nfc_not_enabled)
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
        ndefTag?.let { tag ->
            val size = tag.getSize()
            if (tag.isWriteProtected()) {
                nfcStatusTextView.text = getString(R.string.write_protected_nfc_detected)
            }
            else if (size == null) {
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

    private fun writeTag(t: NdefTag, withAppRecord: Boolean, setWriteProtection: Boolean, setCopyProtection: Boolean) {

        PreferenceService.delete(DATA_MASTER_PASSWORD_TOKEN_NFC_TAG_ID, this)
        if (setCopyProtection) {
            val tagId = ndefTag?.tagId
            if (tagId != null) {
                PreferenceService.putString(DATA_MASTER_PASSWORD_TOKEN_NFC_TAG_ID, tagId, this)
            } else {
                toastText(this, R.string.cannot_copy_protect_nfc_tag)
                return
            }
        }


        encData?.let { data ->
            if (setWriteProtection) {
                if (t.canSetWriteProtection()) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.title_write_nfc_tag)
                        .setMessage(R.string.message_make_nfc_tag_write_protected)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            checkAndWriteTag(data, t, withAppRecord, true)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                }
                else {
                    toastText(this, R.string.cannot_write_protect_nfc_tag)
                }
            }
            else {
                checkAndWriteTag(data, t, withAppRecord, false)
            }
        }
    }

    private fun checkAndWriteTag(encrypted: Encrypted, t: NdefTag, withAppRecord: Boolean, setWriteProtection: Boolean) {


        val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, this)
        val data = SecretService.decryptPassword(tempKey, encrypted)

        try {
            val message = NfcService.createNdefMessage(this, data.toByteArray(), withAppRecord)
            val maxSize = t.getMaxSize()
            if (maxSize != null && maxSize < message.byteArrayLength) {
                toastText(this, R.string.nfc_not_enough_space)
                return
            }
            t.writeData(message, setWriteProtection)

            toastText(this, R.string.nfc_successfully_written)
            nfcStatusTextView.text = getString(R.string.nfc_tap_again)
        } catch (e: Exception) {
            Log.e("NFC", "Cannot write tag", e)
            toastText(this, R.string.nfc_cannot_write)
        } finally {
            data.clear()
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
        const val EXTRA_PROTECT_COPYING_MPT = "protectCopyingMPT"

        const val ACTION_READ_NFC_TAG = 1
    }

}