package de.jepfa.yapm.usecase

import android.content.Intent
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.PreferenceUtil

object ExportEncMasterKeyUseCase: SecureActivityUseCase {

    const val PREFIX = "EMK:"

    override fun execute(activity: SecureActivity): Boolean {
        val encStoredMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, activity)
        val key = activity.masterSecretKey
        if (key != null && encStoredMasterKey != null) {

            val mkKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val encMasterKeyBase64 = SecretService.decryptEncrypted(mkKey, encStoredMasterKey).toBase64String()

            val encHead = SecretService.encryptCommonString(tempKey, "Encrypted master key")
            val encSub = SecretService.encryptCommonString(tempKey, "Store this at a safe place. Future backups don't need to include that master key.")
            val encQrc = SecretService.encryptPassword(tempKey, Password(encMasterKeyBase64))

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putExtra(QrCodeActivity.EXTRA_HEADLINE, encHead.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_QRCODE, typeString(encQrc.toBase64String()))
            activity.startActivity(intent)

            return true
        }
        else {
            return false
        }
    }

    private fun typeString(string: String): String {
        return PREFIX + string
    }
}