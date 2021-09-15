package de.jepfa.yapm.usecase

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_VERSION
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_CREATED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_IMPORTED_AT
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.Constants.UNKNOWN_VAULT_VERSION
import de.jepfa.yapm.util.addFormattedLine
import java.util.*

object ShowVaultInfoUseCase {

    fun execute(activity: BaseActivity, credentialCount: Int, labelCount: Int) {

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.applicationInfo.loadIcon(activity.packageManager)

        val saltBase64 = SaltService.getSaltAsBase64String(activity)
        val vaultId = SaltService.saltToVaultId(saltBase64)
        val vaultVersion = PreferenceService.getAsString(
            DATA_VAULT_VERSION,
            activity) ?: UNKNOWN_VAULT_VERSION
        val vaultCreatedAt = PreferenceService.getAsString(
            DATA_VAULT_CREATED_AT,
            activity)
        val vaultImportedAt = PreferenceService.getAsString(
            DATA_VAULT_IMPORTED_AT,
            activity)
        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)

        val sb = StringBuilder()
        sb.addFormattedLine(activity.getString(R.string.vault_id), vaultId)
        sb.addFormattedLine(activity.getString(R.string.vault_version), vaultVersion)
        sb.addFormattedLine(activity.getString(R.string.cipher_name), activity.getString(cipherAlgorithm.uiLabel))
        sb.addFormattedLine(activity.getString(R.string.credential_count), credentialCount)
        sb.addFormattedLine(activity.getString(R.string.label_count), labelCount)
        if (vaultCreatedAt != null) {
            sb.addFormattedLine(activity.getString(R.string.vault_created_at), vaultCreatedAt)
        }
        if (vaultImportedAt != null) {
            sb.addFormattedLine(activity.getString(R.string.vault_imported_at), vaultImportedAt)
        }
        
        builder.setTitle(R.string.vault_info)
            .setMessage(sb.toString())
            .setIcon(icon)
            .show()

    }

}