package de.jepfa.yapm.usecase

import android.content.Intent
import android.graphics.Color
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity

object ExportPlainMasterPasswordUseCase {

    fun execute(encMasterPasswd: Encrypted, noSessionCheck: Boolean, activity: BaseActivity): Boolean {

        val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)

        val encHead = SecretService.encryptCommonString(tempKey, "Your real master password")
        val encSub = SecretService.encryptCommonString(tempKey, "Store this on a safe place since this is your plain master password.")
        val encQrc = encMasterPasswd

        val intent = Intent(activity, QrCodeActivity::class.java)
        intent.putExtra(QrCodeActivity.EXTRA_HEADLINE, encHead.toBase64String())
        intent.putExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub.toBase64String())
        intent.putExtra(QrCodeActivity.EXTRA_QRCODE, encQrc.toBase64String())
        intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.RED)
        intent.putExtra(QrCodeActivity.EXTRA_NO_SESSION_CHECK, noSessionCheck)
        activity.startActivity(intent)

        return true
    }
}