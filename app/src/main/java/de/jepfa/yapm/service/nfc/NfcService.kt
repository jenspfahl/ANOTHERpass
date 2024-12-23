package de.jepfa.yapm.service.nfc

import android.content.Context
import android.content.Intent
import android.nfc.*
import android.os.Parcelable
import android.util.Log
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.util.Constants.LOG_PREFIX

/**
 * Inspired by https://proandroiddev.com/working-with-nfc-tags-on-android-c1e5af47a3db
 */
object NfcService {

    fun getNfcAdapter(context: Context?): NfcAdapter? {
        if (context == null) return null
        val nfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        return nfcManager.defaultAdapter
    }

    fun isNfcAvailable(context: Context?): Boolean {
        val adapter = getNfcAdapter(context)
        return adapter != null
    }

    fun isNfcEnabled(context: Context?): Boolean {
        val adapter = getNfcAdapter(context)
        return adapter != null && adapter.isEnabled
    }

    fun getNdefTag(intent: Intent, activity: BaseActivity): NdefTag? {
        val tagFromIntent: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return null

        try {
            val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            val data = messages?.run { getData(this, activity) }
            return NdefTag(tagFromIntent, data)
        } catch (e: FormatException) {
            Log.e(LOG_PREFIX + "NFC", "Unsupported tag tapped", e)
            return null
        }
    }

    fun createNdefMessage(activity: BaseActivity, payload: ByteArray, withAppRecord: Boolean) : NdefMessage {
        val mimeType = (
                if (withAppRecord)
                    getMimeType(activity)
                else
                    "text/plain")
        val dataRecord = NdefRecord.createMime(mimeType, payload)

        return NdefMessage(dataRecord)

    }

    private fun getData(rawMessages: Array<Parcelable>, activity: BaseActivity): String {
        val sb = StringBuilder()
        for (i in rawMessages.indices) {
            val message = rawMessages[i] as NdefMessage
            message.records.forEach { record ->
                if (record.tnf == NdefRecord.TNF_MIME_MEDIA) {
                    // only consider own mime types
                    val mimeType = record.toMimeType()
                    if (mimeType == getMimeType(activity) || mimeType == getLegacyMimeType(activity)) {
                        val data = String(record.payload)
                        sb.append(data)
                    }
                }
            }
        }

        return sb.toString()
    }


    private fun getMimeType(activity: BaseActivity) =
        "application/${activity.getApp().packageName}"

    private fun getLegacyMimeType(activity: BaseActivity) =
        "appliation/${activity.getApp().packageName}"


    fun scanNfcTag(fragment: BaseFragment) {
        fragment.getBaseActivity()?.let {
            val intent = Intent(it, NfcActivity::class.java)
            intent.putExtra(NfcActivity.EXTRA_MODE, NfcActivity.EXTRA_MODE_RO)
            intent.putExtra(NfcActivity.EXTRA_NO_SESSION_CHECK, true)
            fragment.startActivityForResult(intent, NfcActivity.ACTION_READ_NFC_TAG)
        }
    }

    fun scanNfcTag(activity: BaseActivity) {

        val intent = Intent(activity, NfcActivity::class.java)
        intent.putExtra(NfcActivity.EXTRA_MODE, NfcActivity.EXTRA_MODE_RO)
        intent.putExtra(NfcActivity.EXTRA_NO_SESSION_CHECK, true)
        activity.startActivityForResult(intent, NfcActivity.ACTION_READ_NFC_TAG)
    }

}