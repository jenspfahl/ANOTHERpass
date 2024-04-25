package de.jepfa.yapm.usecase.secret

import android.content.Intent
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.util.putEncryptedExtra

object ExportEncMasterKeyUseCase: BasicUseCase<SecureActivity>() {

    override fun execute(activity: SecureActivity): Boolean {
        val encStoredMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
        val key = activity.masterSecretKey
        if (key != null && encStoredMasterKey != null) {

            PreferenceService.putCurrentDate(PreferenceService.DATA_MK_EXPORTED_AT, activity)

            val mkKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MK, activity)
            val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, activity)
            val encMasterKey = SecretService.decryptEncrypted(mkKey, encStoredMasterKey)

            val encHead = SecretService.encryptCommonString(
                tempKey,
                activity.getString(R.string.head_export_emk)
            )
            val encSub = SecretService.encryptCommonString(
                tempKey,
                activity.getString(R.string.sub_export_emk)
            )
            val encQrcHeader = SecretService.encryptCommonString(tempKey, encMasterKey.type?.toString()?: "")
            val encQrc = SecretService.encryptEncrypted(tempKey, encMasterKey)

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, encHead)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, encQrcHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, encQrc)

            // will be bypassed to NfcActivity
            intent.putExtra(NfcActivity.EXTRA_WITH_APP_RECORD, true)

            activity.startActivity(intent)

            return true
        }
        else {
            return false
        }
    }

}