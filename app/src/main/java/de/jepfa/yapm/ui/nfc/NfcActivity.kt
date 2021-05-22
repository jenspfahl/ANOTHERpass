package de.jepfa.yapm.ui.nfc

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
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
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.NfcUtil
import de.jepfa.yapm.util.getEncryptedExtra


/**
 * Inspired by https://proandroiddev.com/working-with-nfc-tags-on-android-c1e5af47a3db
 */
class NfcActivity : SecureActivity() {

    private var adapter: NfcAdapter? = null
    private var tag: WritableTag? = null
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
            tag?.let {
                val text =
                    "UUD=${it.tagId} \n" +
                            "size=${it.getSize()} \n" +
                            "maxSize=${it.getMaxSize()} \n" +
                            "freeSize=${it.getFreeSize()} \n" +
                            "data: ${it.data}"

                AlertDialog.Builder(this)
                    .setTitle("NFC tag content")
                    .setMessage(text)
                    .show()
            }
            true
        }

        mode = intent.getStringExtra(EXTRA_MODE)
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
                if (tag == null) {
                    Toast.makeText(this, R.string.nfc_tapping_needed, Toast.LENGTH_LONG).show()
                }

                tag?.let { t ->
                    val size = t.getSize()
                    if (size != null && size > 0) {
                        AlertDialog.Builder(this)
                            .setTitle(R.string.title_write_nfc_tag)
                            .setMessage(R.string.message_write_nfc_tag)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                                writeTag(t)
                                true
                            }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                    }
                    else {
                        writeTag(t)
                    }
                }
            }
        }

        initNfcAdapter()
    }

    override fun lock() {
        recreate()
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        tag?.close()
        disableNfcForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            tag = NfcUtil.getWritableTag(intent)
            val tData = tag?.data
            if (mode == EXTRA_MODE_RO && tData != null) {
                intent.putExtra(EXTRA_SCANNED_NDC_TAG_DATA, tData)
                setResult(ACTION_READ_NFC_TAG, intent)
                finish()
            }
            else {
                updateView()
            }
        }
    }

    private fun updateView() {
        tag?.let {
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

    private fun initNfcAdapter() {
        val nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        adapter = nfcManager.defaultAdapter
        if (adapter == null) {
            Toast.makeText(this, "Enable NFC to use this feature", Toast.LENGTH_LONG).show()
        }
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            adapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            Log.e("NFC", "Error enabling NFC foreground dispatch", ex)
        }
    }

    private fun disableNfcForegroundDispatch() {
        try {
            adapter?.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e("NFC", "Error disabling NFC foreground dispatch", ex)
        }
    }

    private fun writeTag(t: WritableTag) {
        encData?.let { eD ->
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val data = SecretService.decryptPassword(tempKey, eD)
            val maxSize = t.getMaxSize()
            if (maxSize != null && maxSize < data.length) {
                Toast.makeText(this, R.string.nfc_not_enough_space, Toast.LENGTH_LONG).show()
                return
            }
            try {
                val message = NfcUtil.createNdefMessage(this, data.toByteArray())
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
        const val EXTRA_DATA= "data"
        const val EXTRA_NO_SESSION_CHECK = "noSessionCheck"
        const val EXTRA_SCANNED_NDC_TAG_DATA = "scannedData"

        const val ACTION_READ_NFC_TAG = 1
    }

}