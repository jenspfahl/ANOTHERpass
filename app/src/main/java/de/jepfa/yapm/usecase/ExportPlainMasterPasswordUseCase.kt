package de.jepfa.yapm.usecase

import android.content.Intent
import android.graphics.Color
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity

object ExportPlainMasterPasswordUseCase: UseCase {

    override fun execute(activity: SecureActivity): Boolean {

        val key = activity.masterSecretKey
        val encMasterPasswd = Session.getEncMasterPasswd()
        if (key != null && encMasterPasswd != null) {
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)

            val encHead = SecretService.encryptCommonString(tempKey, "Your real master password")
            val encSub = SecretService.encryptCommonString(tempKey, "Store this on a safe place since this is your plain master password.")
            val encQrc = encMasterPasswd

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putExtra(QrCodeActivity.EXTRA_HEADLINE, encHead.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_QRCODE, encQrc.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.RED)
            activity.startActivity(intent)

            return true
        }
        else {
            return false
        }
    }
}