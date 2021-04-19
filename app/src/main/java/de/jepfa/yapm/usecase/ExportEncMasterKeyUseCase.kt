package de.jepfa.yapm.usecase

import android.content.Intent
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.putEncryptedExtra

object ExportEncMasterKeyUseCase: SecureActivityUseCase {

    override fun execute(activity: SecureActivity): Boolean {
        val encStoredMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity)
        val key = activity.masterSecretKey
        if (key != null && encStoredMasterKey != null) {

            val mkKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val encMasterKey = SecretService.decryptEncrypted(mkKey, encStoredMasterKey)

            val encHead = SecretService.encryptCommonString(tempKey, "The Encrypted Master Key")
            val encSub = SecretService.encryptCommonString(tempKey, "Store this at a safe place. Future backups don't need to include that master key.")
            val encQrcHeader = SecretService.encryptCommonString(tempKey, encMasterKey.type)
            val encQrc = SecretService.encryptEncrypted(tempKey, encMasterKey)

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, encHead)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, encQrcHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, encQrc)
            activity.startActivity(intent)

            return true
        }
        else {
            return false
        }
    }

}