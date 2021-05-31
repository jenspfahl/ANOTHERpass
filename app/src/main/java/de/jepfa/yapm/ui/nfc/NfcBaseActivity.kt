package de.jepfa.yapm.ui.nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import de.jepfa.yapm.service.nfc.NdefTag
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.service.nfc.NfcService


/**
 * Inspired by https://proandroiddev.com/working-with-nfc-tags-on-android-c1e5af47a3db
 */
abstract class NfcBaseActivity : SecureActivity() {

    protected var nfcAdapter: NfcAdapter? = null
    internal var ndefTag: NdefTag? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initNfcAdapter()
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        ndefTag?.close()
        disableNfcForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        readTagFromIntent(intent)
    }

    fun readTagFromIntent(intent: Intent?) {
        intent?.let {
            ndefTag = NfcService.getNdefTag(intent)
            handleTag()
        }
    }

    abstract fun handleTag()

    private fun initNfcAdapter() {
        nfcAdapter = NfcService.getNfcAdapter(this)
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            Log.e("NFC", "Error enabling NFC foreground dispatch", ex)
        }
    }

    private fun disableNfcForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e("NFC", "Error disabling NFC foreground dispatch", ex)
        }
    }

}