package de.jepfa.yapm.usecase

import android.content.Intent
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.PreferenceUtil

object ExportPlainMasterPasswordUseCase: UseCase {

    override fun execute(activity: SecureActivity): Boolean {

        val key = activity.masterSecretKey
        val encMasterPasswd = Secret.getEncMasterPasswd()
        if (key != null && encMasterPasswd != null) {
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)

            val encHead = SecretService.encryptCommonString(tempKey, "Your real master password")
            val encSub = SecretService.encryptCommonString(tempKey, "Store this on a safe place since this is your plain master password.")
            val encQrc = encMasterPasswd

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putExtra(QrCodeActivity.EXTRA_HEADLINE, encHead.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_QRCODE, encQrc.toBase64String())
            activity.startActivity(intent)

            return true
        }
        else {
            return false
        }
    }
}