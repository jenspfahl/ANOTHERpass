package de.jepfa.yapm.usecase.vault

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORTED_AT
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.addFormattedLine

object ShowVaultInfoUseCase: InputUseCase<ShowVaultInfoUseCase.Input, SecureActivity>() {

    data class Input(val credentialCount: Int, val labelCount: Int)

    override fun doExecute(input: Input, activity: SecureActivity): Boolean {

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.applicationInfo.loadIcon(activity.packageManager)

        val vaultId = SaltService.getVaultId(activity)
        val vaultVersion = PreferenceService.getAsString(
            PreferenceService.DATA_VAULT_VERSION,
            activity
        ) ?: Constants.UNKNOWN_VAULT_VERSION
        val vaultCreatedAt = PreferenceService.getAsString(
            PreferenceService.DATA_VAULT_CREATED_AT,
            activity
        )
        val vaultImportedAt = PreferenceService.getAsString(
            PreferenceService.DATA_VAULT_IMPORTED_AT,
            activity
        )
        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)

        val sb = StringBuilder()
        sb.addFormattedLine(activity.getString(R.string.vault_id), vaultId)
        sb.addFormattedLine(activity.getString(R.string.vault_version), vaultVersion)
        sb.addFormattedLine(activity.getString(R.string.cipher_name), activity.getString(cipherAlgorithm.uiLabel))
        sb.addFormattedLine(activity.getString(R.string.credential_count), input.credentialCount)
        sb.addFormattedLine(activity.getString(R.string.label_count), input.labelCount)
        if (vaultCreatedAt != null) {
            sb.addFormattedLine(activity.getString(R.string.vault_created_at), vaultCreatedAt)
        }
        if (vaultImportedAt != null) {
            sb.addFormattedLine(activity.getString(R.string.vault_imported_at), vaultImportedAt)
        }
        val vaultExportedAt = PreferenceService.getAsDate(DATA_VAULT_EXPORTED_AT, activity)
        vaultExportedAt?.let {
            sb.addFormattedLine(activity.getString(R.string.vault_exported_at), Constants.SDF_DT_MEDIUM.format(it))
        }

        builder.setTitle(R.string.vault_info)
            .setMessage(sb.toString())
            .setIcon(icon)
            .show()

        return true
    }

}