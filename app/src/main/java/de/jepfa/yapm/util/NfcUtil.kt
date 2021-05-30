package de.jepfa.yapm.util

import android.content.Context
import android.content.Intent
import android.nfc.*
import android.os.Parcelable
import android.util.Log
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.nfc.WritableTag

/**
 * Inspired by https://proandroiddev.com/working-with-nfc-tags-on-android-c1e5af47a3db
 */
object NfcUtil {

    fun getNfcAdapter(context: Context): NfcAdapter? {
        val nfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        return nfcManager.defaultAdapter
    }

    fun isNfcAvailable(context: Context): Boolean {
        val adapter = getNfcAdapter(context)
        return adapter != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = getNfcAdapter(context)
        return adapter != null && adapter.isEnabled
    }

    fun getWritableTag(intent: Intent): WritableTag? {
        val tagFromIntent: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        try {
            val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            val data = messages?.run { getData(this) }
            return WritableTag(tagFromIntent, data)
        } catch (e: FormatException) {
            Log.e("NFC", "Unsupported tag tapped", e)
            return null
        }
    }

    fun createNdefMessage(activity: BaseActivity, payload: ByteArray, withAppRecord: Boolean) : NdefMessage {
        val typeBytes = "appliation/${activity.getApp().packageName}".toByteArray()
        val dataRecord = NdefRecord(NdefRecord.TNF_MIME_MEDIA, typeBytes, null, payload)

        if (withAppRecord) {
            val appRecord = NdefRecord.createApplicationRecord(activity.getApp().packageName)
            return NdefMessage(arrayOf(appRecord, dataRecord))
        }
        else {
            return NdefMessage(dataRecord)
        }

    }

    private fun getData(rawMessages: Array<Parcelable>): String {
        val sb = StringBuilder()
        for (i in rawMessages.indices) {
            val message = rawMessages[i] as NdefMessage
            message.records.forEach { record ->
                if (record.tnf == NdefRecord.TNF_MIME_MEDIA) {
                    val data = String(record.payload)
                    sb.append(data)
                }
            }
        }

        return sb.toString()
    }

    fun scanNfcTag(fragment: BaseFragment) {
        val intent = Intent(fragment.getBaseActivity(), NfcActivity::class.java)
        intent.putExtra(NfcActivity.EXTRA_MODE, NfcActivity.EXTRA_MODE_RO)
        intent.putExtra(NfcActivity.EXTRA_NO_SESSION_CHECK, true)
        fragment.startActivityForResult(intent, NfcActivity.ACTION_READ_NFC_TAG)
    }

}