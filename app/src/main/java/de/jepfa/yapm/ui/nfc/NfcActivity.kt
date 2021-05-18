package de.jepfa.yapm.ui.nfc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.NfcUtil
import kotlin.random.Random


/**
 * Inspired by https://proandroiddev.com/working-with-nfc-tags-on-android-c1e5af47a3db
 */
class NfcActivity : SecureActivity() {

    private var adapter: NfcAdapter? = null
    private var tag: WritableTag? = null
    private lateinit var nfcStatusTextView: TextView

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        checkSession = !intent.getBooleanExtra(QrCodeActivity.EXTRA_NO_SESSION_CHECK, false)

        super.onCreate(savedInstanceState)

        if (checkSession && Session.isDenied()) {
            return
        }

        setContentView(R.layout.activity_nfc)

        val nfcExplanationTextView: TextView = findViewById(R.id.nfc_explanation)
        nfcExplanationTextView.text = "Hold the tag on your device to read from it"
        nfcStatusTextView = findViewById(R.id.read_nfc_status)
        val nfcWriteTagButton: Button = findViewById(R.id.button_write_nfc_tag)
        nfcWriteTagButton.setOnClickListener{
            if (tag == null) {
                Toast.makeText(this, "Tap a NFC tag on your device first", Toast.LENGTH_LONG).show()
            }
            tag?.let {
                try {
                    val message = NfcUtil.createNdefMessage(this, "test ${Random.nextInt()}")
                    it.writeData(message)
                } catch (e: Exception) {
                    Log.e("NFC", "Cannot write tag", e)
                    Toast.makeText(this, "Cannot write NFC tag. Have you tapped it on your device?", Toast.LENGTH_LONG).show()
                }

                Toast.makeText(this, "NFC tag successfully written", Toast.LENGTH_LONG).show()
                nfcStatusTextView.text = "Hold tag on your device again"
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
            updateView()
        }
    }

    private fun updateView() {
        tag?.let {
            nfcStatusTextView.text =
                        "UUD=${it.tagId} \n" +
                        "size=${it.getSize()} \n" +
                        "maxSize=${it.getMaxSize()} \n" +
                        "data: ${it.data}"
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

}